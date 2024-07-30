package niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import data.scripts.ai.missiles.yrxp_LRMissileAI
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.settings.MCTE_settings.MAGFIELD_MISSILE_UNSCRAMBLE_CHANCE
import niko.MCTE.settings.MCTE_settings.MAGSTORM_MISSILE_UNSCRAMBLE_CHANCE
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.terrainCombatEffectIds
import org.lwjgl.util.vector.Vector2f

class magneticFieldEffect(
    var isStorm: Boolean = false,
    var visionMod: Float = 1f,
    var missileMod: Float = 1f,
    var rangeMod: Float = 1f,
    var eccmChanceMod: Float = 1f,
    var missileBreakLockBaseChance: Float = 0f,
    ): baseTerrainEffectScript(), usesDeltaTime {

    override var effectPrototype: combatEffectTypes? = combatEffectTypes.MAGFIELD

    override var deltaTime = 0f

    private val scrambledMissiles: HashMap<MissileAPI, CombatEntityAPI> = HashMap()
    private val timesToTryScramblingMissilesPerSecond = 60f
    override val thresholdForAdvancement: Float = (1/timesToTryScramblingMissilesPerSecond)
    private val thresholdForRepositon: Float = 2f

    var deltaTimeForReposition = deltaTime

    val timer: IntervalUtil = IntervalUtil(0.15f, 0.15f)

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)

        //this.engine.addLayeredRenderingPlugin(magFieldRenderingPlugin(magneticFieldPlugins))
        // way beyond my skill level for now
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        deltaTimeForReposition += deltaTime
        if (engine.isPaused) return
        if (canAdvance(amount)) {
            scrambleMissiles(engine)
            handleCurrentlyScrambledMissiles(engine)
        }
    }

    override fun applyEffects(amount: Float) {

        timer.advance(amount)
        if (timer.intervalElapsed()) {
            for (ship: ShipAPI in engine.ships) {
                if (ship.hullSpec.hullId == "dem_drone") continue
                val mutableStats = ship.mutableStats
                val modifiedRangeMult = getRangeMultForShip(ship)
                mutableStats.ballisticWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)
                mutableStats.energyWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)
                mutableStats.missileWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)
                mutableStats.fighterWingRange.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)

                val visionMult = getVisionMultForShip(ship)
                mutableStats.sightRadiusMod.modifyMult(terrainCombatEffectIds.magneticField, visionMult)

                val modifiedMissileMult = getMissileAuxillaryMultForShip(ship)
                mutableStats.missileGuidance.modifyMult(terrainCombatEffectIds.magneticField, modifiedMissileMult)
                mutableStats.missileMaxTurnRateBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedMissileMult)

                val eccmChanceMult = getECCMChanceMultForShip(ship)
                mutableStats.eccmChance.modifyMult(terrainCombatEffectIds.magneticField, eccmChanceMult)
            }
        }
    }

    private fun getVisionMultForShip(ship: ShipAPI): Float {
        val eccmMult = getECMMultForShip(ship)
        val visionMult = visionMod*eccmMult

        val modifiedVisionMult = ((visionMult).coerceAtMost(1f))

        return modifiedVisionMult
    }

    private fun getECCMChanceMultForShip(ship: ShipAPI): Float {
        return eccmChanceMod
    }

    private fun getMissileAuxillaryMultForShip(ship: ShipAPI): Float {
        val ecmMult = getECMMultForShip(ship)
        return (missileMod*ecmMult).coerceAtMost(1f)
    }

    private fun getRangeMultForShip(ship: ShipAPI): Float {
        val ecmMult = getECMMultForShip(ship)
        return ((rangeMod)*ecmMult).coerceAtMost(1f)
    }

    private fun getECMMultForShip(ship: ShipAPI): Float {
        val mutableStats = ship.mutableStats
        return 1 + (1 - mutableStats.dynamic.getMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD).computeEffective(1f))
    }

    override fun handleNotification(amount: Float): Boolean {
        if (!super.handleNotification(amount)) return false
        val ship = engine.playerShip ?: return false
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
        val stormOrNot = if (isStorm) "storm" else "field"
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference1",
            icon,
            "Magnetic $stormOrNot",
            "${(100-(getVisionMultForShip(ship))*100).roundTo(2)}% less vision",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference2",
            icon,
            "Magnetic $stormOrNot",
            "${(100-(getMissileAuxillaryMultForShip(ship))*100).roundTo(2)}% less missile guidance/turn rate",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference4",
            icon,
            "Magnetic $stormOrNot",
            "${(100-(getRangeMultForShip(ship))*100).roundTo(2)}% less weapon range and fighter range",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference5",
            icon,
            "Magnetic $stormOrNot",
            "${(100-(getECCMChanceMultForShip(ship))*100).roundTo(2)}% less ECCM chance",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference6",
            icon,
            "Magnetic $stormOrNot",
            "${calculateScrambleChancePerSecond(ship) * 100}% chance for missiles' guidance to be scrambled per 1 second(s)",
            true)
        return true
    }

    private fun calculateScrambleChancePerSecond(ship: ShipAPI): Float {
        return (getMissileBreakLockChance(ship)*timesToTryScramblingMissilesPerSecond).roundTo(5)
    }

    override fun handleSounds(amount: Float) {
        if (isStorm) {
            Global.getSoundPlayer().playUILoop("terrain_magstorm", 1f, 1.4f)
        } else {
            Global.getSoundPlayer().playUILoop("terrain_magfield", 1f, 1f)
        }
    }

    private fun scrambleMissiles(engine: CombatEngineAPI) {
        for (missile: MissileAPI in engine.missiles) {
            val missileAI = missile.unwrappedMissileAI
            if (missileAI !is GuidedMissileAI) continue
            if (MCTE_debugUtils.YRXPenabled && missileAI is yrxp_LRMissileAI) continue // prevents a crash
            if (missileAI.target == null || scrambledMissiles[missile] != null) continue

            if (shouldScrambleMissile(missile, missileAI)) scrambleMissile(missile)
        }
    }

    private fun getMissileBreakLockChance(ship: ShipAPI): Float {
        val mutableStats = ship.mutableStats
        var missileBreakLockChance = missileBreakLockBaseChance
        var electronicWarfareMult: Float = mutableStats.dynamic.getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifiedValue
        missileBreakLockChance -= mutableStats.eccmChance.modifiedValue
        missileBreakLockChance *= electronicWarfareMult


        return missileBreakLockChance.coerceAtLeast(0f)
    }

    private fun getMissileBreakLockChance(missile: MissileAPI, missileAI: GuidedMissileAI): Float {
        val source: ShipAPI? = missile.source
        var electronicWarfareMult: Float = 1f
        if (source != null) {
            electronicWarfareMult = getECMMultForShip(source)
        }
        var missileBreakLockChance = missileBreakLockBaseChance
        missileBreakLockChance -= missile.eccmChance
        missileBreakLockChance *= electronicWarfareMult

        return missileBreakLockChance.coerceAtLeast(0f)
    }

    private fun shouldScrambleMissile(missile: MissileAPI, missileAI: GuidedMissileAI): Boolean {
        val missileBreakLockChance = getMissileBreakLockChance(missile, missileAI)
        val randomFloatVal = random.nextFloat()
        if (randomFloatVal <= missileBreakLockChance) return true
        return false
    }

    private fun scrambleMissile(missile: MissileAPI) {
        val missileAI = missile.unwrappedMissileAI
        if (missileAI is GuidedMissileAI) {
            missileAI.target = null
            val magFlare: CombatEntityAPI = createNewMagFlare()
            scrambledMissiles[missile] = magFlare
        }
    }

    private fun createNewMagFlare(): CombatEntityAPI {
        val engine = Global.getCombatEngine()
        val maxHeight = engine.mapHeight
        val maxWidth = engine.mapWidth
        val randomY = (-maxHeight + random.nextFloat() * (maxHeight - (-maxHeight)))
        val randomX = (-maxWidth + random.nextFloat() * (maxWidth - (-maxWidth)))
        val magFlare = engine.spawnProjectile(
            null,
            null,
            MCTE_ids.magFlareWeaponId,
            Vector2f(randomX, randomY),
            0f,
            null
        )
        magFlare.owner = 100
        return magFlare
    }

    private fun despawnMagFlare(magFlare: CombatEntityAPI) {
        magFlare.hitpoints = 0f
        engine.removeEntity(magFlare)
    }

    private fun handleCurrentlyScrambledMissiles(engine: CombatEngineAPI) {
        val iterator = scrambledMissiles.keys.iterator()
        while (iterator.hasNext()) {
            val scrambledMissile = iterator.next()
            val missileAI = scrambledMissile.unwrappedMissileAI
            val magFlare = scrambledMissiles[scrambledMissile]
            if (!engine.isEntityInPlay(scrambledMissile)) {
                if (magFlare != null) despawnMagFlare(magFlare)
                iterator.remove()
                continue
            } else {
                if (shouldUnscrambleMissile(scrambledMissile)) {
                    unscrambleMissile(scrambledMissile, iterator)
                    continue
                }
                if (missileAI is GuidedMissileAI) {
                    missileAI.target = magFlare
                    if (shouldReposition(scrambledMissile, missileAI)) {
                        repositionMagFlare(scrambledMissile, missileAI, magFlare)
                    }
                }
            }
        }
    }

    private fun shouldReposition(scrambledMissile: MissileAPI, missileAI: GuidedMissileAI): Boolean {
        if (deltaTimeForReposition >= thresholdForRepositon) {
            deltaTimeForReposition = 0f
            return true
        }
        return false
    }

    private fun shouldUnscrambleMissile(scrambledMissile: MissileAPI): Boolean {
        val threshold = getBaseUnscrambleChance()
        val randomFloat = random.nextFloat()
        if (randomFloat < threshold) return true
        return false
    }

    fun getBaseUnscrambleChance(): Float {
        if (isStorm) return MAGSTORM_MISSILE_UNSCRAMBLE_CHANCE
        return MAGFIELD_MISSILE_UNSCRAMBLE_CHANCE
    }

    private fun unscrambleMissile(scrambledMissile: MissileAPI, iterator: MutableIterator<MissileAPI>?) {
        val missileAI = scrambledMissile.unwrappedMissileAI
        if (missileAI is GuidedMissileAI) {
            missileAI.target = null
        }
        val magFlare = scrambledMissiles[scrambledMissile]
        if (iterator != null) {
            iterator.remove()
        } else {
            scrambledMissiles.remove(scrambledMissile)
        }
        if (magFlare != null) despawnMagFlare(magFlare)

    }

    private fun repositionMagFlare(scrambledMissile: MissileAPI, missileAI: GuidedMissileAI, magFlare: CombatEntityAPI?) {
        if (magFlare == null) return

        despawnMagFlare(magFlare)
        val newMagFlare = createNewMagFlare()
        missileAI.target = newMagFlare
    }
}

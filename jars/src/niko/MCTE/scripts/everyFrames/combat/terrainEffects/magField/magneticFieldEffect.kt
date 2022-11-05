package niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.AuroraRenderer
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.input.InputEventAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.renderableEffect
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.terrainCombatEffectIds
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

class magneticFieldEffect(
    val isStorm: Boolean,
    val visionMod: Float,
    val missileMod: Float,
    val rangeMod: Float,
    val eccmChanceMod: Float,
    var missileBreakLockBaseChance: Float,
    val magneticFieldPlugins: MutableSet<MagneticFieldTerrainPlugin>
    ): baseTerrainEffectScript(), usesDeltaTime {

    override var deltaTime = 0f

    protected val affectedShips: MutableMap<ShipAPI, Boolean> = HashMap()
    private val scrambledMissiles: HashMap<MissileAPI, CombatEntityAPI> = HashMap()
    override val thresholdForAdvancement: Float = 1f

    var deltaTimeForReposition = deltaTime

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)

        this.engine.addLayeredRenderingPlugin(magFieldRenderingPlugin(magneticFieldPlugins))
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        deltaTimeForReposition += deltaTime
        if (canAdvance(amount)) {
            scrambleMissiles(engine)
            handleCurrentlyScrambledMissiles(engine)
        }
    }

    override fun applyEffects(amount: Float) {
        for (ship: ShipAPI in engine.ships) {
            if (affectedShips[ship] == null) {
                val mutableStats = ship.mutableStats
                val ecmMult = mutableStats.dynamic.getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifiedValue
                val shipWeapons = ship.allWeapons
                var maxRange = 0f
                val modifiedRangeMult = ((rangeMod)*ecmMult).coerceAtMost(1f)
                mutableStats.ballisticWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)
                mutableStats.energyWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)
                mutableStats.missileWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)
                mutableStats.fighterWingRange.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)

                for (weapon: WeaponAPI in shipWeapons) {
                    val weaponRange = weapon.range
                    if (weaponRange > maxRange) maxRange = weaponRange
                }
                val rangeThresholdForMoreVision = 1000f
                val weaponRangeVisionMult = (maxRange/rangeThresholdForMoreVision).coerceAtLeast(1f)
                val modifiedVisionMult = ((visionMod*weaponRangeVisionMult)*ecmMult).coerceAtMost(1f)
                val modifiedMissileMult = (missileMod*ecmMult).coerceAtMost(1f)
                mutableStats.sightRadiusMod.modifyMult(terrainCombatEffectIds.magneticField, modifiedVisionMult)

                mutableStats.missileGuidance.modifyMult(terrainCombatEffectIds.magneticField, modifiedMissileMult)
                mutableStats.missileMaxTurnRateBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedMissileMult)


                mutableStats.eccmChance.modifyMult(terrainCombatEffectIds.magneticField, eccmChanceMod)

                affectedShips[ship] = true
            }
        }
    }

    override fun handleNotification(amount: Float) {
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
        val stormOrNot = if (isStorm) "storm" else "field"
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference1",
            icon,
            "Magnetic $stormOrNot",
            "${100-visionMod*100}% less vision",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference2",
            icon,
            "Magnetic $stormOrNot",
            "${100-missileMod*100}% less missile guidance/turn rate",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference4",
            icon,
            "Magnetic $stormOrNot",
            "${100-rangeMod*100}% less weapon range and fighter range",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference5",
            icon,
            "Magnetic $stormOrNot",
            "${100-eccmChanceMod*100}% less ECCM chance",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference6",
            icon,
            "Magnetic $stormOrNot",
            "$missileBreakLockBaseChance% chance for missiles' guidance to be scrambled per $thresholdForAdvancement seconds",
            true)
    }

    override fun handleSounds(amount: Float) {
        if (isStorm) {
            Global.getSoundPlayer().playUILoop("terrain_magstorm", 1f, 1f)
        } else {
            Global.getSoundPlayer().playUILoop("terrain_magfield", 1f, 0.8f)
        }
    }

    private fun scrambleMissiles(engine: CombatEngineAPI) {
        for (missile: MissileAPI in engine.missiles) {
            val missileAI = missile.unwrappedMissileAI
            if (missileAI !is GuidedMissileAI) continue
            if (missileAI.target == null || scrambledMissiles[missile] != null) continue

            if (shouldScrambleMissile(missile, missileAI)) scrambleMissile(missile)
        }
    }

    private fun getMissileBreakLockChance(missile: MissileAPI, missileAI: GuidedMissileAI): Float {
        var missileBreakLockChance = missileBreakLockBaseChance
        missileBreakLockChance -= missile.eccmChance

        missileBreakLockChance = missileBreakLockChance.coerceAtMost(1f)
        return missileBreakLockChance
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
        val thresholdForRepositon = 2f
        if (deltaTimeForReposition >= thresholdForRepositon) {
            deltaTimeForReposition = 0f
            return true
        }
        return false
    }

    private fun shouldUnscrambleMissile(scrambledMissile: MissileAPI): Boolean {
        val threshold = 0.95f
        val randomFloat = random.nextFloat()
        if (randomFloat >= threshold) return true
        return false
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

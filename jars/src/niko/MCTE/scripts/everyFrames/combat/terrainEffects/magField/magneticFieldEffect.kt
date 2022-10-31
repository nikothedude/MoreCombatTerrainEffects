package niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.combat.ai.missile.MissileAI
import com.fs.starfarer.combat.systems.Flare
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
    ): baseCombatDeltaTimeScript() {
    val random = MathUtils.getRandom()

    private val affectedShips: HashMap<ShipAPI, Boolean> = HashMap()
    private val scrambledMissiles: HashMap<MissileAPI, CombatEntityAPI> = HashMap()
    override val thresholdForAdvancement: Float = 1f

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (Global.getCurrentState() != GameState.COMBAT) return
        super.advance(amount, events)
        val engine = Global.getCombatEngine()

        handleSound()
        handleNotification(engine)

        for (ship: ShipAPI in engine.ships) {
            if (affectedShips[ship] == null) {
                val rangeAndVisionMultForShipSize = when (ship.hullSize) {
                    ShipAPI.HullSize.FIGHTER -> 1f
                    ShipAPI.HullSize.DEFAULT -> 1f
                    ShipAPI.HullSize.FRIGATE -> 1f
                    ShipAPI.HullSize.DESTROYER -> 1.1f
                    ShipAPI.HullSize.CRUISER -> 1.2f
                    ShipAPI.HullSize.CAPITAL_SHIP -> 1.3f
                    else -> 1f
                }
                val ecmMult = ship.mutableStats.dynamic.getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifiedValue
                val modifiedVisionMult = ((visionMod*rangeAndVisionMultForShipSize)*ecmMult).coerceAtMost(1f)
                val modifiedRangeMult = ((rangeMod)*ecmMult).coerceAtMost(1f)
                val modifiedMissileMult = (missileMod*ecmMult).coerceAtMost(1f)
                ship.mutableStats.sightRadiusMod.modifyMult(terrainCombatEffectIds.magneticField, modifiedVisionMult)

                ship.mutableStats.missileGuidance.modifyMult(terrainCombatEffectIds.magneticField, modifiedMissileMult)
                ship.mutableStats.missileMaxTurnRateBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedMissileMult)

                ship.mutableStats.ballisticWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)
                ship.mutableStats.energyWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)
                ship.mutableStats.missileWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, modifiedRangeMult)

                ship.mutableStats.eccmChance.modifyMult(terrainCombatEffectIds.magneticField, eccmChanceMod)

                affectedShips[ship] = true
            }
        }

        if (canAdvance(amount)) {
            scrambleMissiles(engine)
            handleCurrentlyScrambledMissiles(engine)
        }
    }

    private fun handleNotification(engine: CombatEngineAPI) {
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
            "${100-rangeMod*100}% less weapon range",
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
            "$missileBreakLockBaseChance% chance for missiles to lose lock",
            true)
    }

    private fun handleSound() {
        val volume = 1f
        if (isStorm) {
            Global.getSoundPlayer().playUILoop("terrain_magstorm", 1f, volume)
        } else {
            Global.getSoundPlayer().playUILoop("terrain_magfield", 1f, volume)
        }
    }

    private fun scrambleMissiles(engine: CombatEngineAPI) {
        for (missile: MissileAPI in engine.missiles) {
            val missileAI = missile.unwrappedMissileAI
            if (missileAI !is GuidedMissileAI) continue
            if (missileAI.target == null) continue
            var missileBreakLockChance = missileBreakLockBaseChance
            missileBreakLockChance -= missile.eccmChance

            missileBreakLockChance = missileBreakLockChance.coerceAtMost(1f)
            val randomFloatVal = random.nextFloat()
            if (randomFloatVal <= missileBreakLockChance) {
                scrambleMissile(missile)
            }
        }
    }

    private fun scrambleMissile(missile: MissileAPI) {
        val missileAI = missile.unwrappedMissileAI
        if (missileAI is GuidedMissileAI) {
            missileAI.target = null
            if (missileAI is MissileAI) missileAI.isRetargetNearest = false
            missile.eccmChanceOverride = 0f
            val magFlare: CombatEntityAPI = createNewMagFlare()
            scrambledMissiles[missile] = magFlare
        }
    }

    private fun createNewMagFlare(): CombatEntityAPI {
        val engine = Global.getCombatEngine()
        val magFlare = engine.spawnAsteroid(0, 9999f, 9999f, 0f, 0f)
        magFlare.collisionClass = CollisionClass.NONE
        return magFlare
    }

    private fun handleCurrentlyScrambledMissiles(engine: CombatEngineAPI) {
        val iterator = scrambledMissiles.keys.iterator()
        while (iterator.hasNext()) {
            val scrambledMissile = iterator.next()
            val missileAI = scrambledMissile.unwrappedMissileAI
            if (!engine.isEntityInPlay(scrambledMissile)) {
                val magFlare = scrambledMissiles[scrambledMissile]
                magFlare?.hitpoints = 0f
                engine.removeObject(magFlare)
                iterator.remove()
                continue
            }
            // do math
            if (missileAI is GuidedMissileAI) missileAI.target = scrambledMissiles[scrambledMissile]
        }
    }
}
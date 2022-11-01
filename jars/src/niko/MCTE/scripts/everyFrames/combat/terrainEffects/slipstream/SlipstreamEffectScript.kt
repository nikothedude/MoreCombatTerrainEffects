package niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipEngineControllerAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.MCPE_settings
import niko.MCTE.utils.terrainCombatEffectIds
import java.awt.Color
import java.util.*
import kotlin.collections.HashSet

class SlipstreamEffectScript(
    val peakPerformanceMult: Float,
    val fluxDissipationMult: Float,
    val hardFluxGenerationPerFrame: Float,
    val overallSpeedMult: Float):
    baseTerrainEffectScript() {

    val speed: MutableMap<ShipAPI.HullSize, Float> = EnumMap(ShipAPI.HullSize::class.java)
    private val color = Color(255, 100, 211, 255)

    init {
        speed[ShipAPI.HullSize.FIGHTER] = 60f
        speed[ShipAPI.HullSize.DEFAULT] = 50f
        speed[ShipAPI.HullSize.FRIGATE] = 50f
        speed[ShipAPI.HullSize.DESTROYER] = 30f
        speed[ShipAPI.HullSize.CRUISER] = 20f
        speed[ShipAPI.HullSize.CAPITAL_SHIP] = 10f
    }

    val missileSpeed = 60f
    private val affectedMissiles: HashMap<MissileAPI, Boolean> = HashMap()

    override fun applyEffectsToShips() {
        for (ship: ShipAPI in engine.ships) {
            if (affectedShips[ship] == null) {
                val mutableStats = ship.mutableStats
                val speedForSize = speed[ship.hullSize]
                if (speedForSize != null) {
                    val adjustedSpeedMult = (speedForSize * overallSpeedMult)
                    val adjustedMissileSpeedMult = (missileSpeed * overallSpeedMult)
                    mutableStats.maxSpeed.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                    mutableStats.acceleration.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                    mutableStats.deceleration.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                    mutableStats.missileMaxSpeedBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                    mutableStats.missileAccelerationBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                    mutableStats.missileTurnAccelerationBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                }
                val hasSafetyOverrides = ship.variant.hasHullMod("safety_overrides")
                if (!hasSafetyOverrides || MCPE_settings.STACK_SLIPSTREAM_PPT_DEBUFF_WITH_SO) {
                    mutableStats.peakCRDuration.modifyMult(terrainCombatEffectIds.slipstreamEffect, peakPerformanceMult)
                }

                mutableStats.zeroFluxMinimumFluxLevel.modifyFlat(terrainCombatEffectIds.slipstreamEffect, 2f)
                mutableStats.fluxDissipation.modifyMult(terrainCombatEffectIds.slipstreamEffect, fluxDissipationMult)

                affectedShips[ship] = true
            }
        }
        for (missile: MissileAPI in engine.missiles) {
            if (affectedMissiles[missile] == null) {
                affectedMissiles[missile] = true
            }
        }

        generateFlux()

        handleEngines()
    }

    private fun generateFlux() {
        if (engine.isPaused) return
        for (ship: ShipAPI in affectedShips.keys) {
            if (ship.isFighter) continue
            ship.fluxTracker.increaseFlux(hardFluxGenerationPerFrame, true)
        }
    }

    private fun handleEngines() {
        if (engine.isPaused) return
        val engineControllers: MutableSet<ShipEngineControllerAPI> = HashSet()
        val shipIterator = affectedShips.keys.iterator()
        while (shipIterator.hasNext()) {
            val ship: ShipAPI = shipIterator.next()
            if (!engine.isEntityInPlay(ship)) {
                shipIterator.remove()
                continue
            }
            val engineController = ship.engineController
            engineControllers += engineController
        }
        val missileIterator = affectedMissiles.keys.iterator()
        while (missileIterator.hasNext()) {
            val missile: MissileAPI = missileIterator.next()
            if (!engine.isEntityInPlay(missile)) {
                missileIterator.remove()
                continue
            }
            val engineController = missile.engineController
            engineControllers += engineController
        }

        for (engineController in engineControllers) {
            engineController.fadeToOtherColor(this, color, null, 1f, 0.4f)
            val flameExtension = 0.25f * overallSpeedMult
            engineController.extendFlame(this, flameExtension, flameExtension, flameExtension)
        }
    }

    override fun handleNotification() {
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_slipstream3",
            icon,
            "Slipstream",
            "Generating hardflux at rate of ${calculateFluxGeneratedPerSecond()} per second",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_slipstream2",
            icon,
            "Slipstream",
            "Safety overrides applied to all ships, fighters, and missiles",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_slipstream1",
            icon,
            "Slipstream",
            "Systems overcharge",
            true)
    }

    override fun handleSounds() {
        Global.getSoundPlayer().playUILoop("terrain_slipstream", 1f, 1f)
    }

    private fun calculateFluxGeneratedPerSecond(): Float {
        return (hardFluxGenerationPerFrame * 60)
    }

}
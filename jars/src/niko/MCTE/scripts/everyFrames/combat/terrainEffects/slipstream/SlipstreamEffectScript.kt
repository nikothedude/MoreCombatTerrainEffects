package niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipEngineControllerAPI
import com.sun.org.apache.xpath.internal.operations.Bool
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.MCPE_settings
import niko.MCTE.utils.MCPE_settings.SLIPSTREAM_DISABLE_VENTING
import niko.MCTE.utils.MCPE_settings.SLIPSTREAM_INCREASE_TURN_RATE
import niko.MCTE.utils.terrainCombatEffectIds
import java.awt.Color
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class SlipstreamEffectScript(
    val peakPerformanceMult: Float,
    val fluxDissipationMult: Float,
    val hardFluxGenerationPerFrame: Float,
    val overallSpeedMult: Float):
    baseTerrainEffectScript() {

    val speed: MutableMap<ShipAPI.HullSize, Float> = EnumMap(ShipAPI.HullSize::class.java)
    private val color = Color(212, 55, 255, 255)

    protected val affectedShips: MutableMap<ShipAPI, Boolean> = HashMap()

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

    override fun applyEffects(amount: Float) {
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
                    if (SLIPSTREAM_INCREASE_TURN_RATE) {
                        mutableStats.turnAcceleration.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                        mutableStats.maxTurnRate.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                        mutableStats.missileTurnAccelerationBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                        mutableStats.missileMaxTurnRateBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                    }
                    mutableStats.missileMaxSpeedBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                    mutableStats.missileAccelerationBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                }
                val hasSafetyOverrides = ship.variant.hasHullMod("safetyoverrides")
                if (!hasSafetyOverrides || MCPE_settings.STACK_SLIPSTREAM_PPT_DEBUFF_WITH_SO) {
                    mutableStats.peakCRDuration.modifyMult(terrainCombatEffectIds.slipstreamEffect, peakPerformanceMult)
                }

                mutableStats.zeroFluxMinimumFluxLevel.modifyFlat(terrainCombatEffectIds.slipstreamEffect, 2f)
                mutableStats.fluxDissipation.modifyMult(terrainCombatEffectIds.slipstreamEffect, fluxDissipationMult)

                if (SLIPSTREAM_DISABLE_VENTING) {
                    mutableStats.ventRateMult.modifyMult(terrainCombatEffectIds.slipstreamEffect, 0f)
                } else {
                    mutableStats.ventRateMult.modifyMult(terrainCombatEffectIds.slipstreamEffect, 0.62f)
                }

                affectedShips[ship] = true
            }
        }
        for (missile: MissileAPI in engine.missiles) {
            if (affectedMissiles[missile] == null) {
                if (missile.isFlare) continue
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

    override fun handleNotification(amount: Float) {
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
            "niko_MCPE_slipstream4",
            icon,
            "Slipstream",
            "Venting effectiveness reduced",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_slipstream1",
            icon,
            "Slipstream",
            "Systems overcharge",
            true)
    }

    override fun handleSounds(amount: Float) {
        Global.getSoundPlayer().playUILoop("terrain_slipstream", 1f, 0.8f)
    }

    private fun calculateFluxGeneratedPerSecond(): Float {
        return (hardFluxGenerationPerFrame * 60)
    }

}

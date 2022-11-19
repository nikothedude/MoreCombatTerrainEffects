package niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipEngineControllerAPI
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.utils.MCTE_settings
import niko.MCTE.utils.MCTE_settings.SLIPSTREAM_DISABLE_VENTING
import niko.MCTE.utils.MCTE_settings.SLIPSTREAM_INCREASE_TURN_RATE
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
    baseTerrainEffectScript(), usesDeltaTime {

    val speed: MutableMap<ShipAPI.HullSize, Float> = EnumMap(ShipAPI.HullSize::class.java)
    private val color = Color(212, 55, 255, 255)
    private val timesToGenerateFluxPerSecond = 60f

    override var deltaTime: Float = 0f
    override val thresholdForAdvancement: Float = (1/timesToGenerateFluxPerSecond)

    init {
        speed[ShipAPI.HullSize.FIGHTER] = 60f
        speed[ShipAPI.HullSize.DEFAULT] = 50f
        speed[ShipAPI.HullSize.FRIGATE] = 50f
        speed[ShipAPI.HullSize.DESTROYER] = 30f
        speed[ShipAPI.HullSize.CRUISER] = 20f
        speed[ShipAPI.HullSize.CAPITAL_SHIP] = 10f
    }
    private val missileSpeed = 60f
    private val missileZeroFluxApproximation = 60f

    protected val affectedShips: MutableMap<ShipAPI, Boolean> = HashMap()
    private val affectedMissiles: HashMap<MissileAPI, Boolean> = HashMap()

    override fun applyEffects(amount: Float) {
        for (ship: ShipAPI in engine.ships) {
            if (affectedShips[ship] == null) {
                val mutableStats = ship.mutableStats
                val speedForSize = speed[ship.variant.hullSize]
                if (speedForSize != null) {
                    var adjustedSpeedMult = (speedForSize * overallSpeedMult)
                    if (ship.isFighter) {
                        if (MCTE_settings.SLIPSTREAM_FIGHTER_ZERO_FLUX_BOOST) {
                            adjustedSpeedMult += ship.mutableStats.zeroFluxSpeedBoost.modifiedValue
                        }
                    }
                    val adjustedMissileSpeedMult = ((missileSpeed) * overallSpeedMult)
                    var adjustedMissileSpeedMultWithZeroFluxBoost = (adjustedMissileSpeedMult)
                    if (MCTE_settings.SLIPSTREAM_MISSILE_ZERO_FLUX_BOOST) {
                        adjustedMissileSpeedMultWithZeroFluxBoost = ((adjustedMissileSpeedMult + missileZeroFluxApproximation))
                    }
                    mutableStats.maxSpeed.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                    mutableStats.acceleration.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                    mutableStats.deceleration.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                    if (SLIPSTREAM_INCREASE_TURN_RATE) {
                        mutableStats.turnAcceleration.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                        mutableStats.maxTurnRate.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedSpeedMult)
                        mutableStats.missileTurnAccelerationBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                        mutableStats.missileMaxTurnRateBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                    }
                    mutableStats.missileMaxSpeedBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMultWithZeroFluxBoost)
                    mutableStats.missileAccelerationBonus.modifyFlat(terrainCombatEffectIds.slipstreamEffect, adjustedMissileSpeedMult)
                }
                val safetiesOverridden = (mutableStats.zeroFluxMinimumFluxLevel.modifiedValue >= 1)
                if (safetiesOverridden) {
                    if (MCTE_settings.STACK_SLIPSTREAM_PPT_DEBUFF_WITH_SO) {
                        mutableStats.peakCRDuration.modifyMult(terrainCombatEffectIds.slipstreamEffect, peakPerformanceMult)
                    }
                    mutableStats.zeroFluxSpeedBoost.modifyFlat(terrainCombatEffectIds.slipstreamEffect, mutableStats.zeroFluxSpeedBoost.modifiedValue)
                }

                mutableStats.zeroFluxMinimumFluxLevel.modifyFlat(terrainCombatEffectIds.slipstreamEffect, 2f)
                //mutableStats.allowZeroFluxAtAnyLevel.modifyFlat(terrainCombatEffectIds.slipstreamEffect, 1f)
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

        generateFlux(amount)

        handleEngines()
    }

    private fun generateFlux(amount: Float) {
        if (engine.isPaused) return
        if (!canAdvance(amount)) return
        for (ship: ShipAPI in affectedShips.keys) {
            if (ship.isFighter) continue
            val timeMult: Float = ship.mutableStats.timeMult.modifiedValue
            val engineMult: Float = engine.timeMult.modifiedValue
            val totalMult = timeMult + engineMult-1
            ship.fluxTracker.increaseFlux(((hardFluxGenerationPerFrame)*totalMult), true)
        }
    }

    private fun handleEngines() {
        if (engine.isPaused) return
        val engineControllers: MutableSet<ShipEngineControllerAPI> = HashSet()
        val shipIterator = affectedShips.keys.iterator()
        while (shipIterator.hasNext()) {
            val ship: ShipAPI = shipIterator.next()
            if (!engine.isEntityInPlay(ship) || !ship.isAlive) {
                shipIterator.remove()
                continue
            }
            val engineController = ship.engineController ?: continue
            engineControllers += engineController
        }
        val missileIterator = affectedMissiles.keys.iterator()
        while (missileIterator.hasNext()) {
            val missile: MissileAPI = missileIterator.next()
            if (!engine.isEntityInPlay(missile)) {
                missileIterator.remove()
                continue
            }
            val engineController = missile.engineController ?: continue
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
        return (hardFluxGenerationPerFrame*timesToGenerateFluxPerSecond)
    }

}

package niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipEngineControllerAPI
import com.fs.starfarer.api.util.IntervalUtil
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.SLIPSTREAM_DISABLE_VENTING
import niko.MCTE.settings.MCTE_settings.SLIPSTREAM_INCREASE_TURN_RATE
import niko.MCTE.settings.MCTE_settings.SLIPSTREAM_REDUCE_WEAPON_RANGE
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.utils.MCTE_shipUtils.isTangible
import niko.MCTE.utils.terrainCombatEffectIds.slipstreamEffect
import java.awt.Color
import java.util.*

class SlipstreamEffectScript(
    var peakPerformanceMult: Float = 1f,
    var fluxDissipationMult: Float = 1f,
    var hardFluxGenerationPerFrame: Float = 0f,
    var overallSpeedMult: Float = 1f
): baseTerrainEffectScript(), usesDeltaTime {
    override var effectPrototype: combatEffectTypes? = combatEffectTypes.SLIPSTREAM

    private val weaponRangeThresholdForSOThreshold: Float = 0f
    private val RANGE_THRESHOLD: Float = 450f
    private val RANGE_MULT = 0.25f
    val speed: MutableMap<ShipAPI.HullSize, Float> = EnumMap(ShipAPI.HullSize::class.java)
    private val color = Color(212, 55, 255, 255)
    private val timesToGenerateFluxPerSecond = 60f

    override var deltaTime: Float = 0f
    override val thresholdForAdvancement: Float = (1/timesToGenerateFluxPerSecond)

    val timer: IntervalUtil = IntervalUtil(0.15f, 0.15f)

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

    private val affectedMissiles: HashMap<MissileAPI, Boolean> = HashMap()

    override fun applyEffects(amount: Float) {
        timer.advance(amount)
        if (timer.intervalElapsed()) {
            for (ship: ShipAPI in engine.ships) {
                val isIntangible = isShipIntangibleAndDoWeCare(ship)
                val mutableStats = ship.mutableStats
                var adjustedSpeedMult = getSpeedMult(ship)
                if (ship.isFighter && !isIntangible) {
                    if (MCTE_settings.SLIPSTREAM_FIGHTER_ZERO_FLUX_BOOST) {
                        adjustedSpeedMult += ship.mutableStats.zeroFluxSpeedBoost.modifiedValue
                    }
                }
                val adjustedMissileSpeedMult = getMissileSpeedMult(ship)
                var adjustedMissileSpeedMultWithZeroFluxBoost = (adjustedMissileSpeedMult)
                if (MCTE_settings.SLIPSTREAM_MISSILE_ZERO_FLUX_BOOST) {
                    adjustedMissileSpeedMultWithZeroFluxBoost = ((adjustedMissileSpeedMult + missileZeroFluxApproximation))
                }
                mutableStats.maxSpeed.modifyFlat(slipstreamEffect, adjustedSpeedMult)
                mutableStats.acceleration.modifyFlat(slipstreamEffect, adjustedSpeedMult)
                mutableStats.deceleration.modifyFlat(slipstreamEffect, adjustedSpeedMult)
                mutableStats.missileMaxSpeedBonus.modifyFlat(slipstreamEffect, adjustedMissileSpeedMultWithZeroFluxBoost)
                mutableStats.missileAccelerationBonus.modifyFlat(slipstreamEffect, adjustedMissileSpeedMult)
                val zeroFluxMinimumIncrement = if (!isIntangible) 2f else 0f
                val safetiesOverridden = (mutableStats.zeroFluxMinimumFluxLevel.modifiedValue >= zeroFluxMinimumIncrement+1)
                if (!safetiesOverridden || MCTE_settings.STACK_SLIPSTREAM_PPT_DEBUFF_WITH_SO) {
                    val PPTmult = getPPTMult(ship)
                    mutableStats.peakCRDuration.modifyMult(slipstreamEffect, PPTmult)
                }
                if (safetiesOverridden) {
                    val zeroFluxBoostIncrement = if (isIntangible) 0.0000001f else adjustedSpeedMult/overallSpeedMult
                    mutableStats.zeroFluxSpeedBoost.modifyFlat(slipstreamEffect, zeroFluxBoostIncrement)
                }

                if (SLIPSTREAM_INCREASE_TURN_RATE) {
                    val isFighter = ship.isFighter
                    if (safetiesOverridden || isFighter) {
                        val turnIncrement = if (isIntangible) 0f else getTurnRateIncrement(ship)
                        if (!isFighter) {
                            mutableStats.turnAcceleration.modifyFlat(slipstreamEffect, turnIncrement)
                        }
                        mutableStats.maxTurnRate.modifyFlat(slipstreamEffect, turnIncrement)
                    }
                    val missileTurnIncrement = if (isIntangible) 0f else getMissileTurnRateIncrement(ship)
                    // logically speaking, a missile with zero-flux boost would turn a lot faster
                    //mutableStats.missileTurnAccelerationBonus.modifyFlat(slipstreamEffect, missileTurnMult)
                    mutableStats.missileMaxTurnRateBonus.modifyFlat(slipstreamEffect, missileTurnIncrement)
                }

                mutableStats.zeroFluxMinimumFluxLevel.modifyFlat(slipstreamEffect, zeroFluxMinimumIncrement)
                //mutableStats.allowZeroFluxAtAnyLevel.modifyFlat(terrainCombatEffectIds.slipstreamEffect, 1f)
                val fluxDissipationModifiedMult = getFluxDissipationModifiedMult(ship)
                mutableStats.fluxDissipation.modifyMult(slipstreamEffect, fluxDissipationModifiedMult)

                if (SLIPSTREAM_DISABLE_VENTING) {
                    val disableMult = if (!isIntangible) 0f else 1f
                    mutableStats.ventRateMult.modifyMult(slipstreamEffect, disableMult)
                } else {
                    val slowMult = if (!isIntangible) 0.62f else 1f
                    mutableStats.ventRateMult.modifyMult(slipstreamEffect, slowMult)
                }

                if (SLIPSTREAM_REDUCE_WEAPON_RANGE) {
                    val appliedBefore = mutableStats.weaponRangeThreshold.getMultStatMod(slipstreamEffect) != null
                    val baseWeaponRangeThreshold = mutableStats.weaponRangeThreshold.modifiedValue
                    val shouldTryToApply = (!(!appliedBefore && baseWeaponRangeThreshold > weaponRangeThresholdForSOThreshold))
                    // havent applied before but we have a threshold? uh oh
                    if (shouldTryToApply) {
                        if (!isIntangible) {
                            mutableStats.weaponRangeThreshold.modifyFlat(slipstreamEffect, RANGE_THRESHOLD)
                            mutableStats.weaponRangeMultPastThreshold.modifyMult(slipstreamEffect, RANGE_MULT)
                        }
                    }
                    if (isIntangible) {
                        mutableStats.weaponRangeThreshold.unmodifyFlat(slipstreamEffect)
                        mutableStats.weaponRangeMultPastThreshold.unmodifyMult(slipstreamEffect)
                    }
                }
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

    private fun getMissileTurnRateIncrement(ship: ShipAPI): Float {
        return 10f
    }

    private fun getTurnRateIncrement(ship: ShipAPI): Float {
        return 10f
    }

    private fun isShipIntangibleAndDoWeCare(ship: ShipAPI): Boolean {
        return (MCTE_settings.SLIPSTREAM_AFFECT_INTANGIBLE && !ship.isTangible())
    }

    private fun getFluxDissipationModifiedMult(ship: ShipAPI): Float {
        if (isShipIntangibleAndDoWeCare(ship)) return 1f
        return fluxDissipationMult
    }

    private fun getPPTMult(ship: ShipAPI): Float {
        if (isShipIntangibleAndDoWeCare(ship)) return 1f
        return peakPerformanceMult
    }

    private fun getMissileSpeedMult(ship: ShipAPI): Float {
        return ((missileSpeed) * overallSpeedMult)
    }

    private fun getSpeedMult(ship: ShipAPI): Float {
        if (isShipIntangibleAndDoWeCare(ship)) return 1f
        val speedForSize: Float? = speed[ship.hullSize]
        var mult = (speedForSize?.times(overallSpeedMult)) ?: 1f
        return mult
    }

    private fun generateFlux(amount: Float) {
        if (engine.isPaused) return
        if (!canAdvance(amount)) return
        for (ship: ShipAPI in engine.ships) {
            if (ship.isFighter) continue
            if (!ship.isTangible()) continue
            val maxFlux = ship.maxFlux
            if (maxFlux <= getHardFluxGenForShip(ship)) continue
            val engineMult: Float = engine.timeMult.modifiedValue
            val totalMult = engineMult
            ship.fluxTracker.increaseFlux(((getHardFluxGenForShip(ship))*totalMult), true)
        }
    }

    private fun handleEngines() {
        if (engine.isPaused) return
        val engineControllers: MutableSet<ShipEngineControllerAPI> = HashSet()
        for (ship in engine.ships) {
            if (isShipIntangibleAndDoWeCare(ship)) continue
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

    override fun handleNotification(amount: Float): Boolean {
        if (!super.handleNotification(amount)) return false
        val playerShip = engine.playerShip ?: return false
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
        if (!isShipIntangibleAndDoWeCare(playerShip)) {
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_slipstream3",
                icon,
                "Slipstream",
                "Generating hardflux at rate of ${calculateFluxGeneratedPerSecond(playerShip).roundTo(2)} per second",
                true)
        }
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_slipstream2",
            icon,
            "Slipstream",
            "Safety overrides applied to all tangible ships, fighters, and missiles",
            true)
        if (!isShipIntangibleAndDoWeCare(playerShip)) {
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
        return true
    }

    override fun handleSounds(amount: Float) {
        Global.getSoundPlayer().playUILoop("terrain_slipstream", 1f, 0.8f)
    }

    private fun calculateFluxGeneratedPerSecond(ship: ShipAPI): Float {
        return (getHardFluxGenForShip(ship)*timesToGenerateFluxPerSecond)
    }

    private fun getHardFluxGenForShip(ship: ShipAPI): Float {
        if (!ship.isTangible()) return 0f
        return hardFluxGenerationPerFrame
    }

}

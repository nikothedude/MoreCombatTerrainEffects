package niko.MCTE.scripts.everyFrames.combat.terrainEffects.blackHole

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin
import com.fs.starfarer.api.util.IntervalUtil
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.utils.MCTE_miscUtils.applyForceWithSuppliedMass
import niko.MCTE.utils.MCTE_miscUtils.getAllObjects
import niko.MCTE.settings.MCTE_settings.BLACKHOLE_BASE_GRAVITY
import niko.MCTE.settings.MCTE_settings.BLACKHOLE_GRAVITY_ENABLED
import niko.MCTE.settings.MCTE_settings.BLACKHOLE_PPT_COMPENSATION
import niko.MCTE.settings.MCTE_settings.SOLAR_SHIELDING_EFFECT_MULT
import niko.MCTE.utils.MCTE_miscUtils
import niko.MCTE.utils.MCTE_miscUtils.replaceExistingEffect
import niko.MCTE.utils.terrainCombatEffectIds
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

class blackHoleEffectScript(
    val anglesToIntensity: MutableMap<Float, Float>,
    val timeMult: Float,
    val playerCoordinates: Vector2f
    //val gravityPointsToIntensity: MutableMap<Vector2f, Float> = HashMap()
): baseTerrainEffectScript(), usesDeltaTime {
    private val timesToApplyForcePerSecond = 60f

    override var deltaTime: Float = 0f
    override val thresholdForAdvancement: Float = (1/timesToApplyForcePerSecond)

    val timer: IntervalUtil = IntervalUtil(0.15f, 0.15f)


    private val originalValues: MutableMap<ShipAPI, MutableMap<StatBonus, MutableMap<String, MCTE_miscUtils.originalTerrainValue>>> = HashMap(HashMap(HashMap()))

    override fun applyEffects(amount: Float) {
        timer.advance(amount)
        if (timer.intervalElapsed()) {
            applyStats()
        }
        applyGravity(amount)
    }

    private fun applyGravity(amount: Float) {
        if (engine.isPaused) return
        if (!BLACKHOLE_GRAVITY_ENABLED) return
        if (!canAdvance(amount)) return
        for (entry in anglesToIntensity.entries) {
            val angle = entry.key
            val intensity = entry.value

            for (entity: CombatEntityAPI in engine.getAllObjects()) {
                if (!engine.isInPlay(entity)) continue
                var mass = entity.mass
                if (mass == 0f) {
                    if (entity is DamagingProjectileAPI) {
                        mass = (entity.damageAmount)/100
                    }
                }
                val pushForce = getGravityForceForEntity(entity, intensity)
                applyForceWithSuppliedMass(entity, mass, MathUtils.getPointOnCircumference(Vector2f(0f, 0f), 1f, angle), pushForce)
                //entity.applyForce(angle, pushForce)
            }
        }
    }

    private fun getGravityForceForEntity(entity: CombatEntityAPI, baseIntensity: Float): Float {
        var timeMult = 1f
        val engineMult: Float = engine.timeMult.modifiedValue
        if (entity is ShipAPI) {
            timeMult = entity.mutableStats.timeMult.modifiedValue
        }
        val adjustedIntensity = baseIntensity * BLACKHOLE_BASE_GRAVITY
        val totalTimeMult = engineMult
        val mult = if (entity is DamagingProjectileAPI) 0.3f else 1f
        return (((adjustedIntensity))*totalTimeMult)*mult
    }

    private fun applyStats() {
        for (ship: ShipAPI in engine.ships) {

            val modifiedTimeMult = getTimeMultForShip(ship)
            val mutableStats = ship.mutableStats
            mutableStats.timeMult.modifyMult(terrainCombatEffectIds.blackHoleEffect, modifiedTimeMult)

            replaceExistingEffect(originalValues, getPPTCompensation(ship), "event_horizon_stat_mod_1", "event_horizon_stat_mod_2", ship, mutableStats)

        }
        val playerShip: ShipAPI? = engine.playerShip
        if (playerShip != null) {
            val playerMult = getTimeMultForShip(playerShip)
            engine.timeMult.modifyMult(terrainCombatEffectIds.blackHoleEffect, 1/playerMult)
        } else {
            engine.timeMult.unmodifyMult(terrainCombatEffectIds.blackHoleEffect)
        }
    }

    private fun getPPTCompensation(ship: ShipAPI): Float {
        //val coronaEffect = (ship.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifiedValue).coerceAtLeast(1f)
        return BLACKHOLE_PPT_COMPENSATION///coronaEffect)
    }

    private fun getTimeMultForShip(ship: ShipAPI): Float {
        val baseEffect = (ship.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).baseValue)
        val currentEffect = (ship.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifiedValue)
        val solarShieldingEffect = (baseEffect - currentEffect) * SOLAR_SHIELDING_EFFECT_MULT
        val adjustedEffect = (baseEffect - solarShieldingEffect)

        return (timeMult*adjustedEffect).coerceAtLeast(1f)
    }

    override fun handleNotification(amount: Float): Boolean {
        if (!super.handleNotification(amount)) return false
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_blackHole2",
            icon,
            "Event Horizon",
            "Time dilation multiplied by ${getTimeMultForShip(engine.playerShip).roundTo(2)}x",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_blackHole1",
            icon,
            "Event Horizon",
            "Relativity Disrupted due to strong gravity",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_blackHole3",
            icon,
            "Event Horizon",
            "Extreme gravitational field interfering with battlespace",
            true
        )

        return true
    }

    override fun handleSounds(amount: Float) {
        Global.getSoundPlayer().playUILoop("terrain_corona_am", 1f, 1f)
    }
}
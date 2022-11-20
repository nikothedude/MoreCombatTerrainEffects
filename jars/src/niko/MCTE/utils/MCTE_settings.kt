package niko.MCTE.utils

import com.fs.starfarer.api.Global
import org.json.JSONException
import org.lazywizard.lazylib.ext.json.getFloat
import java.io.IOException
import kotlin.jvm.Throws

object MCTE_settings {
    var SHOW_ERRORS_IN_GAME: Boolean = true
    var MAG_FIELD_EFFECT_ENABLED: Boolean = true
    var DEEP_HYPERSPACE_EFFECT_ENABLED: Boolean = true
    var HYPERSTORM_EFFECT_ENABLED: Boolean = true
    var SLIPSTREAM_EFFECT_ENABLED: Boolean = true
    var DEBRIS_FIELD_EFFECT_ENABLED: Boolean = true
    var DUST_CLOUD_EFFECT_ENABLED: Boolean = true
    var EXTRA_NEBULA_EFFECTS_ENABLED: Boolean = true
    var BLACK_HOLE_EFFECT_ENABLED: Boolean = true
    var PULSAR_EFFECT_ENABLED: Boolean = true

    //MAGFIELD SETTINGS
    var MAGFIELD_VISION_MULT: Float = 0.6f
    var MAGFIELD_MISSILE_MULT: Float = 0.8f

    var MAGFIELD_RANGE_MULT: Float = 0.8f
    var MAGFIELD_ECCM_MULT: Float = 0.8f
    var MAGFIELD_MISSILE_SCRAMBLE_CHANCE: Float = 0.6f
    //MAGSTORM SETTINGS
    var MAGSTORM_VISION_MULT: Float = 0.6f
    var MAGSTORM_MISSILE_MULT: Float = 0.8f
    var MAGSTORM_RANGE_MULT: Float = 0.8f
    var MAGSTORM_ECCM_MULT: Float = 0.8f
    var MAGSTORM_MISSILE_SCRAMBLE_CHANCE: Float = 0.6f
    //SLIPSTREAM SETTINGS
    var SLIPSTREAM_PPT_MULT: Float = 0.33f
    var SLIPSTREAM_FLUX_DISSIPATION_MULT: Float = 3f

    var SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT: Float = 3f
    var SLIPSTREAM_HARDFLUX_GEN_PER_FRAME: Float = 2f
    var STACK_SLIPSTREAM_PPT_DEBUFF_WITH_SO: Boolean = true
    var SLIPSTREAM_DISABLE_VENTING: Boolean = false
    var SLIPSTREAM_INCREASE_TURN_RATE: Boolean = false
    var SLIPSTREAM_FIGHTER_ZERO_FLUX_BOOST: Boolean = false
    var SLIPSTREAM_MISSILE_ZERO_FLUX_BOOST: Boolean = false
    //NEBULA SETTINGS
    var NEBULA_VISION_MULT: Float = 0.8f

    var NEBULA_RANGE_MULT: Float = 0.8f
    var NEBULA_SPEED_DECREMENT: Float = -10f
    var NEBULA_DISABLE_ZERO_FLUX_BOOST: Boolean = true
    //HYPERCLOUD SETTINGS
    var MIN_HYPERCLOUDS_TO_ADD_PER_CELL: Int = 8

    var MAX_HYPERCLOUDS_TO_ADD_PER_CELL: Int = 20
    //HYPERSTORM SETTINGS
    var HYPERSTORM_ENERGY_DAMAGE = 2000f
    var HYPERSTORM_EMP_DAMAGE = 6500f
    var MIN_TIME_BETWEEN_HYPERSTORM_STRIKES = 5f
    var MAX_TIME_BETWEEN_HYPERSTORM_STRIKES = 9f
    var HYPERSTORM_GRACE_INCREMENT = 3f
    var HYPERSTORM_MAX_ARC_CHARGE_TIME = 4.6f
    var HYPERSTORM_MIN_ARC_CHARGE_TIME = 3.2f
    var HYPERSTORM_MIN_ARC_RANGE = 1000f
    var HYPERSTORM_MAX_ARC_RANGE = 1600f
    var HYPERSTORM_CENTROID_REFINEMENT_ITERATIONS = 2500
    var HYPERSTORM_ARC_FORCE: Float = 2000f
    var HYPERSTORM_SPEED_THRESHOLD: Float = 10f

    var EMP_DAMAGE_FEAR_MULT: Float = 0.3f

    // BLACK HOLE SETTINGS
    var BLACKHOLE_TIMEMULT_MULT = 1.5f
    var BLACKHOLE_PPT_COMPENSATION = 1f
    var BLACKHOLE_BASE_GRAVITY = 2f

    var BLACKHOLE_GRAVITY_ENABLED = true

    // PULSAR
    var PULSAR_FORCE_ENABLED = true
    var PULSAR_BASE_FORCE: Float = 0.3f
    var PULSAR_INTENSITY_BASE_MULT: Float = 1f
    var PULSAR_PPT_COMPENSATION: Float = 1f
    var PULSAR_HARDFLUX_GEN_INCREMENT: Float = 10f
    var PULSAR_EMP_DAMAGE_BONUS_FOR_WEAPONS_INCREMENT: Float = 10f
    var PULSAR_SHIELD_DESTABILIZATION_MULT_INCREMENT: Float = 25f

    var PULSAR_EMP_CHANCE_INCREMENT: Float = 0.01f
    var PULSAR_EMP_DAMAGE_INCREMENT: Float = 20f
    var PULSAR_DAMAGE_INCREMENT: Float = 0.5f

    @JvmStatic
    fun getHyperstormFearThreshold(): Float {
        return (HYPERSTORM_ENERGY_DAMAGE + HYPERSTORM_EMP_DAMAGE*EMP_DAMAGE_FEAR_MULT)*0.4f
    }


    @JvmStatic
    @Throws(JSONException::class, IOException::class)
    fun loadSettings() {
        MCTE_debugUtils.log.info("reloading settings")
        val configJson = Global.getSettings().loadJSON(MCTE_ids.masterConfig)

        SHOW_ERRORS_IN_GAME = configJson.getBoolean("showErrorsInGame")
        MAG_FIELD_EFFECT_ENABLED = configJson.getBoolean("enableMagFieldEffect")
        DEEP_HYPERSPACE_EFFECT_ENABLED = configJson.getBoolean("enableDeepHyperspaceEffect")
        HYPERSTORM_EFFECT_ENABLED = configJson.getBoolean("enableHyperstormEffect")
        SLIPSTREAM_EFFECT_ENABLED = configJson.getBoolean("enableSlipstreamEffect")
        DEBRIS_FIELD_EFFECT_ENABLED = configJson.getBoolean("enableDebrisFieldEffect")
        DUST_CLOUD_EFFECT_ENABLED = configJson.getBoolean("enableDustcloudEffect")
        EXTRA_NEBULA_EFFECTS_ENABLED = configJson.getBoolean("enableExtraNebulaEffects")
        BLACK_HOLE_EFFECT_ENABLED = configJson.getBoolean("enableBlackHoleEffectReplacement")
        PULSAR_EFFECT_ENABLED = configJson.getBoolean("enablePulsarEffect")

        //MAGFIELD
        MAGFIELD_VISION_MULT = configJson.getFloat("magFieldVisionMult")
        MAGFIELD_MISSILE_MULT = configJson.getFloat("magFieldMissileManeuverabilityAndGuidanceMult")
        MAGFIELD_RANGE_MULT = configJson.getFloat("magFieldWeaponAndFighterRangeMult")
        MAGFIELD_ECCM_MULT = configJson.getFloat("magFieldEccmChanceMult")
        MAGFIELD_MISSILE_SCRAMBLE_CHANCE = configJson.getFloat("magFieldMissileScrambleChance")
        //MAGSTORM
        MAGSTORM_VISION_MULT = configJson.getFloat("magStormVisionMult")
        MAGSTORM_MISSILE_MULT = configJson.getFloat("magStormMissileManeuverabilityAndGuidanceMult")
        MAGSTORM_RANGE_MULT = configJson.getFloat("magStormWeaponAndFighterRangeMult")
        MAGSTORM_ECCM_MULT = configJson.getFloat("magStormEccmChanceMult")
        MAGSTORM_MISSILE_SCRAMBLE_CHANCE = configJson.getFloat("magStormMissileScrambleChance")

        //SLIPSTREAM
        SLIPSTREAM_PPT_MULT = configJson.getFloat("slipstreamPPTMult")
        SLIPSTREAM_FLUX_DISSIPATION_MULT = configJson.getFloat("slipstreamFluxDissipationMult")
        SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT = configJson.getFloat("slipstreamOverallSpeedMultIncrement")
        SLIPSTREAM_HARDFLUX_GEN_PER_FRAME = configJson.getFloat("slipstreamHardFluxGenPerFrame")
        STACK_SLIPSTREAM_PPT_DEBUFF_WITH_SO = configJson.getBoolean("stackSlipstreamPPTDebuffWithSO")
        SLIPSTREAM_DISABLE_VENTING = configJson.getBoolean("slipstreamDisableVenting")
        SLIPSTREAM_INCREASE_TURN_RATE = configJson.getBoolean("slipstreamIncreaseTurnRate")

        SLIPSTREAM_FIGHTER_ZERO_FLUX_BOOST = configJson.getBoolean("slipstreamFighterZeroFluxBoost")
        SLIPSTREAM_MISSILE_ZERO_FLUX_BOOST = configJson.getBoolean("slipstreamMissileZeroFluxBoost")

        //NEBULA
        NEBULA_VISION_MULT = configJson.getFloat("nebulaVisionMult")
        NEBULA_RANGE_MULT = configJson.getFloat("nebulaRangeMult")
        NEBULA_SPEED_DECREMENT = configJson.getFloat("nebulaSpeedDecrement")
        NEBULA_DISABLE_ZERO_FLUX_BOOST = configJson.getBoolean("nebulaDisableZeroFluxBoost")

        //HYPERCLOUD
        MIN_HYPERCLOUDS_TO_ADD_PER_CELL = configJson.getInt("minimumHypercloudsPerCell")
        MAX_HYPERCLOUDS_TO_ADD_PER_CELL = configJson.getInt("maximumHypercloudsPerCell")

        //HYPERSTORM
        HYPERSTORM_ENERGY_DAMAGE = configJson.getFloat("hyperstormEnergyDamage")
        HYPERSTORM_EMP_DAMAGE = configJson.getFloat("hyperstormEMPDamage")
        MIN_TIME_BETWEEN_HYPERSTORM_STRIKES = configJson.getFloat("minTimeBetweenHyperstormStrikes")
        MAX_TIME_BETWEEN_HYPERSTORM_STRIKES = configJson.getFloat("maxTimeBetweenHyperstormStrikes")
        HYPERSTORM_GRACE_INCREMENT = configJson.getFloat("amountOfTimeShipsHaveBetweenStrikes")
        HYPERSTORM_MIN_ARC_RANGE = configJson.getFloat("hyperstormMinArcRange")
        HYPERSTORM_MAX_ARC_RANGE = configJson.getFloat("hyperstormMaxArcRange")
        HYPERSTORM_MIN_ARC_CHARGE_TIME = (configJson.getFloat("hyperstormMinArcChargeTime")).coerceAtLeast(0f)
        HYPERSTORM_MAX_ARC_CHARGE_TIME = (configJson.getFloat("hyperstormMaxArcChargeTime")).coerceAtLeast(HYPERSTORM_MIN_ARC_CHARGE_TIME)
        HYPERSTORM_ARC_FORCE = configJson.getFloat("hyperstormArcForce")
        HYPERSTORM_SPEED_THRESHOLD = configJson.getFloat("hyperstormSpeedThreshold")

        HYPERSTORM_CENTROID_REFINEMENT_ITERATIONS = configJson.getInt("hyperstormCentroidRefinementIterations")

        // BLACK HOLE
        BLACKHOLE_TIMEMULT_MULT = configJson.getFloat("blackholeTimemultMult")
        BLACKHOLE_PPT_COMPENSATION = configJson.getFloat("blackholePPTCompensation")/100
        BLACKHOLE_BASE_GRAVITY = configJson.getFloat("blackholeBaseGravity")
        BLACKHOLE_GRAVITY_ENABLED = configJson.getBoolean("blackholeGravityEnabled")

        // PULSAR
        PULSAR_FORCE_ENABLED = configJson.getBoolean("pulsarForceEnabled")
        PULSAR_BASE_FORCE = configJson.getFloat("pulsarBaseForce")
        PULSAR_INTENSITY_BASE_MULT = configJson.getFloat("pulsarIntensityBaseMult")
        PULSAR_PPT_COMPENSATION = configJson.getFloat("pulsarPPTCompensation")/100
        PULSAR_HARDFLUX_GEN_INCREMENT = configJson.getFloat("pulsarHardfluxGenIncrement")
        PULSAR_EMP_DAMAGE_BONUS_FOR_WEAPONS_INCREMENT = configJson.getFloat("pulsarEMPDamageBonusForWeaponsIncrement")
        PULSAR_SHIELD_DESTABILIZATION_MULT_INCREMENT = configJson.getFloat("pulsarShieldDestabilizationMultIncrement")
        PULSAR_EMP_CHANCE_INCREMENT = configJson.getFloat("pulsarEMPChanceIncrement")
        PULSAR_EMP_DAMAGE_INCREMENT = configJson.getFloat("pulsarEMPDamageIncrement")
        PULSAR_DAMAGE_INCREMENT = configJson.getFloat("pulsarDamageIncrement")

    }
}

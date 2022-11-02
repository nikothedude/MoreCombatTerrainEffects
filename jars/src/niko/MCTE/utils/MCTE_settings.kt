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
    var SLIPSTREAM_EFFECT_ENABLED: Boolean = true
    var DEBRIS_FIELD_EFFECT_ENABLED: Boolean = true
    var DUST_CLOUD_EFFECT_ENABLED: Boolean = true
    var EXTRA_NEBULA_EFFECTS_ENABLED: Boolean = true

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
    var SLIPSTREAM_HARDFLUX_GEN_PER_FRAME: Float = 1f
    var STACK_SLIPSTREAM_PPT_DEBUFF_WITH_SO: Boolean = true
    var SLIPSTREAM_DISABLE_VENTING: Boolean = false
    var SLIPSTREAM_INCREASE_TURN_RATE: Boolean = false

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
    var HYPERSTORM_ARCSITE_X_VARIATION = 500f
    var HYPERSTORM_ARCSITE_Y_VARIATION = 500f
    var HYPERSTORM_MIN_ARC_RANGE = 1000f
    var HYPERSTORM_MAX_ARC_RANGE = 1600f


    @JvmStatic
    @Throws(JSONException::class, IOException::class)
    fun loadSettings() {
        MCTE_debugUtils.log.info("reloading settings")
        val configJson = Global.getSettings().loadJSON(MCTE_ids.masterConfig)
        SHOW_ERRORS_IN_GAME = configJson.getBoolean("showErrorsInGame")
        MAG_FIELD_EFFECT_ENABLED = configJson.getBoolean("enableMagFieldEffect")
        DEEP_HYPERSPACE_EFFECT_ENABLED = configJson.getBoolean("enableDeepHyperspaceEffect")
        SLIPSTREAM_EFFECT_ENABLED = configJson.getBoolean("enableSlipstreamEffect")
        DEBRIS_FIELD_EFFECT_ENABLED = configJson.getBoolean("enableDebrisFieldEffect")
        DUST_CLOUD_EFFECT_ENABLED = configJson.getBoolean("enableDustcloudEffect")
        EXTRA_NEBULA_EFFECTS_ENABLED = configJson.getBoolean("enableExtraNebulaEffects")

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
        HYPERSTORM_ARCSITE_X_VARIATION = configJson.getFloat("hyperstormArcSiteXVariation")
        HYPERSTORM_ARCSITE_Y_VARIATION = configJson.getFloat("hyperstormArcSiteYVariation")
        HYPERSTORM_MIN_ARC_RANGE = configJson.getFloat("hyperstormMinArcRange")
        HYPERSTORM_MAX_ARC_RANGE = configJson.getFloat("hyperstormMaxArcRange")
    }
}

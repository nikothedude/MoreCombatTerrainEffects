package niko.MCTE.settings

import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettings
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_ids.modId
import niko.MCTE.utils.terrainScriptsTracker
import org.json.JSONException
import org.lazywizard.lazylib.ext.json.getFloat
import java.io.IOException
import java.lang.NullPointerException
import kotlin.jvm.Throws

object MCTE_settings {

    var SHOW_SIDEBAR_INFO: Boolean = true

    var SLIPSTREAM_AFFECT_INTANGIBLE: Boolean = true
    var SHOW_ERRORS_IN_GAME: Boolean = true
    var SOLAR_SHIELDING_EFFECT_MULT: Float = 1f
    var MAG_FIELD_EFFECT_ENABLED: Boolean = true
    var DEEP_HYPERSPACE_EFFECT_ENABLED: Boolean = true
    var HYPERSTORM_EFFECT_ENABLED: Boolean = true
    var SLIPSTREAM_EFFECT_ENABLED: Boolean = true
    var DEBRIS_FIELD_EFFECT_ENABLED: Boolean = false
    var DUST_CLOUD_EFFECT_ENABLED: Boolean = false
    var EXTRA_NEBULA_EFFECTS_ENABLED: Boolean = true
    var BLACK_HOLE_EFFECT_ENABLED: Boolean = true
    var PULSAR_EFFECT_ENABLED: Boolean = true

    //MAGFIELD SETTINGS
    var MAGFIELD_VISION_MULT: Float = 0.6f
    var MAGFIELD_MISSILE_MULT: Float = 0.8f

    var MAGFIELD_RANGE_MULT: Float = 0.8f
    var MAGFIELD_ECCM_MULT: Float = 0.8f
    var MAGFIELD_MISSILE_SCRAMBLE_CHANCE: Float = 0.6f

    var MAGFIELD_MISSILE_UNSCRAMBLE_CHANCE: Float = (0.0005f)
    //MAGSTORM SETTINGS
    var MAGSTORM_VISION_MULT: Float = 0.6f
    var MAGSTORM_MISSILE_MULT: Float = 0.8f
    var MAGSTORM_RANGE_MULT: Float = 0.8f
    var MAGSTORM_ECCM_MULT: Float = 0.8f
    var MAGSTORM_MISSILE_SCRAMBLE_CHANCE: Float = 0.6f
    var MAGSTORM_MISSILE_UNSCRAMBLE_CHANCE: Float = (0.0005f)
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
    var HYPERSTORM_UNTARGETABILITY_MASS_THRESHOLD: Float = 400f
    var HYPERSTORM_PRIMARY_RANDOMNESS_MULT: Float = 0.2f

    var HYPERSTORM_MASS_TARGETTING_COEFFICIENT: Float = 0.01f
    var HYPERSTORM_SPEED_TARGETTING_COEFFICIENT: Float = 0.3f
    var HYPERSTORM_MASS_MAX_TARGETTING_MULT: Float = 8f
    var HYPERSTORM_SPEED_MAX_TARGETTING_MULT: Float = 6f

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
        return (HYPERSTORM_ENERGY_DAMAGE + HYPERSTORM_EMP_DAMAGE * EMP_DAMAGE_FEAR_MULT)*0.4f
    }


    @JvmStatic
    @Throws(JSONException::class, IOException::class, NullPointerException::class)
    fun loadSettings() {
        //MCTE_debugUtils.log.info("reloading settings")

        SHOW_SIDEBAR_INFO = LunaSettings.getBoolean(modId, "MCTE_showSidebarInfo")!!
        SHOW_ERRORS_IN_GAME = LunaSettings.getBoolean(modId, "MCTE_showErrorsInGame")!!
        SOLAR_SHIELDING_EFFECT_MULT = LunaSettings.getFloat(modId, "MCTE_solarShieldingMult")!!
        MAG_FIELD_EFFECT_ENABLED = LunaSettings.getBoolean(modId,"MCTE_magneticFieldToggle")!!
        DEEP_HYPERSPACE_EFFECT_ENABLED = LunaSettings.getBoolean(modId,"MCTE_deepHyperspaceToggle")!!
        HYPERSTORM_EFFECT_ENABLED = LunaSettings.getBoolean(modId,"MCTE_hyperstormsToggle")!!
        SLIPSTREAM_EFFECT_ENABLED = LunaSettings.getBoolean(modId,"MCTE_slipstreamToggle")!!
        //DEBRIS_FIELD_EFFECT_ENABLED = LunaSettings.getBoolean(modId,"enableDebrisFieldEffect")!!
        //DUST_CLOUD_EFFECT_ENABLED = LunaSettings.getBoolean(modId,"enableDustcloudEffect")!!
        EXTRA_NEBULA_EFFECTS_ENABLED = LunaSettings.getBoolean(modId,"MCTE_nebulaToggle")!!
        BLACK_HOLE_EFFECT_ENABLED = LunaSettings.getBoolean(modId,"MCTE_blackholeToggle")!!
        PULSAR_EFFECT_ENABLED = LunaSettings.getBoolean(modId,"MCTE_pulsarToggle")!!

        //MAGFIELD
        MAGFIELD_VISION_MULT = LunaSettings.getFloat(modId,"MCTE_magneticFieldVisionMult")!!
        MAGFIELD_MISSILE_MULT = LunaSettings.getFloat(modId,"MCTE_magneticFieldMissileGuidanceAndManeuverabilityMult")!!
        MAGFIELD_RANGE_MULT = LunaSettings.getFloat(modId,"MCTE_magneticFieldWeaponAndFighterRangeMult")!!
        MAGFIELD_ECCM_MULT = LunaSettings.getFloat(modId,"MCTE_magneticFieldECCMChanceMult")!!
        MAGFIELD_MISSILE_SCRAMBLE_CHANCE = LunaSettings.getFloat(modId,"MCTE_magneticFieldMissileScrambleChance")!!
        MAGFIELD_MISSILE_UNSCRAMBLE_CHANCE = LunaSettings.getFloat(modId,"MCTE_magneticFieldMissileUnscrambleChance")!!
        //MAGSTORM
        MAGSTORM_VISION_MULT = LunaSettings.getFloat(modId,"MCTE_magneticStormVisionMult")!!
        MAGSTORM_MISSILE_MULT = LunaSettings.getFloat(modId,"MCTE_magneticStormMissileGuidanceAndManeuverabilityMult")!!
        MAGSTORM_RANGE_MULT = LunaSettings.getFloat(modId,"MCTE_magneticStormWeaponAndFighterRangeMult")!!
        MAGSTORM_ECCM_MULT = LunaSettings.getFloat(modId,"MCTE_magneticStormECCMChanceMult")!!
        MAGSTORM_MISSILE_SCRAMBLE_CHANCE = LunaSettings.getFloat(modId,"MCTE_magneticStormMissileScrambleChance")!!
        MAGSTORM_MISSILE_UNSCRAMBLE_CHANCE = LunaSettings.getFloat(modId,"MCTE_magneticStormMissileUnscrambleChance")!!

        //SLIPSTREAM
        SLIPSTREAM_PPT_MULT = LunaSettings.getFloat(modId,"MCTE_slipstreamPPTMult")!!
        SLIPSTREAM_FLUX_DISSIPATION_MULT = LunaSettings.getFloat(modId,"MCTE_slipstreamFluxDissipationMult")!!
        SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT = LunaSettings.getFloat(modId,"MCTE_slipstreamOverallSpeedMult")!!
        SLIPSTREAM_HARDFLUX_GEN_PER_FRAME = LunaSettings.getFloat(modId,"MCTE_slipstreamHardfluxGenPerFrame")!!
        STACK_SLIPSTREAM_PPT_DEBUFF_WITH_SO = LunaSettings.getBoolean(modId,"MCTE_slipstreamStackPPTWithSO")!!
        SLIPSTREAM_DISABLE_VENTING = LunaSettings.getBoolean(modId,"MCTE_slipstreamDisableVenting")!!
        SLIPSTREAM_INCREASE_TURN_RATE = LunaSettings.getBoolean(modId,"MCTE_slipstreamIncreaseTurnRate")!!

        SLIPSTREAM_AFFECT_INTANGIBLE = LunaSettings.getBoolean(modId, "MCTE_slipstreamAffectIntangible")!!

        SLIPSTREAM_FIGHTER_ZERO_FLUX_BOOST = LunaSettings.getBoolean(modId,"MCTE_slipstreamFighterZeroFluxBoost")!!
        SLIPSTREAM_MISSILE_ZERO_FLUX_BOOST = LunaSettings.getBoolean(modId,"MCTE_slipstreamMissileZeroFluxBoost")!!

        //NEBULA
        NEBULA_VISION_MULT = LunaSettings.getFloat(modId,"MCTE_nebulaVisionMult")!!
        NEBULA_RANGE_MULT = LunaSettings.getFloat(modId,"MCTE_nebulaRangeMult")!!
        NEBULA_SPEED_DECREMENT = LunaSettings.getFloat(modId,"MCTE_nebulaSpeedIncrement")!!
        NEBULA_DISABLE_ZERO_FLUX_BOOST = LunaSettings.getBoolean(modId,"MCTE_nebulaDisableZeroFluxBoost")!!

        //HYPERCLOUD
        MIN_HYPERCLOUDS_TO_ADD_PER_CELL = LunaSettings.getInt(modId,"MCTE_deepHyperspaceMinimumCloudsPerCell")!!
        MAX_HYPERCLOUDS_TO_ADD_PER_CELL = LunaSettings.getInt(modId,"MCTE_deepHyperspaceMaximumCloudsPerCell")!!

        //HYPERSTORM
        HYPERSTORM_ENERGY_DAMAGE = LunaSettings.getFloat(modId,"MCTE_hyperstormEnergyDamage")!!
        HYPERSTORM_EMP_DAMAGE = LunaSettings.getFloat(modId,"MCTE_hyperstormEMPDamage")!!
        MIN_TIME_BETWEEN_HYPERSTORM_STRIKES = LunaSettings.getFloat(modId,"MCTE_hyperstormMinTimeBetweenStrikes")!!
        MAX_TIME_BETWEEN_HYPERSTORM_STRIKES = LunaSettings.getFloat(modId,"MCTE_hyperstormMaxTimeBetweenStrikes")!!
        HYPERSTORM_GRACE_INCREMENT = LunaSettings.getFloat(modId,"MCTE_hyperstormGracePeriod")!!
        HYPERSTORM_MIN_ARC_RANGE = LunaSettings.getFloat(modId,"MCTE_hyperstormMinArcRange")!!
        HYPERSTORM_MAX_ARC_RANGE = LunaSettings.getFloat(modId,"MCTE_hyperstormMaxArcRange")!!
        HYPERSTORM_MIN_ARC_CHARGE_TIME = (LunaSettings.getFloat(modId,"MCTE_hyperstormMinChargeTime"))!!.coerceAtLeast(0f)
        HYPERSTORM_MAX_ARC_CHARGE_TIME = (LunaSettings.getFloat(modId,"MCTE_hyperstormMaxChargeTime"))!!.coerceAtLeast(HYPERSTORM_MIN_ARC_CHARGE_TIME)
        HYPERSTORM_ARC_FORCE = LunaSettings.getFloat(modId,"MCTE_hyperstormLightningForce")!!
        HYPERSTORM_SPEED_THRESHOLD = LunaSettings.getFloat(modId,"MCTE_hyperstormSpeedThreshold")!!
        HYPERSTORM_UNTARGETABILITY_MASS_THRESHOLD = LunaSettings.getFloat(modId, "MCTE_hyperstormSpeedUntargetabilityThreshold")!!
        HYPERSTORM_MASS_TARGETTING_COEFFICIENT = LunaSettings.getFloat(modId, "MCTE_hyperstormMassTargettingCoefficient")!!
        HYPERSTORM_SPEED_TARGETTING_COEFFICIENT = LunaSettings.getFloat(modId, "MCTE_hyperstormSpeedTargettingCoefficient")!!
        HYPERSTORM_MASS_MAX_TARGETTING_MULT = LunaSettings.getFloat(modId, "MCTE_hyperstormMassTargettingMaxMult")!!
        HYPERSTORM_SPEED_MAX_TARGETTING_MULT = LunaSettings.getFloat(modId, "MCTE_hyperstormSpeedTargettingMaxMult")!!
        HYPERSTORM_PRIMARY_RANDOMNESS_MULT = LunaSettings.getFloat(modId, "MCTE_hyperstormPrimaryRandomnessThreshold")!!/100

        HYPERSTORM_CENTROID_REFINEMENT_ITERATIONS = LunaSettings.getInt(modId,"MCTE_refinementIterations")!!

        // BLACK HOLE
        BLACKHOLE_TIMEMULT_MULT = LunaSettings.getFloat(modId,"MCTE_blackHoleTimemultMult")!!
        BLACKHOLE_PPT_COMPENSATION = LunaSettings.getFloat(modId,"MCTE_blackHolePPTCompensation")!!/100
        BLACKHOLE_BASE_GRAVITY = LunaSettings.getFloat(modId,"MCTE_gravityMult")!!
        BLACKHOLE_GRAVITY_ENABLED = LunaSettings.getBoolean(modId,"MCTE_blackHoleGravityEnabled")!!

        // PULSAR
        PULSAR_FORCE_ENABLED = LunaSettings.getBoolean(modId,"MCTE_pulsarForceEnabled")!!
        PULSAR_BASE_FORCE = LunaSettings.getFloat(modId,"MCTE_pulsarForceMult")!!
        PULSAR_INTENSITY_BASE_MULT = LunaSettings.getFloat(modId,"MCTE_pulsarBaseIntensityMult")!!
        PULSAR_PPT_COMPENSATION = LunaSettings.getFloat(modId,"MCTE_pulsarPPTCompensation")!!/100
        PULSAR_HARDFLUX_GEN_INCREMENT = LunaSettings.getFloat(modId,"MCTE_pulsarHardFluxGenPerFrame")!!
        PULSAR_EMP_DAMAGE_BONUS_FOR_WEAPONS_INCREMENT = LunaSettings.getFloat(modId,"MCTE_pulsarEMPDamageBonusForWeapons")!!
        PULSAR_SHIELD_DESTABILIZATION_MULT_INCREMENT = LunaSettings.getFloat(modId,"MCTE_pulsarShieldEffectMult")!!
        PULSAR_EMP_CHANCE_INCREMENT = LunaSettings.getFloat(modId,"MCTE_pulsarRandomArcChance")!!
        PULSAR_EMP_DAMAGE_INCREMENT = LunaSettings.getFloat(modId,"MCTE_pulsarRandomArcEMPDamage")!!
        PULSAR_DAMAGE_INCREMENT = LunaSettings.getFloat(modId,"MCTE_pulsarRandomArcEnergyDamage")!!

    }
}

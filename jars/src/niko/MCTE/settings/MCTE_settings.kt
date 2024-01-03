package niko.MCTE.settings

import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettings
import niko.MCTE.utils.MCTE_ids.masterConfig
import niko.MCTE.utils.MCTE_ids.modId
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
    var SLIPSTREAM_REDUCE_WEAPON_RANGE: Boolean = true
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

    var HYPERSTORM_TIMES_TO_ARC_AGAINST_SHIP = 5

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

    // COMMS RELAY
    var COMMS_RELAY_ENABLED = true
    var COMMS_RELAY_MAX_DISTANCE = 5000f
    var COMMS_RELAY_MIN_DISTANCE = 500f

    @JvmStatic
    fun getHyperstormFearThreshold(): Float {
        return (HYPERSTORM_ENERGY_DAMAGE + HYPERSTORM_EMP_DAMAGE * EMP_DAMAGE_FEAR_MULT)*0.4f
    }


    @JvmStatic
    @Throws(JSONException::class, IOException::class, NullPointerException::class)
    fun loadSettings() {
        val configJson = Global.getSettings().loadJSON(masterConfig)
        //MCTE_debugUtils.log.info("reloading settings")

        SHOW_SIDEBAR_INFO = configJson.getBoolean("MCTE_showSidebarInfo")!!
        SHOW_ERRORS_IN_GAME = configJson.getBoolean( "MCTE_showErrorsInGame")!!
        SOLAR_SHIELDING_EFFECT_MULT = configJson.getFloat( "MCTE_solarShieldingMult")!!
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
        MAGFIELD_VISION_MULT = configJson.getFloat("MCTE_magneticFieldVisionMult")!!
        MAGFIELD_MISSILE_MULT = configJson.getFloat("MCTE_magneticFieldMissileGuidanceAndManeuverabilityMult")!!
        MAGFIELD_RANGE_MULT = configJson.getFloat("MCTE_magneticFieldWeaponAndFighterRangeMult")!!
        MAGFIELD_ECCM_MULT = configJson.getFloat("MCTE_magneticFieldECCMChanceMult")!!
        MAGFIELD_MISSILE_SCRAMBLE_CHANCE = configJson.getFloat("MCTE_magneticFieldMissileScrambleChance")!!
        MAGFIELD_MISSILE_UNSCRAMBLE_CHANCE = configJson.getFloat("MCTE_magneticFieldMissileUnscrambleChance")!!
        //MAGSTORM
        MAGSTORM_VISION_MULT = configJson.getFloat("MCTE_magneticStormVisionMult")!!
        MAGSTORM_MISSILE_MULT = configJson.getFloat("MCTE_magneticStormMissileGuidanceAndManeuverabilityMult")!!
        MAGSTORM_RANGE_MULT = configJson.getFloat("MCTE_magneticStormWeaponAndFighterRangeMult")!!
        MAGSTORM_ECCM_MULT = configJson.getFloat("MCTE_magneticStormECCMChanceMult")!!
        MAGSTORM_MISSILE_SCRAMBLE_CHANCE = configJson.getFloat("MCTE_magneticStormMissileScrambleChance")!!
        MAGSTORM_MISSILE_UNSCRAMBLE_CHANCE = configJson.getFloat("MCTE_magneticStormMissileUnscrambleChance")!!

        //SLIPSTREAM
        SLIPSTREAM_PPT_MULT = configJson.getFloat("MCTE_slipstreamPPTMult")!!
        SLIPSTREAM_FLUX_DISSIPATION_MULT = configJson.getFloat("MCTE_slipstreamFluxDissipationMult")!!
        SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT = configJson.getFloat("MCTE_slipstreamOverallSpeedMultIncrement")!!
        SLIPSTREAM_HARDFLUX_GEN_PER_FRAME = configJson.getFloat("MCTE_slipstreamHardfluxGenPerFrame")!!
        STACK_SLIPSTREAM_PPT_DEBUFF_WITH_SO = configJson.getBoolean("MCTE_slipstreamStackPPTWithSO")!!
        SLIPSTREAM_DISABLE_VENTING = configJson.getBoolean("MCTE_slipstreamDisableVenting")!!
        SLIPSTREAM_INCREASE_TURN_RATE = configJson.getBoolean("MCTE_slipstreamIncreaseTurnRate")!!

        SLIPSTREAM_AFFECT_INTANGIBLE = configJson.getBoolean("MCTE_slipstreamAffectIntangible")!!

        SLIPSTREAM_FIGHTER_ZERO_FLUX_BOOST = configJson.getBoolean("MCTE_slipstreamFighterZeroFluxBoost")!!
        SLIPSTREAM_MISSILE_ZERO_FLUX_BOOST = configJson.getBoolean("MCTE_slipstreamMissileZeroFluxBoost")!!
        SLIPSTREAM_REDUCE_WEAPON_RANGE = configJson.getBoolean("MCTE_slipstreamWeaponRangeReduction")!!

        //NEBULA
        NEBULA_VISION_MULT = configJson.getFloat("MCTE_nebulaVisionMult")!!
        NEBULA_RANGE_MULT = configJson.getFloat("MCTE_nebulaRangeMult")!!
        NEBULA_SPEED_DECREMENT = configJson.getFloat("MCTE_nebulaSpeedIncrement")!!
        NEBULA_DISABLE_ZERO_FLUX_BOOST = configJson.getBoolean("MCTE_nebulaDisableZeroFluxBoost")!!

        //HYPERCLOUD
        MIN_HYPERCLOUDS_TO_ADD_PER_CELL = configJson.getInt("MCTE_deepHyperspaceMinimumCloudsPerCell")!!
        MAX_HYPERCLOUDS_TO_ADD_PER_CELL = configJson.getInt("MCTE_deepHyperspaceMaximumCloudsPerCell")!!

        //HYPERSTORM
        HYPERSTORM_ENERGY_DAMAGE = configJson.getFloat("MCTE_hyperstormEnergyDamage")!!
        HYPERSTORM_EMP_DAMAGE = configJson.getFloat("MCTE_hyperstormEMPDamage")!!
        MIN_TIME_BETWEEN_HYPERSTORM_STRIKES = configJson.getFloat("MCTE_hyperstormMinTimeBetweenStrikes")!!
        MAX_TIME_BETWEEN_HYPERSTORM_STRIKES = configJson.getFloat("MCTE_hyperstormMaxTimeBetweenStrikes")!!
        HYPERSTORM_GRACE_INCREMENT = configJson.getFloat("MCTE_hyperstormGracePeriod")!!
        HYPERSTORM_MIN_ARC_RANGE = configJson.getFloat("MCTE_hyperstormMinArcRange")!!
        HYPERSTORM_MAX_ARC_RANGE = configJson.getFloat("MCTE_hyperstormMaxArcRange")!!
        HYPERSTORM_MIN_ARC_CHARGE_TIME = (configJson.getFloat("MCTE_hyperstormMinChargeTime"))!!.coerceAtLeast(0f)
        HYPERSTORM_MAX_ARC_CHARGE_TIME = (configJson.getFloat("MCTE_hyperstormMaxChargeTime"))!!.coerceAtLeast(HYPERSTORM_MIN_ARC_CHARGE_TIME)
        HYPERSTORM_ARC_FORCE = configJson.getFloat("MCTE_hyperstormLightningForce")!!
        HYPERSTORM_SPEED_THRESHOLD = configJson.getFloat("MCTE_hyperstormSpeedThreshold")!!
        HYPERSTORM_UNTARGETABILITY_MASS_THRESHOLD = configJson.getFloat( "MCTE_hyperstormSpeedUntargetabilityThreshold")!!
        HYPERSTORM_MASS_TARGETTING_COEFFICIENT = configJson.getFloat( "MCTE_hyperstormMassTargettingCoefficient")!!
        HYPERSTORM_SPEED_TARGETTING_COEFFICIENT = configJson.getFloat( "MCTE_hyperstormSpeedTargettingCoefficient")!!
        HYPERSTORM_MASS_MAX_TARGETTING_MULT = configJson.getFloat( "MCTE_hyperstormMassTargettingMaxMult")!!
        HYPERSTORM_SPEED_MAX_TARGETTING_MULT = configJson.getFloat( "MCTE_hyperstormSpeedTargettingMaxMult")!!
        HYPERSTORM_PRIMARY_RANDOMNESS_MULT = configJson.getFloat( "MCTE_hyperstormPrimaryRandomnessThreshold")!!/100

        HYPERSTORM_CENTROID_REFINEMENT_ITERATIONS = configJson.getInt("MCTE_refinementIterations")

        HYPERSTORM_TIMES_TO_ARC_AGAINST_SHIP = configJson.getInt("MCTE_hyperstormTimesToArc")

        // BLACK HOLE
        BLACKHOLE_TIMEMULT_MULT = configJson.getFloat("MCTE_blackHoleTimemultMult")!!
        BLACKHOLE_PPT_COMPENSATION = configJson.getFloat("MCTE_blackHolePPTCompensation")!!/100
        BLACKHOLE_BASE_GRAVITY = configJson.getFloat("MCTE_gravityMult")!!
        BLACKHOLE_GRAVITY_ENABLED = configJson.getBoolean("MCTE_blackHoleGravityEnabled")!!

        // PULSAR
        PULSAR_FORCE_ENABLED = configJson.getBoolean("MCTE_pulsarForceEnabled")!!
        PULSAR_BASE_FORCE = configJson.getFloat("MCTE_pulsarForceMult")!!
        PULSAR_INTENSITY_BASE_MULT = configJson.getFloat("MCTE_pulsarBaseIntensityMult")!!
        PULSAR_PPT_COMPENSATION = configJson.getFloat("MCTE_pulsarPPTCompensation")!!/100
        PULSAR_HARDFLUX_GEN_INCREMENT = configJson.getFloat("MCTE_pulsarHardFluxGenPerFrameIncrement")!!
        PULSAR_EMP_DAMAGE_BONUS_FOR_WEAPONS_INCREMENT = configJson.getFloat("MCTE_pulsarEMPDamageBonusForWeaponsIncrement")!!
        PULSAR_SHIELD_DESTABILIZATION_MULT_INCREMENT = configJson.getFloat("MCTE_pulsarShieldEffectMultIncrement")!!
        PULSAR_EMP_CHANCE_INCREMENT = configJson.getFloat("MCTE_pulsarRandomArcChanceIncrement")!!
        PULSAR_EMP_DAMAGE_INCREMENT = configJson.getFloat("MCTE_pulsarRandomArcEMPDamageIncrement")!!
        PULSAR_DAMAGE_INCREMENT = configJson.getFloat("MCTE_pulsarRandomArcEnergyDamageIncrement")!!

        // COMMS RELAY

    }
}

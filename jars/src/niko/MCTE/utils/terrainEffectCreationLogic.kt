package niko.MCTE.utils

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.cloudCell
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magneticFieldEffect
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream.SlipstreamEffectScript
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.MCTE_nebulaUtils.getCloudsInRadius
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

object terrainEffectCreationLogic {

    fun createSlipstreamEffectScript(slipstreamInstances: Int): SlipstreamEffectScript {

        var peakPerformanceMult = 1f
        var fluxDissipationMult = 1f
        var overallSpeedMult = 1f
        var hardFluxGenerationPerFrame = 0f

        var timesToAdd = slipstreamInstances
        while (timesToAdd-- > 0) {
            peakPerformanceMult *= MCTE_settings.SLIPSTREAM_PPT_MULT
            fluxDissipationMult *= MCTE_settings.SLIPSTREAM_FLUX_DISSIPATION_MULT
            overallSpeedMult += MCTE_settings.SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT

            hardFluxGenerationPerFrame += MCTE_settings.SLIPSTREAM_HARDFLUX_GEN_PER_FRAME
        }
        val slipstreamPlugin = SlipstreamEffectScript(
            peakPerformanceMult,
            fluxDissipationMult,
            hardFluxGenerationPerFrame,
            overallSpeedMult
        )
        return slipstreamPlugin
    }

    fun createMagFieldEffectScript(entries: MutableList<Boolean>, effectMult: Float = 1f): magneticFieldEffect {
        var isStorm = false
        var visionMod = 1f
        var missileMod = 1f
        var rangeMod = 1f
        var eccmChanceMod = 1f
        var missileBreakLockBaseChance = 0f

        for (entry in entries) {
            val isInFlare = entry
            if (isInFlare) isStorm = true

            visionMod *= if (isInFlare) MCTE_settings.MAGSTORM_VISION_MULT * effectMult else MCTE_settings.MAGFIELD_VISION_MULT * effectMult
            missileMod *= if (isInFlare) MCTE_settings.MAGSTORM_MISSILE_MULT * effectMult else MCTE_settings.MAGFIELD_MISSILE_MULT * effectMult
            rangeMod *= if (isInFlare) MCTE_settings.MAGSTORM_RANGE_MULT * effectMult else MCTE_settings.MAGFIELD_RANGE_MULT * effectMult
            eccmChanceMod *= if (isInFlare) MCTE_settings.MAGSTORM_ECCM_MULT * effectMult else MCTE_settings.MAGFIELD_ECCM_MULT * effectMult
            missileBreakLockBaseChance += if (isInFlare) MCTE_settings.MAGSTORM_MISSILE_SCRAMBLE_CHANCE * effectMult else MCTE_settings.MAGFIELD_MISSILE_SCRAMBLE_CHANCE * effectMult
        }

        missileBreakLockBaseChance = missileBreakLockBaseChance.coerceAtMost(1f)
        val magFieldPlugin = magneticFieldEffect(
            isStorm,
            visionMod,
            missileMod,
            rangeMod,
            eccmChanceMod,
            missileBreakLockBaseChance,
        )
        return magFieldPlugin
    }
}
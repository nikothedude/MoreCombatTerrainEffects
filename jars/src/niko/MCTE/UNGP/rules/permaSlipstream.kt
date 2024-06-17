package niko.MCTE.UNGP.rules

import com.fs.starfarer.api.combat.CombatEngineAPI
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream.SlipstreamEffectScript
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.terrainEffectCreationLogic
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings

class permaSlipstream: UNGPterrainEffect() {
    override var classOfScript: Class<out baseTerrainEffectScript>? = SlipstreamEffectScript::class.java
    var effectMult = 1f

    override fun updateDifficultyCache(difficulty: UNGP_SpecialistSettings.Difficulty?) {
        if (difficulty == null) return

        effectMult = difficulty.getLinearValue(1f, 1f)
        super.updateDifficultyCache(difficulty)
    }

    override fun modifyScript(script: baseTerrainEffectScript) {
        if (script !is SlipstreamEffectScript) return
        script.peakPerformanceMult *= MCTE_settings.SLIPSTREAM_PPT_MULT / effectMult
        script.fluxDissipationMult += MCTE_settings.SLIPSTREAM_FLUX_DISSIPATION_MULT * effectMult
        script.overallSpeedMult += MCTE_settings.SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT * effectMult

        script.hardFluxGenerationPerFrame += MCTE_settings.SLIPSTREAM_HARDFLUX_GEN_PER_FRAME * effectMult
    }

    override fun createNewScriptInstance(engine: CombatEngineAPI): baseTerrainEffectScript {
        return combatEffectTypes.SLIPSTREAM.createInformedEffectInstance(effectMult)
    }

    override fun getDescriptionParams(index: Int, difficulty: UNGP_SpecialistSettings.Difficulty?): String {
        return when (index) {
            0 -> "slipstream"
            1 -> "safety overrides"
            2 -> "all ships, fighters, and missiles"
            3 -> (MCTE_settings.SLIPSTREAM_HARDFLUX_GEN_PER_FRAME * effectMult).toString()
            else -> ""
        }
    }
}
package niko.MCTE.stationAugments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream.SlipstreamEffectScript
import niko.MCTE.settings.MCTE_settings

class MCTE_slipstreamAugment : MCTE_terrainAugment() {
    override var classOfScript: Class<out baseTerrainEffectScript>? = SlipstreamEffectScript::class.java

    override fun modifyScript(existingScript: baseTerrainEffectScript) {
        if (existingScript !is SlipstreamEffectScript) return

        existingScript.peakPerformanceMult *= MCTE_settings.SLIPSTREAM_PPT_MULT
        existingScript.fluxDissipationMult += MCTE_settings.SLIPSTREAM_FLUX_DISSIPATION_MULT
        existingScript.overallSpeedMult += MCTE_settings.SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT

        existingScript.hardFluxGenerationPerFrame += MCTE_settings.SLIPSTREAM_HARDFLUX_GEN_PER_FRAME
    }

    override fun createTerrainEffect(station: ShipAPI, engine: CombatEngineAPI) {
        val script = combatEffectTypes.SLIPSTREAM.createInformedEffectInstance(1f)
        script.start()
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Using a less far-fetched variant of the slipsurge theory, the station can destabilize reality just enough to cause " +
            "a transient slipstream - too weak to impact the drive bubble, but enough to overcharge the systems of any nearby ships, including itself.",
            5f
        )

        tooltip.addPara(
            "Applies %s to all ships, fighters, and missiles in combat with the station.",
            5f,
            Misc.getHighlightColor(),
            "safety overrides"
        )
    }
}
package niko.MCTE.stationAugments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.blackHole.blackHoleEffectScript

class MCTE_blackHoleAugment : MCTE_terrainAugment() {

    companion object {
        const val MASS_INCREMENT = 2000f
    }

    override var classOfScript: Class<out baseTerrainEffectScript>? = blackHoleEffectScript::class.java

    override fun applyInCombat(station: ShipAPI) {
        super.applyInCombat(station)

        station.mass += MASS_INCREMENT
    }

    override fun modifyScript(existingScript: baseTerrainEffectScript) {
        /*if (existingScript !is SlipstreamEffectScript) return

        val effectMult = (MCTE_settings.UNGP_EFFECT_BASE_MULT)

        existingScript.peakPerformanceMult *= MCTE_settings.SLIPSTREAM_PPT_MULT / effectMult
        existingScript.fluxDissipationMult += MCTE_settings.SLIPSTREAM_FLUX_DISSIPATION_MULT * effectMult
        existingScript.overallSpeedMult += MCTE_settings.SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT * effectMult

        existingScript.hardFluxGenerationPerFrame += MCTE_settings.SLIPSTREAM_HARDFLUX_GEN_PER_FRAME * effectMult*/
    }

    override fun createTerrainEffect(station: ShipAPI, engine: CombatEngineAPI) {
        val script = combatEffectTypes.BLACKHOLE.createInformedEffectInstance(hashMapOf(Pair(station, 0.2f)), 1.35f)
        script.start()
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "The static drive field of a station can be modified to amplify the mass of the station enough to create a event horizon. " +
                    "Naturally, all traffic near the station is prohibited when this modification is active - which thankfully " +
                    "is only during a battle.",
            5f,
        )

        tooltip.addPara(
            "Applies the %s terrain effect to any combats including the station, which %s aboard all ships and %s.",
            5f,
            Misc.getHighlightColor(),
            "black hole", "significantly increases timeflow", "pulls ships into the station"
        )
        tooltip.addPara("Also %s.", 5f, Misc.getHighlightColor(), "increases station mass by ${MASS_INCREMENT.toInt()}")
    }
}
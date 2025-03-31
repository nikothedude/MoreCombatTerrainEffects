package niko.MCTE.stationAugments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.pulsarBeam.pulsarEffectScript

class MCTE_pulsarAugment: MCTE_terrainAugment() {
    override var classOfScript: Class<out baseTerrainEffectScript>? = pulsarEffectScript::class.java

    override fun modifyScript(existingScript: baseTerrainEffectScript) {
        /*if (existingScript !is SlipstreamEffectScript) return

        val effectMult = (MCTE_settings.UNGP_EFFECT_BASE_MULT)

        existingScript.peakPerformanceMult *= MCTE_settings.SLIPSTREAM_PPT_MULT / effectMult
        existingScript.fluxDissipationMult += MCTE_settings.SLIPSTREAM_FLUX_DISSIPATION_MULT * effectMult
        existingScript.overallSpeedMult += MCTE_settings.SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT * effectMult

        existingScript.hardFluxGenerationPerFrame += MCTE_settings.SLIPSTREAM_HARDFLUX_GEN_PER_FRAME * effectMult*/
    }

    override fun createTerrainEffect(station: ShipAPI, engine: CombatEngineAPI) {
        var movementAngle = 270f
        val side = station.owner
        if (side == 0) {
            movementAngle = 90f
        }

        val script = combatEffectTypes.PULSAR.createInformedEffectInstance(hashMapOf(Pair(movementAngle, 0.6f)), 1f)
        script.start()
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "The static drive field of a station can be modified to violently churn and warp, creating an incredibly hostile environment much like a %s.",
            5f,
            Misc.getHighlightColor(),
            "pulsar beam"
        )

        tooltip.addPara(
            "Applies the %s terrain effect to any combats including the station, which periodically %s all ships, %s, %s and %s.",
            5f,
            Misc.getHighlightColor(),
            "pulsar beam", "EMPs", "reduces shield efficiency", "applies EMP damage to all projectiles", "pushes ships away"
        )
    }
}
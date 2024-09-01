package niko.MCTE.stationAugments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.pulsarBeam.pulsarEffectScript

class MCTE_pulsarAugment(market: MarketAPI?, id: String) : MCTE_terrainAugment(market, id) {
    override var classOfScript: Class<out baseTerrainEffectScript>? = pulsarEffectScript::class.java
    override val augmentCost: Float = 15f
    override val name: String = "Ionized dispersal unit"
    override val spriteId: String = "graphics/icons/industry/mining.png"

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
}
package niko.MCTE.stationAugments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.deepHyperspaceEffectScript

class MCTE_stormDispersal: MCTE_terrainAugment() {

    override var classOfScript: Class<out baseTerrainEffectScript>? = deepHyperspaceEffectScript::class.java

    override fun modifyScript(existingScript: baseTerrainEffectScript) {
        if (existingScript !is deepHyperspaceEffectScript) return
        existingScript.stormingCells.addAll(combatEffectTypes.instantiateHyperstormCells(Global.getCombatEngine(), 1f, true))
    }

    override fun createTerrainEffect(station: ShipAPI, engine: CombatEngineAPI) {
        val script = combatEffectTypes.HYPERSPACE.createInformedEffectInstance(combatEffectTypes.instantiateHyperstormCells(engine, 1f, true)) as deepHyperspaceEffectScript
        script.terrainName = "Ionized Nebulae"
        script.start()
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "Prior to battle, the station can release clouds of ionized gas via modified cargo shuttles, altering the battlespace in a way similar to a hyperspace storm.",
            5f
        )

        tooltip.addPara(
            "Disperses nebulae around the station, which %s. The station is %s to the clouds, as they %s.",
            5f,
            Misc.getHighlightColor(),
            "strike nearby ships with lightning", "mostly immune", "ignore stationary targets"
        )
    }
}

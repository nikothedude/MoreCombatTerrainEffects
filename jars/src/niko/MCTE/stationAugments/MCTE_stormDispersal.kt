package niko.MCTE.stationAugments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.deepHyperspaceEffectScript

class MCTE_stormDispersal(market: MarketAPI?, id: String) : MCTE_terrainAugment(market, id) {

    override var classOfScript: Class<out baseTerrainEffectScript>? = deepHyperspaceEffectScript::class.java
    override val augmentCost: Float = 10f

    override val name: String = "Storm Dispersal System"
    override val spriteId: String = "graphics/icons/industry/mining.png"

    override fun modifyScript(existingScript: baseTerrainEffectScript) {
        if (existingScript !is deepHyperspaceEffectScript) return
        existingScript.stormingCells.addAll(combatEffectTypes.instantiateHyperstormCells(Global.getCombatEngine(), 1f, true))
    }

    override fun createTerrainEffect(station: ShipAPI, engine: CombatEngineAPI) {
        val script = combatEffectTypes.HYPERSPACE.createInformedEffectInstance(combatEffectTypes.instantiateHyperstormCells(engine, 1f, true))
        script.start()
    }
}

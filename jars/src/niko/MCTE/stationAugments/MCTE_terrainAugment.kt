package niko.MCTE.stationAugments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.terrainScriptsTracker
import niko_SA.augments.core.stationAttachment

abstract class MCTE_terrainAugment: stationAttachment() {

    abstract var classOfScript: Class<out baseTerrainEffectScript>?

    override fun applyInCombat(station: ShipAPI) {
        val engine = Global.getCombatEngine()
        if (modifyExistingScript(station, engine)) return
        createTerrainEffect(station, engine)
    }

    open fun modifyExistingScript(station: ShipAPI, engine: CombatEngineAPI): Boolean {
        val existingScript = getExistingScript(engine) ?: return false
        modifyScript(existingScript)
        return true
    }

    abstract fun modifyScript(existingScript: baseTerrainEffectScript)
    abstract fun createTerrainEffect(station: ShipAPI, engine: CombatEngineAPI)

    open fun getExistingScript(engine: CombatEngineAPI): baseTerrainEffectScript? {
        return terrainScriptsTracker.terrainScripts[classOfScript]?.randomOrNull()
    }

    override fun getBlueprintValue(): Int {
        return 30000
    }

}
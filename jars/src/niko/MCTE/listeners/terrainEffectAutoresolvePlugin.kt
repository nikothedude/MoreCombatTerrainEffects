package niko.MCTE.listeners

import com.fs.starfarer.api.campaign.listeners.CoreAutoresolveListener
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.terrainEffectScriptAdder
import niko.MCTE.utils.MCTE_debugUtils

class terrainEffectAutoresolvePlugin: CoreAutoresolveListener {
    override fun modifyDataForFleet(data: BattleAutoresolverPluginImpl.FleetAutoresolveData?) {
        if (data == null) return

        val battle = data.fleet.battle
        if (battle == null) {
            MCTE_debugUtils.displayError("my assumption was wrong")
            return
        }
        val scripts = terrainEffectScriptAdder.getTerrainAffectingBattle(battle)
        combatEffectTypes.values().forEach { it.modifyAutoresolve(data, scripts, battle) }
    }
}
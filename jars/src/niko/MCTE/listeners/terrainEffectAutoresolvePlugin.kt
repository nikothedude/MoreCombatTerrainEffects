package niko.MCTE.listeners

import com.fs.starfarer.api.campaign.listeners.CoreAutoresolveListener
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl

class terrainEffectAutoresolvePlugin: CoreAutoresolveListener {
    override fun modifyDataForFleet(data: BattleAutoresolverPluginImpl.FleetAutoresolveData?) {
        if (data == null) return


    }
}
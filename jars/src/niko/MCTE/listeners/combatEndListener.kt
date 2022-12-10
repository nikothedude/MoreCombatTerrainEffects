package niko.MCTE.listeners

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import niko.MCTE.utils.terrainScriptsTracker

class combatEndListener(permaRegister: Boolean) : BaseCampaignEventListener(permaRegister) {

    override fun reportPlayerEngagement(result: EngagementResultAPI?) {
        for (script in ArrayList(terrainScriptsTracker.activeScripts)) {
            script.stop()
        }
    }

}
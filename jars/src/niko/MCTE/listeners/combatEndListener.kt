package niko.MCTE.listeners

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import niko.MCTE.utils.terrainScriptsTracker

class combatEndListener(permaRegister: Boolean) : BaseCampaignEventListener(permaRegister) {

    override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        super.reportBattleOccurred(primaryWinner, battle)
        if (battle?.isPlayerInvolved == true) {
            for (script in ArrayList(terrainScriptsTracker.activeScripts)) {
                script.stop()
            }
        }
    }
}
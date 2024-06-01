package niko.MCTE

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.SectorEntityToken
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_ids.cachedInteractionTargetId

class niko_MCTE_campaignPlugin: BaseCampaignPlugin() {
    // hijacking as a listener
    override fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken?): PluginPick<InteractionDialogPlugin>? {
        if (interactionTarget == null) return null
        Global.getSector().memoryWithoutUpdate.set(cachedInteractionTargetId, interactionTarget, 0f)

        return null
    }
}
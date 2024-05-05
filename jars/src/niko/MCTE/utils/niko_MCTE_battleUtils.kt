package niko.MCTE.utils

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorAPI
import data.utilities.niko_MPC_debugUtils

object niko_MCTE_battleUtils {

    @JvmStatic
    fun BattleAPI.getContainingLocation(): LocationAPI? {
        if (Global.getCurrentState() != GameState.CAMPAIGN && !Global.getCombatEngine().isInCampaign) {
            niko_MPC_debugUtils.log.info("$this not in campaign, returning null for getContainingLocation()")
            return null //todo: is this a bad idea
        }
        var containingLocation: LocationAPI? = null
        val sector: SectorAPI? = Global.getSector()
        val playerFleet: CampaignFleetAPI? = sector?.playerFleet
        if (isPlayerInvolved && playerFleet != null) {
            containingLocation = Global.getSector()?.playerFleet?.containingLocation
        } else {
            for (fleet in bothSides) { //have to do this, because some fleet dont HAVE a containing location
                if (fleet.containingLocation != null) { //ideally, this will only iterate once or twice before finding a location
                    containingLocation = fleet.containingLocation
                    break //we found a location, no need to check everyone else
                }
            }
        }
        return containingLocation
    }

}
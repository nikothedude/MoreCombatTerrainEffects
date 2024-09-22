package niko.MCTE.listeners

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ids.Tags
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.terrain.ObjectiveTerrainPlugin
import niko.MCTE.utils.MCTE_ids

class objectiveTerrainAdder: BaseCampaignEventListener(false) {
    override fun reportFleetJumped(
        fleet: CampaignFleetAPI?,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {
        super.reportFleetJumped(fleet, from, to)
        if (fleet == null || !fleet.isPlayerFleet) return

        if (to == null) return
        to.destination?.containingLocation?.createObjectiveTerrain()
    }
}

fun LocationAPI.createObjectiveTerrain() {
    for (objective in getEntitiesWithTag(Tags.OBJECTIVE)) {
        if (objective.memoryWithoutUpdate[MCTE_ids.OBJECTIVE_TERRAIN_MEMID] != null) continue

        var params: ObjectiveTerrainPlugin.ObjectiveTerrainParams? = null
        var maxRadius = 0f

        if (objective.hasTag(Tags.COMM_RELAY)) {
            maxRadius = MCTE_settings.COMMS_RELAY_MAX_DISTANCE
        }
    }
}

fun SectorEntityToken.addObjectiveTerrain(params: ObjectiveTerrainPlugin.ObjectiveTerrainParams) {
    val terrain = containingLocation.addTerrain("MCTE_objectiveTerrain", params)
    memoryWithoutUpdate[MCTE_ids.OBJECTIVE_TERRAIN_MEMID] = terrain.customPlugin
}

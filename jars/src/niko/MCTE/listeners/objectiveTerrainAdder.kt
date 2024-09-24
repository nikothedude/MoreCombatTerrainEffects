package niko.MCTE.listeners

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.impl.campaign.ids.Tags
import niko.MCTE.ObjectiveEffect
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
        if (!MCTE_settings.OBJECTIVES_ENABLED) return
        if (fleet == null || !fleet.isPlayerFleet) return

        if (to == null) return
        to.destination?.containingLocation?.createObjectiveTerrain()
    }
}

fun LocationAPI.createObjectiveTerrain() {
    for (objective in getEntitiesWithTag(Tags.OBJECTIVE)) {
        if (objective.memoryWithoutUpdate[MCTE_ids.OBJECTIVE_TERRAIN_MEMID] != null) continue

        var params: ObjectiveTerrainPlugin.ObjectiveTerrainParams?
        var maxRadius = 0f
        var effect: ObjectiveEffect? = null

        if (objective.hasTag(Tags.COMM_RELAY)) {
            effect = ObjectiveEffect.COMMS_RELAY
        }
        if (objective.hasTag(Tags.SENSOR_ARRAY)) {
            effect = ObjectiveEffect.SENSOR_ARRAY
        }
        if (objective.hasTag(Tags.NAV_BUOY)) {
            effect = ObjectiveEffect.NAV_BUOY
        }

        if (effect != null) {
            maxRadius = effect.getMaxDistance() * 2f
            maxRadius += (effect.getMinDistance() * 2f)
            params = ObjectiveTerrainPlugin.ObjectiveTerrainParams(
                maxRadius,
                0f,
                objective,
                effect
            )
            objective.addObjectiveTerrain(params)
        }
    }
}

fun SectorEntityToken.addObjectiveTerrain(params: ObjectiveTerrainPlugin.ObjectiveTerrainParams) {
    val terrain = (containingLocation.addTerrain("MCTE_objectiveTerrain", params) as CampaignTerrainAPI)
    memoryWithoutUpdate[MCTE_ids.OBJECTIVE_TERRAIN_MEMID] = terrain.plugin
    terrain.setCircularOrbit(this, 0f, 0f, 10f)
}

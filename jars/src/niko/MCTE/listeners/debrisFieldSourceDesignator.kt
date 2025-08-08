package niko.MCTE.listeners

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import niko.MCTE.utils.MCTE_ids.MCTE_debris_info
import niko.MCTE.utils.niko_MCTE_battleUtils.getContainingLocation
import org.lwjgl.util.vector.Vector2f

class debrisFieldSourceDesignator(permaRegister: Boolean) : BaseCampaignEventListener(permaRegister) {

    override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        super.reportBattleOccurred(primaryWinner, battle)

        if (battle == null)
            return
        val containingLocation = battle.getContainingLocation() ?: return

        val fleetOneCurrentSnapshot = battle.combinedOne.fleetData.membersListCopy
        val fleetTwoCurrentSnapshot = battle.combinedTwo.fleetData.membersListCopy

        Global.getSector().addScript(
            delayedDebrisFieldChecker(battle,
            primaryWinner,
            battle.snapshotSideOne,
            battle.snapshotSideTwo,
            fleetOneCurrentSnapshot,
            fleetTwoCurrentSnapshot,
            containingLocation,
            battle.computeCenterOfMass())
        )
    }
}

class delayedDebrisFieldChecker(
    val battle: BattleAPI,
    val winnerOfRound: CampaignFleetAPI?,
    val snapshotSideOne: MutableList<CampaignFleetAPI>,
    val snapshotSideTwo: MutableList<CampaignFleetAPI>,
    val fleetOneCurrentSnapshot: MutableList<FleetMemberAPI>,
    val fleetTwoCurrentSnapshot: MutableList<FleetMemberAPI>,
    val containingLocation: LocationAPI,
    val location: Vector2f): EveryFrameScript {

    val delay = 1
    var timesRan = 0
    var done = false

    override fun isDone(): Boolean = done

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        timesRan++
        if (timesRan < delay)
            return
        checkForDebris()
        delete()
    }

    private fun checkForDebris() {
        for (terrain in containingLocation.terrainCopy) {
            val plugin = terrain.plugin
            if (plugin !is DebrisFieldTerrainPlugin) continue
            //if (!terrain.young) continue // todo
            if (plugin.params.source != DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE) continue
            if (!plugin.containsPoint(location, plugin.params.middleRadius)) continue
            if (terrain.memoryWithoutUpdate[MCTE_debris_info] != null) continue

            debrisFound(plugin, terrain)
            return
        }
    }

    private fun debrisFound(plugin: DebrisFieldTerrainPlugin, terrain: CampaignTerrainAPI) {

        val variantDifference: MutableList<ShipVariantAPI> = ArrayList()
        for (member in fleetOneCurrentSnapshot + fleetTwoCurrentSnapshot) variantDifference += member.variant
        /*for (fleet in snapshotSideOne) {
            for (member in fleet.fleetData.membersInPriorityOrder) {
                var lostInCombat = true
                for (memberTwo in fleetOneCurrentSnapshot) {
                    if (memberTwo.id == member.id) {
                        lostInCombat = false
                        break
                    }
                }
                if (lostInCombat) {
                    variantDifference += member.variant
                }
            }
        }
        for (fleet in snapshotSideTwo) {
            for (member in fleet.fleetData.membersInPriorityOrder) {
                var lostInCombat = true
                for (memberTwo in fleetTwoCurrentSnapshot) {
                    if (memberTwo.id == member.id) {
                        lostInCombat = false
                        break
                    }
                }
                if (lostInCombat) {
                    variantDifference += member.variant
                }
            }
        }*/

        terrain.memoryWithoutUpdate[MCTE_debris_info] = debrisFieldCreationData(
            variantDifference
        )
    }

    fun delete() {
        done = true
        Global.getSector().removeScript(this)
    }
}

data class debrisFieldCreationData(
    val variantsPotentiallyLost: MutableList<ShipVariantAPI>
)
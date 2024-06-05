package niko.MCTE.nexerelin

import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.ui.TooltipMakerAPI
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.plugins.BaseGroundBattlePlugin
import niko.MCTE.utils.MCTE_ids

abstract class groundBattleTerrainEffect: BaseGroundBattlePlugin() {
    // Open so things can override that dont strictly use terrain.
    open fun currentlyApplying(): Boolean {
        if (getAffectingTerrain().isEmpty()) return false
        return true
    }

    open fun getAffectingTerrain(): MutableSet<CampaignTerrainAPI> {
        val affectingTerrain: MutableSet<CampaignTerrainAPI> = HashSet()

        val entity = getEntity() ?: return affectingTerrain
        val containingLoc = entity.containingLocation ?: return affectingTerrain

        for (terrain in containingLoc.terrainCopy) {
            if (terrainIsAffecting(terrain)) affectingTerrain += terrain
        }
        return affectingTerrain
    }

    abstract fun terrainIsAffecting(terrain: CampaignTerrainAPI): Boolean

    fun getEntity(): SectorEntityToken? = intel.market?.primaryEntity

    fun createLogInstance(): TerrainGroundBattleLog {
        val log = TerrainGroundBattleLog(intel)
        log.params[MCTE_ids.NEX_TERRAIN_BATTLE_LOG_PLUGIN_ID] = this
        return log
    }

    open fun writeLog(tooltip: TooltipMakerAPI, log: TerrainGroundBattleLog) {
        return
    }
}
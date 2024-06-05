package niko.MCTE.nexerelin

import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.utilities.niko_MPC_ids
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.GroundBattleLog
import niko.MCTE.utils.MCTE_ids

class TerrainGroundBattleLog(intel: GroundBattleIntel, type: String = "TerrainLog", turn: Int = intel.turnNum): GroundBattleLog(intel, type, turn) {
    override fun writeLog(tooltip: TooltipMakerAPI?) {

        if (tooltip == null) return

        val plugin: groundBattleTerrainEffect = params[MCTE_ids.NEX_TERRAIN_BATTLE_LOG_PLUGIN_ID] as groundBattleTerrainEffect
        plugin.writeLog(tooltip, this)
    }
}
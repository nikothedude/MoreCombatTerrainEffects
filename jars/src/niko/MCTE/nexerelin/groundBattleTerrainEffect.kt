package niko.MCTE.nexerelin

import exerelin.campaign.intel.groundbattle.GroundBattleIntel

abstract class groundBattleTerrainEffect() {
    abstract fun apply()
    abstract fun unapply()
}
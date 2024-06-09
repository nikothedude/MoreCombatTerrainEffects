package niko.MCTE

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaAkaMainyuTerrainPlugin
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.settings.MCTE_settings.loadAllSettings
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.settings.MCTE_settings.loadSettings
import niko.MCTE.utils.MCTE_debugUtils.YRXPenabled
import org.apache.log4j.Level
import org.apache.log4j.lf5.LogLevel

class niko_MCTE_modPlugin : BaseModPlugin() {

    override fun onApplicationLoad() {
        super.onApplicationLoad()

        try {
            loadAllSettings()
        } catch (ex: Exception) {
            MCTE_debugUtils.displayError("onApplicationLoad loadSettings exception caught, logging info", logType = Level.ERROR)
            MCTE_debugUtils.log.debug("info:", ex)
        }

        MCTE_debugUtils.graphicsLibEnabled = Global.getSettings().modManager.isModEnabled("shaderLib")
        MCTE_debugUtils.KOLenabled = Global.getSettings().modManager.isModEnabled("knights_of_ludd")
        MCTE_debugUtils.MPCenabled = Global.getSettings().modManager.isModEnabled("niko_morePlanetaryConditions")
        YRXPenabled = Global.getSettings().modManager.isModEnabled("yrxp")
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        LunaSettings.addSettingsListener(settingsChangedListener())
        Global.getSector().registerPlugin(niko_MCTE_campaignPlugin())

        //Global.getSector().addTransientListener(debrisFieldSourceDesignator(false))
    }

    class settingsChangedListener : LunaSettingsListener {
        override fun settingsChanged(modID: String) {
            try {
                loadAllSettings()
            } catch (ex: Exception) {
                MCTE_debugUtils.displayError("settingsChangedListener exception caught, logging info", logType = Level.ERROR)
                MCTE_debugUtils.log.debug("info:", ex)
            }
        }
    }
}
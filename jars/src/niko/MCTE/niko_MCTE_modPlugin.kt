package niko.MCTE

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaAkaMainyuTerrainPlugin
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.settings.MCTE_settings.loadSettings
import org.apache.log4j.Level
import org.apache.log4j.lf5.LogLevel

class niko_MCTE_modPlugin : BaseModPlugin() {

    override fun onApplicationLoad() {
        super.onApplicationLoad()

        try {
            loadSettings()
        } catch (ex: Exception) {
            MCTE_debugUtils.displayError("onApplicationLoad loadSettings exception caught, logging info", logType = Level.ERROR)
            MCTE_debugUtils.log.debug("info:", ex)
        }

        MCTE_debugUtils.graphicsLibEnabled = Global.getSettings().modManager.isModEnabled("shaderLib")
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        //Global.getSector().listenerManager.addListener(settingsChangedListener(), true)
        LunaSettings.addSettingsListener(settingsChangedListener())

        //Global.getSector().listenerManager.addListener(combatEndListener(false), true)
    }

    class settingsChangedListener : LunaSettingsListener {
        override fun settingsChanged(modID: String) {
            try {
                loadSettings()
            } catch (ex: Exception) {
                MCTE_debugUtils.displayError("settingsChangedListener exception caught, logging info", logType = Level.ERROR)
                MCTE_debugUtils.log.debug("info:", ex)
            }
        }
    }
}
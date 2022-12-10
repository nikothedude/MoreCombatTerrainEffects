package niko.MCTE

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.listeners.combatEndListener
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.settings.MCTE_settings.loadSettings
import org.apache.log4j.Level

class niko_MCTE_modPlugin : BaseModPlugin() {

    @Throws(RuntimeException::class)
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

        Global.getSector().listenerManager.addListener(settingsChangedListener(), true)
        Global.getSector().listenerManager.addListener(combatEndListener(false), true)
    }

    class settingsChangedListener : LunaSettingsListener {
        override fun settingsChanged() {
            try {
                loadSettings()
            } catch (ex: Exception) {
                MCTE_debugUtils.displayError("settingsChangedListener exception caught, logging info", logType = Level.ERROR)
                MCTE_debugUtils.log.debug("info:", ex)
            }
        }
    }
}
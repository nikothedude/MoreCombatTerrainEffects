package niko.MCTE

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_settings.loadSettings

class niko_MCTE_modPlugin : BaseModPlugin() {
    @Throws(Exception::class)
    override fun onApplicationLoad() {
        super.onApplicationLoad()

        try {
            loadSettings()
        } catch (ex: Exception) {
            throw RuntimeException(MCTE_ids.masterConfig + " loading failed during application load! Exception: " + ex)
        }

        MCTE_debugUtils.graphicsLibEnabled = Global.getSettings().modManager.isModEnabled("shaderLib")
    }
}
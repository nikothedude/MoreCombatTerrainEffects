package niko.MCTE

import com.fs.starfarer.api.BaseModPlugin
import niko.MCTE.utils.MCPE_ids
import niko.MCTE.utils.MCPE_settings.loadSettings

class niko_MCTE_modPlugin : BaseModPlugin() {
    @Throws(Exception::class)
    override fun onApplicationLoad() {
        super.onApplicationLoad()

        try {
            loadSettings()
        } catch (ex: Exception) {
            throw RuntimeException(MCPE_ids.masterConfig + " loading failed during application load! Exception: " + ex)
        }
    }
}
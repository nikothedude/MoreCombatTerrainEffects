package niko.MCTE.utils

import com.fs.starfarer.api.Global
import org.json.JSONException
import java.io.IOException
import kotlin.jvm.Throws

object MCPE_settings {
    var SHOW_ERRORS_IN_GAME: Boolean = true

    @JvmStatic
    @Throws(JSONException::class, IOException::class)
    fun loadSettings() {
        MCPE_debugUtils.log.info("reloading settings")
        val configJson = Global.getSettings().loadJSON(MCPE_ids.masterConfig)
        SHOW_ERRORS_IN_GAME = configJson.getBoolean("showErrorsInGame")
    }
}
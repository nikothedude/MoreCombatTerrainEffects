package niko.MCTE.utils

import com.fs.starfarer.api.Global
import org.json.JSONException
import java.io.IOException
import kotlin.jvm.Throws

object MCPE_settings {
    var SHOW_ERRORS_IN_GAME: Boolean = true
    var MAG_FIELD_EFFECT_ENABLED: Boolean = true
    var DEEP_HYPERSPACE_EFFECT_ENABLED: Boolean = true
    var SLIPSTREAM_EFFECT_ENABLED: Boolean = true
    var DEBRIS_FIELD_EFFECT_ENABLED: Boolean = true
    var DUST_CLOUD_EFFECT_ENABLED: Boolean = true
    var EXTRA_NEBULA_EFFECTS_ENABLED: Boolean = true

    @JvmStatic
    @Throws(JSONException::class, IOException::class)
    fun loadSettings() {
        MCPE_debugUtils.log.info("reloading settings")
        val configJson = Global.getSettings().loadJSON(MCPE_ids.masterConfig)
        SHOW_ERRORS_IN_GAME = configJson.getBoolean("showErrorsInGame")
        MAG_FIELD_EFFECT_ENABLED: Boolean = configJson.getBoolean("enableMagFieldEffect")
        DEEP_HYPERSPACE_EFFECT_ENABLED: Boolean = configJson.getBoolean("enableDeepHyperspaceEffect")
        SLIPSTREAM_EFFECT_ENABLED: Boolean = configJson.getBoolean("enableSlipstreamEffect")
        DEBRIS_FIELD_EFFECT_ENABLED: Boolean = configJson.getBoolean("enableDebrisFieldEffect")
        DUST_CLOUD_EFFECT_ENABLED: Boolean = configJson.getBoolean("enableDustcloudEffect")
        EXTRA_NEBULA_EFFECTS_ENABLED: Boolean = configJson.getBoolean("enableExtraNebulaEffects")
    }
}

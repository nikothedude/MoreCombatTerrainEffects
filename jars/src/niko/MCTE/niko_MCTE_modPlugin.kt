package niko.MCTE

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.settings.MCTE_settings.loadAllSettings
import niko.MCTE.stationAugments.MCTE_stormDispersal
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_debugUtils.SA_enabled
import niko.MCTE.utils.MCTE_debugUtils.YRXPenabled
import niko_SA.augments.core.stationAugmentData
import niko_SA.augments.core.stationAugmentStore.allAugments
import org.apache.log4j.Level

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
        MCTE_debugUtils.SA_enabled = Global.getSettings().modManager.isModEnabled("niko_stationAugments")

        if (SA_enabled) {
            addStationAugmentsToStore()
        }
    }

    private fun addStationAugmentsToStore() {
        allAugments["MCTE_stormDispersal"] = stationAugmentData(
            { market: MarketAPI -> MCTE_stormDispersal(market, "MCTE_stormDispersal") },
            false
        )
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
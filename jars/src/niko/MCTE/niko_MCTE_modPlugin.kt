package niko.MCTE

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.codex.CodexData
import niko.MCTE.listeners.objectiveTerrainAdder
import niko.MCTE.settings.MCTE_settings.loadAllSettings
import niko.MCTE.stationAugments.MCTE_blackHoleAugment
import niko.MCTE.stationAugments.MCTE_pulsarAugment
import niko.MCTE.stationAugments.MCTE_slipstreamAugment
import niko.MCTE.stationAugments.MCTE_stormDispersal
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_debugUtils.SA_enabled
import niko.MCTE.utils.MCTE_debugUtils.YRXPenabled
import niko_SA.augments.core.stationAugmentData
import niko_SA.augments.core.stationAugmentStore.allAugments
import org.apache.log4j.Level
import niko.MCTE.listeners.objectiveTerrainAdder.Companion.createObjectiveTerrain
import niko.MCTE.listeners.terrainEffectAutoresolvePlugin

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
        MCTE_debugUtils.indEvoEnabled = Global.getSettings().modManager.isModEnabled("IndEvo")
        YRXPenabled = Global.getSettings().modManager.isModEnabled("yrxp")
        MCTE_debugUtils.SA_enabled = Global.getSettings().modManager.isModEnabled("niko_stationAugments")
        MCTE_debugUtils.nexEnabled = Global.getSettings().modManager.isModEnabled("nexerelin")

        if (SA_enabled) {
            addStationAugmentsToStore()
        }
    }

    private fun addStationAugmentsToStore() {
        allAugments["MCTE_stormDispersal"] = stationAugmentData(
            { market: MarketAPI? -> MCTE_stormDispersal(market, "MCTE_stormDispersal") },
            hashSetOf(Factions.TRITACHYON),
            mutableMapOf(Pair("SA_augmentRare", 10f)),
            0.1f
        )
        allAugments["MCTE_slipstreamAugment"] = stationAugmentData(
            { market: MarketAPI? -> MCTE_slipstreamAugment(market, "MCTE_slipstreamAugment") },
            HashSet(),
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
        allAugments["MCTE_blackholeAugment"] = stationAugmentData(
            { market: MarketAPI? -> MCTE_blackHoleAugment(market, "MCTE_blackholeAugment") },
            HashSet(),
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
        allAugments["MCTE_pulsarAugment"] = stationAugmentData(
            { market: MarketAPI? -> MCTE_pulsarAugment(market, "MCTE_pulsarAugment") },
            HashSet(),
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        LunaSettings.addSettingsListener(settingsChangedListener())
        Global.getSector().registerPlugin(niko_MCTE_campaignPlugin())
        Global.getSector().addTransientListener(objectiveTerrainAdder())
        Global.getSector().listenerManager.addListener(terrainEffectAutoresolvePlugin(), true)

        Global.getSector().playerFleet?.containingLocation?.createObjectiveTerrain()
        //Global.getSector().addTransientListener(debrisFieldSourceDesignator(false))
    }

    override fun onAboutToStartGeneratingCodex() {
        super.onAboutToStartGeneratingCodex()
        CodexData.addCodexInfo()
    }

    override fun onAboutToLinkCodexEntries() {
        super.onAboutToLinkCodexEntries()
        CodexData.linkCodexInfo()
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
package niko.MCTE.nexerelin

import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import exerelin.campaign.intel.groundbattle.plugins.BaseGroundBattlePlugin
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magneticFieldEffect

class groundBattleTerrainScanner: BaseGroundBattlePlugin() {

    override fun advance(days: Float) {
        super.advance(days)

        if (intel.market.containingLocation == null) return
        if (intel.market.primaryEntity == null) return

        evaluateTerrainAndAddEffects()
    }

    override fun apply() {
        super.apply()

        for (plugin in plugins) plugin.apply(this)
    }

    override fun unapply() {
        super.unapply()

        for (plugin in plugins) plugin.unapply(this)
    }

    fun getAffectingTerrain(): MutableSet<CampaignTerrainAPI> {
        if (intel.market.containingLocation == null) return HashSet()
        if (intel.market.primaryEntity == null) return HashSet()

        val affectingTerrain: MutableSet<CampaignTerrainAPI> = HashSet()

        val market = intel.market
        val primaryEntity = market.primaryEntity

        for (terrain: CampaignTerrainAPI in market.containingLocation.terrainCopy) {
            if (terrain.plugin.containsEntity(primaryEntity)) {
                affectingTerrain += terrain
            }
        }
        return affectingTerrain
    }

    override fun getModifierTooltip(): TooltipMakerAPI.TooltipCreator {
        val affectingTerrain = getAffectingTerrain()
        if (affectingTerrain.isEmpty()) return super.getModifierTooltip()

        val tooltipCreator =



        return tooltipCreator
    }

    private fun evaluateTerrainAndAddEffects() {
        val market = intel.market
        val primaryEntity = market.primaryEntity

        for (terrain: CampaignTerrainAPI in market.containingLocation.terrainCopy) {
            if (terrain.plugin.containsEntity(primaryEntity)) {
                addEffect(market, primaryEntity, terrain)
            }
        }
    }

    private fun addEffect(market: MarketAPI, primaryEntity: SectorEntityToken, terrain: CampaignTerrainAPI) {
        val newEffect = classesToPlugins[terrain.plugin.javaClass]!!.getDeclaredConstructor().newInstance(this)
    }

}
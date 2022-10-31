package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.SlipstreamTerrainPlugin
import com.fs.starfarer.api.input.InputEventAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magFieldNotification
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magneticFieldEffect
import org.lwjgl.util.vector.Vector2f

// script to dodge plugin incompatability
class terrainEffectScriptAdder: BaseEveryFrameCombatPlugin() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        val engine = Global.getCombatEngine() ?: return
        val playerFleet = Global.getSector().playerFleet ?: return
        val playerLocation = playerFleet.containingLocation ?: return
        val playerCoordinates = playerFleet.location ?: return

        evaluateTerrainAndAddScripts(engine, playerFleet, playerLocation, playerCoordinates)

        engine.removePlugin(this)
    }

    private fun evaluateTerrainAndAddScripts(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f) {

        addSlipstreamScripts(engine, playerFleet, playerLocation, playerCoordinates)
        addMagneticFieldScripts(engine, playerFleet, playerLocation)
        addPulsarBeamScripts(engine, playerFleet, playerLocation, playerCoordinates)
        addDebrisFieldScripts(engine, playerFleet, playerLocation, playerCoordinates)
        addNebulaScripts(engine, playerFleet, playerLocation, playerCoordinates)
        addHyperspaceTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates)
    }

    private fun addHyperspaceTerrainScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f) {

    }

    private fun addNebulaScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f) {

    }

    private fun addDebrisFieldScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f) {

    }

    private fun addSlipstreamScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f) {

    }

    private fun addMagneticFieldScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI) {

        var isStorm = false
        var visionMod = 1f
        var missileMod = 1f
        var rangeMod = 1f
        var eccmChanceMod = 1f
        var missileBreakLockBaseChance = 0f
        var canAddPlugin = false
        val magneticFields: MutableSet<CampaignTerrainPlugin> = HashSet()
        for (terrain: CampaignTerrainAPI in playerLocation.terrainCopy) {
            val plugin = terrain.plugin
            if (plugin is MagneticFieldTerrainPlugin) {
                if (!plugin.containsEntity(playerFleet)) continue
                val isInFlare = (plugin.terrainName == "Magnetic Storm")
                if (isInFlare) isStorm = true

                visionMod *= if (isInFlare) 0.3f else 0.7f
                missileMod *= if (isInFlare) 0.1f else 0.7f
                rangeMod *= if (isInFlare) 0.28f else 0.6f
                eccmChanceMod *= if (isInFlare) 0.2f else 0.7f
                missileBreakLockBaseChance += if (isInFlare) 0.7f else 0.2f
                canAddPlugin = true
                magneticFields += plugin
            }
        }
        if (canAddPlugin) {
            missileBreakLockBaseChance = missileBreakLockBaseChance.coerceAtMost(1f)
            if (isStorm) {
                Global.getSoundPlayer().playUILoop("terrain_magstorm", 1f, 0.1f)
            } else {
                Global.getSoundPlayer().playUILoop("terrain_magfield", 1f, 0.1f)
            }
            engine.addPlugin(magneticFieldEffect(
                isStorm,
                visionMod,
                missileMod,
                rangeMod,
                eccmChanceMod,
                missileBreakLockBaseChance))
        }
    }

    private fun addPulsarBeamScripts(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f) {
        for (terrain: CampaignTerrainAPI in playerLocation.terrainCopy) {
            val plugin = terrain.plugin
            if (plugin is PulsarBeamTerrainPlugin) {
                if (!plugin.containsEntity(playerFleet)) continue
                val intensity = plugin.getIntensityAtPoint(playerCoordinates)

            }
        }
    }
}

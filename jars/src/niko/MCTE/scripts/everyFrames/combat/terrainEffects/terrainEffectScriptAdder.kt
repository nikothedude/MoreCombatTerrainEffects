package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.terrain.*
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.input.InputEventAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magneticFieldEffect
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream.SlipstreamEffectScript
import niko.MCTE.utils.MCPE_settings.DEBRIS_FIELD_EFFECT_ENABLED
import niko.MCTE.utils.MCPE_settings.DEEP_HYPERSPACE_EFFECT_ENABLED
import niko.MCTE.utils.MCPE_settings.DUST_CLOUD_EFFECT_ENABLED
import niko.MCTE.utils.MCPE_settings.MAG_FIELD_EFFECT_ENABLED
import niko.MCTE.utils.MCPE_settings.SLIPSTREAM_EFFECT_ENABLED
import niko.MCTE.utils.MCPE_settings.loadSettings
import org.lwjgl.util.vector.Vector2f
import java.util.*
import kotlin.collections.HashSet

// script to dodge plugin incompatability
class terrainEffectScriptAdder: baseNikoCombatScript() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        loadSettings()

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

        val magneticFieldPlugins: MutableSet<MagneticFieldTerrainPlugin> = HashSet()
        val slipstreamPlugins: MutableSet<SlipstreamTerrainPlugin2> = HashSet()
        val debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin> = HashSet()
        val hyperspaceTerrainPlugins: MutableSet<HyperspaceTerrainPlugin> = HashSet()
        val ringTerrainPlugins: MutableSet<RingSystemTerrainPlugin> = HashSet()

        for (terrain: CampaignTerrainAPI in playerLocation.terrainCopy) {
            val terrainPlugin = terrain.plugin
                if (terrainPlugin.containsEntity(playerFleet)) {
                if (terrainPlugin is MagneticFieldTerrainPlugin) magneticFieldPlugins += terrainPlugin
                if (terrainPlugin is SlipstreamTerrainPlugin2) slipstreamPlugins += terrainPlugin
                if (terrainPlugin is DebrisFieldTerrainPlugin) debrisFieldPlugins += terrainPlugin
                if (terrainPlugin is HyperspaceTerrainPlugin) hyperspaceTerrainPlugins += terrainPlugin
                if (terrainPlugin is RingSystemTerrainPlugin) ringTerrainPlugins += terrainPlugin
            }
        }
        addMagneticFieldScripts(engine, playerFleet, playerLocation, magneticFieldPlugins)
        addSlipstreamScripts(engine, playerFleet, playerLocation, playerCoordinates, slipstreamPlugins)
        addDebrisFieldScripts(engine, playerFleet, playerLocation, playerCoordinates, debrisFieldPlugins)
        addHyperspaceTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, hyperspaceTerrainPlugins)
        addRingSystemTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, ringTerrainPlugins)
    }

    private fun addRingSystemTerrainScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, ringTerrainPlugins: MutableSet<RingSystemTerrainPlugin>) {
        if (!DUST_CLOUD_EFFECT_ENABLED) return
    }

    private fun addHyperspaceTerrainScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, hyperspaceTerrainPlugins: MutableSet<HyperspaceTerrainPlugin>) {
        if (!DEEP_HYPERSPACE_EFFECT_ENABLED) return
    }

    private fun addDebrisFieldScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin>) {
        if (!DEBRIS_FIELD_EFFECT_ENABLED) return
    }

    private fun addSlipstreamScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, slipstreamPlugins: MutableSet<SlipstreamTerrainPlugin2>) {
        if (!SLIPSTREAM_EFFECT_ENABLED) return

        var canAddPlugin = false

        var peakPerformanceMult = 1f
        var fluxDissipationMult = 1f
        var overallSpeedMult = 1f
        var hardFluxGenerationPerFrame = 0f

        for (plugin: SlipstreamTerrainPlugin2 in slipstreamPlugins) {
            peakPerformanceMult *= 0.33f
            fluxDissipationMult *= 3
            overallSpeedMult++

            hardFluxGenerationPerFrame++
            canAddPlugin = true
        }
        if (canAddPlugin) {
            engine.addPlugin(
                SlipstreamEffectScript(
                peakPerformanceMult,
                fluxDissipationMult,
                hardFluxGenerationPerFrame,
                overallSpeedMult
            )
            )
        }
    }

    private fun addMagneticFieldScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, magneticFieldPlugins: MutableSet<MagneticFieldTerrainPlugin>) {
        if (!MAG_FIELD_EFFECT_ENABLED) return

        var isStorm = false
        var visionMod = 1f
        var missileMod = 1f
        var rangeMod = 1f
        var eccmChanceMod = 1f
        var missileBreakLockBaseChance = 0f
        var canAddPlugin = false
        for (plugin: MagneticFieldTerrainPlugin in magneticFieldPlugins) {
            val isInFlare = (plugin.terrainName == "Magnetic Storm")
            if (isInFlare) isStorm = true

            visionMod *= if (isInFlare) 0.3f else 0.6f
            missileMod *= if (isInFlare) 0.45f else 0.8f
            rangeMod *= if (isInFlare) 0.28f else 0.8f
            eccmChanceMod *= if (isInFlare) 0.2f else 0.8f
            missileBreakLockBaseChance += if (isInFlare) 0.3f else 0.05f
            canAddPlugin = true
        }
        if (canAddPlugin) {
            missileBreakLockBaseChance = missileBreakLockBaseChance.coerceAtMost(1f)
            engine.addPlugin(magneticFieldEffect(
                isStorm,
                visionMod,
                missileMod,
                rangeMod,
                eccmChanceMod,
                missileBreakLockBaseChance))
        }
    }
}

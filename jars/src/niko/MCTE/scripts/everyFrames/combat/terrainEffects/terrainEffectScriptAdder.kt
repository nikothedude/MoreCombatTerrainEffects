package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.terrain.*
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin.CellStateTracker
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.input.InputEventAPI
import data.scripts.campaign.terrain.niko_MPC_mesonField
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.blackHole.blackHoleEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.cloudCell
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.deepHyperspaceEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.dustCloud.dustCloudEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magneticFieldEffect
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.mesonField.mesonFieldEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.pulsarBeam.pulsarEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream.SlipstreamEffectScript
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.BLACKHOLE_TIMEMULT_MULT
import niko.MCTE.settings.MCTE_settings.BLACK_HOLE_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.DEEP_HYPERSPACE_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.DUST_CLOUD_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.MAGFIELD_ECCM_MULT
import niko.MCTE.settings.MCTE_settings.MAGFIELD_MISSILE_MULT
import niko.MCTE.settings.MCTE_settings.MAGFIELD_MISSILE_SCRAMBLE_CHANCE
import niko.MCTE.settings.MCTE_settings.MAGFIELD_RANGE_MULT
import niko.MCTE.settings.MCTE_settings.MAGFIELD_VISION_MULT
import niko.MCTE.settings.MCTE_settings.MAGSTORM_ECCM_MULT
import niko.MCTE.settings.MCTE_settings.MAGSTORM_MISSILE_MULT
import niko.MCTE.settings.MCTE_settings.MAGSTORM_MISSILE_SCRAMBLE_CHANCE
import niko.MCTE.settings.MCTE_settings.MAGSTORM_RANGE_MULT
import niko.MCTE.settings.MCTE_settings.MAGSTORM_VISION_MULT
import niko.MCTE.settings.MCTE_settings.MAG_FIELD_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.MESON_FIELD_VISION_MULT
import niko.MCTE.settings.MCTE_settings.MESON_FIELD_WEAPON_RANGE_INCREMENT
import niko.MCTE.settings.MCTE_settings.MESON_STORM_SYSTEM_RANGE_MULT
import niko.MCTE.settings.MCTE_settings.MESON_STORM_VISION_MULT
import niko.MCTE.settings.MCTE_settings.MESON_STORM_WEAPON_RANGE_INCREMENT
import niko.MCTE.settings.MCTE_settings.MESON_STORM_WING_RANGE_INCREMENT
import niko.MCTE.settings.MCTE_settings.PULSAR_DAMAGE_INCREMENT
import niko.MCTE.settings.MCTE_settings.PULSAR_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.PULSAR_EMP_CHANCE_INCREMENT
import niko.MCTE.settings.MCTE_settings.PULSAR_EMP_DAMAGE_BONUS_FOR_WEAPONS_INCREMENT
import niko.MCTE.settings.MCTE_settings.PULSAR_EMP_DAMAGE_INCREMENT
import niko.MCTE.settings.MCTE_settings.PULSAR_HARDFLUX_GEN_INCREMENT
import niko.MCTE.settings.MCTE_settings.PULSAR_INTENSITY_BASE_MULT
import niko.MCTE.settings.MCTE_settings.PULSAR_SHIELD_DESTABILIZATION_MULT_INCREMENT
import niko.MCTE.settings.MCTE_settings.SLIPSTREAM_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.SLIPSTREAM_FLUX_DISSIPATION_MULT
import niko.MCTE.settings.MCTE_settings.SLIPSTREAM_HARDFLUX_GEN_PER_FRAME
import niko.MCTE.settings.MCTE_settings.SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT
import niko.MCTE.settings.MCTE_settings.SLIPSTREAM_PPT_MULT
import niko.MCTE.settings.MCTE_settings.loadSettings
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.terrainScriptsTracker
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f

// script to dodge plugin incompatability
class terrainEffectScriptAdder: baseNikoCombatScript() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        terrainScriptsTracker.terrainScripts.clear()
        loadSettings()

        val engine = Global.getCombatEngine() ?: return

        if (engine.isMission && engine.missionId?.isNotEmpty() == true) {
           // evaluateMissionParameters(engine, engine.missionId)
        } else if (!terrainEffectsBlocked()){
            val playerFleet = Global.getSector()?.playerFleet ?: return
            val playerLocation = playerFleet.containingLocation ?: return
            val playerCoordinates = playerFleet.location ?: return
            evaluateTerrainAndAddScripts(engine, playerFleet, playerLocation, playerCoordinates)
            evaluateStrategicEmplacements(engine, playerFleet, playerLocation, playerCoordinates)
        }

        engine.removePlugin(this)
    }

    /// Should approximate if the current battleplugin disallowed terrain effects, and return the result
    private fun terrainEffectsBlocked(): Boolean {
        if (MCTE_settings.BLOCK_EFFECTS_ON_ENTITY_INTERACTION) {
            val cachedEntity =
                Global.getSector().memoryWithoutUpdate[MCTE_ids.cachedInteractionTargetId] as? SectorEntityToken
            if (cachedEntity != null && cachedEntity.hasTag(Tags.PROTECTS_FROM_CORONA_IN_BATTLE)) return true
        }

        if (MCTE_settings.BLOCK_EFFECTS_ON_ENTITY_PROXIMITY) {
            val playerFleet = Global.getSector().playerFleet
            val containingLocation = playerFleet.containingLocation

            for (entity in containingLocation.getEntitiesWithTag(Tags.PROTECTS_FROM_CORONA_IN_BATTLE)) {
                val entityRadius = entity.radius
                val playerRadius = playerFleet.radius

                val dist = MathUtils.getDistance(entity, playerFleet)

                if (dist <= (entityRadius + playerRadius + 10f)) return true
            }
        }
        return false
    }

    private fun evaluateTerrainAndAddScripts(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f) {

        val magneticFieldPlugins: MutableSet<MagneticFieldTerrainPlugin> = HashSet()
        val slipstreamPlugins: MutableSet<SlipstreamTerrainPlugin2> = HashSet()
        //val debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin> = HashSet()
        val hyperspaceTerrainPlugins: MutableSet<HyperspaceTerrainPlugin> = HashSet()
        val blackHoleTerrainPlugins: MutableSet<EventHorizonPlugin> = HashSet()
        val pulsarPlugins: MutableSet<PulsarBeamTerrainPlugin> = HashSet()
        val coronaPlugins: MutableSet<StarCoronaTerrainPlugin> = HashSet()
        val ionStormPlugins: MutableSet<StarCoronaAkaMainyuTerrainPlugin> = HashSet()

        // MODDED TERRAIN
        val mesonFieldPlugins: MutableSet<CampaignTerrainPlugin> = HashSet() // cant type it correctly else itd probs crash

        for (terrain: CampaignTerrainAPI in playerLocation.terrainCopy) {
            val terrainPlugin = terrain.plugin
            if (terrainPlugin.containsEntity(playerFleet)) {
                if (terrainPlugin is PulsarBeamTerrainPlugin) pulsarPlugins += terrainPlugin
                if (terrainPlugin is MagneticFieldTerrainPlugin) magneticFieldPlugins += terrainPlugin
                if (terrainPlugin is SlipstreamTerrainPlugin2) slipstreamPlugins += terrainPlugin
              //  if (terrainPlugin is DebrisFieldTerrainPlugin) debrisFieldPlugins += terrainPlugin
                if (terrainPlugin is HyperspaceTerrainPlugin) hyperspaceTerrainPlugins += terrainPlugin
                if (terrainPlugin is EventHorizonPlugin) blackHoleTerrainPlugins += terrainPlugin
                if (terrainPlugin is StarCoronaTerrainPlugin) {
                    if (terrainPlugin is StarCoronaAkaMainyuTerrainPlugin) {
                        ionStormPlugins += terrainPlugin
                    } else {
                        coronaPlugins += terrainPlugin
                    }
                }
                // MPC
                if (MCTE_debugUtils.MPCenabled && terrainPlugin is niko_MPC_mesonField) mesonFieldPlugins += terrainPlugin
            }
        }
        addMagneticFieldScripts(engine, playerFleet, playerLocation, magneticFieldPlugins)
        addSlipstreamScripts(engine, playerFleet, playerLocation, playerCoordinates, slipstreamPlugins)
        addPulsarScripts(engine, playerFleet, playerLocation, playerCoordinates, pulsarPlugins)
      //  addDebrisFieldScripts(engine, playerFleet, playerLocation, playerCoordinates, debrisFieldPlugins)
        addHyperspaceTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, hyperspaceTerrainPlugins)
        addBlackHoleTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, blackHoleTerrainPlugins)
        addMesonFieldTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, mesonFieldPlugins)
        //addRingSystemTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, ringTerrainPlugins)
        // dust clouds already have an effect
    }

    private fun addMesonFieldTerrainScripts(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f,
        mesonFieldPlugins: MutableSet<CampaignTerrainPlugin>
    ) {
        if (!MCTE_debugUtils.MPCenabled || !MCTE_settings.MESON_FIELD_ENABLED) return
        if (mesonFieldPlugins.isEmpty()) return

        val entries = ArrayList<Boolean>()
        val plugins: MutableSet<niko_MPC_mesonField> = mesonFieldPlugins as MutableSet<niko_MPC_mesonField>
        for (plugin in plugins) {
            val flareManager = plugin.flareManager ?: continue
            if (flareManager.isInActiveFlareArc(playerFleet)) {
                entries += true
            } else {
                entries += false
            }
        }

        val mesonFieldScript = combatEffectTypes.MESONFIELD.createInformedEffectInstance(entries, 1f)
        mesonFieldScript.start()
    }

    private fun evaluateStrategicEmplacements(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f) {

        val commRelays: MutableSet<SectorEntityToken> = HashSet()
        val navBuoys: MutableSet<SectorEntityToken> = HashSet()
        val sensorRelays: MutableSet<SectorEntityToken> = HashSet()

        for (strategicObject: SectorEntityToken in playerLocation.getEntitiesWithTag(Tags.OBJECTIVE)) {
            if (strategicObject.hasTag(Tags.COMM_RELAY)) commRelays += strategicObject
            if (strategicObject.hasTag(Tags.SENSOR_ARRAY)) sensorRelays += strategicObject
            if (strategicObject.hasTag(Tags.NAV_BUOY)) navBuoys += strategicObject
        }

        //addCommRelayScripts(engine, playerFleet, playerLocation, playerCoordinates, commRelays)
        //addNavBuoyScripts(engine, playerFleet, playerLocation, playerCoordinates, navBuoys)
        //addSensorRelayScripts(engine, playerFleet, playerLocation, playerCoordinates, sensorRelays)
    }

    /*private fun addCommRelayScripts(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f,
        commRelays: MutableSet<SectorEntityToken>
    ) {

        val battle = playerFleet.battle ?: return
        var effectStrength = 0f

        for (commRelay in commRelays) {
            val distance = MathUtils.getDistance(playerCoordinates, commRelay.location)
            if (distance > COMMS_RELAY_MAX_DISTANCE) continue

            val mult = (1 - (distance / COMMS_RELAY_MAX_DISTANCE))

            //val contribution
        }

        //val
    }*/

    private fun addPulsarScripts(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f,
        pulsarPlugins: MutableSet<PulsarBeamTerrainPlugin>
    ) {
        if (!PULSAR_EFFECT_ENABLED) return

        val angleToIntensity: MutableMap<Float, Float> = HashMap()
        var canAddScript: Boolean = false

        for (plugin: PulsarBeamTerrainPlugin in pulsarPlugins) {
            val intensity = (plugin.getIntensityAtPoint(playerCoordinates))*PULSAR_INTENSITY_BASE_MULT

            var angle = (VectorUtils.getAngle(plugin.entity.location, playerCoordinates))
            angleToIntensity[angle] = intensity
            canAddScript = true
        }
        if (canAddScript) {
            val script = combatEffectTypes.PULSAR.createInformedEffectInstance(angleToIntensity, 1f)
            script.start()
        }
    }

    private fun addBlackHoleTerrainScripts(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f,
        blackHoleTerrainPlugins: MutableSet<EventHorizonPlugin>
    ) {
        if (!BLACK_HOLE_EFFECT_ENABLED) return
        if (blackHoleTerrainPlugins.isEmpty()) return

        var timeMult = 1f
        val angleToIntensity: MutableMap<Float, Float> = HashMap()
        var canAddScript: Boolean = false

        for (plugin: EventHorizonPlugin in blackHoleTerrainPlugins) {
            if (playerLocation != plugin.entity.containingLocation) continue
            val intensity = plugin.getIntensityAtPoint(playerCoordinates)
            timeMult += getBlackholeTimeMultIncrement(engine, playerFleet, playerLocation, playerCoordinates, plugin, intensity)
            angleToIntensity[(VectorUtils.getAngle(playerCoordinates, plugin.entity.location))] = intensity

            canAddScript = true
        }
        if (canAddScript) {
            val script = combatEffectTypes.BLACKHOLE.createInformedEffectInstance(angleToIntensity, timeMult)
            script.start()
        }
    }

    private fun getBlackholeTimeMultIncrement(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f,
        plugin: EventHorizonPlugin,
        intensity: Float
    ): Float {
        return intensity*BLACKHOLE_TIMEMULT_MULT
    }

    /*private fun getGravityPointOfBlackHole(
        engine: CombatEngineAPI,
        playerCoordinates: Vector2f,
        plugin: EventHorizonPlugin
    ): Vector2f {
        val maxWidth = engine.mapWidth
        val maxHeight = engine.mapHeight
        val angle = (VectorUtils.getAngle(playerCoordinates, plugin.entity.location))

        val aTemp = angle % Math.PI /2
        val radius = getRadiusOfMap()
        val amplitude = radius/cos(aTemp)
        val x = MathUtils.clamp((cos(angle) * amplitude).toFloat(), -maxWidth, maxWidth)
        val y = MathUtils.clamp((sin(angle) * amplitude).toFloat(), -maxHeight, maxHeight)

        return Vector2f(x, y)
    } */

    private fun getRadiusOfMap(): Float {
        val maxHeight = engine.mapHeight
        val maxWidth = engine.mapWidth
        val ratio = maxHeight/maxWidth
        return ((maxWidth)*ratio)
    }

    private fun addRingSystemTerrainScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, ringTerrainPlugins: MutableSet<RingSystemTerrainPlugin>) {
        if (!DUST_CLOUD_EFFECT_ENABLED) return

        var flatSpeedMalice = 0f
        val speedMaliceIncrement = 5f
        var canAddScript = false

        for (plugin: RingSystemTerrainPlugin in ringTerrainPlugins) {
            flatSpeedMalice -= speedMaliceIncrement
            canAddScript = true
        }
        if (canAddScript) {
            engine.addPlugin(
                dustCloudEffectScript(
                flatSpeedMalice
            ))
        }
    }

    private fun addHyperspaceTerrainScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, hyperspaceTerrainPlugins: MutableSet<HyperspaceTerrainPlugin>) {
        if (!DEEP_HYPERSPACE_EFFECT_ENABLED || engine.nebula == null || !engine.isInCampaign) return
        if (hyperspaceTerrainPlugins.isEmpty()) return

        val cells: MutableSet<cloudCell> = HashSet()

        for (plugin: HyperspaceTerrainPlugin in hyperspaceTerrainPlugins) {
            var isStorming = false

            val cellAtPlayer: CellStateTracker = plugin.getCellAt(playerCoordinates, 100f) ?: continue
            if (cellAtPlayer.isStorming && HYPERSTORM_EFFECT_ENABLED) {
                isStorming = true
            }
            cells.addAll(combatEffectTypes.instantiateHyperstormCells(engine, 1f, isStorming))
        }
        if (cells.isEmpty()) return
        val script = combatEffectTypes.HYPERSPACE.createInformedEffectInstance(cells)
        script.start()
    }

    /* private fun instantiateDeephyperspaceNebulae(
         pluginToStorming: HashMap<HyperspaceTerrainPlugin, Boolean>): MutableMap<NebulaParticle, Boolean> {
         engine.addNebulaParticle()

     } */

    /*private fun addDebrisFieldScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin>) {
        if (!DEBRIS_FIELD_EFFECT_ENABLED) return

        var canAddPlugin = false

        var debrisDensity = 0f
        var hazardDensityMult = 0f
        val specialSalvage = HashSet<Any>()

        for (plugin: DebrisFieldTerrainPlugin in debrisFieldPlugins) {
            val terrainEntity = plugin.entity
            val params = plugin.getParams()
            val density = params.density
            var accidentProbability: Float = (0.2f + 0.8f * (1f - density)).coerceAtMost(0.9f)
            val dropValue = plugin.entity.dropValue
            val randomDropValue = plugin.entity.dropRandom
            var lootValue = 0f
            for (data in dropValue) {
                lootValue += data.value
            }
            for (data in randomDropValue) {
                lootValue += if (data.value > 0) {
                    data.value.toFloat()
                } else {
                    500f // close enough
                }
            }
            lootValue *= density
            debrisDensity += density

            val specialSalvage = Misc.getSalvageSpecial(terrainEntity)

            //savageEntity.java

            if (density > 0f) {
                canAddPlugin = true
            }
        }

        if (canAddPlugin) {
            val representations: MutableSet<debrisFieldParamsRepresentation> = HashSet()
            for (plugin in debrisFieldPlugins) {
                representations += debrisFieldParamsRepresentation(plugin)
            }
            engine.addPlugin(
                debrisFieldEffectScript(
                    debrisDensity.toDouble(),
                    representations
            ))
        }
    }
*/
    private fun addSlipstreamScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, slipstreamPlugins: MutableSet<SlipstreamTerrainPlugin2>) {
        if (!SLIPSTREAM_EFFECT_ENABLED) return
        if (slipstreamPlugins.isEmpty()) return


        val plugin = combatEffectTypes.SLIPSTREAM.createInformedEffectInstance(1f)
        plugin.start()
    }

    private fun addMagneticFieldScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, magneticFieldPlugins: MutableSet<MagneticFieldTerrainPlugin>) {
        if (!MAG_FIELD_EFFECT_ENABLED) return
        if (magneticFieldPlugins.isEmpty()) return

        val entries = ArrayList<Boolean>()

        for (plugin in magneticFieldPlugins) {
            entries += plugin.flareManager.isInActiveFlareArc(playerFleet)
        }

        val plugin = combatEffectTypes.MAGFIELD.createInformedEffectInstance(entries, 1f)
        plugin.start()
    }
}

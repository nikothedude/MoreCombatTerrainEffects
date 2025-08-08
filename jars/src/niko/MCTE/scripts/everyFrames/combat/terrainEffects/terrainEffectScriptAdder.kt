package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.terrain.*
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin.CellStateTracker
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.terrain.niko_MPC_mesonField
import indevo.exploration.minefields.MineBeltTerrainPlugin
import indevo.industries.artillery.entities.ArtilleryStationEntityPlugin
import niko.MCTE.ObjectiveEffect
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.ArtilleryStationEffect
import niko.MCTE.scripts.everyFrames.combat.MCTEEffectScript
import niko.MCTE.scripts.everyFrames.combat.baseNikoCombatScript
import niko.MCTE.scripts.everyFrames.combat.objectiveEffects.CommsRelayEffectScript
import niko.MCTE.scripts.everyFrames.combat.objectiveEffects.NavBuoyEffectScript
import niko.MCTE.scripts.everyFrames.combat.objectiveEffects.SensorArrayEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.debrisField.debrisFieldEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.debrisField.debrisFieldParamsRepresentation
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.cloudCell
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.dustCloud.dustCloudEffectScript
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.BLACKHOLE_TIMEMULT_MULT
import niko.MCTE.settings.MCTE_settings.BLACK_HOLE_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.DEBRIS_FIELD_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.DEEP_HYPERSPACE_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.DUST_CLOUD_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.MAG_FIELD_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.PULSAR_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.PULSAR_INTENSITY_BASE_MULT
import niko.MCTE.settings.MCTE_settings.SLIPSTREAM_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.loadSettings
import niko.MCTE.terrain.ObjectiveTerrainPlugin
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_reflectionUtils
import niko.MCTE.utils.niko_MCTE_battleUtils.getContainingLocation
import niko.MCTE.utils.terrainScriptsTracker
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f

// script to dodge plugin incompatability
class terrainEffectScriptAdder: baseNikoCombatScript() {


    companion object {
        fun getTerrainAffectingBattle(battle: BattleAPI, battleLoc: LocationAPI? = null): TerrainScriptList {

            var battleLoc = battleLoc
            if (battleLoc == null) {
                battleLoc = battle.getContainingLocation() ?: return TerrainScriptList()
            }
            val terrainFocus = if (battle.isPlayerInvolved) Global.getSector().playerFleet.location else battle.computeCenterOfMass()

            val magneticFieldPlugins: MutableSet<MagneticFieldTerrainPlugin> = HashSet()
            val slipstreamPlugins: MutableSet<SlipstreamTerrainPlugin2> = HashSet()
            val debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin> = HashSet()
            val hyperspaceTerrainPlugins: MutableSet<HyperspaceTerrainPlugin> = HashSet()
            val blackHoleTerrainPlugins: MutableSet<EventHorizonPlugin> = HashSet()
            val pulsarPlugins: MutableSet<PulsarBeamTerrainPlugin> = HashSet()
            val coronaPlugins: MutableSet<StarCoronaTerrainPlugin> = HashSet()
            val ionStormPlugins: MutableSet<StarCoronaAkaMainyuTerrainPlugin> = HashSet()

            // MODDED TERRAIN
            val mesonFieldPlugins: MutableSet<CampaignTerrainPlugin> = HashSet() // cant type it correctly else itd probs crash
            val mineFieldPlugins: MutableSet<CampaignTerrainPlugin> = HashSet()

            for (terrain: CampaignTerrainAPI in battleLoc.terrainCopy) {
                val terrainPlugin = terrain.plugin
                if (terrainPlugin.containsEntity(battleLoc.createToken(terrainFocus))) {
                    if (terrainPlugin is PulsarBeamTerrainPlugin) pulsarPlugins += terrainPlugin
                    if (terrainPlugin is MagneticFieldTerrainPlugin) magneticFieldPlugins += terrainPlugin
                    if (terrainPlugin is SlipstreamTerrainPlugin2) slipstreamPlugins += terrainPlugin
                    if (terrainPlugin is DebrisFieldTerrainPlugin) debrisFieldPlugins += terrainPlugin
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
                    // INDEVO
                    if (MCTE_debugUtils.indEvoEnabled && terrainPlugin is MineBeltTerrainPlugin) mineFieldPlugins += terrainPlugin
                }
            }

            return TerrainScriptList(
                magneticFieldPlugins,
                slipstreamPlugins,
                hyperspaceTerrainPlugins,
                blackHoleTerrainPlugins,
                pulsarPlugins,
                coronaPlugins,
                ionStormPlugins,
                mesonFieldPlugins,
                mineFieldPlugins,
                debrisFieldPlugins,
            )
        }

        fun getMinefieldHostileSides(mineFieldPlugins: MutableSet<MineBeltTerrainPlugin>, battle: BattleAPI): HashMap<MineBeltTerrainPlugin, MutableSet<BattleSide>> {
            val minefieldToHostileSide = HashMap<MineBeltTerrainPlugin, MutableSet<BattleSide>>()

            for (plugin in mineFieldPlugins) {
                val market = plugin.primary
                val factionId = market.factionId

                if (battle.sideOne.any { plugin.canAttackFleet(it) }) { // this doesnt work :( it crashes :( it cant recognize the function :(
                    if (minefieldToHostileSide[plugin] == null) minefieldToHostileSide[plugin] = HashSet()
                    minefieldToHostileSide[plugin]!! += BattleSide.ONE
                }
                if (battle.sideTwo.any { plugin.canAttackFleet(it) }) {
                    if (minefieldToHostileSide[plugin] == null) minefieldToHostileSide[plugin] = HashSet()
                    minefieldToHostileSide[plugin]!! += BattleSide.TWO
                }
            }

            return minefieldToHostileSide
        }

        // i dont like that this exists but whatever lol
        private fun MineBeltTerrainPlugin.canAttackFleet(fleet: CampaignFleetAPI): Boolean {
            var friend = false

            val m = primary

            if (m != null && !m.isPlanetConditionMarketOnly) {
                friend = fleet.isPlayerFleet && m.isPlayerOwned || fleet.faction.id == m.factionId || fleet.faction.getRelationshipLevel(m.factionId).isAtWorst(RepLevel.INHOSPITABLE)
                if (!m.isPlayerOwned && fleet.isPlayerFleet && !fleet.isTransponderOn) {
                    friend = false
                }
            }
            if (fleet.memoryWithoutUpdate.contains(MemFlags.MEMORY_KEY_MISSION_IMPORTANT)) friend = true
            if (friend) return false

            for (area in disabledAreas) if (area.contains(fleet)) return false
            return true
        }
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        terrainScriptsTracker.terrainScripts.clear()
        loadSettings()

        val engine = Global.getCombatEngine() ?: return

        if ((engine.isMission && engine.missionId?.isNotEmpty() == true) || engine.isSimulation) {
           // evaluateMissionParameters(engine, engine.missionId)
            engine.removePlugin(this)
        } else {
            val playerFleet = Global.getSector()?.playerFleet ?: return
            val playerLocation = playerFleet.containingLocation ?: return
            val playerCoordinates = playerFleet.location ?: return
            if (!terrainEffectsBlocked()) {
                evaluateTerrainAndAddScripts(engine, playerFleet, playerLocation, playerCoordinates)
            }
            evaluateStrategicEmplacements(engine, playerFleet, playerLocation, playerCoordinates)
            if (MCTE_debugUtils.indEvoEnabled) {
                //evaluateArtilleryStations(engine, playerFleet, playerLocation, playerCoordinates)
            }
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
            val playerFleet = Global.getSector().playerFleet ?: return false
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
        playerCoordinates: Vector2f
    ) {

        val battle = playerFleet.battle ?: return
        val scriptList = getTerrainAffectingBattle(battle)

        addMagneticFieldScripts(engine, playerFleet, playerLocation, scriptList.magneticFieldPlugins)
        addSlipstreamScripts(engine, playerFleet, playerLocation, playerCoordinates, scriptList.slipstreamPlugins)
        addPulsarScripts(engine, playerFleet, playerLocation, playerCoordinates, scriptList.pulsarPlugins)
        addDebrisFieldScripts(engine, playerFleet, playerLocation, playerCoordinates, scriptList.debrisFieldPlugins)
        addHyperspaceTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, scriptList.hyperspaceTerrainPlugins)
        addBlackHoleTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, scriptList.blackHoleTerrainPlugins)
        addMesonFieldTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, scriptList.mesonFieldPlugins)
        addMinefieldTerrainPlugins(engine, playerFleet, playerLocation, playerCoordinates, scriptList.mineFieldPlugins)
        //addRingSystemTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, ringTerrainPlugins)
        // dust clouds already have an effect
    }

    private fun addMinefieldTerrainPlugins(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f,
        mineFieldPlugins: MutableSet<CampaignTerrainPlugin>
    ) {
        if (!MCTE_debugUtils.indEvoEnabled || !MCTE_settings.MINEFIELD_ENABLED) return
        if (mineFieldPlugins.isEmpty()) return

        val castedPlugins = (mineFieldPlugins as MutableSet<MineBeltTerrainPlugin>)
        val battle = playerFleet.battle ?: return

        val minefieldToHostileSide = getMinefieldHostileSides(castedPlugins, battle)

        val targetsToInstances = HashMap<Int, Int>()
        targetsToInstances[0] = 0 // on player side
        targetsToInstances[1] = 0 // on enemy side
        //sidesToInstances[100] = 0 // targetting both sides

        for (entry in minefieldToHostileSide) {
            val minefield = entry.key
            val sides = entry.value

            val hostileToSideOne = sides.contains(BattleSide.ONE)
            val hostileToSideTwo = sides.contains(BattleSide.TWO)
            val playerSide = battle.pickSide(playerFleet)

            if (hostileToSideOne && hostileToSideTwo) {
                targetsToInstances[0] = targetsToInstances[0]!! + 1
                targetsToInstances[1] = targetsToInstances[1]!! + 1 // attack both sides
            } else {
                if ((playerSide == BattleSide.ONE && hostileToSideOne) || (playerSide == BattleSide.TWO && hostileToSideTwo)) {
                    targetsToInstances[0] = targetsToInstances[0]!! + 1 // attack enemy
                } else {
                    targetsToInstances[1] = targetsToInstances[1]!! + 1 // attack player
                }
            }

            //sides.forEach { sidesToInstances[it] += 1}
            //val location = battle.computeCenterOfMass()
            //val entity = stationToEntity[station]!!
            //val angle = VectorUtils.getAngle(entity.location, location)

            //ArtilleryStationEffect(owner, angle, station.type).start()
        }
        if (targetsToInstances.any { it.value > 0 }) {
            combatEffectTypes.MINEFIELD.createInformedEffectInstance(targetsToInstances).start()
        }
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

        if (!MCTE_settings.OBJECTIVES_ENABLED) return

        val commRelays: MutableSet<ObjectiveTerrainPlugin> = HashSet()
        val navBuoys: MutableSet<ObjectiveTerrainPlugin> = HashSet()
        val sensorRelays: MutableSet<ObjectiveTerrainPlugin> = HashSet()

        for (terrain in playerLocation.terrainCopy.filter { it.plugin is ObjectiveTerrainPlugin }) {
            val plugin = terrain.plugin as ObjectiveTerrainPlugin
            val effect = plugin.ourParams.effect

            val objective = plugin.ourParams.relatedEntity as CustomCampaignEntityAPI
            //if (!effect.wantToAssist(playerFleet, objective)) continue
            /*val percent = effect.getPercentEffectiveness(playerFleet, objective)
            if (percent <= 0f) continue*/

            when (effect) {
                ObjectiveEffect.COMMS_RELAY -> commRelays += plugin
                ObjectiveEffect.NAV_BUOY -> navBuoys += plugin
                ObjectiveEffect.SENSOR_ARRAY -> sensorRelays += plugin
            }
        }

        addObjectiveScripts(engine, playerFleet, playerLocation, playerCoordinates, commRelays) { owner: Int, strength: Float, battle: BattleAPI ->
            CommsRelayEffectScript(
                owner,
                strength,
                battle
            )
        }
        addObjectiveScripts(engine, playerFleet, playerLocation, playerCoordinates, navBuoys) { owner: Int, strength: Float, battle: BattleAPI ->
            NavBuoyEffectScript(
                owner,
                strength,
                battle
            )
        }
        addObjectiveScripts(engine, playerFleet, playerLocation, playerCoordinates, sensorRelays) { owner: Int, strength: Float, battle: BattleAPI ->
            SensorArrayEffectScript(
                owner,
                strength,
                battle
            )
        }
    }

    private fun addObjectiveScripts(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f,
        plugins: MutableSet<ObjectiveTerrainPlugin>,
        createInstance: (owner: Int, strength: Float, battle: BattleAPI) -> MCTEEffectScript
    ) {
        val battle = playerFleet.battle ?: return
        val sideToStrength = hashMapOf(Pair(BattleSide.ONE, 0f), Pair(BattleSide.TWO, 0f))

        for (plugin in plugins) {
            val params = plugin.ourParams
            val effect = params.effect

            val objective = params.relatedEntity as CustomCampaignEntityAPI

            val commRelayFaction = objective.faction
            val dummyFleet = FleetFactory.createEmptyFleet(commRelayFaction.id, FleetTypes.PATROL_LARGE, null)
            val pickedSide = battle.pickSide(dummyFleet) ?: continue
            if (pickedSide == BattleSide.NO_JOIN) continue

            val percentStrength = (effect.getPercentEffectiveness(playerFleet, objective))
            var strength = (effect.getBaseStrength() * percentStrength)
            if (!objective.hasTag(Tags.MAKESHIFT)) {
                strength *= MCTE_settings.PRISTINE_OBJECTIVE_EFFECT_MULT
            }
            sideToStrength[pickedSide] = strength

            dummyFleet.despawn()

            for (entry in sideToStrength) {
                val side = entry.key
                val strength = entry.value
                if (strength > 0f) {
                    val owner = if (battle.isPlayerSide(battle.getSide(side))) 0 else 1
                    createInstance(owner, strength, battle).start()
                    //CommsRelayEffectScript(owner, strength, battle).start()
                }
            }
        }
    }

    private fun evaluateArtilleryStations(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f
    ) {
        val battle = playerFleet.battle ?: return
        val stationToHostileSide = HashMap<ArtilleryStationEntityPlugin, MutableSet<BattleSide>>()
        val sideToHostileStation = hashMapOf(Pair(BattleSide.ONE, HashSet<ArtilleryStationEntityPlugin>()), Pair(BattleSide.TWO, HashSet()))
        val stationToEntity = HashMap<ArtilleryStationEntityPlugin, CustomCampaignEntityAPI>()

        for (artyStation in ArtilleryStationEntityPlugin.getArtilleriesInLoc(playerLocation)) {
            val plugin = artyStation.customPlugin as ArtilleryStationEntityPlugin
            stationToEntity[plugin] = artyStation as CustomCampaignEntityAPI
            if (battle.sideOne.any { MCTE_reflectionUtils.invoke("isValid", plugin.orInitScript, it) == true }) { // this doesnt work :( it crashes :( it cant recognize the function :(
                sideToHostileStation[BattleSide.ONE]!! += plugin

                if (stationToHostileSide[plugin] == null) stationToHostileSide[plugin] = HashSet()
                stationToHostileSide[plugin]!! += BattleSide.ONE
            }
            if (battle.sideTwo.any { MCTE_reflectionUtils.invoke("isValid", plugin.orInitScript, it) == true }) {
                sideToHostileStation[BattleSide.TWO]!! += plugin

                if (stationToHostileSide[plugin] == null) stationToHostileSide[plugin] = HashSet()
                stationToHostileSide[plugin]!! += BattleSide.TWO
            }
        }

        for (entry in stationToHostileSide) {
            val station = entry.key
            val sides = entry.value

            var owner = 0

            val hostileToSideOne = sides.contains(BattleSide.ONE)
            val hostileToSideTwo = sides.contains(BattleSide.TWO)
            val playerSide = battle.pickSide(playerFleet)

            if (hostileToSideOne && hostileToSideTwo) {
                owner = 100
            } else {
                if (playerSide == BattleSide.ONE && hostileToSideOne) owner = 1
                if (playerSide == BattleSide.TWO && hostileToSideTwo) owner = 1
            }

            val location = battle.computeCenterOfMass()
            val entity = stationToEntity[station]!!
            val angle = VectorUtils.getAngle(entity.location, location)

            ArtilleryStationEffect(owner, angle, station.type).start()
        }
    }

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

    private fun addDebrisFieldScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin>) {
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
            combatEffectTypes.DEBRISFIELD.createInformedEffectInstance(
                debrisDensity,
                representations
            ).start()
        }
    }

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

        val plugin = combatEffectTypes.MAGFIELD.createInformedEffectInstance(entries, 1f, magneticFieldPlugins)
        plugin.start()
    }

    data class TerrainScriptList(
        val magneticFieldPlugins: MutableSet<MagneticFieldTerrainPlugin> = HashSet(),
        val slipstreamPlugins: MutableSet<SlipstreamTerrainPlugin2> = HashSet(),
        //val debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin> = HashSet()
        val hyperspaceTerrainPlugins: MutableSet<HyperspaceTerrainPlugin> = HashSet(),
        val blackHoleTerrainPlugins: MutableSet<EventHorizonPlugin> = HashSet(),
        val pulsarPlugins: MutableSet<PulsarBeamTerrainPlugin> = HashSet(),
        val coronaPlugins: MutableSet<StarCoronaTerrainPlugin> = HashSet(),
        val ionStormPlugins: MutableSet<StarCoronaAkaMainyuTerrainPlugin> = HashSet(),
        val mesonFieldPlugins: MutableSet<CampaignTerrainPlugin> = HashSet(), // cant type it correctly else itd probs crash
        val mineFieldPlugins: MutableSet<CampaignTerrainPlugin> = HashSet(),
        val debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin> = HashSet()
    )
}

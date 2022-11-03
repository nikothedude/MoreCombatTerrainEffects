package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.impl.campaign.terrain.*
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin.CellStateTracker
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.combat.entities.terrain.A
import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.cloudParams
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.deepHyperspaceEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.dustCloud.dustCloudEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magneticFieldEffect
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream.SlipstreamEffectScript
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_miscUtils
import niko.MCTE.utils.MCTE_miscUtils.getCellCentroid
import niko.MCTE.utils.MCTE_miscUtils.getRadiusOfCell
import niko.MCTE.utils.MCTE_settings.DEBRIS_FIELD_EFFECT_ENABLED
import niko.MCTE.utils.MCTE_settings.DEEP_HYPERSPACE_EFFECT_ENABLED
import niko.MCTE.utils.MCTE_settings.DUST_CLOUD_EFFECT_ENABLED
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_CENTROID_REFINEMENT_ITERATIONS
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_EFFECT_ENABLED
import niko.MCTE.utils.MCTE_settings.MAGFIELD_ECCM_MULT
import niko.MCTE.utils.MCTE_settings.MAGFIELD_MISSILE_MULT
import niko.MCTE.utils.MCTE_settings.MAGFIELD_MISSILE_SCRAMBLE_CHANCE
import niko.MCTE.utils.MCTE_settings.MAGFIELD_RANGE_MULT
import niko.MCTE.utils.MCTE_settings.MAGFIELD_VISION_MULT
import niko.MCTE.utils.MCTE_settings.MAGSTORM_ECCM_MULT
import niko.MCTE.utils.MCTE_settings.MAGSTORM_MISSILE_MULT
import niko.MCTE.utils.MCTE_settings.MAGSTORM_MISSILE_SCRAMBLE_CHANCE
import niko.MCTE.utils.MCTE_settings.MAGSTORM_RANGE_MULT
import niko.MCTE.utils.MCTE_settings.MAGSTORM_VISION_MULT
import niko.MCTE.utils.MCTE_settings.MAG_FIELD_EFFECT_ENABLED
import niko.MCTE.utils.MCTE_settings.MAX_HYPERCLOUDS_TO_ADD_PER_CELL
import niko.MCTE.utils.MCTE_settings.MIN_HYPERCLOUDS_TO_ADD_PER_CELL
import niko.MCTE.utils.MCTE_settings.SLIPSTREAM_EFFECT_ENABLED
import niko.MCTE.utils.MCTE_settings.SLIPSTREAM_FLUX_DISSIPATION_MULT
import niko.MCTE.utils.MCTE_settings.SLIPSTREAM_HARDFLUX_GEN_PER_FRAME
import niko.MCTE.utils.MCTE_settings.SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT
import niko.MCTE.utils.MCTE_settings.SLIPSTREAM_PPT_MULT
import niko.MCTE.utils.MCTE_settings.loadSettings
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.logging.i
import org.lwjgl.util.vector.Vector2f

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
        //addRingSystemTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, ringTerrainPlugins)
        // dust clouds already have an effect
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
        if (!DEEP_HYPERSPACE_EFFECT_ENABLED || engine.nebula == null) return
        if (MCTE_debugUtils.isMacOS()) {
            MCTE_debugUtils.log.info("Rejected hyperspace terrain due to potential macOS crashes.")
            return
        }
        val nebula = engine.nebula
        var canAddScript = false

        val pluginToStorming: HashMap<HyperspaceTerrainPlugin, Boolean> = HashMap()

        for (plugin: HyperspaceTerrainPlugin in hyperspaceTerrainPlugins) {
            var isStorming = false

            val cellAtPlayer: CellStateTracker? = plugin.getCellAt(playerCoordinates, 100f)
            if (cellAtPlayer == null) {
                MCTE_debugUtils.displayError("failed to locate cell tracker despite player being in a hypercloud")
                continue
            }
            if (cellAtPlayer.isStorming && HYPERSTORM_EFFECT_ENABLED) {
                isStorming = true
            }
            canAddScript = true
            pluginToStorming[plugin] = isStorming
        }
        if (canAddScript) {
            val deepHyperspaceNebulas: MutableMap<MutableMap<MutableSet<Cloud>, Vector2f>, Boolean> = instantiateDeephyperspaceNebulae(pluginToStorming)
            val stormingNebulae: MutableSet<MutableSet<Cloud>> = HashSet()
            val stormingNebulaeToCentroid: MutableMap<MutableSet<Cloud>, Vector2f> = HashMap()
            for (mapOfCellsToCentroid in deepHyperspaceNebulas.keys) if (deepHyperspaceNebulas[mapOfCellsToCentroid] == true) {
                stormingNebulae += mapOfCellsToCentroid.keys
                stormingNebulaeToCentroid += mapOfCellsToCentroid
            }
            val stormingNebulaeToRadius = getRadiusOfHyperstorms(stormingNebulaeToCentroid, nebula)

            val stormingNebulaeWithParams = HashMap<MutableSet<Cloud>, cloudParams>()
            for (cell: MutableSet<Cloud> in stormingNebulae) {
                val centroid = getCellCentroidRepeatadly(nebula, cell)
                if (centroid == null) {
                    MCTE_debugUtils.displayError("centroid null when making params")
                    continue
                }
                val radius = getRadiusOfCell(cell, nebula, centroid)
                val cloudParams = cloudParams(
                    centroid,
                    radius
                )
                stormingNebulaeWithParams[cell] = cloudParams
            }

            engine.addPlugin(deepHyperspaceEffectScript(stormingNebulaeWithParams))
        }
    }

    private fun getCellCentroidRepeatadly(nebula: CombatNebulaAPI?, cell: MutableSet<Cloud>): Vector2f? {
        if (nebula == null) {
            MCTE_debugUtils.displayError("nebula null during centroid repeat")
            return Vector2f(0f, 0f)
        }
        val amountOfTimes = HYPERSTORM_CENTROID_REFINEMENT_ITERATIONS
        var centroid = getCellCentroid(nebula, cell)
        var indexVal = 0
        while (indexVal < amountOfTimes) {
            indexVal++
            centroid = getCellCentroid(nebula, cell, centroid)
        }
        return centroid
    }

    fun getRadiusOfHyperstorms(cellMap: MutableMap<MutableSet<Cloud>, Vector2f>, nebula: CombatNebulaAPI): MutableMap<MutableSet<Cloud>, Float> {
        val cellsToRadius = HashMap<MutableSet<Cloud>, Float>()
        for (cell in cellMap.keys) {
            val centroid = cellMap[cell]
            if (centroid == null) {
                MCTE_debugUtils.displayError("centroid null during getRadiusOfHyperstorms in terraineffectadder")
                continue
            }
            val radius = MCTE_miscUtils.getRadiusOfCell(cell, nebula, centroid)
            cellsToRadius[cell] = radius
        }
        return cellsToRadius
    }

    private fun instantiateDeephyperspaceNebulae(pluginToStorming: MutableMap<HyperspaceTerrainPlugin, Boolean>): HashMap<MutableMap<MutableSet<Cloud>, Vector2f>, Boolean> {
        val nebulaManager = (engine.nebula as? A) ?: return HashMap()
        val mapHeight = engine.mapHeight
        val mapWidth = engine.mapWidth

        val deepHyperspaceNebulae = HashMap<MutableMap<MutableSet<Cloud>, Vector2f>, Boolean>()

        for (plugin in pluginToStorming.keys) {
            val isStorming = (pluginToStorming[plugin] == true)

            val minNebulaeToAdd = MIN_HYPERCLOUDS_TO_ADD_PER_CELL
            val maxNebulaeToAdd = MAX_HYPERCLOUDS_TO_ADD_PER_CELL
            val nebulaeToAdd: Int = (minNebulaeToAdd..maxNebulaeToAdd).random()

            var addedNebulae = 0f
            while (addedNebulae < nebulaeToAdd) {
                addedNebulae++
                // copypasted form battlecreationplugin
                val x = random.nextFloat() * mapWidth - mapWidth / 2
                val y = random.nextFloat() * mapHeight - mapHeight / 2
                var radius = 100f + random.nextFloat() * 400f
                radius += 100f + 500f * random.nextFloat()

                nebulaManager.spawnCloud(Vector2f(x, y), radius)
                val cellToCentroid: MutableMap<MutableSet<Cloud>, Vector2f> = HashMap()
                val nebula: Cloud = nebulaManager.getCloud(x, y)
                val nebulaCell: MutableSet<Cloud> = HashSet()
                nebulaCell += nebula
                var possibleCellInhabitant = nebula.Object()
                while (possibleCellInhabitant != null) {
                    nebulaCell += possibleCellInhabitant
                    possibleCellInhabitant = possibleCellInhabitant.Object()
                }
                val cellCentroid = getCellCentroid(nebulaManager, nebulaCell) ?: return deepHyperspaceNebulae
                cellToCentroid[nebulaCell] = cellCentroid
                deepHyperspaceNebulae[cellToCentroid] = isStorming
            }
        }
        return deepHyperspaceNebulae
    }

    /* private fun instantiateDeephyperspaceNebulae(
         pluginToStorming: HashMap<HyperspaceTerrainPlugin, Boolean>): MutableMap<NebulaParticle, Boolean> {
         engine.addNebulaParticle()

     } */

    private fun addDebrisFieldScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin>) {
        if (!DEBRIS_FIELD_EFFECT_ENABLED) return

        var canAddPlugin = false

        var debrisDensityMult = 0f
        var hazardDensityMult = 0f

        for (plugin: DebrisFieldTerrainPlugin in debrisFieldPlugins) {
            debrisDensityMult++
            hazardDensityMult++

            canAddPlugin = true
        }

        /*if (canAddPlugin) {
            engine.addPlugin(
                debrisFieldEffectScript(
                debrisDensityMult,
                hazardDensityMult
            ))
        }*/
    }

    private fun addSlipstreamScripts(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI, playerCoordinates: Vector2f, slipstreamPlugins: MutableSet<SlipstreamTerrainPlugin2>) {
        if (!SLIPSTREAM_EFFECT_ENABLED) return

        var canAddPlugin = false

        var peakPerformanceMult = 1f
        var fluxDissipationMult = 1f
        var overallSpeedMult = 1f
        var hardFluxGenerationPerFrame = 0f

        for (plugin: SlipstreamTerrainPlugin2 in slipstreamPlugins) {
            peakPerformanceMult *= SLIPSTREAM_PPT_MULT
            fluxDissipationMult *= SLIPSTREAM_FLUX_DISSIPATION_MULT
            overallSpeedMult += SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT

            hardFluxGenerationPerFrame += SLIPSTREAM_HARDFLUX_GEN_PER_FRAME
            canAddPlugin = true
        }
        if (canAddPlugin) {
            engine.addPlugin(
                SlipstreamEffectScript(
                peakPerformanceMult,
                fluxDissipationMult,
                hardFluxGenerationPerFrame,
                overallSpeedMult
            ))
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

            visionMod *= if (isInFlare) MAGSTORM_VISION_MULT else MAGFIELD_VISION_MULT
            missileMod *= if (isInFlare) MAGSTORM_MISSILE_MULT else MAGFIELD_MISSILE_MULT
            rangeMod *= if (isInFlare) MAGSTORM_RANGE_MULT else MAGFIELD_RANGE_MULT
            eccmChanceMod *= if (isInFlare) MAGSTORM_ECCM_MULT else MAGFIELD_ECCM_MULT
            missileBreakLockBaseChance += if (isInFlare) MAGSTORM_MISSILE_SCRAMBLE_CHANCE else MAGFIELD_MISSILE_SCRAMBLE_CHANCE
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

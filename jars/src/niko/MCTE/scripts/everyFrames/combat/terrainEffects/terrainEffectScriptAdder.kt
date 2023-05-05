package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI.CloudAPI
import com.fs.starfarer.api.impl.campaign.terrain.*
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin.CellStateTracker
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.entities.terrain.A
import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.blackHole.blackHoleEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.debrisField.debrisFieldEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.cloudCell
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.deepHyperspaceEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.dustCloud.dustCloudEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magneticFieldEffect
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.pulsarBeam.pulsarEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream.SlipstreamEffectScript
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_nebulaUtils.getCellCentroid
import niko.MCTE.utils.MCTE_nebulaUtils.getRadiusOfCell
import niko.MCTE.settings.MCTE_settings.BLACKHOLE_TIMEMULT_MULT
import niko.MCTE.settings.MCTE_settings.BLACK_HOLE_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.DEBRIS_FIELD_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.DEEP_HYPERSPACE_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.DUST_CLOUD_EFFECT_ENABLED
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_CENTROID_REFINEMENT_ITERATIONS
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
import niko.MCTE.settings.MCTE_settings.MAX_HYPERCLOUDS_TO_ADD_PER_CELL
import niko.MCTE.settings.MCTE_settings.MIN_HYPERCLOUDS_TO_ADD_PER_CELL
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
import niko.MCTE.utils.MCTE_nebulaUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector
import org.lwjgl.util.vector.Vector2f

// script to dodge plugin incompatability
class terrainEffectScriptAdder: baseNikoCombatScript() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        loadSettings()

        val engine = Global.getCombatEngine() ?: return
        val playerFleet = Global.getSector()?.playerFleet ?: return
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
        //val debrisFieldPlugins: MutableSet<DebrisFieldTerrainPlugin> = HashSet()
        val hyperspaceTerrainPlugins: MutableSet<HyperspaceTerrainPlugin> = HashSet()
        val blackHoleTerrainPlugins: MutableSet<EventHorizonPlugin> = HashSet()
        val pulsarPlugins: MutableSet<PulsarBeamTerrainPlugin> = HashSet()

        for (terrain: CampaignTerrainAPI in playerLocation.terrainCopy) {
            val terrainPlugin = terrain.plugin
                if (terrainPlugin.containsEntity(playerFleet)) {
                if (terrainPlugin is PulsarBeamTerrainPlugin) pulsarPlugins += terrainPlugin
                if (terrainPlugin is MagneticFieldTerrainPlugin) magneticFieldPlugins += terrainPlugin
                if (terrainPlugin is SlipstreamTerrainPlugin2) slipstreamPlugins += terrainPlugin
                //if (terrainPlugin is DebrisFieldTerrainPlugin) debrisFieldPlugins += terrainPlugin
                if (terrainPlugin is HyperspaceTerrainPlugin) hyperspaceTerrainPlugins += terrainPlugin
                if (terrainPlugin is EventHorizonPlugin) blackHoleTerrainPlugins += terrainPlugin
            }
        }
        addMagneticFieldScripts(engine, playerFleet, playerLocation, magneticFieldPlugins)
        addSlipstreamScripts(engine, playerFleet, playerLocation, playerCoordinates, slipstreamPlugins)
        addPulsarScripts(engine, playerFleet, playerLocation, playerCoordinates, pulsarPlugins)
        //addDebrisFieldScripts(engine, playerFleet, playerLocation, playerCoordinates, debrisFieldPlugins)
        addHyperspaceTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, hyperspaceTerrainPlugins)
        addBlackHoleTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, blackHoleTerrainPlugins)
        //addRingSystemTerrainScripts(engine, playerFleet, playerLocation, playerCoordinates, ringTerrainPlugins)
        // dust clouds already have an effect
    }

    private fun addPulsarScripts(
        engine: CombatEngineAPI,
        playerFleet: CampaignFleetAPI,
        playerLocation: LocationAPI,
        playerCoordinates: Vector2f,
        pulsarPlugins: MutableSet<PulsarBeamTerrainPlugin>
    ) {
        if (!PULSAR_EFFECT_ENABLED) return

        val pluginToIntensity: MutableMap<PulsarBeamTerrainPlugin, Float> = HashMap()
        val pluginToAngle: MutableMap<PulsarBeamTerrainPlugin, Float> = HashMap()
        var canAddScript: Boolean = false
        var hardFluxGenPerFrame = 0f
        var bonusEMPDamageForWeapons = 0f
        var shieldDestabilziationMult = 1f

        var EMPChancePerFrame = 0f
        var EMPDamage = 0f
        var energyDamage = 0f

        for (plugin: PulsarBeamTerrainPlugin in pulsarPlugins) {
            val intensity = (plugin.getIntensityAtPoint(playerCoordinates))*PULSAR_INTENSITY_BASE_MULT

            hardFluxGenPerFrame += (PULSAR_HARDFLUX_GEN_INCREMENT * intensity)
            bonusEMPDamageForWeapons += (PULSAR_EMP_DAMAGE_BONUS_FOR_WEAPONS_INCREMENT * intensity)
            shieldDestabilziationMult += (PULSAR_SHIELD_DESTABILIZATION_MULT_INCREMENT * intensity)

            EMPChancePerFrame += (PULSAR_EMP_CHANCE_INCREMENT * intensity)
            EMPDamage += (PULSAR_EMP_DAMAGE_INCREMENT * intensity)
            energyDamage += (PULSAR_DAMAGE_INCREMENT * intensity)

            pluginToIntensity[plugin] = (intensity)
            pluginToAngle[plugin] = (VectorUtils.getAngle(plugin.entity.location, playerCoordinates))
            canAddScript = true
        }
        if (canAddScript) {
            val pulsarPlugin = pulsarEffectScript(
                pulsarPlugins,
                pluginToIntensity,
                pluginToAngle,
                hardFluxGenPerFrame,
                bonusEMPDamageForWeapons,
                shieldDestabilziationMult,
                EMPChancePerFrame,
                EMPDamage,
                energyDamage
                //gravityPointsToIntensity
            )
            pulsarPlugin.start()
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

        var timeMult = 1f
        val pluginToIntensity: MutableMap<EventHorizonPlugin, Float> = HashMap()
        val pluginToAngle: MutableMap<EventHorizonPlugin, Float> = HashMap()
        var canAddScript: Boolean = false

        for (plugin: EventHorizonPlugin in blackHoleTerrainPlugins) {
            if (playerLocation != plugin.entity.containingLocation) continue
            val intensity = plugin.getIntensityAtPoint(playerCoordinates)
            timeMult += getBlackholeTimeMultIncrement(engine, playerFleet, playerLocation, playerCoordinates, plugin, intensity)
            pluginToIntensity[plugin] = intensity
            pluginToAngle[plugin] = (VectorUtils.getAngle(playerCoordinates, plugin.entity.location))
            //gravityPointsToIntensity[getGravityPointOfBlackHole(engine, playerCoordinates, plugin)] = intensity

            canAddScript = true
        }
        if (canAddScript) {
            val blackHolePlugin = blackHoleEffectScript(
                timeMult,
                pluginToIntensity,
                pluginToAngle,
                playerCoordinates
            )
            blackHolePlugin.start()
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
        if (!MCTE_debugUtils.isWindows) {
            MCTE_debugUtils.log.info("Rejected hyperspace terrain due to potential non-windows OS crashes.")
            return
        }
        val nebula = engine.nebula
        var canAddScript = false

        val pluginToStorming: HashMap<HyperspaceTerrainPlugin, Boolean> = HashMap()

        for (plugin: HyperspaceTerrainPlugin in hyperspaceTerrainPlugins) {
            var isStorming = false

            val cellAtPlayer: CellStateTracker = plugin.getCellAt(playerCoordinates, 100f) ?: continue
            if (cellAtPlayer.isStorming && HYPERSTORM_EFFECT_ENABLED) {
                isStorming = true
            }
            canAddScript = true
            pluginToStorming[plugin] = isStorming
        }
        if (canAddScript) {
            val deepHyperspaceNebulas: MutableMap<MutableMap<MutableSet<Cloud>, Vector2f>, Boolean> = instantiateDeephyperspaceNebulae(pluginToStorming)

            val stormingNebulaeToCentroid: MutableMap<MutableSet<Cloud>, Vector2f> = HashMap()
            val stormingNebulae: MutableSet<MutableSet<Cloud>> = HashSet()
            for (mapOfCloudsToCentroid in deepHyperspaceNebulas.keys) if (deepHyperspaceNebulas[mapOfCloudsToCentroid] == true) {
                stormingNebulae += mapOfCloudsToCentroid.keys
                stormingNebulaeToCentroid += mapOfCloudsToCentroid
            }

            val cloudCells = HashSet<cloudCell>()
            for (cell in stormingNebulae) {
                val centroid = getCellCentroidRepeatadly(nebula, cell)
                if (centroid == null) {
                    MCTE_debugUtils.displayError("centroid null when making params")
                    continue
                }
                val radius = getRadiusOfCell(cell, nebula, centroid)
                val cloudCell = cloudCell(
                    centroid,
                    radius,
                    cell
                )
                cloudCells += cloudCell
            }
            val hyperspacePlugin = deepHyperspaceEffectScript(cloudCells)
            hyperspacePlugin.start()
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
            val radius = getRadiusOfCell(cell, nebula, centroid)
            cellsToRadius[cell] = radius
        }
        return cellsToRadius
    }

    private fun instantiateDeephyperspaceNebulae(pluginToStorming: MutableMap<HyperspaceTerrainPlugin, Boolean>): HashMap<MutableMap<MutableSet<Cloud>, Vector2f>, Boolean> {
        val nebulaManager = (engine.nebula as? A ?: return HashMap())
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
                // copypasted from battlecreationplugin
                val x = random.nextFloat() * mapWidth - mapWidth / 2
                val y = random.nextFloat() * mapHeight - mapHeight / 2

                val tile: IntArray? = MCTE_nebulaUtils.getNebulaTile(Vector2f(x, y))
                if (tile == null) {
                    MCTE_debugUtils.displayError("tile was null in iunstantiateing deep hyerpsace etc")
                    continue
                }

                var radius = 100f + random.nextFloat() * 400f
                radius += 100f + 500f * random.nextFloat()

                nebulaManager.spawnCloud(Vector2f(x, y), radius)
                val nebula: Cloud = nebulaManager.getCloud(x, y) as Cloud

                val cellToCentroid: MutableMap<MutableSet<Cloud>, Vector2f> = HashMap()
                val nebulaCell: MutableSet<Cloud> = HashSet()
                nebulaCell += nebula
                var possibleCellInhabitant = nebula.flowDest //TODO: tesssst
                while (possibleCellInhabitant != null) {
                    nebulaCell += possibleCellInhabitant
                    possibleCellInhabitant = possibleCellInhabitant.flowDest
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
            engine.addPlugin(
                debrisFieldEffectScript(
                    debrisDensity.toDouble(),
                    specialSalvage
            ))
        }
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
            val slipstreamPlugin = SlipstreamEffectScript(
                peakPerformanceMult,
                fluxDissipationMult,
                hardFluxGenerationPerFrame,
                overallSpeedMult
            )
            slipstreamPlugin.start()
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
            val flareManager = plugin.flareManager
            val isInFlare = flareManager.isInActiveFlareArc(playerFleet)
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
            val magFieldPlugin = magneticFieldEffect(
                isStorm,
                visionMod,
                missileMod,
                rangeMod,
                eccmChanceMod,
                missileBreakLockBaseChance,
                magneticFieldPlugins
            )
            magFieldPlugin.start()
        }
    }
}

package niko.MCTE

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.blackHole.blackHoleEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.cloudCell
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.deepHyperspaceEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magneticFieldEffect
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.mesonField.mesonFieldEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.minefield.minefieldEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.pulsarBeam.pulsarEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream.SlipstreamEffectScript
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.BLACKHOLE_TIMEMULT_MULT
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_nebulaUtils
import niko.MCTE.utils.MCTE_nebulaUtils.getCloudsInRadius
import niko.MCTE.utils.MCTE_reflectionUtils
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

enum class combatEffectTypes {
    MAGFIELD {
        override fun createEffectInstance(): magneticFieldEffect {
            return magneticFieldEffect()
        }

        override fun modifyEffectInstance(instance: baseTerrainEffectScript, vararg args: Any) {
            if (instance !is magneticFieldEffect) return
            val entries: MutableList<Boolean> = args[0] as? ArrayList<Boolean> ?: return
            val effectMult = args[1] as? Float ?: return
            val plugins = args[2] as? Collection<MagneticFieldTerrainPlugin>

            for (entry in entries) {
                val isInFlare = entry
                if (isInFlare) instance.isStorm = true

                if (effectMult == 0f) {
                    instance.eccmChanceMod = 0f
                } else {
                    instance.eccmChanceMod *= if (isInFlare) MCTE_settings.MAGSTORM_ECCM_MULT / effectMult else MCTE_settings.MAGFIELD_ECCM_MULT / effectMult
                }

                instance.visionMod *= if (isInFlare) MCTE_settings.MAGSTORM_VISION_MULT * effectMult else MCTE_settings.MAGFIELD_VISION_MULT * effectMult
                instance.missileMod *= if (isInFlare) MCTE_settings.MAGSTORM_MISSILE_MULT * effectMult else MCTE_settings.MAGFIELD_MISSILE_MULT * effectMult
                instance.rangeMod *= if (isInFlare) MCTE_settings.MAGSTORM_RANGE_MULT * effectMult else MCTE_settings.MAGFIELD_RANGE_MULT * effectMult
                instance.missileBreakLockBaseChance += if (isInFlare) MCTE_settings.MAGSTORM_MISSILE_SCRAMBLE_CHANCE * effectMult else MCTE_settings.MAGFIELD_MISSILE_SCRAMBLE_CHANCE * effectMult

                if (plugins != null) {
                    instance.magneticFieldPlugins.addAll(plugins)
                }
            }
        }
    },
    MESONFIELD {
        override fun createEffectInstance(): mesonFieldEffectScript {
            return mesonFieldEffectScript()
        }

        override fun modifyEffectInstance(instance: baseTerrainEffectScript, vararg args: Any) {
            if (instance !is mesonFieldEffectScript) return
            val entries: MutableList<Boolean> = args[0] as? ArrayList<Boolean> ?: return
            val effectMult = args[1] as? Float ?: return

            for (entry in entries) {
                val isStorm = entry

                if (isStorm) {
                    instance.weaponRangeIncrement += MCTE_settings.MESON_STORM_WEAPON_RANGE_INCREMENT * effectMult
                    instance.fighterRangeIncrement += MCTE_settings.MESON_STORM_WING_RANGE_INCREMENT * effectMult
                    instance.systemRangeMult *= MCTE_settings.MESON_STORM_SYSTEM_RANGE_MULT * effectMult
                    instance.visionMult += MCTE_settings.MESON_STORM_VISION_MULT * effectMult
                    instance.isStorm = true
                } else {
                    instance.weaponRangeIncrement += MCTE_settings.MESON_FIELD_WEAPON_RANGE_INCREMENT * effectMult
                    instance.visionMult += MCTE_settings.MESON_FIELD_VISION_MULT * effectMult
                }
            }
        }
    },
    MINEFIELD {
        override fun createEffectInstance(): baseTerrainEffectScript {
            return minefieldEffectScript()
        }

        override fun modifyEffectInstance(instance: baseTerrainEffectScript, vararg args: Any) {
            if (instance !is minefieldEffectScript) return

            val newSides = args[0] as? HashMap<Int, Int> ?: return
            instance.targetSidesToInstance.forEach { instance.targetSidesToInstance[it.key] = instance.targetSidesToInstance[it.key]!! + newSides[it.key]!! }
        }
    },
    BLACKHOLE {
        override fun createEffectInstance(): blackHoleEffectScript {
            return blackHoleEffectScript()
        }

        override fun modifyEffectInstance(instance: baseTerrainEffectScript, vararg args: Any) {
            if (instance !is blackHoleEffectScript) return
            val pushDirs: MutableMap<Float, Float> = args[0] as? HashMap<Float, Float> ?: return
            val effectMult = args[1] as? Float ?: return

            for (entry in pushDirs) {
                instance.anglesToIntensity[entry.key] = entry.value
                instance.timeMult += (entry.value * BLACKHOLE_TIMEMULT_MULT)
            }

            if (effectMult == 0f) {
                instance.timeMult = 0f
            } else {
                instance.timeMult *= effectMult
            }
        }
    },
    HYPERSPACE {
        override fun createEffectInstance(): deepHyperspaceEffectScript {
            return deepHyperspaceEffectScript()
        }

        override fun modifyEffectInstance(instance: baseTerrainEffectScript, vararg args: Any) {
            if (instance !is deepHyperspaceEffectScript) return
            val cells: MutableSet<cloudCell> = args[0] as? HashSet<cloudCell> ?: return

            instance.stormingCells.addAll(cells)
        }
    },
    SLIPSTREAM {
        override fun createEffectInstance(): SlipstreamEffectScript {
            return SlipstreamEffectScript()
        }

        override fun modifyEffectInstance(instance: baseTerrainEffectScript, vararg args: Any) {
            if (instance !is SlipstreamEffectScript) return
            val effectMult: Float = args[0] as? Float ?: return
            if (effectMult == 0f) return

            instance.peakPerformanceMult *= MCTE_settings.SLIPSTREAM_PPT_MULT / effectMult
            instance.overallSpeedMult += MCTE_settings.SLIPSTREAM_OVERALL_SPEED_MULT_INCREMENT * effectMult
            instance.fluxDissipationMult += MCTE_settings.SLIPSTREAM_FLUX_DISSIPATION_MULT * effectMult
            instance.hardFluxGenerationPerFrame += MCTE_settings.SLIPSTREAM_HARDFLUX_GEN_PER_FRAME * effectMult
        }
    },
    PULSAR {
        override fun createEffectInstance(): pulsarEffectScript {
            return pulsarEffectScript()
        }

        override fun modifyEffectInstance(instance: baseTerrainEffectScript, vararg args: Any) {
            if (instance !is pulsarEffectScript) return
            val pushDirs: MutableMap<Float, Float> = args[0] as? HashMap<Float, Float> ?: return
            var effectMult: Float = args[1] as? Float ?: return

            for (entry in pushDirs) {
                instance.anglesToIntensity[entry.key] = entry.value
            }
            instance.refreshOverallIntensity()

            effectMult *= instance.overallIntensity
            if (effectMult == 0f) return
            instance.EMPChancePerFrame += MCTE_settings.PULSAR_EMP_CHANCE_INCREMENT * effectMult // TODO make this respect intensity
            instance.hardFluxGenerationPerFrame += MCTE_settings.PULSAR_HARDFLUX_GEN_INCREMENT * effectMult
            instance.bonusEMPDamageForWeapons += MCTE_settings.PULSAR_EMP_DAMAGE_BONUS_FOR_WEAPONS_INCREMENT * effectMult
            instance.shieldDestabilziationMult += MCTE_settings.PULSAR_SHIELD_DESTABILIZATION_MULT_INCREMENT * effectMult
            instance.EMPdamage += MCTE_settings.PULSAR_EMP_DAMAGE_INCREMENT * effectMult
            instance.energyDamage += MCTE_settings.PULSAR_DAMAGE_INCREMENT * effectMult
        }
    };

    protected abstract fun createEffectInstance(): baseTerrainEffectScript
    fun createInformedEffectInstance(vararg args: Any): baseTerrainEffectScript {
        val instance = createEffectInstance()
        modifyEffectInstance(instance, *args)
        return instance
    }

    protected open fun modifyEffectInstance(instance: baseTerrainEffectScript, vararg args: Any) {
        return
    }

    companion object {
        fun instantiateHyperstormCells(engine: CombatEngineAPI, sizeMult: Float, isStorming: Boolean, nebula: CombatNebulaAPI = engine.nebula): MutableSet<cloudCell> {
            val deepHyperspaceNebulas: MutableMap<MutableMap<MutableSet<Cloud>, Vector2f>, Boolean> = instantiateDeephyperspaceNebulae(engine, sizeMult, isStorming)

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
                val radius = MCTE_nebulaUtils.getRadiusOfCell(cell, nebula, centroid)
                val cloudCell = cloudCell(
                    centroid,
                    radius,
                    cell
                )
                cloudCells += cloudCell
            }
            return cloudCells
        }

        fun getCellCentroidRepeatadly(nebula: CombatNebulaAPI?, cell: MutableSet<Cloud>): Vector2f? {
            if (nebula == null) {
                MCTE_debugUtils.displayError("nebula null during centroid repeat")
                return Vector2f(0f, 0f)
            }
            val amountOfTimes = MCTE_settings.HYPERSTORM_CENTROID_REFINEMENT_ITERATIONS
            var centroid = MCTE_nebulaUtils.getCellCentroid(nebula, cell)
            var indexVal = 0
            while (indexVal < amountOfTimes) {
                indexVal++
                centroid = MCTE_nebulaUtils.getCellCentroid(nebula, cell, centroid)
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
                val radius = MCTE_nebulaUtils.getRadiusOfCell(cell, nebula, centroid)
                cellsToRadius[cell] = radius
            }
            return cellsToRadius
        }

        fun instantiateDeephyperspaceNebulae(engine: CombatEngineAPI, sizeMult: Float, isStorming: Boolean): HashMap<MutableMap<MutableSet<Cloud>, Vector2f>, Boolean> {
            val random = MathUtils.getRandom()

            val nebulaManager = engine.nebula
            val mapHeight = engine.mapHeight
            val mapWidth = engine.mapWidth

            val deepHyperspaceNebulae = HashMap<MutableMap<MutableSet<Cloud>, Vector2f>, Boolean>()

            val minNebulaeToAdd = MCTE_settings.MIN_HYPERCLOUDS_TO_ADD_PER_CELL
            val maxNebulaeToAdd = MCTE_settings.MAX_HYPERCLOUDS_TO_ADD_PER_CELL
            val nebulaeToAdd: Float = (minNebulaeToAdd..maxNebulaeToAdd).random() * sizeMult

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

                val cellSize: Float = nebulaManager.tileSizeInPixels
                if (cellSize == 0f) return HashMap() // why can this happen

                val radiusInTiles: Int = (radius / cellSize).toInt()

                MCTE_reflectionUtils.invoke("spawnCloud", nebulaManager, Vector2f(x, y), radius) // REFLECTION MAGIC

                val nebula: Cloud = nebulaManager.getCloud(x, y) as Cloud

                val nebulaCell: MutableSet<Cloud> = nebula.getCloudsInRadius(radiusInTiles, engine.nebula)

                val cellToCentroid: MutableMap<MutableSet<Cloud>, Vector2f> = HashMap()
                nebulaCell += nebula
                var possibleCellInhabitant = nebula.flowDest //TODO: tesssst
                while (possibleCellInhabitant != null) {
                    nebulaCell += possibleCellInhabitant
                    possibleCellInhabitant = possibleCellInhabitant.flowDest
                }
                val cellCentroid =
                    MCTE_nebulaUtils.getCellCentroid(nebulaManager, nebulaCell) ?: return deepHyperspaceNebulae
                cellToCentroid[nebulaCell] = cellCentroid
                deepHyperspaceNebulae[cellToCentroid] = isStorming
            }
            return deepHyperspaceNebulae
        }
    }

}
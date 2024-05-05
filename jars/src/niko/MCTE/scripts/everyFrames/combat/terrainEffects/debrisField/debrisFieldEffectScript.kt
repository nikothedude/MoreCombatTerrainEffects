/*package niko.MCTE.scripts.everyFrames.combat.terrainEffects.debrisField

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.CombatFleetManagerAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.fleet.ShipRolePick
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource
import com.fs.starfarer.api.util.WeightedRandomPicker
import niko.MCTE.listeners.debrisFieldCreationData
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.settings.MCTE_settings.MAX_SHIPS_ALLOWED
import niko.MCTE.settings.MCTE_settings.debrisFieldHulkificationLocationX
import niko.MCTE.settings.MCTE_settings.debrisFieldHulkificationLocationY
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_miscUtils.getGlobalDebrisFieldShipSourcePicker
import niko.MCTE.utils.MCTE_reflectionUtils.get
import niko.MCTE.utils.MCTE_reflectionUtils.invoke
import org.dark.shaders.light.LightShader.DO_NOT_RENDER
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import javax.xml.transform.Source
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.plusAssign
import kotlin.collections.set

class debrisFieldEffectScript(
    val density: Double,
    val pluginParams: MutableSet<debrisFieldParamsRepresentation>,
): baseTerrainEffectScript() {
    var isDone = false
    var elapsed = 0f
    var firstRun = true

    val effectiveDensityDecrement = 0.1

    var totalShips = 0

    override fun applyEffects(amount: Float) {
        if (isDone) return
        elapsed += amount
        if (firstRun) {
            val fleetManager = Global.getCombatEngine().getFleetManager(100)
            val cachedSuppressDeployment = fleetManager.isSuppressDeploymentMessages
            fleetManager.isSuppressDeploymentMessages = true
            for (params in pluginParams) {
                var effectiveDensity = params.getEffectiveDensity()
                while (effectiveDensity > 0f) {
                    if (totalShips >= MAX_SHIPS_ALLOWED) break
                    effectiveDensity -= effectiveDensityDecrement
                    val hulk = createHulk(fleetManager, params) ?: break //just abort
                    val hulkId = hulk.id
                    params.shipIdToPieces[hulkId] = splitUpHulk(hulk, timesToSplitPicker = params.timesToSplitPicker)
                }
            }
            fleetManager.isSuppressDeploymentMessages = cachedSuppressDeployment
            firstRun = false
        }
        else if (elapsed > 0.4) {
            for (param in pluginParams) {
                for (list in param.shipIdToPieces.values) {
                    for (ship in list) {
                        ship.velocity.x *= 0.1f
                        ship.velocity.y *= 0.1f
                    }
                }
            }
            delete()
        }
    }

    private fun splitUpHulk(hulk: ShipAPI, timesSplit: Float = 0f, timesToSplitPicker: WeightedRandomPicker<Int>): MutableSet<ShipAPI> {
        var timesSplit = timesSplit
        var failedSplits = 0f
        val amountOfFailedSplitsTilDone = 3f
        val piecesOfHulk = HashSet<ShipAPI>()
        piecesOfHulk += hulk
        val timesToSplit = (timesToSplitPicker.pick() - timesSplit)

        while (timesSplit < timesToSplit) {
            val splitResult = hulk.splitShip()
            if (splitResult == null) {
                failedSplits++
                //MCTE_debugUtils.log.info("null splitship result in splituphulk, incrementing failedsplits")
                if (failedSplits >= amountOfFailedSplitsTilDone) {
                    //MCTE_debugUtils.log.info("failedsplits $failedSplits exceeded or met $amountOfFailedSplitsTilDone, exiting loop")
                    break
                }
                continue
            } else failedSplits = 0f
            timesSplit++
            splitResult.tags.add(MCTE_ids.MCTE_HULK)
            piecesOfHulk += splitUpHulk(splitResult, timesSplit, timesToSplitPicker)
            totalShips++
            if (totalShips >= MAX_SHIPS_ALLOWED)
                break
        }

        for (piece in piecesOfHulk) {
            piece.velocity.set(0f, 0f)
            piece.customData[DO_NOT_RENDER] = true
        }

        return piecesOfHulk
    }

    private fun createHulk(fleetManager: CombatFleetManagerAPI, params: debrisFieldParamsRepresentation): ShipAPI? {

        if (totalShips >= MAX_SHIPS_ALLOWED) return null
        val variant = params.getVariantForHulk() ?: return null
        val fleetMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant)
        val hulk = fleetManager.spawnFleetMember(fleetMember, getLocationForInitialHulk(), getFacingForInitialHulk(), 0f)
        if (hulk == null) {
            MCTE_debugUtils.displayError("null hulk during createHulk(), aborting")
            return null
        }
        totalShips++
        hulkifyShip(hulk)
        return hulk
    }

    private fun hulkifyShip(hulk: ShipAPI) {
        hulk.owner = 100 // prevents the explosion message
        hulk.explosionScale = 0f
        hulk.mutableStats.dynamic.getStat(Stats.EXPLOSION_RADIUS_MULT).modifyMult("hulkification", 0f)
        hulk.collisionClass = CollisionClass.FIGHTER

        val cachedSize = hulk.hullSize
        hulk.hullSize = ShipAPI.HullSize.FIGHTER

        val cachedX = hulk.location.x
        val cachedY = hulk.location.y

        hulk.location.set(debrisFieldHulkificationLocationX, debrisFieldHulkificationLocationY)

        hulk.hitpoints = 0.0f
        while (hulk.isAlive) {
            engine!!.applyDamage(hulk, hulk.location, 5f, DamageType.ENERGY, 0f, true, false, null, false)
        }

        val armorGrid = hulk.armorGrid
        val width = invoke("getGridWidth", armorGrid) as Int
        val height = invoke("getGridHeight", armorGrid) as Int
        var iterVar = 0
        while (iterVar++ < width) {
            var iterVarTwo = 0
            while (iterVarTwo++ < height) {
                armorGrid.setArmorValue(iterVar, iterVarTwo, 0f)
                val location = armorGrid.getLocation(iterVar, iterVarTwo)
                engine.applyDamage(hulk, location, 20f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, null, false)
            }
        }

        hulk.collisionClass = CollisionClass.SHIP
        hulk.hullSize = cachedSize

        hulk.location.set(cachedX, cachedY)
        hulk.tags.add(MCTE_ids.MCTE_HULK)
    }

    private fun getLocationForInitialHulk(): Vector2f {
        val effectiveWidth = engine.mapHeight * 0.8f
        val effectiveHeight = engine.mapHeight * 0.75f
        val x = MathUtils.getRandomNumberInRange(effectiveWidth, -effectiveWidth)
        val y = MathUtils.getRandomNumberInRange(effectiveHeight, -effectiveHeight)
        return Vector2f(x, y)

        /*val engine = Global.getCombatEngine()
        val maxIterations = 10
        var iterations = 0

        val location = Vector2f(0f, 0f)

        while (iterations++ <= maxIterations) {
            val effectiveWidth = engine.mapHeight * 0.5f
            val effectiveHeight = engine.mapHeight * 0.45f
            val x = MathUtils.getRandomNumberInRange(effectiveWidth, -effectiveWidth)
            val y = MathUtils.getRandomNumberInRange(effectiveHeight, -effectiveHeight)
            location.set(x, y)

            var abort = false
            for (ship in engine.ships) {
                if (MathUtils.isWithinRange(ship.location, location, ship.collisionRadius)) {
                    abort = true
                    break
                }
            }
            if (abort)
                continue

            return location
        }
        return location*/
    }

    private fun getFacingForInitialHulk(): Float {
        return MathUtils.getRandomNumberInRange(0f, 360f)
    }

    override fun handleSounds(amount: Float) {
        return
    }

    private fun delete() {
        isDone = true
        engine.removePlugin(this)
    }
}

class debrisFieldParamsRepresentation(var density: Float, var source: DebrisFieldSource, val plugin: DebrisFieldTerrainPlugin?) {
    //ship ids are the same across the master ship and its fragments
    val shipIdToPieces: MutableMap<String, MutableSet<ShipAPI>> = HashMap()
    val timesToSplitPicker = WeightedRandomPicker<Int>()
    init {
        addWeightsToPicker()
    }

    private fun addWeightsToPicker() {
        when(source) {
            DebrisFieldSource.PLAYER_SALVAGE, DebrisFieldSource.SALVAGE -> {
                timesToSplitPicker.add(3, 20f)
                timesToSplitPicker.add(2, 20f)
                timesToSplitPicker.add(1, 20f)
                timesToSplitPicker.add(0, 10f)
            }
            DebrisFieldSource.MIXED, DebrisFieldSource.GEN -> {
                timesToSplitPicker.add(5, 5f)
                timesToSplitPicker.add(2, 30f)
                timesToSplitPicker.add(1, 10f)
                timesToSplitPicker.add(0, 100000f)
            }
            DebrisFieldSource.BATTLE -> {
                timesToSplitPicker.add(2, 10f)
                timesToSplitPicker.add(1, 20f)
                timesToSplitPicker.add(0, 200000f)
            }
        }
    }

    fun getVariantForHulk(): String? {
        return when(source) {
            DebrisFieldSource.BATTLE -> getVariantFromBattle()
            else -> getGlobalDebrisFieldShipSourcePicker().pick()
        }
    }

    fun getEffectiveDensity(): Double {
        return density*5.0
    }


    private fun getVariantFromBattle(): String? {
        if (plugin == null) return getGlobalDebrisFieldShipSourcePicker().pick()
        val creationParams: debrisFieldCreationData = plugin.entity.memoryWithoutUpdate[MCTE_ids.MCTE_debris_info] as? debrisFieldCreationData ?: return getGlobalDebrisFieldShipSourcePicker().pick()
        val variantDifference = creationParams.variantsPotentiallyLost
        val variantPicked = variantDifference.randomOrNull()
        if (variantPicked != null) {
            variantDifference -= variantPicked
            return variantPicked.originalVariant
        }
        return null

        /*val pickedFaction = creationParams.factionsInvolved.randomOrNull() ?: return getGlobalDebrisFieldShipSourcePicker().pick()
        val picker: WeightedRandomPicker<ShipRolePick> = WeightedRandomPicker(pickedFaction.pickShip())
        return pickedFaction.pickShip()*/
    }

    constructor(plugin: DebrisFieldTerrainPlugin) : this(plugin.params.density, plugin.params.source, plugin)



}*/
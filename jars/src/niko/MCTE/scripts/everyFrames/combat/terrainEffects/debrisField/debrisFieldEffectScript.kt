package niko.MCTE.scripts.everyFrames.combat.terrainEffects.debrisField

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.fs.starfarer.combat.entities.BattleObjective
import data.scripts.SotfModPlugin
import niko.MCTE.combatEffectTypes
import niko.MCTE.listeners.debrisFieldCreationData
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.MAX_SHIPS_ALLOWED
import niko.MCTE.settings.MCTE_settings.debrisFieldHulkificationLocationX
import niko.MCTE.settings.MCTE_settings.debrisFieldHulkificationLocationY
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_miscUtils.getGlobalDebrisFieldShipSourcePicker
import niko.MCTE.utils.MCTE_reflectionUtils.invoke
import org.dark.shaders.light.LightShader.DO_NOT_RENDER
import org.json.JSONException
import org.json.JSONObject
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.roundToInt

class debrisFieldEffectScript(): baseTerrainEffectScript() {

    companion object {
        val vanillaObjectivesToWeight = mutableMapOf(
            Pair("nav_buoy", 10f),
            Pair("sensor_array", 10f),
            Pair("comm_relay", 10f),
        )
    }

    var density: Double = 0.0
    var pluginParams: MutableSet<debrisFieldParamsRepresentation> = HashSet()

    override var effectPrototype: combatEffectTypes? = combatEffectTypes.DEBRISFIELD

    var isDone = false
    var elapsed = 0f
    var firstRun = true

    val effectiveDensityDecrement = 0.1

    var totalShips = 0

    override fun applyEffects(amount: Float) {
        if (isDone) return
        elapsed += amount
        if (firstRun) {
            density = density.coerceAtMost(10.0)
            val fleetManager = Global.getCombatEngine().getFleetManager(100)
            val cachedSuppressDeployment = fleetManager.isSuppressDeploymentMessages
            fleetManager.isSuppressDeploymentMessages = true
            for (params in pluginParams) {
                var effectiveDensity = params.getEffectiveDensity()
                while (effectiveDensity > 0f) {
                    if (totalShips >= MAX_SHIPS_ALLOWED) break
                    effectiveDensity -= effectiveDensityDecrement
                    val hulk = createHulk(fleetManager, params) ?: break
                    //just abort
                    val hulkId = hulk.id
                    params.shipIdToPieces[hulkId] = splitUpHulk(hulk, timesToSplitPicker = params.timesToSplitPicker)
                }
            }
            fleetManager.isSuppressDeploymentMessages = cachedSuppressDeployment

            deployObjectives()
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

    private fun deployObjectives() {
        val engine = Global.getCombatEngine()
        if (engine.objectives.isEmpty()) return // respecting vanilla
        //if (engine.context.otherGoal == FleetGoal.ESCAPE || engine.context.playerGoal == FleetGoal.ESCAPE) return

        val effectiveWidth = engine.mapWidth * 0.34f
        val effectiveHeight = engine.mapHeight * 0.34f

        var picksLeft = (density * MCTE_settings.DEBRIS_FIELD_OBJECTIVES_PER_DENSITY * MathUtils.getRandomNumberInRange(0.9f, 1.1f)).roundToInt().coerceAtMost(MCTE_settings.DEBRIS_FIELD_MAX_OBJECTIVE_SPAWNS).coerceAtLeast(1)
        var escape = (engine.context.otherGoal == FleetGoal.ESCAPE || engine.context.playerGoal == FleetGoal.ESCAPE)

        val picker = WeightedRandomPicker<String>(random)
        val picked = ArrayList<String>()
        if (!MCTE_debugUtils.sotfEnabled) {
            vanillaObjectivesToWeight.forEach {
                picker.add(it.key, it.value)
            }
            while (picksLeft-- > 0) {
                picked += picker.pick()
            }
        } else {
            // taken from SotfBattleCreationPluginImpl
            val playerFleet = Global.getSector().playerFleet
            val loc: LocationAPI = playerFleet.containingLocation

            val battle = playerFleet.battle
            var fpOne = 0f
            battle.sideOne.forEach { fpOne += it.fleetData.fleetPointsUsed }
            var fpTwo = 0f
            battle.sideTwo.forEach { fpTwo += it.fleetData.fleetPointsUsed }
            val fpBoth = fpOne + fpTwo
            val maxFPForObj = Global.getSettings().getFloat("maxNoObjectiveBattleSize")

            val objectives = SotfModPlugin.OBJECTIVE_DATA
            for (i in 0 until objectives.length()) {
                var should_add_objective = true
                val row = objectives.getJSONObject(i)
                try {
                    if (row.getString("id").isEmpty()) {
                        continue
                    }
                } catch (ex: JSONException) {
                    continue
                }

                try {
                    if (row.getString("hyperspace") == "true") {
                        if (!loc.isHyperspace) {
                            should_add_objective = false
                        }
                    } else if (row.getString("hyperspace") == "false") {
                        if (loc.isHyperspace) {
                            should_add_objective = false
                        }
                    }
                } catch (ex: JSONException) {
                    //SotfBattleCreationPluginImpl.log.info("no hyperspace setting for objective ID " + row.getString("id"))
                }

                if (!loc.isHyperspace) {
                    val system = loc as StarSystemAPI
                    try {
                        if (row.getString("tag") != "" && !system.hasTag(row.getString("tag"))) {
                            should_add_objective = false
                        }
                    } catch (ex: JSONException) {
                        //SotfBattleCreationPluginImpl.log.info("no tag set for objective ID " + row.getString("id"))
                    }
                } else {
                    try {
                        if (row.getString("tag") != "") {
                            should_add_objective = false
                        }
                    } catch (ex: JSONException) {
                        //SotfBattleCreationPluginImpl.log.info("no tag set for objective ID " + row.getString("id"))
                    }
                }

                try {
                    if (row.getString("escape") == "true") {
                        if (!escape) {
                            should_add_objective = false
                        }
                    } else if (row.getString("escape") == "false") {
                        if (escape) {
                            should_add_objective = false
                        }
                    }
                } catch (ex: JSONException) {
                    //SotfBattleCreationPluginImpl.log.info("no escape setting for objective ID " + row.getString("id"))
                }

                try {
                    if (!row.getString("flag").isBlank()) {
                        for (flag in row.getString("flag").split(Regex("(, *)"))) {
                            if (!Global.getSector().memoryWithoutUpdate.contains("$$flag")) {
                                should_add_objective = false
                            }
                        }
                    }
                } catch (ex: JSONException) {

                }

                try {
                    if (!row.getString("not_flag").isBlank()) {
                        for (flag in row.getString("not_flag").split(Regex("(, *)"))) {
                            if (!Global.getSector().memoryWithoutUpdate.contains("$$flag")) {
                                should_add_objective = false
                            }
                        }
                    }
                } catch (ex: JSONException) {

                }

                try {
                    val minFP = row.getInt("minFP")
                    if (fpBoth < (maxFPForObj + minFP)) {
                        should_add_objective = false
                    }
                } catch (ex: JSONException) {
                    //SotfBattleCreationPluginImpl.log.info("no minFP for objective ID " + row.getString("id"))
                }

                try {
                    var maxFP = row.getInt("maxFP")
                    if (maxFP == -1) {
                        maxFP = 999999
                    }
                    if (fpBoth > (maxFPForObj + maxFP)) {
                        should_add_objective = false
                    }
                } catch (ex: JSONException) {
                    //SotfBattleCreationPluginImpl.log.info("no maxFP for objective ID " + row.getString("id"))
                }

                if (should_add_objective) {
                    picker.add(row.getString("obj_id"), row.getDouble("weight").toFloat())
                }
            }

            while (picksLeft-- > 0) {
                picked += picker.pick()
            }
        }

        val objectives = HashSet<BattleObjective>()
        for (id in picked) {
            val x = MathUtils.getRandomNumberInRange(-effectiveWidth, effectiveWidth) // using OUR random, here
            val y = MathUtils.getRandomNumberInRange(-effectiveHeight, effectiveHeight)

            val objective = BattleObjective(
                id,
                Vector2f(x, y),
                BattleObjectiveAPI.Importance.NORMAL
            )
            objectives += objective
            engine.addEntity(objective)
        }

    }

    private fun splitUpHulk(hulk: ShipAPI, timesSplit: Float = 0f, timesToSplitPicker: WeightedRandomPicker<Int>): MutableSet<ShipAPI> {
        var timesSplit = timesSplit
        var failedSplits = 0f
        val amountOfFailedSplitsTilDone = 3f
        val piecesOfHulk = HashSet<ShipAPI>()
        piecesOfHulk += hulk
        val timesToSplit = (timesToSplitPicker.pick() - timesSplit)
        //val timesToSplit = 0

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

        if (piecesOfHulk.size > 1) {
            for (piece in piecesOfHulk) {
                //val xsign = if (prob(50)) 1 else -1
                //val ysign = if (prob(50)) 1 else -1
                //piece.location.scale(3f)
                //piece.velocity.set(0f, 0f)
                piece.customData[DO_NOT_RENDER] = true
                engine.addPlugin(PieceStasisScript(piece))
            }
        }

        return piecesOfHulk
    }

    class PieceStasisScript(val piece: ShipAPI): BaseEveryFrameCombatPlugin() {
        val interval = IntervalUtil(0.5f, 0.7f)
        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)
            interval.advance(amount)
            if (interval.intervalElapsed()) {
                piece.velocity.set(0f, 0f)
                piece.facing = MathUtils.getRandomNumberInRange(0f, 360f)
                Global.getCombatEngine().removePlugin(this)
            }
        }
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

        hulk.velocity.set(MathUtils.getRandomNumberInRange(-3f, 3f), MathUtils.getRandomNumberInRange(-3f, 3f))
        hulk.angularVelocity = MathUtils.getRandomNumberInRange(-10f, 10f)

        engine.addPlugin(HulkDamageDecalAdder(hulk))
    }

    class HulkDamageDecalAdder(val hulk: ShipAPI): BaseEveryFrameCombatPlugin() {
        var timesRan = 0
        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            timesRan++
            if (timesRan < 2) return

            val armorGrid = hulk.armorGrid
            val width = invoke("getGridWidth", armorGrid) as Int
            val height = invoke("getGridHeight", armorGrid) as Int
            var iterVar = 0
            while (iterVar++ < width) {
                var iterVarTwo = 0
                while (iterVarTwo++ < height) {
                    armorGrid.setArmorValue(iterVar, iterVarTwo, 0f)
                    val location = armorGrid.getLocation(iterVar, iterVarTwo)
                    Global.getCombatEngine().applyDamage(hulk, location, 20f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, null, false)
                }
            }
            Global.getCombatEngine().removePlugin(this)
        }
    }

    private fun getLocationForInitialHulk(): Vector2f {
        val effectiveWidth = engine.mapWidth * 0.4f
        val effectiveHeight = engine.mapHeight * 0.4f
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

    override fun handleNotification(amount: Float): Boolean {
        //Global.getSoundPlayer().playUILoop("terrain_magfield", 1f, 1f)
        return super.handleNotification(amount)
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
            //variantDifference -= variantPicked
            return variantPicked.originalVariant
        }
        return null

        /*val pickedFaction = creationParams.factionsInvolved.randomOrNull() ?: return getGlobalDebrisFieldShipSourcePicker().pick()
        val picker: WeightedRandomPicker<ShipRolePick> = WeightedRandomPicker(pickedFaction.pickShip())
        return pickedFaction.pickShip()*/
    }

    constructor(plugin: DebrisFieldTerrainPlugin) : this(plugin.params.density, plugin.params.source, plugin)



}

/*class debrisFieldParamsRepresentation(var density: Float, var source: DebrisFieldSource, val plugin: DebrisFieldTerrainPlugin?) {
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
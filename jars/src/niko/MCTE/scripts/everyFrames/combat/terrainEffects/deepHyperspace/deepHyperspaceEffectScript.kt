package niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_EMP_DAMAGE
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_ENERGY_DAMAGE
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_GRACE_INCREMENT
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_MAX_ARC_RANGE
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_MIN_ARC_RANGE
import niko.MCTE.utils.MCTE_settings.MAX_TIME_BETWEEN_HYPERSTORM_STRIKES
import niko.MCTE.utils.MCTE_settings.MIN_TIME_BETWEEN_HYPERSTORM_STRIKES
import niko.MCTE.utils.MCTE_shipUtils.arc
import niko.MCTE.utils.MCTE_shipUtils.cosmeticArc
import niko.MCTE.utils.MCTE_shipUtils.isTangible
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

class deepHyperspaceEffectScript(
    val stormingCellsWithParams: HashMap<MutableSet<Cloud>, cloudParams>,
): baseTerrainEffectScript() {

    val standardSpeedForTargettingChance: Float = 100f
    val targettedEntities: MutableMap<CombatEntityAPI, MutableSet<hyperstormArcPreparation>> = HashMap()
    val entitiesToNotTarget: MutableMap<CombatEntityAPI, Float> = HashMap()
    var nebulaHandler: CombatNebulaAPI = engine.nebula

    val dummyShip: ShipAPI = createNewDummyShip()

    private fun createNewDummyShip(): ShipAPI {
        val fleetMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "talon")
        val fleetManager = engine.getFleetManager(100)
        fleetMember.shipName = "Hyperstorm Lightning"
        val dummyShip = fleetManager.spawnFleetMember(fleetMember, Vector2f(999999f, 999999f), 5f, 0f)
        dummyShip.name = "Hyperstorm Lightning"
        dummyShip.collisionClass = CollisionClass.NONE
        dummyShip.shipAI = null
        dummyShip.owner = 100
        return dummyShip

    }

    init {
        for (cell in stormingCellsWithParams.keys) {
            stormingCellsWithParams[cell]?.let { resetArcCooldown(it) }
        }
    }

    private fun resetArcCooldown(params: cloudParams): Float {
        val min = MIN_TIME_BETWEEN_HYPERSTORM_STRIKES
        val max = MAX_TIME_BETWEEN_HYPERSTORM_STRIKES
        val cooldown = min + random.nextFloat() * (max - min)
        params.cooldown = cooldown
        return cooldown
    }

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        if (Global.getCurrentState() != GameState.COMBAT) return
        nebulaHandler = this.engine.nebula

        if (nebulaHandler == null) {
            this.engine.removePlugin(this)
        }
    }

    override fun applyEffects(amount: Float) {
        for (cell in ArrayList(stormingCellsWithParams.keys)) {
            tryToArc(cell, amount)
        }
        decrementGracePeriods(amount)
    }

    protected fun tryToArc(cell: MutableSet<Cloud>, amount: Float): Boolean {
        if (engine.isPaused) return false
        val params = stormingCellsWithParams[cell] ?: return false
        if (params.preparingToArc()) return false
        params.cooldown = (params.cooldown - amount).coerceAtLeast(0f)
        val timeTilNextArc = params.cooldown

        if (timeTilNextArc <= 0f) {
            doArc(cell, amount)
            return true
        }
        return false
    }

    private fun doArc(cell: MutableSet<Cloud>, amount: Float) {
        val params = stormingCellsWithParams[cell] ?: return
        resetArcCooldown(params)
        prepareArc(amount, cell, getArcRange(cell))
    }

    private fun prepareArc(amount: Float, cell: MutableSet<Cloud>, maxRadius: Float) {
        val params = stormingCellsWithParams[cell] ?: return
        val randomizedPosition = getArcSite(cell)
        val shipsAndMissiles = HashSet<CombatEntityAPI>()
        shipsAndMissiles.addAll(this.getEntitiesWithinRange(cell, randomizedPosition, maxRadius, amount))
        var target: CombatEntityAPI? = null
        for (shipOrMissile in shipsAndMissiles) {
            if (combatEntityIsValidArcTarget(cell, amount, shipOrMissile, randomizedPosition, maxRadius)) {
                target = shipOrMissile
                break
            }
        }
        if (target != null) {
            if (shouldSkipPrepAndJustArc(target)) {
                arc(cell, engine, randomizedPosition, dummyShip, target, maxRadius, getActualDamageForEntity(target), getEMPDamageForEntity(target))
            } else {
                val preparationScript = hyperstormArcPreparation(this, cell, target, randomizedPosition, maxRadius)
                params.preparationScript = preparationScript
                engine.addPlugin(preparationScript)
            }
        } else {
            val targetVector = getArcSite(cell)
            cosmeticArc(cell, engine, randomizedPosition, targetVector)
        }
    }

    private fun getEntitiesWithinRange(
        cell: MutableSet<Cloud>,
        ourCoordinates: Vector2f = getArcSite(cell),
        maxRadius: Float = getArcRange(cell),
        amount: Float): MutableList<CombatEntityAPI> {

        val entitiesWithinRange = ArrayList<CombatEntityAPI>()
        val shipIterator = engine.shipGrid.getCheckIterator(ourCoordinates, maxRadius, maxRadius)
        val missileIterator = engine.missileGrid.getCheckIterator(ourCoordinates, maxRadius, maxRadius)
        while (shipIterator.hasNext()) {
            val ship = shipIterator.next() as ShipAPI
            entitiesWithinRange += ship
        }
        while (missileIterator.hasNext()) {
            val missile = missileIterator.next() as MissileAPI
            entitiesWithinRange += missile
        }
        return entitiesWithinRange
    }

    private fun combatEntityIsValidArcTarget(
        cell: MutableSet<Cloud>,
        amount: Float,
        shipOrMissile: CombatEntityAPI,
        arcCoordinates: Vector2f,
        maxRadius: Float = getArcRange(cell)): Boolean {
        if (shipOrMissile is ShipAPI && shipOrMissile.isShuttlePod) return false

        val graceValue = entitiesToNotTarget[shipOrMissile]
        if (graceValue != null && graceValue > 0) {
            return false
        }

        val targetCoordinates = shipOrMissile.location
        val distance = MathUtils.getDistance(targetCoordinates, arcCoordinates)
        if (distance > maxRadius) return false
        val modifier = getTargettingChanceMult(shipOrMissile)
        val randomFloat = random.nextFloat()

        return (randomFloat <= (((maxRadius - distance)/(maxRadius/100))*modifier))
    }

    private fun getArcSite(cell: MutableSet<Cloud>): Vector2f {
        val params = stormingCellsWithParams[cell] ?: return Vector2f(0f, 0f)
        val radius: Float = params.radius
        val ourCoordinates = params.centroid
        val currentx = ourCoordinates.x
        val minx = currentx - radius/1.1f
        val maxx = currentx + radius/1.1f
        val currenty = ourCoordinates.y
        val miny = currenty - radius/1.1f
        val maxy = currenty + radius/1.1f

        val adjustedx = (minx + random.nextFloat() * (maxx - minx))
        val adjustedy = (miny + random.nextFloat() * (maxy - miny))

        val arcSite = Vector2f(adjustedx, adjustedy)
        return arcSite
    }

    private fun getArcRange(cell: MutableSet<Cloud>): Float {
        val params = stormingCellsWithParams[cell] ?: return 0f
        val minRadius = HYPERSTORM_MIN_ARC_RANGE + params.radius
        val maxRadius = HYPERSTORM_MAX_ARC_RANGE + params.radius
        return minRadius + random.nextFloat() * (maxRadius - minRadius)
    }

    private fun ensureCellHasRadiusStored(cell: MutableSet<Cloud>) {
        return
        /*if (cloudCellToRadius[cell] == null) {
            cloudCellToRadius[cell] = getRadius(cell, nebulaHandler)
        }
        if (cloudCellToCentroid[cell] == null) {
            val centroid = MCTE_miscUtils.getCellCentroid(nebulaHandler, cell)
            if (centroid != null) cloudCellToCentroid[cell] = centroid
        }
        if (cloudCellToCooldown[cell] == null) {
            cloudCellToCooldown[cell] = 0f
        }*/
    }


    private fun decrementGracePeriods(amount: Float) {
        for (entity: CombatEntityAPI in entitiesToNotTarget.keys) {
            entitiesToNotTarget[entity] = (entitiesToNotTarget[entity]!! - amount).coerceAtLeast(0f)
        }
    }

    override fun handleNotification(amount: Float) {
        if (isStorming()) {
            val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
            val playerShip = engine.playerShip
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_hyperStorm2",
                icon,
                "Hyperspace Storm",
                "Lightning strikes on this ship do ${getActualDamageForEntity(playerShip)} energy damage and ${getEMPDamageForEntity(playerShip)} EMP damage",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_hyperStorm1",
                icon,
                "Hyperspace Storm",
                "Storming hyperclouds periodically striking ships with lightning",
                true)
        }
    }

    fun getActualDamageForEntity(entity: CombatEntityAPI?): Float {
        if (entity == null || !entity.isTangible()) return 0f
        val mult = getActualDamageMultForEntity(entity)

        return HYPERSTORM_ENERGY_DAMAGE*mult
    }

    fun getActualDamageMultForEntity(entity: CombatEntityAPI): Float {
        var mult = 1f
        if (entity is ShipAPI) {
            val mutableStats = entity.mutableStats
            mult += mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifiedValue - 1
        }

        return mult
    }

    fun getEMPDamageForEntity(entity: CombatEntityAPI?): Float {
        if (entity == null || !entity.isTangible()) return 0f
        val mult = getEMPDamageMultForEntity(entity)

        return HYPERSTORM_EMP_DAMAGE*mult
    }

    fun getEMPDamageMultForEntity(entity: CombatEntityAPI): Float {
        var mult = 1f
        if (entity is ShipAPI) {
            val mutableStats = entity.mutableStats
            mult += mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifiedValue - 1
        }

        return mult
    }

    override fun handleSounds(amount: Float) {
        if (isStorming()) {
            Global.getSoundPlayer().playUILoop("terrain_hyperspace_storm", 1f, 1f)
        } else {
            Global.getSoundPlayer().playUILoop("terrain_hyperspace_deep", 1f, 1f)
        }
    }

    private fun isStorming(): Boolean {
        return stormingCellsWithParams.isNotEmpty()
    }

    private fun shouldSkipPrepAndJustArc(target: CombatEntityAPI): Boolean {
        return (target is MissileAPI || (target is ShipAPI && target.isFighter && target != engine.playerShip))
    }

    private fun getTargettingChanceMult(shipOrMissile: CombatEntityAPI): Float {
        var modifier = 1f
        if (!shipOrMissile.isTangible()) return 0f

        val speed = shipOrMissile.velocity.length()
        modifier = ((standardSpeedForTargettingChance*2 - speed)/100).coerceAtLeast(0f)

        return modifier.coerceAtLeast(0f)
    }

    fun giveGraceToTarget(target: CombatEntityAPI) {
        val value = entitiesToNotTarget[target]
        if (value == null) entitiesToNotTarget[target] = 0f
        entitiesToNotTarget[target] = entitiesToNotTarget[target]!! + getGraceIncrement()
    }

    private fun getGraceIncrement(): Float {
        return HYPERSTORM_GRACE_INCREMENT
    }

    /*fun getRadius(cell: MutableSet<Cloud>, nebula: CombatNebulaAPI): Float {  //todo: this wont work lmfao
        if (cloudCellToCentroid[cell] == null) {
            val centroid = MCTE_miscUtils.getCellCentroid(nebula, cell) ?: return 0f
            cloudCellToCentroid[cell] = centroid
        }
        val centroid = cloudCellToCentroid[cell] ?: return 0f
        return MCTE_miscUtils.getRadiusOfCell(cell, nebula, centroid)
    }*/
}

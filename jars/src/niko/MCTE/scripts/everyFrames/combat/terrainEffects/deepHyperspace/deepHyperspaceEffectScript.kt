package niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.impl.campaign.ids.Stats
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.MCTE_arcUtils.arc
import niko.MCTE.utils.MCTE_arcUtils.cosmeticArc
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_EMP_DAMAGE
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_ENERGY_DAMAGE
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_GRACE_INCREMENT
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_SPEED_THRESHOLD
import niko.MCTE.settings.MCTE_settings.MAX_TIME_BETWEEN_HYPERSTORM_STRIKES
import niko.MCTE.utils.MCTE_shipUtils.isTangible
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

class deepHyperspaceEffectScript(
    val stormingCells: MutableSet<cloudCell>,
): baseTerrainEffectScript() {

    val warnedShipsToCells: MutableMap<ShipAPI, MutableSet<cloudCell>> = HashMap()
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
        for (cell in stormingCells) {
            cell.resetArcCooldown()
        }
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
        handleWarnedShips(amount)
        for (cell in ArrayList(stormingCells)) {
            warnNearbyShipsOfCell(cell, amount)
            tryToArc(cell, amount)
        }
        decrementGracePeriods(amount)
    }

    private fun handleWarnedShips(amount: Float) {
        val shipIterator = warnedShipsToCells.keys.iterator()
        while (shipIterator.hasNext()) {
            val ship = shipIterator.next()
            if (!ship.isAlive) {
                shipIterator.remove()
                continue
            }
            val cellIterator = warnedShipsToCells[ship]!!.iterator()
            while (cellIterator.hasNext()) {
                val cell = cellIterator.next()
                if (MathUtils.getDistance(ship.location, cell.centroid) < cell.radius*10 || !shipCaresAboutBeingInRangeOfStorm(ship, cell)) {
                    cellIterator.remove()
                    continue
                } else {
                    warnShipItIsInRangeOfStorm(ship, cell)
                }
            }
            if (warnedShipsToCells[ship]!!.isEmpty()) {
                unwarnShip(ship)
                shipIterator.remove()
                continue
            }
        }
    }

    private fun warnNearbyShipsOfCell(cell: cloudCell, amount: Float) {
        val shipIterator = engine.shipGrid.getCheckIterator(cell.centroid, cell.radius*10, cell.radius*10)
        while (shipIterator.hasNext()) {
            val ship = shipIterator.next() as ShipAPI
            if (!ship.isAlive) continue
            if (shipCaresAboutBeingInRangeOfStorm(ship, cell)) {
                warnShipItIsInRangeOfStorm(ship, cell)
            }
        }
    }

    protected fun tryToArc(cell: cloudCell, amount: Float): Boolean {
        if (engine.isPaused) return false
        if (cell.preparingToArc()) return false
        cell.cooldown = (cell.cooldown - amount).coerceAtLeast(0f)
        val timeTilNextArc = cell.cooldown

        if (timeTilNextArc <= 0f) {
            doArc(cell, amount)
            return true
        }
        return false
    }

    private fun doArc(cell: cloudCell, amount: Float) {
        cell.resetArcCooldown()
        prepareArc(amount, cell, cell.getArcRange())
    }

    private fun prepareArc(amount: Float, cell: cloudCell, maxRadius: Float) {
        val randomizedPosition = cell.getArcSite()
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
                arc(cell, randomizedPosition, dummyShip, target, maxRadius, getRawActualDamageForEntity(target), getRawEMPDamageForEntity(target))
            } else {
                val preparationScript = hyperstormArcPreparation(this, cell, target, randomizedPosition, maxRadius)
                cell.preparationScript = preparationScript
                engine.addPlugin(preparationScript)
            }
        } else {
            val targetVector = cell.getArcSite()
            cosmeticArc(randomizedPosition, targetVector)
        }
    }

    private fun warnShipItIsInRangeOfStorm(ship: ShipAPI, cell: cloudCell) {
        if (shipCaresAboutBeingInRangeOfStorm(ship, cell)) {
            ship.aiFlags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT)
            addShipAndCellToWarnedMap(ship, cell)
        }
    }

    private fun unwarnShip(ship: ShipAPI, remove: Boolean = false) {
        ship.aiFlags.unsetFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT)
        if (remove) warnedShipsToCells -= ship
    }

    private fun addShipAndCellToWarnedMap(ship: ShipAPI, cell: cloudCell) {
        if (warnedShipsToCells[ship] == null) {
            warnedShipsToCells[ship] = HashSet()
        }
        warnedShipsToCells[ship]!!.add(cell)
    }

    private fun getWarningDuration(ship: ShipAPI): Float {
        return MAX_TIME_BETWEEN_HYPERSTORM_STRIKES*2
    }

    private fun shipCaresAboutBeingInRangeOfStorm(ship: ShipAPI, cell: cloudCell): Boolean {
        var modifier = getTargettingChanceMult(ship)

        val captain: PersonAPI? = ship.captain
        if (captain != null) {
            when (captain.personalityAPI.id) {
                Personalities.RECKLESS -> return false
                Personalities.AGGRESSIVE -> modifier -= 0.7f
                Personalities.CAUTIOUS -> modifier += 0.7f
                Personalities.TIMID -> modifier += 5f
                else -> {}
            }
        }
        val actualDamage = getModifiedActualDamageForEntity(ship)
        val empDamage = getModifiedEMPDamageForEntity(ship)
        //val shieldEfficiency = ship.mutableStats.shieldDamageTakenMult.modifiedValue
        // cant shield if venting lmao
        val hullEfficiency = ship.mutableStats.hullDamageTakenMult.modifiedValue

        val currentHull = ship.hullLevel
        if (actualDamage > currentHull*2.3) modifier += 4f

        val score = ((((actualDamage) + (empDamage* MCTE_settings.EMP_DAMAGE_FEAR_MULT)))/hullEfficiency)*modifier

        return (MCTE_settings.getHyperstormFearThreshold() <= score)
    }

    private fun getEntitiesWithinRange(
        cell: cloudCell,
        ourCoordinates: Vector2f = cell.getArcSite(),
        maxRadius: Float = cell.getArcRange(),
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
        cell: cloudCell,
        amount: Float,
        shipOrMissile: CombatEntityAPI,
        arcCoordinates: Vector2f,
        maxRadius: Float = cell.getArcRange()): Boolean {
        if (shipOrMissile is ShipAPI && (shipOrMissile.isShuttlePod)) return false

        val graceValue = entitiesToNotTarget[shipOrMissile]
        if (graceValue != null && graceValue > 0) {
            return false
        }

        val targetCoordinates = shipOrMissile.location
        val distance = MathUtils.getDistance(targetCoordinates, arcCoordinates)
        if (distance > maxRadius) return false
        val modifier = getTargettingChanceMult(shipOrMissile)
        val randomFloat = random.nextFloat()

        return (randomFloat <= (((maxRadius - distance)/(maxRadius))*modifier))
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
                "Lightning strikes on this ship do ${getRawActualDamageForEntity(playerShip).roundTo(2)} energy damage and ${getRawEMPDamageForEntity(playerShip).roundTo(2)} EMP damage",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_hyperStorm1",
                icon,
                "Hyperspace Storm",
                "Storming hyperclouds periodically striking ships with lightning",
                true)
        }
    }

    fun getRawActualDamageForEntity(entity: CombatEntityAPI?): Float {
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

    fun getModifiedActualDamageForEntity(ship: ShipAPI): Float {
        val rawValue = getRawActualDamageForEntity(ship)
        var modifiedValue = (rawValue * ship.mutableStats.energyDamageTakenMult.modifiedValue)

        return modifiedValue
    }

    fun getRawEMPDamageForEntity(entity: CombatEntityAPI?): Float {
        if (entity == null || !entity.isTangible()) return 0f
        val mult = getEMPDamageMultForEntity(entity)

        return HYPERSTORM_EMP_DAMAGE*mult
    }

    fun getModifiedEMPDamageForEntity(ship: ShipAPI): Float {
        val rawValue = getRawEMPDamageForEntity(ship)
        var modifiedValue = (rawValue * ship.mutableStats.empDamageTakenMult.modifiedValue)

        return modifiedValue
    }

    fun getEMPDamageMultForEntity(entity: CombatEntityAPI): Float {
        var mult = 1f
        if (entity is ShipAPI) {
            val mutableStats = entity.mutableStats
            mult += mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifiedValue - 1
        }

        return mult
    }

    fun getTargettingChanceMult(shipOrMissile: CombatEntityAPI): Float {
        if (!shipOrMissile.isTangible()) return 0f
        val speed = shipOrMissile.velocity.length()
        var modifier: Float = 1f
        modifier *= MathUtils.clamp((((speed - HYPERSTORM_SPEED_THRESHOLD)/HYPERSTORM_SPEED_THRESHOLD)), 0f, 3.5f)
        modifier *= ((shipOrMissile.mass - 600)/100).coerceAtLeast(1f)

        return modifier.coerceAtLeast(0f)
    }

    fun getGraceIncrement(): Float {
        return HYPERSTORM_GRACE_INCREMENT
    }

    override fun handleSounds(amount: Float) {
        if (isStorming()) {
            Global.getSoundPlayer().playUILoop("terrain_hyperspace_storm", 1f, 1f)
        } else {
            Global.getSoundPlayer().playUILoop("terrain_hyperspace_deep", 1f, 1f)
        }
    }

    private fun isStorming(): Boolean {
        return stormingCells.isNotEmpty()
    }

    private fun shouldSkipPrepAndJustArc(target: CombatEntityAPI): Boolean {
        return (target is MissileAPI || (target is ShipAPI && target.isFighter && target != engine.playerShip))
    }

    fun giveGraceToTarget(target: CombatEntityAPI) {
        val value = entitiesToNotTarget[target]
        if (value == null) entitiesToNotTarget[target] = 0f
        entitiesToNotTarget[target] = entitiesToNotTarget[target]!! + getGraceIncrement()
    }
}

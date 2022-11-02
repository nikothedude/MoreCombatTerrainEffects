package niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_ARCSITE_X_VARIATION
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_ARCSITE_Y_VARIATION
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_EMP_DAMAGE
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_ENERGY_DAMAGE
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_GRACE_INCREMENT
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_MAX_ARC_RANGE
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_MIN_ARC_RANGE
import niko.MCTE.utils.MCTE_settings.MAX_TIME_BETWEEN_HYPERSTORM_STRIKES
import niko.MCTE.utils.MCTE_settings.MIN_TIME_BETWEEN_HYPERSTORM_STRIKES
import niko.MCTE.utils.MCTE_shipUtils
import niko.MCTE.utils.MCTE_shipUtils.arc
import niko.MCTE.utils.MCTE_shipUtils.cosmeticArc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

class deepHyperspaceEffectScript(
    stormingNebulae: MutableSet<Cloud>
): baseTerrainEffectScript() {

    val targettedEntities: MutableMap<CombatEntityAPI, MutableSet<hyperstormArcPreparation>> = HashMap()
    val entitiesToNotTarget: MutableMap<CombatEntityAPI, Float> = HashMap()
    lateinit var nebulaHandler: CombatNebulaAPI

    val stormingNebulae: MutableMap<Cloud, Float> = HashMap()
    val arcingNebulae: MutableMap<Cloud, hyperstormArcPreparation?> = HashMap()

    val dummyShip: ShipAPI = createNewDummyShip()

    private fun createNewDummyShip(): ShipAPI {
        val dummyShip = CombatUtils.spawnShipOrWingDirectly("talon", FleetMemberType.SHIP, MCTE_ids.HYPERSTORM_FLEETSIDE, 1f, Vector2f(99999f, 99999f), 0f)
        dummyShip.setName("Hyperstorm Lightning")
        dummyShip.collisionClass = CollisionClass.NONE
        dummyShip.shipAI = null
        return dummyShip

    }

    init {
        for (Cloud in stormingNebulae) {
            Cloud.resetArcCooldown()
        }
    }

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        if (Global.getCurrentState() != GameState.COMBAT) return
        nebulaHandler = this.engine.nebula
    }

    override fun applyEffects(amount: Float) {
        val shipsAndMissiles: MutableList<CombatEntityAPI> = ArrayList()
        for (cloud in stormingNebulae.keys) {
            cloud.tryToArc(amount, shipsAndMissiles)
        }
    }

    override fun handleNotification(amount: Float) {
        if (isStorming()) {
            val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_hyperStorm1",
                icon,
                "Hyperspace Storm",
                "Storming hyperclouds periodically striking ships with lightning",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_hyperStorm2",
                icon,
                "Hyperspace Storm",
                "Lightning strikes do $HYPERSTORM_ENERGY_DAMAGE energy damage and $HYPERSTORM_EMP_DAMAGE EMP damage",
                true)
        }
    }

    override fun handleSounds(amount: Float) {
        if (isStorming()) {
            Global.getSoundPlayer().playUILoop("terrain_hyperspace_storm", 1f, 1f)
        } else {
            Global.getSoundPlayer().playUILoop("terrain_hyperspace_deep", 1f, 1f)
        }
    }

    private fun isStorming(): Boolean {
        return stormingNebulae.isNotEmpty()
    }

    protected fun Cloud.tryToArc(amount: Float, shipsAndMissiles: MutableList<CombatEntityAPI>): Boolean {
        if (engine.isPaused) return false
        if (arcingNebulae[this] != null) return false
        stormingNebulae[this] = stormingNebulae[this]!! - amount
        val timeTilNextArc = stormingNebulae[this]!!

        if (timeTilNextArc <= 0f) {
            doArc(amount, shipsAndMissiles)
            return true
        }
        return false
    }

    private fun Cloud.doArc(amount: Float, shipsAndMissiles: MutableList<CombatEntityAPI>) {
        resetArcCooldown()
        prepareArc(amount, this, getArcRadius(), shipsAndMissiles)
    }

    private fun prepareArc(amount: Float, cloud: Cloud, maxRadius: Float, shipsAndMissiles: MutableList<CombatEntityAPI>) {
        if (shipsAndMissiles.isEmpty()) shipsAndMissiles.addAll(engine.ships + engine.missiles)
        var target: CombatEntityAPI? = null
        val ourCoordinates = cloud.`return`()
        val randomizedPosition = cloud.getArcSite(ourCoordinates)
        for (shipOrMissile in shipsAndMissiles) {
            if (cloud.combatEntityIsValidArcTarget(amount, shipOrMissile, randomizedPosition, maxRadius)) {
                target = shipOrMissile
                break
            }
        }
        if (target != null) {
            if (target is MissileAPI || (target is ShipAPI && target.isFighter && target != engine.playerShip)) {
                cloud.arc(engine, randomizedPosition, dummyShip, target, maxRadius)
            } else {
                val preparationScript = hyperstormArcPreparation(this, cloud, target, randomizedPosition, maxRadius, dummyShip)
                arcingNebulae[cloud] = preparationScript
                engine.addPlugin(preparationScript)
            }
        } else {
            val targetVector = cloud.getArcSite(ourCoordinates)
            cloud.cosmeticArc(engine, randomizedPosition, targetVector)
        }
    }

    private fun Cloud.getArcRadius(): Float {
        val minRadius = HYPERSTORM_MIN_ARC_RANGE
        val maxRadius = HYPERSTORM_MAX_ARC_RANGE
        return minRadius + random.nextFloat() * (maxRadius - minRadius)
    }

    private fun Cloud.resetArcCooldown(): Float {
        val min = MIN_TIME_BETWEEN_HYPERSTORM_STRIKES
        val max = MAX_TIME_BETWEEN_HYPERSTORM_STRIKES
        val cooldown = min + random.nextFloat() * (max - min)
        stormingNebulae[this] = cooldown
        return cooldown
    }

    private fun Cloud.combatEntityIsValidArcTarget(amount: Float, shipOrMissile: CombatEntityAPI, arcCoordinates: Vector2f, maxRadius: Float = getArcRadius()): Boolean {
        val graceValue = entitiesToNotTarget[shipOrMissile]
        if (graceValue != null && graceValue > 0) {
            entitiesToNotTarget[shipOrMissile] = (entitiesToNotTarget[shipOrMissile]!! - amount).coerceAtLeast(0f)
            return false
        }

        val targetCoordinates = shipOrMissile.location
        val distance = MathUtils.getDistance(targetCoordinates, arcCoordinates)
        if (distance > maxRadius) return false
        var modifier = 1f
        if (shipOrMissile is ShipAPI) {
            val mutableStats = shipOrMissile.mutableStats
            modifier *= mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifiedValue
        }
        val randomFloat = random.nextFloat()

        return (randomFloat <= (((maxRadius - distance)/(maxRadius/100))*modifier))
    }

    private fun Cloud.getArcSite(ourCoordinates: Vector2f = `return`()): Vector2f {
        val currentx = ourCoordinates.x
        val minx = currentx - HYPERSTORM_ARCSITE_X_VARIATION
        val maxx = currentx + HYPERSTORM_ARCSITE_X_VARIATION
        val currenty = ourCoordinates.y
        val miny = currenty - HYPERSTORM_ARCSITE_Y_VARIATION
        val maxy = currenty + HYPERSTORM_ARCSITE_Y_VARIATION

        val adjustedx = minx + random.nextFloat() * (maxx - minx)
        val adjustedy = miny + random.nextFloat() * (maxy - miny)

        val arcSite = Vector2f(adjustedx, adjustedy)
        return arcSite
    }

    fun giveGraceToTarget(target: CombatEntityAPI) {
        val value = entitiesToNotTarget[target]
        if (value == null) entitiesToNotTarget[target] = 0f
        entitiesToNotTarget[target] = entitiesToNotTarget[target]!! + getGraceIncrement()
    }

    private fun getGraceIncrement(): Float {
        return HYPERSTORM_GRACE_INCREMENT
    }
}

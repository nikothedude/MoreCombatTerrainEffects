package niko.MCTE.scripts.everyFrames.combat.terrainEffects.minefield

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.settings.MCTE_settings.MINEFIELD_MAX_MINES_PER_TICK
import niko.MCTE.settings.MCTE_settings.MINEFIELD_MAX_SECS_BETWEEN_TICKS
import niko.MCTE.settings.MCTE_settings.MINEFIELD_MIN_MINES_PER_TICK
import niko.MCTE.settings.MCTE_settings.MINEFIELD_MIN_SECS_BETWEEN_TICKS
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.max
import kotlin.math.min

class minefieldEffectScript : baseTerrainEffectScript() {

    companion object {
        const val MINE_SPAWN_CHANCE = 0.15f
        const val MCTE_MINEFIELD_KEY = "MCTE_minefieldKey"
    }

    class IncomingMine {
        var targetOwner = 0
        var mineLoc: Vector2f? = null
        var delay = 0f
        var target: ShipAPI? = null
    }

    class MinefieldData {
        var tracker = IntervalUtil(MINEFIELD_MIN_SECS_BETWEEN_TICKS, MINEFIELD_MAX_SECS_BETWEEN_TICKS)
        var incoming: List<IncomingMine> = ArrayList()
    }

    override var effectPrototype: combatEffectTypes? = combatEffectTypes.MINEFIELD
    var targetSidesToInstance = hashMapOf(Pair(0, 0), Pair(1, 0))

    val playerAlignedDummy = createNewDummyShip(0)
    val enemyAlignedDummy = createNewDummyShip(1)

    private fun createNewDummyShip(owner: Int): ShipAPI {
        val fleetMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "talon_Interceptor")
        val fleetManager = engine.getFleetManager(100)
        val originalValue = fleetManager.isSuppressDeploymentMessages
        fleetManager.isSuppressDeploymentMessages = true
        fleetMember.shipName = "Minefield"
        val dummyShip = fleetManager.spawnFleetMember(fleetMember, Vector2f(999999f, 999f), 5f, 0f)
        dummyShip.name = "Minefield"
        dummyShip.collisionClass = CollisionClass.NONE
        dummyShip.shipAI = null
        dummyShip.originalOwner = owner
        dummyShip.owner = owner
        if (owner == 0) dummyShip.isAlly = true
        dummyShip.mutableStats.hullDamageTakenMult.modifyMult("MCTE_minefieldEffect", 0f)
        dummyShip.alphaMult = 0f
        fleetManager.isSuppressDeploymentMessages = originalValue

        return dummyShip
    }

    override fun handleSounds(amount: Float) {
        Global.getSoundPlayer().playUILoop("terrain_asteroid_field", 1f, 1f)
    }

    override fun applyEffects(amount: Float) {
        if (engine.isPaused) return

        var data = engine.customData[MCTE_MINEFIELD_KEY] as? MinefieldData
        if (data == null) {
            data = MinefieldData()
            engine.customData[MCTE_MINEFIELD_KEY] = data
        }

        for (inc in ArrayList<IncomingMine>(data.incoming)) {
            inc.delay -= amount
            if (inc.delay <= 0) {
                spawnMine(inc.targetOwner, inc.mineLoc, inc.target)
                data.incoming -= inc
            }
        }

        data.tracker.advance(amount)
        if (!data.tracker.intervalElapsed()) return

        val picker = WeightedRandomPicker<IncomingMine>()
        for (entry in targetSidesToInstance) {
            val instances = entry.value
            if (instances <= 0) continue
            val side = entry.key
            val fleetManager = engine.getFleetManager(side)

            var iterationsRemaining = instances
            while (iterationsRemaining-- > 0) {
                for (deployed in fleetManager.deployedCopyDFM) {
                    val enemy = deployed.ship
                    if (enemy.isHulk) continue
                    if (enemy.isFighter) continue
                    if (enemy.isDrone) continue
                    if (enemy.isStation) continue
                    if (enemy.isStationModule) continue
                    if (enemy.travelDrive != null && enemy.travelDrive.isActive) continue

                    if (Math.random().toFloat() > MINE_SPAWN_CHANCE) continue

                    val mineLoc = Misc.getPointAtRadius(
                        enemy.location,
                        enemy.collisionRadius + 400f + 200f * Math.random().toFloat()
                    )
                    val minOk: Float = 400f + enemy.collisionRadius
                    if (!isAreaClear(mineLoc, minOk)) continue

                    val inc = IncomingMine()
                    inc.delay = Math.random().toFloat() * 1.5f
                    inc.target = enemy
                    inc.mineLoc = mineLoc
                    inc.targetOwner = if (side == 1) 0 else 1

                    picker.add(inc)
                }
            }
        }
        val numToSpawn = max(1, min(MathUtils.getRandomNumberInRange(MINEFIELD_MIN_MINES_PER_TICK, MINEFIELD_MAX_MINES_PER_TICK), picker.items.size))

        var i = 0
        while (i < numToSpawn && !picker.isEmpty) {
            val inc = picker.pickAndRemove()
            data.incoming += inc
            i++
        }
    }

    override fun handleNotification(amount: Float): Boolean {
        if (!super.handleNotification(amount)) return false

        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
        val playerShip = engine.playerShip.owner ?: return false

        if (targetSidesToInstance[0]!! > 0) { // player targeted
            engine.maintainStatusForPlayerShip(
                "niko_MCTE_minefield1",
                icon,
                "Minefield (MCTE FEATURE - NOT INDEVO)",
                "Stealth mines targeting allies",
                true
            )
        }
        if (targetSidesToInstance[1]!! > 0) { // enemy targeted
            engine.maintainStatusForPlayerShip(
                "niko_MCTE_minefield2",
                icon,
                "Minefield (MCTE FEATURE - NOT INDEVO)",
                "Stealth mines targeting enemies",
                false
            )
        }
        return true
    }

    fun isAreaClear(loc: Vector2f?, range: Float): Boolean {
        val engine = Global.getCombatEngine()
        for (other in engine.ships) {
            if (other.isFighter) continue
            if (other.isDrone) continue
            val dist = Misc.getDistance(loc, other.location)
            if (dist < range) {
                return false
            }
        }
        for (other in Global.getCombatEngine().asteroids) {
            val dist = Misc.getDistance(loc, other.location)
            if (dist < other.collisionRadius + 100f) {
                return false
            }
        }
        return true
    }

    fun spawnMine(targetOwner: Int, mineLoc: Vector2f?, target: ShipAPI?) {
        if (target == null) return
        if (mineLoc == null) return

        val source = if (targetOwner == 1) enemyAlignedDummy else playerAlignedDummy
        val mineDir = Misc.getAngleInDegrees(mineLoc, target.location)
        val currLoc = Misc.getPointAtRadius(mineLoc, 50f + Math.random().toFloat() * 50f)
        val mine = engine.spawnProjectile(
            source, null,
            getWeaponId(),
            currLoc,
            mineDir, null
        ) as MissileAPI
        mine.owner = source.owner

        mine.flightTime = Math.random().toFloat()
        mine.fadeOutThenIn(1f)
        Global.getSoundPlayer().playSound("mine_spawn", 1f, 1f, mine.location, mine.velocity)
    }

    private fun getWeaponId(): String {
        return "minelayer1"
    }
}
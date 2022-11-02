package niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseNikoCombatScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.utils.MCTE_shipUtils.arc
import niko.MCTE.utils.MCTE_shipUtils.telegraphArc
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f

class hyperstormArcPreparation(
    val parentScript: deepHyperspaceEffectScript,
    val hyperStormNebula: Cloud,
    val target: CombatEntityAPI,
    val coordinatesToSpawnArcFrom: Vector2f,
    val maxRadius: Float,
    val dummyShip: ShipAPI
): baseNikoCombatScript(), usesDeltaTime {

    override var deltaTime: Float = 0f
    override val thresholdForAdvancement = getAdvancementThreshold()
    var deltaTimeForReposition: Float = deltaTime
    val thresholdForReposition = 0.1f

    var threatIndicator: CombatEntityAPI? = createThreatIndicator()

    private fun getAdvancementThreshold(): Float {
        val max = 4.6f
        val min = 3.2f

        return min + random.nextFloat() * (max - min)
    }

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        if (Global.getCurrentState() != GameState.COMBAT) return
        if (this.engine.isPaused) return
        if (parentScript.targettedEntities[target] == null) {
            parentScript.targettedEntities[target] = HashSet()
        }
        parentScript.targettedEntities[target]!! += this

        hyperStormNebula.telegraphArc(this.engine, coordinatesToSpawnArcFrom, dummyShip, target)

        //target.aiFlags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT, thresholdForAdvancement)
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return
        if (engine.isPaused) return
        repositionThreatIndicator(amount)
        if (canAdvance(amount)) {
            doArc()
        } else {
            tryToTelegraph()
        }
    }

    private fun tryToTelegraph() {
        val threshold = (deltaTime * (1/thresholdForAdvancement))*.5
        val randomFloat = random.nextFloat()
        if (randomFloat <= threshold) {
            hyperStormNebula.telegraphArc(engine, coordinatesToSpawnArcFrom, dummyShip, target, maxRadius)
        }
    }

    private fun doArc() {

        hyperStormNebula.arc(engine, coordinatesToSpawnArcFrom, dummyShip, target, maxRadius)
        val prepScriptsTargettingTarget = parentScript.targettedEntities[target]
        prepScriptsTargettingTarget!! -= this
        if (prepScriptsTargettingTarget.isEmpty()) parentScript.giveGraceToTarget(target)
        parentScript.arcingNebulae[hyperStormNebula] = null

        delete()
    }

    private fun delete() {
        engine.removePlugin(this)
        deleteThreatIndicator()
    }

    private fun deleteThreatIndicator() {
        if (threatIndicator != null) {
            threatIndicator!!.hitpoints = 0.00001f
            engine.removeEntity(threatIndicator)
        }
    }

    private fun repositionThreatIndicator(amount: Float) {
        deltaTimeForReposition += amount
        if (deltaTimeForReposition >= thresholdForReposition) {
            deltaTimeForReposition = 0f
            deleteThreatIndicator()
            threatIndicator = createThreatIndicator()
        }
    }

    private fun createThreatIndicator(): CombatEntityAPI? {
        val threatIndicator = engine.spawnProjectile(
            null,
            null,
            MCTE_ids.hyperstormThreatIndicatorId,
            getThreatIndicatorPlacement(),
            0f,
            null
        )
        if (threatIndicator is MissileAPI) {
            threatIndicator.untilMineExplosion = (thresholdForAdvancement - deltaTime).coerceAtLeast(thresholdForReposition)
        }
        return threatIndicator
    }

    private fun getThreatIndicatorPlacement(): Vector2f {
        val targetLocation = target.location
        val direction = VectorUtils.getDirectionalVector(targetLocation, coordinatesToSpawnArcFrom)
        val directionToReturn = Vector2f(targetLocation.x + (direction.x*50), targetLocation.y + (direction.y*50))

        return directionToReturn
    }

}
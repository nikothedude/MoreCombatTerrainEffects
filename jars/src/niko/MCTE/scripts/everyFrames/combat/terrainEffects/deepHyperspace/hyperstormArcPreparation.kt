package niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.input.InputEventAPI
import niko.MCTE.scripts.everyFrames.combat.baseNikoCombatScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.utils.MCTE_arcUtils.arc
import niko.MCTE.utils.MCTE_arcUtils.telegraphArc
import niko.MCTE.utils.MCTE_ids
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_MAX_ARC_CHARGE_TIME
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_MIN_ARC_CHARGE_TIME
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_TIMES_TO_ARC_AGAINST_SHIP
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.pow

class hyperstormArcPreparation(
    val parentScript: deepHyperspaceEffectScript,
    val hyperStormNebula: cloudCell,
    val target: CombatEntityAPI,
    val coordinatesToSpawnArcFrom: Vector2f,
    val maxRadius: Float,
): baseNikoCombatScript(), usesDeltaTime {

    override var deltaTime: Float = 0f
    override val thresholdForAdvancement = getAdvancementThreshold()
    var deltaTimeForReposition: Float = deltaTime
    val thresholdForReposition = 0.1f

    var threatIndicator: CombatEntityAPI? = createThreatIndicator()

    private fun getAdvancementThreshold(): Float {
        val min = HYPERSTORM_MIN_ARC_CHARGE_TIME
        val max = HYPERSTORM_MAX_ARC_CHARGE_TIME

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

        telegraphArc(hyperStormNebula, coordinatesToSpawnArcFrom, parentScript.dummyShip, target,
            volume = getTelegraphVolume())
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return
        if (engine.isPaused) return
        val engineMult = engine.timeMult.modifiedValue
        val adjustedAmount = amount * engineMult
        repositionThreatIndicator(adjustedAmount)
        if (canAdvance(adjustedAmount)) {
            doArc()
        } else {
            tryToTelegraph()
        }
    }

    private fun tryToTelegraph() {
        val engineMult = engine.timeMult.modifiedValue
        val threshold = getTelegraphThreshold()
        val randomFloat = (random.nextFloat())/engineMult
        val modifier = parentScript.getActualDamageMultForEntity(target)
        if (randomFloat <= (threshold*modifier)) {
            telegraphArc(hyperStormNebula, getTelegraphArcStartPoint(), parentScript.dummyShip, target, maxRadius, getTelegraphVolume())
        }
    }

    private fun getTelegraphArcStartPoint(): Vector2f {
        val telegraphLocationX = MathUtils.getRandomNumberInRange(coordinatesToSpawnArcFrom.x-50, coordinatesToSpawnArcFrom.x+50)
        val telegraphLocationY = MathUtils.getRandomNumberInRange(coordinatesToSpawnArcFrom.y-50, coordinatesToSpawnArcFrom.y+50)

        return Vector2f(telegraphLocationX, telegraphLocationY)
    }

    private fun getTelegraphVolume(): Float {
        return ((getTelegraphThreshold()+1).pow(4.6)).toFloat() //arbitrary pow arg
    }

    private fun getTelegraphThreshold(): Double {
        return (deltaTime * (1/thresholdForAdvancement))*.5
    }

    private fun doArc() {

        val timesToArc = HYPERSTORM_TIMES_TO_ARC_AGAINST_SHIP

        val damage = (parentScript.getRawActualDamageForEntity(target) / timesToArc)
        val emp = (parentScript.getRawEMPDamageForEntity(target) / timesToArc)

        arc(hyperStormNebula, coordinatesToSpawnArcFrom, parentScript.dummyShip, target, maxRadius, damage, emp, timesToArc)

        delete()
    }

    fun delete() {
        val prepScriptsTargettingTarget = parentScript.targettedEntities[target]
        prepScriptsTargettingTarget!! -= this
        if (prepScriptsTargettingTarget.isEmpty()) parentScript.giveGraceToTarget(target)
        hyperStormNebula.preparationScript = null

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
            threatIndicator.untilMineExplosion = (thresholdForAdvancement - deltaTime).coerceAtLeast(0.1f)
            //threatIndicator.damageAmount = HYPERSTORM_ENERGY_DAMAGE
            threatIndicator.damage.damage = parentScript.getRawActualDamageForEntity(target)
            threatIndicator.damage.fluxComponent = parentScript.getRawEMPDamageForEntity(target)
            /*threatIndicator.collisionClass = CollisionClass.FIGHTER
            threatIndicator.hitpoints = 9999999f*/
        }
        threatIndicator.owner = 100
        return threatIndicator
    }

    private fun getThreatIndicatorPlacement(): Vector2f {
        val targetLocation = target.location
        val direction = VectorUtils.getDirectionalVector(targetLocation, coordinatesToSpawnArcFrom)
        val directionToReturn = Vector2f(targetLocation.x + (direction.x*50), targetLocation.y + (direction.y*50))

        return directionToReturn
    }

}

package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.cloudParams
import org.dark.shaders.light.LightShader
import org.dark.shaders.light.StandardLight
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.combat.applyForce
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.sqrt

object MCTE_shipUtils {

    val damageArcThickness = 45f
    val arcFringeColor = Color(139, 45, 253, 255)
    val arcCoreColor = Color(158, 79, 255, 255)

    @JvmStatic
    fun CombatEntityAPI.isTangible(): Boolean {
        if (this is ShipAPI) {
            if (isPhased) return false
        }
        return (collisionClass != CollisionClass.NONE)
    }

    @JvmStatic
    fun CombatEntityAPI.isAffectedByNebulaSecondary(nebula: CombatNebulaAPI): Boolean {
        if (!isTangible()) return false
        if (nebula.locationHasNebula(location.x, location.y)) return true
        return false
    }

    fun getNearestShipToPoint(engine: CombatEngineAPI, coordinates: Vector2f, maxDistance: Float = Float.MAX_VALUE): ShipAPI? {
        var closest: ShipAPI? = null
        var distance: Float
        var closestDistance = Float.MAX_VALUE

        for (ship in engine.ships) {
            if (ship.isShuttlePod || ship.isHulk) continue
            distance = MathUtils.getDistance(ship, coordinates)
            if (distance < maxDistance && distance < closestDistance) {
                closest = ship
                closestDistance = distance
            }
        }
        return closest
    }

    @JvmStatic
    @JvmOverloads
    fun arc(cell: MutableSet<Cloud>, params: cloudParams? = null, engine: CombatEngineAPI, coordinatesToSpawnArcFrom: Vector2f, source: ShipAPI, target: CombatEntityAPI,
                  maxDistance: Float = Float.MAX_VALUE, actualDamage: Float, empDamage: Float) {

        val distance = MathUtils.getDistance(coordinatesToSpawnArcFrom, target.location)
        if ((!target.isTangible() || distance > maxDistance) && params != null) {
            cosmeticArc(cell, engine, coordinatesToSpawnArcFrom, params.getArcSite())
            return
        }
        val modifier = getArcOverallDamageMod(cell, engine, coordinatesToSpawnArcFrom, source, target, maxDistance, actualDamage)

        engine.spawnEmpArc(
            source,
            coordinatesToSpawnArcFrom,
            null,
            target,
            DamageType.ENERGY,
            actualDamage*modifier,
            empDamage*modifier,
            maxDistance,
            null,
            damageArcThickness,
            arcFringeColor,
            arcCoreColor
        ) // manually play sounds, since no sound normally plays when striking hulks
        Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 2.3f, coordinatesToSpawnArcFrom, Vector2f(0f, 0f))
        Global.getSoundPlayer().playSound("MCTE_hyperStormArcSound", 1f, 1f, target.location, Vector2f(0f, 0f))
        doMainArcLighting(target.location, coordinatesToSpawnArcFrom)

        val angle = VectorUtils.getAngle(coordinatesToSpawnArcFrom, target.location)
        target.applyForce(angle, MCTE_settings.HYPERSTORM_ARC_FORCE*modifier)
    }

    private fun getArcOverallDamageMod(cell: MutableSet<Cloud>, engine: CombatEngineAPI, coordinatesToSpawnArcFrom: Vector2f, source: ShipAPI, target: CombatEntityAPI, maxDistance: Float, actualDamage: Float): Float {
        var modifier = 1f
        if (!target.isTangible()) {
            return 0f
        }
        return modifier
    }

    fun telegraphArc(cell: MutableSet<Cloud>, params: cloudParams? = null, engine: CombatEngineAPI, coordinatesToSpawnArcFrom: Vector2f, source: ShipAPI,
                     target: CombatEntityAPI, maxDistance: Float = Float.MAX_VALUE, volume: Float) {
        val distance = MathUtils.getDistance(coordinatesToSpawnArcFrom, target.location)
        if ((!target.isTangible() || distance > maxDistance) && params != null) {
            cosmeticTelegraphArc(cell, engine, coordinatesToSpawnArcFrom, params.getArcSite(), volume)
            return
        }
        val energyDamage = 3f
        val empDamage = 1f
        val modifier = getArcOverallDamageMod(cell, engine, coordinatesToSpawnArcFrom, source, target, maxDistance, energyDamage)
        engine.spawnEmpArc(
            source,
            coordinatesToSpawnArcFrom,
            null,
            target,
            DamageType.ENERGY,
            energyDamage*modifier,
            empDamage*modifier,
            maxDistance,
            null,
            1f,
            arcFringeColor,
            arcCoreColor
        )
        Global.getSoundPlayer().playSound("MCTE_telegraphArcSound", 1f, volume, target.location, Vector2f(0f, 0f))
        doTelegraphArcLighting(target.location, coordinatesToSpawnArcFrom)
    }

    fun cosmeticTelegraphArc(cell: MutableSet<Cloud>, engine: CombatEngineAPI, coordinatesToSpawnArcFrom: Vector2f, target: Vector2f, volume: Float) {
        engine.spawnEmpArcVisual(
            coordinatesToSpawnArcFrom,
            null,
            target,
            null,
            1f,
            arcFringeColor,
            arcCoreColor
        )
        Global.getSoundPlayer().playSound("MCTE_telegraphArcSound", 1f, volume, target, Vector2f(0f, 0f))
        doTelegraphArcLighting(target, coordinatesToSpawnArcFrom)
    }

    fun cosmeticArc(cell: MutableSet<Cloud>, engine: CombatEngineAPI, coordinatesToSpawnArcFrom: Vector2f, target: Vector2f) {
        engine.spawnEmpArcVisual(
            coordinatesToSpawnArcFrom,
            null,
            target,
            null,
            damageArcThickness,
            arcFringeColor,
            arcCoreColor
        )
        Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 2.3f, coordinatesToSpawnArcFrom, Vector2f(0f, 0f))
        Global.getSoundPlayer().playSound("MCTE_hyperStormArcSound", 1f, 0.09f, target, Vector2f(0f, 0f))
        doMainArcLighting(target, coordinatesToSpawnArcFrom)
    }

    private fun doMainArcLighting(target: Vector2f, coordinatesToSpawnArcFrom: Vector2f) {
        if (!MCTE_debugUtils.graphicsLibEnabled) return
        val dist: Float = MathUtils.getDistance(coordinatesToSpawnArcFrom, target)
        val engine = Global.getCombatEngine()
        val viewPort = engine.viewport
        val size = (33f * sqrt(dist))
        if (!viewPort.isNearViewport(target, size) || !viewPort.isNearViewport(coordinatesToSpawnArcFrom, size)) return
        val intensity = MathUtils.getRandomNumberInRange(1.05f, 1.1f)
        val specIntensity = MathUtils.getRandomNumberInRange(1.6f, 1.7f)
        val specSize = (50f * sqrt(dist))
        val specMult = 3f
        doArcLighting(target, coordinatesToSpawnArcFrom, intensity, size, specIntensity, specSize, specMult,
            0.39f, 0.47f, 0.5f, 0.6f)
    }

    private fun doTelegraphArcLighting(target: Vector2f, coordinatesToSpawnArcFrom: Vector2f) {
        if (!MCTE_debugUtils.graphicsLibEnabled) return
        val dist: Float = MathUtils.getDistance(coordinatesToSpawnArcFrom, target)
        val size = (5f * sqrt(dist))
        val intensity = 0.2f
        val specIntensity = 0.5f
        val specSize = (10f * sqrt(dist))
        val specMult = 0.9f
        doArcLighting(target, coordinatesToSpawnArcFrom, intensity, size, specIntensity, specSize, specMult,
            0.1f, 0.2f, 0.3f, 0.4f)
    }

    private fun doArcLighting(targetLoc: Vector2f, coordinatesToSpawnArcFrom: Vector2f,
        intensity: Float, size: Float,
        specIntensity: Float, specSize: Float, specularMult: Float,
        fadeOutMin: Float, fadeOutMax: Float,
        specFadeOutMin: Float, specFadeOutMax: Float) {
        if (!MCTE_debugUtils.graphicsLibEnabled) return
        val engine = Global.getCombatEngine()
        val viewPort = engine.viewport
        if (!viewPort.isNearViewport(targetLoc, size) || !viewPort.isNearViewport(coordinatesToSpawnArcFrom, size)) return

        val midLoc = MathUtils.getMidpoint(coordinatesToSpawnArcFrom, targetLoc)
        val zero = Vector2f(0f, 0f)

        val light = StandardLight(
            coordinatesToSpawnArcFrom,
            targetLoc,
            zero,
            zero,
            null
        )

        light.intensity = intensity
        light.size = size
        light.setColor(arcFringeColor)
        light.fadeOut(MathUtils.getRandomNumberInRange(fadeOutMin, fadeOutMax))
        LightShader.addLight(light)

        val specLight = StandardLight(
            midLoc,
            zero,
            zero,
            null
        )
        specLight.intensity = specIntensity
        specLight.specularMult = specularMult
        specLight.size = specSize
        specLight.setColor(arcCoreColor)
        specLight.fadeOut(MathUtils.getRandomNumberInRange(specFadeOutMin, specFadeOutMax))
        LightShader.addLight(specLight)
    }
}

package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.cloudCell
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.MCTE_shipUtils.isTangible
import org.dark.shaders.light.LightShader
import org.dark.shaders.light.StandardLight
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.combat.applyForce
import org.lwjgl.util.vector.Vector2f
import kotlin.math.sqrt

object MCTE_arcUtils {

    @JvmStatic
    @JvmOverloads
    fun arc(cell: cloudCell, coordinatesToSpawnArcFrom: Vector2f, source: ShipAPI, target: CombatEntityAPI,
            maxDistance: Float = Float.MAX_VALUE, actualDamage: Float, empDamage: Float, times: Int = 1) {

        var timesLeft = times

        val engine = Global.getCombatEngine() ?: return
        val distance = MathUtils.getDistance(coordinatesToSpawnArcFrom, target.location)
        if (!target.isTangible() || distance > maxDistance) {
            cosmeticArc(coordinatesToSpawnArcFrom, cell.getArcSite())
            return
        }
        val modifier = getArcOverallDamageMod(target)

        while (timesLeft > 0) {
            timesLeft--
            engine.spawnEmpArc(
                source,
                coordinatesToSpawnArcFrom,
                null,
                target,
                DamageType.ENERGY,
                actualDamage * modifier,
                empDamage * modifier,
                maxDistance,
                null,
                MCTE_shipUtils.damageArcThickness,
                MCTE_shipUtils.arcFringeColor,
                MCTE_shipUtils.arcCoreColor
            ) // manually play sounds, since no sound normally plays when striking hulks
        }
        Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 2.5f, coordinatesToSpawnArcFrom, Vector2f(0f, 0f))
        Global.getSoundPlayer().playSound("MCTE_hyperStormArcSound", 1f, 1f, target.location, Vector2f(0f, 0f))
        doMainArcLighting(target.location, coordinatesToSpawnArcFrom)

        val angle = VectorUtils.getAngle(coordinatesToSpawnArcFrom, target.location)
        target.applyForce(angle, MCTE_settings.HYPERSTORM_ARC_FORCE*modifier)
    }

    private fun getArcOverallDamageMod(target: CombatEntityAPI): Float {
        var modifier = 1f
        if (!target.isTangible()) {
            return 0f
        }
        return modifier
    }

    fun telegraphArc(cell: cloudCell, coordinatesToSpawnArcFrom: Vector2f, source: ShipAPI,
                     target: CombatEntityAPI, maxDistance: Float = Float.MAX_VALUE, volume: Float) {

        val engine = Global.getCombatEngine() ?: return
        val distance = MathUtils.getDistance(coordinatesToSpawnArcFrom, target.location)
        if ((!target.isTangible() || distance > maxDistance)) {
            cosmeticTelegraphArc(coordinatesToSpawnArcFrom, cell.getArcSite(), volume)
            return
        }
        val energyDamage = 3f
        val empDamage = 1f
        val modifier = getArcOverallDamageMod(target)
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
            MCTE_shipUtils.arcFringeColor,
            MCTE_shipUtils.arcCoreColor
        )
        Global.getSoundPlayer().playSound("MCTE_telegraphArcSound", 1f, volume, target.location, Vector2f(0f, 0f))
        doTelegraphArcLighting(target.location, coordinatesToSpawnArcFrom)
    }

    fun cosmeticTelegraphArc(coordinatesToSpawnArcFrom: Vector2f, target: Vector2f, volume: Float) {
        val engine: CombatEngineAPI = Global.getCombatEngine() ?: return
        engine.spawnEmpArcVisual(
            coordinatesToSpawnArcFrom,
            null,
            target,
            null,
            1f,
            MCTE_shipUtils.arcFringeColor,
            MCTE_shipUtils.arcCoreColor
        )
        Global.getSoundPlayer().playSound("MCTE_telegraphArcSound", 1f, volume, target, Vector2f(0f, 0f))
        doTelegraphArcLighting(target, coordinatesToSpawnArcFrom)
    }

    fun cosmeticArc(coordinatesToSpawnArcFrom: Vector2f, target: Vector2f) {
        val engine: CombatEngineAPI = Global.getCombatEngine() ?: return
        engine.spawnEmpArcVisual(
            coordinatesToSpawnArcFrom,
            null,
            target,
            null,
            MCTE_shipUtils.damageArcThickness,
            MCTE_shipUtils.arcFringeColor,
            MCTE_shipUtils.arcCoreColor
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
        val size = (34f * sqrt(dist))
        if (!viewPort.isNearViewport(target, size) || !viewPort.isNearViewport(coordinatesToSpawnArcFrom, size)) return
        val intensity = MathUtils.getRandomNumberInRange(1.7f, 1.8f)
        val specIntensity = MathUtils.getRandomNumberInRange(1.4f, 1.9f)
        val specSize = (59f * sqrt(dist))
        val specMult = 5f
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
                              specFadeOutMin: Float, specFadeOutMax: Float,) {
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
        light.setColor(MCTE_shipUtils.arcFringeColor)
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
        specLight.setColor(MCTE_shipUtils.arcCoreColor)
        specLight.fadeOut(MathUtils.getRandomNumberInRange(specFadeOutMin, specFadeOutMax))
        LightShader.addLight(specLight)
    }
}
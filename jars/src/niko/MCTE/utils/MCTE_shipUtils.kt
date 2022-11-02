package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_EMP_DAMAGE
import niko.MCTE.utils.MCTE_settings.HYPERSTORM_ENERGY_DAMAGE
import niko.MCTE.utils.MCTE_shipUtils.cosmeticArc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

object MCTE_shipUtils {

    @JvmStatic
    fun CombatEntityAPI.isTangible(): Boolean {
        var isTangible = false
        if (this is ShipAPI) {
            if (isPhased) return false
        }
        return (collisionClass != CollisionClass.NONE)
    }

    @JvmStatic
    fun ShipAPI.isAffectedByNebulaSecondary(nebula: CombatNebulaAPI): Boolean {
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

    fun Cloud.arc(engine: CombatEngineAPI, coordinatesToSpawnArcFrom: Vector2f, source: ShipAPI, target: CombatEntityAPI, maxDistance: Float = Float.MAX_VALUE) {
        var energyDamage = HYPERSTORM_ENERGY_DAMAGE
        var empDamage = HYPERSTORM_EMP_DAMAGE
        if (!target.isTangible()) {
            energyDamage = 0f
            empDamage = 0f
        }
        engine.spawnEmpArc(
            source,
            coordinatesToSpawnArcFrom,
            null,
            target,
            DamageType.ENERGY,
            energyDamage,
            empDamage,
            maxDistance,
            null,
            50f,
            Color(154, 51, 255, 255),
            Color(255, 255, 255, 255)
        ) // manually play sounds, since no sound normally plays when striking hulks
        Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 1.5f, coordinatesToSpawnArcFrom, Vector2f(0f, 0f))
        Global.getSoundPlayer().playSound("MCTE_hyperStormArcSound", 1f, 1f, target.location, Vector2f(0f, 0f))
    }
    fun Cloud.telegraphArc(engine: CombatEngineAPI, coordinatesToSpawnArcFrom: Vector2f, source: ShipAPI, target: CombatEntityAPI, maxDistance: Float = Float.MAX_VALUE) {
        var energyDamage = 3f
        var empDamage = 1f
        val modifier = 1f
        if (!target.isTangible()) {
            modifier = 0f
        } else {
            if (target is ShipAPI) {
                val variant = target.variant
                if (variant.hasHullMod(HullMods.SOLAR_SHIELDING)) modifier -= 0.3f
            }
        }
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
            Color(106, 162, 215, 255),
            Color(255, 255, 255, 255)
        )
        Global.getSoundPlayer().playSound("MCTE_telegraphArcSound", 1f, 1f, target.location, Vector2f(0f, 0f))
    }

    fun Cloud.cosmeticArc(engine: CombatEngineAPI, coordinatesToSpawnArcFrom: Vector2f, target: Vector2f) {
        engine.spawnEmpArcVisual(
            coordinatesToSpawnArcFrom,
            null,
            target,
            null,
            3f,
            Color(154, 51, 255, 255),
            Color(255, 255, 255, 255)
        )
        Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 1.5f, coordinatesToSpawnArcFrom, Vector2f(0f, 0f))
        Global.getSoundPlayer().playSound("MCTE_hyperStormArcSound", 1f, 1f, target, Vector2f(0f, 0f))
    }
}

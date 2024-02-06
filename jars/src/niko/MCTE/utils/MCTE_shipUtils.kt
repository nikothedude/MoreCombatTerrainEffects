package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.util.Misc
import niko.MCTE.utils.MCTE_nebulaUtils.getNebulaTile
import niko.MCTE.utils.MCTE_nebulaUtils.getNebulaTiles
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.plus
import org.lazywizard.lazylib.ext.rotate
import org.lazywizard.lazylib.ext.rotateAroundPivot
import org.lwjgl.util.vector.Vector2f
import java.awt.Color


object MCTE_shipUtils {

    const val NEBULA_BUFFER_SIZE: Float = 125f
    val damageArcThickness = 45f
    val arcFringeColor = Color(139, 45, 253, 255)
    val arcCoreColor = Color(158, 79, 255, 255)

    @JvmStatic
    fun CombatEntityAPI.isTangible(): Boolean {
        if (this is ShipAPI) { // cant find a way to overload extension methods
            if (isPhased) return false
        }
        return (collisionClass != CollisionClass.NONE)
    }

    @JvmStatic
    fun CombatEntityAPI.isInsideNebulaAuxillary(): Boolean { // a more accurate method of determing if a ship is in a nebula
        val nebula = Global.getCombatEngine()?.nebula ?: return false
        if (!isTangible()) return false
        val coreTile = getNebulaTile()
        if (coreTile != null) if (nebula.tileHasNebula(coreTile[0], coreTile[1])) return true

        val bounds: BoundsAPI? = exactBounds
        if (bounds != null) {
            bounds.update(location, facing)
            for (segment in bounds.segments) {
                val loc1 = segment.p1// + location).rotateAroundPivot(location, this.facing)
                val tile1 = getNebulaTile(loc1)
                if (tile1 != null) {
                    if (nebula.tileHasNebula(tile1[0], tile1[1])) {
                        return true
                    }
                }
                if (nebula.locationHasNebula(loc1.x, loc1.y)) return true

                val loc2 = segment.p2// + location).rotateAroundPivot(location, this.facing)
                val tile2 = getNebulaTile(loc2)
                if (tile2 != null) {
                    if (nebula.tileHasNebula(tile2[0], tile2[1])) {
                        return true
                    }
                }
                if (nebula.locationHasNebula(loc2.x, loc2.y)) return true
            }
        }
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

    fun ShipAPI.hasInsulatedEngines(): Boolean {
        return variant.hasHullMod(HullMods.INSULATEDENGINE)
    }
}

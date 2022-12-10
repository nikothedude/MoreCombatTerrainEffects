package niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace

import com.fs.starfarer.combat.entities.terrain.Cloud
import niko.MCTE.settings.MCTE_settings
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

class cloudCell(
    var centroid: Vector2f,
    var radius: Float,
    val clouds: MutableSet<Cloud>
) {

    fun preparingToArc(): Boolean {
        return preparationScript != null
    }

    var cooldown = 0f
    var preparationScript: hyperstormArcPreparation? = null

    fun getArcSite(): Vector2f {
        val radius: Float = radius
        val ourCoordinates = centroid
        val currentx = ourCoordinates.x
        val minx = currentx - radius/1.1f
        val maxx = currentx + radius/1.1f
        val currenty = ourCoordinates.y
        val miny = currenty - radius/1.1f
        val maxy = currenty + radius/1.1f

        val adjustedx = (minx + MathUtils.getRandom().nextFloat() * (maxx - minx))
        val adjustedy = (miny + MathUtils.getRandom().nextFloat() * (maxy - miny))

        val arcSite = Vector2f(adjustedx, adjustedy)
        return arcSite
    }

    fun getArcRange(): Float {
        val minRadius = MCTE_settings.HYPERSTORM_MIN_ARC_RANGE + radius
        val maxRadius = MCTE_settings.HYPERSTORM_MAX_ARC_RANGE + radius
        return minRadius + MathUtils.getRandom().nextFloat() * (maxRadius - minRadius)
    }

    fun resetArcCooldown() {
        val min = MCTE_settings.MIN_TIME_BETWEEN_HYPERSTORM_STRIKES
        val max = MCTE_settings.MAX_TIME_BETWEEN_HYPERSTORM_STRIKES
        cooldown = min + MathUtils.getRandom().nextFloat() * (max - min)
    }

}
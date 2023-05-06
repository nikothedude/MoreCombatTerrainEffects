package niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace

import com.fs.starfarer.api.Global
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
        val engine = Global.getCombatEngine() ?: return Vector2f(0f, 0f)
        val nebula = engine.nebula ?: return Vector2f(0f, 0f)

        val randomLocation = clouds.randomOrNull()?.location ?: return Vector2f(0f, 0f)

        val upperBoundX: Float = (randomLocation.x) + (nebula.tileSizeInPixels)/2
        val lowerBoundX: Float = (randomLocation.x) - (nebula.tileSizeInPixels)/2
        val upperBoundY = (randomLocation.y) + (nebula.tileSizeInPixels)/2
        val lowerBoundY = (randomLocation.y) - (nebula.tileSizeInPixels)/2

        val randX = MathUtils.getRandomNumberInRange(lowerBoundX, upperBoundX)
        val randY = MathUtils.getRandomNumberInRange(lowerBoundY, upperBoundY)

        val arcSite = Vector2f(randX, randY)
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
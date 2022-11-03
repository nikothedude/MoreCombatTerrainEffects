package niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace

import org.lwjgl.util.vector.Vector2f

class cloudParams(
    var centroid: Vector2f,
    var radius: Float
) {
    fun preparingToArc(): Boolean {
        return preparationScript != null
    }

    var cooldown = 0f
    var preparationScript: hyperstormArcPreparation? = null
}

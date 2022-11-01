package niko.MCTE.scripts.everyFrames.combat.terrainEffects.debrisField

import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript

class debrisFieldEffectScript(
    val debrisDensityMult: Float,
    val hazardDensityMult: Float
): baseTerrainEffectScript() {
    val isDone = false

    override fun applyEffects(amount: Float) {
        if (isDone) return
    }

    override fun handleNotification(amount: Float) {
        TODO("Not yet implemented")
    }

    override fun handleSounds(amount: Float) {
        TODO("Not yet implemented")
    }
}
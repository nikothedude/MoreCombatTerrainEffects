package niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField

abstract class baseCombatDeltaTimeScript: baseNikoCombatScript() {
    var deltaTime = 0f
    abstract val thresholdForAdvancement: Float

    protected open fun canAdvance(amount: Float): Boolean {
        deltaTime += amount
        if (deltaTime >= thresholdForAdvancement) {
            deltaTime = 0f
            return true
        }
        return false
    }
}
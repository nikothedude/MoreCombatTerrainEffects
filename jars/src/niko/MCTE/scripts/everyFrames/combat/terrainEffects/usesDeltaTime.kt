package niko.MCTE.scripts.everyFrames.combat.terrainEffects

interface usesDeltaTime {
    var deltaTime: Float
    val thresholdForAdvancement: Float

     fun canAdvance(amount: Float): Boolean {
        deltaTime += amount
        if (deltaTime >= thresholdForAdvancement) {
            deltaTime = 0f
            return true
        }
        return false
    }
}
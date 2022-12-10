package niko.MCTE.utils

import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript

object terrainScriptsTracker {
    @Transient
    val activeScripts: MutableSet<baseTerrainEffectScript> = HashSet()
}
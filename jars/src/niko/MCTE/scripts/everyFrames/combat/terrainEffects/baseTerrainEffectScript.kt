package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.MCTEEffectScript
import niko.MCTE.scripts.everyFrames.combat.baseNikoCombatScript
import niko.MCTE.utils.terrainScriptsTracker

abstract class baseTerrainEffectScript(): MCTEEffectScript() {

    abstract var effectPrototype: combatEffectTypes?

    override fun start() {
        super.start()
        terrainScriptsTracker.addScript(this)
    }

    override fun stop() {
        super.stop()
        terrainScriptsTracker.removeScript(this)
    }
}

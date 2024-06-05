package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import org.lazywizard.lazylib.MathUtils

abstract class baseNikoCombatScript(): BaseEveryFrameCombatPlugin() {
    val engine = Global.getCombatEngine()
    val random = MathUtils.getRandom()
}
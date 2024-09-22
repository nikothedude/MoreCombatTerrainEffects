package niko.MCTE.scripts.everyFrames.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import niko.MCTE.settings.MCTE_settings
import org.lazywizard.lazylib.MathUtils

abstract class baseNikoCombatScript(): BaseEveryFrameCombatPlugin() {
    val engine = Global.getCombatEngine()
    val random = MathUtils.getRandom()
}
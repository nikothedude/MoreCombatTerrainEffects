package niko.MCTE.scripts.everyFrames.combat

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.input.InputEventAPI
import niko.MCTE.settings.MCTE_settings

abstract class MCTEEffectScript: baseNikoCombatScript() {
    var initialized = false

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        initialized = true
        //terrainScriptsTracker.activeScripts += this
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return
        if (!initialized) { // i fucking hate this oml
            val engine = Global.getCombatEngine() ?: return
            init(engine)
            if (!initialized)
                return
        }

        applyEffects(amount)
        handleSounds(amount)
        handleNotification(amount)
    }

    abstract fun handleSounds(amount: Float)

    abstract fun applyEffects(amount: Float)

    open fun start() {
        engine.addPlugin(this)
    }

    open fun stop() {
        engine.removePlugin(this)
    }

    open fun handleNotification(amount: Float): Boolean {
        if (!MCTE_settings.SHOW_SIDEBAR_INFO)
            return false
        return true
    }
}
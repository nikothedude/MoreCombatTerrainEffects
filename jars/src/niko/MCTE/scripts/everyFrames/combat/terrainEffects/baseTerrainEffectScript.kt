package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.input.InputEventAPI
import niko.MCTE.combatEffectTypes
import niko.MCTE.settings.MCTE_settings.SHOW_SIDEBAR_INFO
import niko.MCTE.utils.terrainScriptsTracker

abstract class baseTerrainEffectScript(): baseNikoCombatScript() {

    var initialized = false
    abstract var effectPrototype: combatEffectTypes?

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

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        initialized = true
        //terrainScriptsTracker.activeScripts += this
    }

    open fun handleNotification(amount: Float): Boolean {
        if (!SHOW_SIDEBAR_INFO)
            return false
        return true
    }

    abstract fun handleSounds(amount: Float)

    abstract fun applyEffects(amount: Float)
    fun start() {
        engine.addPlugin(this)
        terrainScriptsTracker.addScript(this)
    }

    fun stop() {
        engine.removePlugin(this)
        terrainScriptsTracker.removeScript(this)
    }
}

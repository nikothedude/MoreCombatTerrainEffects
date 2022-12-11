package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.utils.terrainScriptsTracker

abstract class baseTerrainEffectScript: baseNikoCombatScript() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        applyEffects(amount)
        handleSounds(amount)
        handleNotification(amount)
    }

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        //terrainScriptsTracker.activeScripts += this
    }

    abstract fun handleNotification(amount: Float)

    abstract fun handleSounds(amount: Float)

    abstract fun applyEffects(amount: Float)
    fun start() {
        engine.addPlugin(this)
       // terrainScriptsTracker.activeScripts += this
    }

    fun stop() {
        engine.removePlugin(this)
        //terrainScriptsTracker.activeScripts -= this
    }
}

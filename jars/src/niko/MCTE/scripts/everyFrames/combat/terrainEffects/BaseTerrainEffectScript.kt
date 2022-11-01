package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI

abstract class baseTerrainEffectScript: baseNikoCombatScript() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        applyEffects(amount)
        handleSounds(amount)
        handleNotification(amount)
    }

    abstract fun handleNotification(amount: Float)

    abstract fun handleSounds(amount: Float)

    abstract fun applyEffects(amount: Float)
}

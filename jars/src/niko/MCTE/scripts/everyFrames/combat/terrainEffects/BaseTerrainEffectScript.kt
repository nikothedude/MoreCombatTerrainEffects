package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI

abstract class baseTerrainEffectScript: baseNikoCombatScript() {

    protected val affectedShips: HashMap<ShipAPI, Boolean> = HashMap()

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        applyEffectsToShips()
        handleSounds()
        handleNotification()
    }

    abstract fun handleNotification()

    abstract fun handleSounds()

    abstract fun applyEffectsToShips()
}

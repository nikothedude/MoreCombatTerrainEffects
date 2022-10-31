package niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.input.InputEventAPI

class missileScrambledSimulationScript(
    missile: MissileAPI
): baseCombatDeltaTimeScript() {
    override val thresholdForAdvancement: Float = 0.2f

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return
        if (!canAdvance(amount)) return


    }
}
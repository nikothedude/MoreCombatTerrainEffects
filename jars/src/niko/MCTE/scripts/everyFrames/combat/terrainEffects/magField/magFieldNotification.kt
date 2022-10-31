package niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI

class magFieldNotification(val isStorm: Boolean): baseNikoCombatScript() {
    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return
        if (isStorm) {
            Global.getSoundPlayer().playUILoop("terrain_magstorm", 1f, 1f)
        } else {
            Global.getSoundPlayer().playUILoop("terrain_magfield", 1f, 1f)
        }
        val engine = Global.getCombatEngine()
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_magFieldInterference",
            icon,
            "Magnetic Field",
            "test",
            true)
    }
}
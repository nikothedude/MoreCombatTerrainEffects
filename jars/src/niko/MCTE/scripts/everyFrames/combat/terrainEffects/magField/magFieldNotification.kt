package niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI

class magFieldNotification: baseNikoCombatScript() {
    var playerShip: ShipAPI? = null

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        if (Global.getCurrentState() != GameState.COMBAT) return

        val engine = Global.getCombatEngine()
        val playerShip = engine.playerShip
        if (playerShip != this.playerShip) {
            this.playerShip = playerShip
            val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_magFieldInterference",
                icon,
                "Magnetic Field",
                "test",
                true)
        }
    }
}
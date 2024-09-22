package niko.MCTE.scripts.everyFrames.combat.objectiveEffects

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import niko.MCTE.scripts.everyFrames.combat.MCTEEffectScript

class CommsRelayEffectScript(
    val owner: Int,
    val CPRate: Float,
    val battle: BattleAPI
): MCTEEffectScript() {
    var boostedShip: ShipAPI? = null
        set(value) {
            field?.mutableStats?.dynamic?.getStat(Stats.COMMAND_POINT_RATE_FLAT)?.unmodify(modId)
            value?.mutableStats?.dynamic?.getStat(Stats.COMMAND_POINT_RATE_FLAT)?.modifyFlat(modId, CPRate)

            field = value
        }

    companion object {
        const val modId = "MCTE_commsRelayEffect"
    }

    override fun handleSounds(amount: Float) {
        return
    }

    override fun applyEffects(amount: Float) {
        if (boostedShip == null || !shouldAffectShip(boostedShip!!)) {
            boostedShip = engine.getFleetManager(owner).deployedCopyDFM?.firstOrNull { shouldAffectShip(it.ship) }?.ship
        }
    }

    override fun handleNotification(amount: Float): Boolean {
        if (!super.handleNotification(amount)) return false
        val playerShip = engine.playerShip ?: return false
        if (!shouldAffectShip(playerShip)) return false

        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_neutral")
        if (boostedShip != playerShip) {
            boostedShip = playerShip
        }

        engine.maintainStatusForPlayerShip(
            "MCTE_commsRelayScriptNotifOne",
            icon,
            "External comms network",
            "External comm relays routing data through flagship",
            false
        )

        return true
    }

    private fun shouldAffectShip(ship: ShipAPI): Boolean {
        return (ship.isAlive && !ship.isFighter && ship.owner == owner)
    }
}
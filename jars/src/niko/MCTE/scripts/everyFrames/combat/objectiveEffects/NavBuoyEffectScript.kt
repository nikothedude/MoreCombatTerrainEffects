package niko.MCTE.scripts.everyFrames.combat.objectiveEffects

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import niko.MCTE.scripts.everyFrames.combat.MCTEEffectScript

class NavBuoyEffectScript(
    val owner: Int,
    val strength: Float,
    val battle: BattleAPI
): MCTEEffectScript() {
    var boostedShip: ShipAPI? = null
        set(value) {
            field?.mutableStats?.dynamic?.getMod(Stats.COORDINATED_MANEUVERS_FLAT)?.unmodify(modId)
            value?.mutableStats?.dynamic?.getMod(Stats.COORDINATED_MANEUVERS_FLAT)?.modifyFlat(modId, strength)

            field = value
        }

    companion object {
        const val modId = "MCTE_navBuoyEffect"
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
            "MCTE_navBuoyNotif",
            icon,
            "External nav relay",
            "External nav relays routing data through flagship",
            false
        )

        return true
    }

    private fun shouldAffectShip(ship: ShipAPI): Boolean {
        return (ship.isAlive && !ship.isFighter && ship.owner == owner)
    }
}
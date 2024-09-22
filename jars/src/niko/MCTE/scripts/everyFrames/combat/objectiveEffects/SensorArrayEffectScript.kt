package niko.MCTE.scripts.everyFrames.combat.objectiveEffects

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.combat.ShipAPI
import niko.MCTE.scripts.everyFrames.combat.MCTEEffectScript
import niko.MCTE.utils.MCTE_stringUtils

class SensorArrayEffectScript(
    val owner: Int,
    val strength: Float,
    val battle: BattleAPI
): MCTEEffectScript() {
    override fun handleSounds(amount: Float) {
        return
    }

    companion object {
        const val modId = "MCTE_sensorArrayModifier"
    }

    override fun applyEffects(amount: Float) {
        for (deployed in engine.getFleetManager(owner).deployedCopyDFM) {
            val ship = deployed.ship
            if (!shouldAffectShip(ship)) continue
            ship.mutableStats.ballisticWeaponRangeBonus.modifyFlat(modId, strength)
            ship.mutableStats.beamWeaponRangeBonus.modifyFlat(modId, strength)
            ship.mutableStats.energyWeaponRangeBonus.modifyFlat(modId, strength)
        }
    }

    override fun handleNotification(amount: Float): Boolean {
        if (!super.handleNotification(amount)) return false
        val playerShip = engine.playerShip ?: return false
        if (!shouldAffectShip(playerShip)) return false

        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_neutral")
        engine.maintainStatusForPlayerShip(
            "MCTE_sensorArrayNotif",
            icon,
            "External target computing",
            "Non-missile weapon range increased by +$strength",
            false
        )

        return true
    }

    private fun shouldAffectShip(ship: ShipAPI): Boolean {
        return (ship.isAlive && !ship.isFighter && ship.owner == owner)
    }
}
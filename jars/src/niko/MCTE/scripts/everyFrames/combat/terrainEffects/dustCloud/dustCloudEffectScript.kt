package niko.MCTE.scripts.everyFrames.combat.terrainEffects.dustCloud

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.terrainCombatEffectIds

class dustCloudEffectScript(
    val baseSpeedMalice: Float
): baseTerrainEffectScript() {

    protected val affectedShips: MutableMap<ShipAPI, Boolean> = HashMap()

    override fun applyEffects(amount: Float) {
        for (ship: ShipAPI in engine.ships) {
            if (affectedShips[ship] == null) {
                val mutableStats = ship.mutableStats
                mutableStats.maxSpeed.modifyFlat(terrainCombatEffectIds.dustCloudEffect, baseSpeedMalice)
                mutableStats.acceleration.modifyFlat(terrainCombatEffectIds.dustCloudEffect, baseSpeedMalice)
                mutableStats.deceleration.modifyFlat(terrainCombatEffectIds.dustCloudEffect, baseSpeedMalice)

                mutableStats.missileMaxSpeedBonus.modifyFlat(terrainCombatEffectIds.dustCloudEffect, baseSpeedMalice)
                mutableStats.missileAccelerationBonus.modifyFlat(terrainCombatEffectIds.dustCloudEffect, baseSpeedMalice)

                mutableStats.projectileSpeedMult.modifyFlat(terrainCombatEffectIds.dustCloudEffect, baseSpeedMalice)

                affectedShips[ship] = true
            }
        }
    }

    /*override fun handleNotification(amount: Float) {
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_dustCloud1",
            icon,
            "Dust Cloud",
            "Ship, Missile, Projectile speed reduced by $baseSpeedMalice",
            true)
    }*/

    override fun handleSounds(amount: Float) {
        return
    }
}
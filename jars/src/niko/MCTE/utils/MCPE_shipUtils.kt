package niko.MCTE.utils

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.combat.ShipAPI

object MCPE_shipUtils {

    @JvmStatic
    fun ShipAPI.isTangible(): Boolean {
        return (!isPhased)
    }

    @JvmStatic
    fun ShipAPI.isAffectedByNebulaSecondary(nebula: CombatNebulaAPI): Boolean {
        if (!isTangible()) return false
        if (nebula.locationHasNebula(location.x, location.y)) return true
        return false
    }
}
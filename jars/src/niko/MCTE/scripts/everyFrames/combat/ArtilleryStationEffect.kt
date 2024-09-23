package niko.MCTE.scripts.everyFrames.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.loading.WeaponGroupSpec
import com.fs.starfarer.api.loading.WeaponGroupType
import indevo.industries.artillery.entities.ArtilleryStationEntityPlugin
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f

//TODO: Finish

class ArtilleryStationEffect(
    val owner: Int,
    val angle: Float,
    val artilleryType: String
): MCTEEffectScript() {

    var fire = false
    val dummyShip = createNewDummyShip()
    val weaponId = when (artilleryType) {
        ArtilleryStationEntityPlugin.TYPE_MORTAR -> "IndEvo_artillery_mortar"
        ArtilleryStationEntityPlugin.TYPE_MISSILE -> "IndEvo_artillery_missile"
        ArtilleryStationEntityPlugin.TYPE_RAILGUN -> "IndEvo_artillery_railgun"
        else -> {
            "heavymortar"
        }
    }

    companion object {

    }

    private fun createNewDummyShip(): ShipAPI {
        val hullSpec = Global.getSettings().getHullSpec("claw")
        val variant = Global.getSettings().createEmptyVariant("claw_Fighter", hullSpec)

        variant.addWeapon("WS 001", weaponId)

        var g = WeaponGroupSpec(WeaponGroupType.LINKED)
        g.addSlot("WS 001")
        variant.addWeaponGroup(g)

        var drone = Global.getCombatEngine().createFXDrone(variant)
        drone.location.set(0f, 0f)
        drone.isDrone = true
        drone.collisionClass = CollisionClass.NONE
        drone.alphaMult = 0f
        drone.owner = owner
        drone.getMutableStats().getHullDamageTakenMult().modifyMult("dem", 0f) // so it's non-targetable

        drone.mutableStats.ballisticWeaponRangeBonus.modifyMult("dem", 100f)
        drone.mutableStats.missileWeaponRangeBonus.modifyMult("dem", 100f)
        drone.name = "Artillery"

        drone.mutableStats.fluxCapacity.modifyFlat("dem", 5000000f)
        drone.mutableStats.fluxDissipation.modifyFlat("dem", 5000000f)

        engine.addEntity(drone)
        return drone
    }

    override fun handleSounds(amount: Float) {
        return
    }

    override fun applyEffects(amount: Float) {
        if (engine.isPaused) return

        if (fire) {
            fire = false

            val randomShip = engine.ships.randomOrNull() ?: return
            val angle = VectorUtils.getAngle(dummyShip.location, randomShip.location)
            dummyShip.facing = angle

            dummyShip.allWeapons.firstOrNull()?.setForceFireOneFrame(true)
        }
    }
}
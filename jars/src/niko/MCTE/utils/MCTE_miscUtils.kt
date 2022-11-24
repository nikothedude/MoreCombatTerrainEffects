package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.fs.starfarer.combat.entities.terrain.A
import com.fs.starfarer.combat.entities.terrain.Cloud
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.roundToInt

object MCTE_miscUtils {
    private val debrisFieldShipSourcePicker: WeightedRandomPicker<String> = WeightedRandomPicker()

    fun getGlobalDebrisFieldShipSourcePicker(): WeightedRandomPicker<String> {
        if (debrisFieldShipSourcePicker.isEmpty) {
            buildDebrisFieldSource()
        }
        return debrisFieldShipSourcePicker
    }

    private fun buildDebrisFieldSource() {
        val sector = Global.getSector() ?: return MCTE_debugUtils.displayError("buildDebrisFieldSource called with null sector")
        val settings = Global.getSettings()
        val independants = sector.getFaction(Factions.INDEPENDENT)

        val roleEntryIds: Set<String> = setOf(ShipRoles.COMBAT_SMALL, ShipRoles.COMBAT_MEDIUM, ShipRoles.COMBAT_LARGE,
            ShipRoles.COMBAT_CAPITAL, ShipRoles.CARRIER_SMALL, ShipRoles.CARRIER_MEDIUM, ShipRoles.CARRIER_LARGE,
            ShipRoles.PHASE_SMALL, ShipRoles.PHASE_MEDIUM, ShipRoles.PHASE_LARGE, ShipRoles.CIV_RANDOM,
            ShipRoles.COMBAT_FREIGHTER_SMALL, ShipRoles.COMBAT_FREIGHTER_MEDIUM, ShipRoles.COMBAT_FREIGHTER_LARGE,
            ShipRoles.FREIGHTER_SMALL, ShipRoles.FREIGHTER_MEDIUM, ShipRoles.FREIGHTER_LARGE,
            ShipRoles.TANKER_SMALL, ShipRoles.TANKER_MEDIUM, ShipRoles.TANKER_LARGE,
            ShipRoles.PERSONNEL_SMALL, ShipRoles.PERSONNEL_MEDIUM, ShipRoles.PERSONNEL_LARGE,
            ShipRoles.TUG, ShipRoles.CRIG, ShipRoles.UTILITY)

        for (id in roleEntryIds) {
            val roleEntries = settings.getDefaultEntriesForRole(id)
            for (entry in roleEntries) {
                if (independants.knowsShip(Global.getSettings().getVariant(entry.variantId).hullSpec.hullId)) {
                    debrisFieldShipSourcePicker.add(entry.variantId, entry.weight)
                }
            }
        }
    }

    fun CombatEngineAPI.getAllObjects(): MutableSet<CombatEntityAPI> {
        val allEntities = HashSet<CombatEntityAPI>()
        allEntities.addAll(listOf(ships, projectiles, asteroids).flatten())

        return allEntities
    }

    fun applyForceWithSuppliedMass(entity: CombatEntityAPI, mass: Float, direction: Vector2f, force: Float) {
        // Filter out forces without a direction
        var force = force
        if (VectorUtils.isZeroVector(direction)) {
            return
        }

        // Force is far too weak otherwise
        force *= 100f

        // Avoid divide-by-zero errors...
        val clampedMass = Math.max(1f, mass)
        // Calculate the velocity change and its resulting vector
        // Don't bother going over Starsector's speed cap
        val velChange = Math.min(1250f, force / clampedMass)
        val dir = Vector2f()
        direction.normalise(dir)
        dir.scale(velChange)
        // Apply our velocity change
        Vector2f.add(dir, entity.velocity, entity.velocity)
    }
}

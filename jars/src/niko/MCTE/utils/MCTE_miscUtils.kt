package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f

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

    fun replaceExistingEffect(valuesMap: MutableMap<ShipAPI, MutableMap<StatBonus, MutableMap<String, originalTerrainValue>>>, compensation: Float, peakCRKey: String, crLossKey: String, ship: ShipAPI, mutableStats: MutableShipStatsAPI) {
        var compensation = compensation
        if (compensation == 1f) compensation += 0.000000000000001f //since not doing this doesnt cause a recompute
        val peakCRDurationValue = getOriginalValue(valuesMap, mutableStats.peakCRDuration, peakCRKey, ship)
        if (peakCRDurationValue != null) {
            mutableStats.peakCRDuration.modifyMult(peakCRKey, (compensation + peakCRDurationValue.value) - (compensation * peakCRDurationValue.value))
        }
        val crLossPerSecondValue = getOriginalValue(valuesMap, mutableStats.crLossPerSecondPercent, crLossKey, ship)
        if (crLossPerSecondValue != null) {
            mutableStats.crLossPerSecondPercent.modifyMult(crLossKey, (compensation + crLossPerSecondValue.value) - (compensation * crLossPerSecondValue.value))
        }
    }

    fun getOriginalValue(valuesMap: MutableMap<ShipAPI, MutableMap<StatBonus, MutableMap<String, originalTerrainValue>>>, stat: StatBonus, key: String, ship: ShipAPI): originalTerrainValue? {
        val cachedValue = valuesMap[ship]?.get(stat)?.get(key)
        if (cachedValue != null) return cachedValue
        for (mod in stat.multBonuses) {
            if (mod.key == key) {
                val value = mod.value.value
                val originalValue = originalTerrainValue(key, value)
                val shipMap = valuesMap[ship]
                if (shipMap == null) valuesMap[ship] = HashMap()
                val statMap = valuesMap[ship]!![stat]
                if (statMap == null) valuesMap[ship]!![stat] = HashMap()
                valuesMap[ship]!![stat]!![key] = originalValue
                return originalValue
            }
        }
        return null
    }

    class originalTerrainValue(val key: String, val value: Float)
}

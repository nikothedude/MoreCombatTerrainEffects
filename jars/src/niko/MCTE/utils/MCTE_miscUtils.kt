package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles
import com.fs.starfarer.api.loading.RoleEntryAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.fs.starfarer.combat.entities.terrain.A
import com.fs.starfarer.combat.entities.terrain.Cloud
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.util.Objects
import javax.management.relation.Role

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

    private const val failuresTilDecision = 900
    private const val incrementValue = 1

    fun getCellCentroid(nebulaHandler: CombatNebulaAPI, nebulaCell: MutableSet<Cloud>, ourCoordinates: Vector2f? = null): Vector2f? {
        if (nebulaHandler is A) {
            var ourCoordinates = ourCoordinates
            if (ourCoordinates == null) {
                ourCoordinates = nebulaCell.firstOrNull()?.`return`() ?: return null
            }
            val firstCloud = nebulaCell.firstOrNull() ?: return null

            val topCoord = getTopCoord(nebulaHandler, nebulaCell, Vector2f(ourCoordinates))
            val bottomCoord = getBottomCoord(nebulaHandler, nebulaCell, Vector2f(ourCoordinates))
            val leftCoord = getLeftCoord(nebulaHandler, nebulaCell, Vector2f(ourCoordinates))
            val rightCoord = getRightCoord(nebulaHandler, nebulaCell, Vector2f(ourCoordinates))

            val centroidX = (topCoord.x + rightCoord.x + leftCoord.x + bottomCoord.x)/4
            val centroidY = (topCoord.y + rightCoord.y + leftCoord.y + bottomCoord.y)/4

            val centroid = Vector2f(centroidX, centroidY)
            return centroid
        }
        MCTE_debugUtils.displayError("$nebulaHandler, nebula failed cast to A")
        return null
    }

    fun getTopCoord(nebulaHandler: A, nebulaCell: MutableSet<Cloud>, rayCastCoordinate: Vector2f): Vector2f {
        var failureIndex = 0

        val cachedRayCastCoordinate = Vector2f(rayCastCoordinate)
        while (failureIndex < failuresTilDecision) {
            rayCastCoordinate.y += incrementValue
            val Cloud = nebulaHandler.getCloud(rayCastCoordinate.x, rayCastCoordinate.y)
            if (!nebulaCell.contains(Cloud)) {
                if (digToSeeIfCloudInCell(nebulaHandler, nebulaCell, Cloud)) {
                    nebulaCell += Cloud
                } else {
                    failureIndex++
                    continue
                }
            }
            cachedRayCastCoordinate.x = rayCastCoordinate.x
            cachedRayCastCoordinate.y = rayCastCoordinate.y
            failureIndex = 0
        }

        return cachedRayCastCoordinate
    }

    fun getBottomCoord(nebulaHandler: A, nebulaCell: MutableSet<Cloud>, rayCastCoordinate: Vector2f): Vector2f {
        var failureIndex = 0

        val cachedRayCastCoordinate = Vector2f(rayCastCoordinate)
        while (failureIndex < failuresTilDecision) {
            rayCastCoordinate.y -= incrementValue
            val Cloud = nebulaHandler.getCloud(rayCastCoordinate.x, rayCastCoordinate.y)
            if (!nebulaCell.contains(Cloud)) {
                if (digToSeeIfCloudInCell(nebulaHandler, nebulaCell, Cloud)) {
                    nebulaCell += Cloud
                } else {
                    failureIndex++
                    continue
                }
            }
            cachedRayCastCoordinate.x = rayCastCoordinate.x
            cachedRayCastCoordinate.y = rayCastCoordinate.y
            failureIndex = 0
        }

        return cachedRayCastCoordinate
    }

    fun getLeftCoord(nebulaHandler: A, nebulaCell: MutableSet<Cloud>, rayCastCoordinate: Vector2f): Vector2f {
        var failureIndex = 0

        val cachedRayCastCoordinate = Vector2f(rayCastCoordinate)
        while (failureIndex < failuresTilDecision) {
            rayCastCoordinate.x -= incrementValue
            val Cloud = nebulaHandler.getCloud(rayCastCoordinate.x, rayCastCoordinate.y)
            if (!nebulaCell.contains(Cloud)) {
                if (digToSeeIfCloudInCell(nebulaHandler, nebulaCell, Cloud)) {
                    nebulaCell += Cloud
                } else {
                    failureIndex++
                    continue
                }
            }
            cachedRayCastCoordinate.x = rayCastCoordinate.x
            cachedRayCastCoordinate.y = rayCastCoordinate.y
            failureIndex = 0
        }

        return cachedRayCastCoordinate
    }

    fun getRightCoord(nebulaHandler: A, nebulaCell: MutableSet<Cloud>, rayCastCoordinate: Vector2f): Vector2f {
        var failureIndex = 0

        val cachedRayCastCoordinate = Vector2f(rayCastCoordinate)
        while (failureIndex < failuresTilDecision) {
            rayCastCoordinate.x += incrementValue
            val Cloud = nebulaHandler.getCloud(rayCastCoordinate.x, rayCastCoordinate.y)
            if (!nebulaCell.contains(Cloud)) {
                if (digToSeeIfCloudInCell(nebulaHandler, nebulaCell, Cloud)) {
                    nebulaCell += Cloud
                } else {
                    failureIndex++
                    continue
                }
            }
            cachedRayCastCoordinate.x = rayCastCoordinate.x
            cachedRayCastCoordinate.y = rayCastCoordinate.y
            failureIndex = 0
        }

        return cachedRayCastCoordinate
    }

    fun digToSeeIfCloudInCell(nebulaHandler: A, nebulaCell: MutableSet<Cloud>, cloud: Cloud?): Boolean {
        if (cloud == null) return false

        var possibleCellInhabitant = cloud.Object()
        while (possibleCellInhabitant != null) {
            if (nebulaCell.contains(possibleCellInhabitant)) {
                return true
            }
            possibleCellInhabitant = possibleCellInhabitant.Object()
        }
        return false
    }

    fun getRadiusOfCell(cell: MutableSet<Cloud>, nebula: CombatNebulaAPI, centroid: Vector2f): Float {
        if (nebula is A) {
            val topCoord = getTopCoord(nebula, cell, Vector2f(centroid))
            val bottomCoord = getBottomCoord(nebula, cell, Vector2f(centroid))
            val leftCoord = getLeftCoord(nebula, cell, Vector2f(centroid))
            val rightCoord = getRightCoord(nebula, cell, Vector2f(centroid))

            val combinedValue = (
                    MathUtils.getDistance(topCoord, centroid) +
                    MathUtils.getDistance(bottomCoord, centroid) +
                    MathUtils.getDistance(leftCoord, centroid) +
                    MathUtils.getDistance(rightCoord, centroid)
                    )/4

            return combinedValue/2
        }
        MCTE_debugUtils.displayError("$nebula, nebula failed cast to A")
        return 0f

    }

    fun CombatEngineAPI.getAllObjects(): MutableSet<CombatEntityAPI> {
        val allEntities = HashSet<CombatEntityAPI>()
        allEntities.addAll(listOf(ships, projectiles, asteroids).flatten())

        return allEntities
    }
}

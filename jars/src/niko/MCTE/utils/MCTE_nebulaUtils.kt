package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BoundsAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.combat.entities.terrain.A
import com.fs.starfarer.combat.entities.terrain.Cloud
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.plusAssign
import org.lwjgl.util.vector.Vector2f
import kotlin.math.roundToInt

object MCTE_nebulaUtils {
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

    @JvmStatic
    fun CombatEntityAPI.getNebulaTiles(): MutableSet<IntArray>? {
        val tiles: MutableSet<IntArray> = HashSet()
        val coreTile = getNebulaTile() ?: return null
        tiles += coreTile

        val bounds: BoundsAPI? = exactBounds
        if (bounds != null) {
            for (segment in bounds.segments) {
                val loc = segment.p1 ?: segment.p2 ?: continue
                loc += location
                val tile = getNebulaTile(loc) ?: continue
                if (tiles.any { it.contentEquals(tile) }) continue
                tiles += tile
            }
        }
        return tiles
    }

    @JvmStatic
    fun CombatEntityAPI.getNebulaTile(): IntArray? {
        return getNebulaTile(location)
    }

    @JvmStatic
    fun getNebulaTile(location: Vector2f): IntArray? {
        val engine = Global.getCombatEngine() ?: return null
        val nebula = engine.nebula ?: return null

        val height: Float = engine.mapHeight + (MCTE_shipUtils.NEBULA_BUFFER_SIZE * 2f)
        val width: Float = engine.mapWidth + (MCTE_shipUtils.NEBULA_BUFFER_SIZE * 2f)

        val cellSize: Float = nebula.tileSizeInPixels

        val x = location.x
        val y = location.y

        val modifiedX = (((x)+width/2)/cellSize).roundToInt()
        val modifiedY = (((y)+height/2)/cellSize).roundToInt()
        val array: IntArray = intArrayOf(modifiedX, modifiedY)

        return array
    }
}
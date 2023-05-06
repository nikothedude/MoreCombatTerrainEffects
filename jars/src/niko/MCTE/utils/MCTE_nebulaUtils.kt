package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BoundsAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI.CloudAPI
import com.fs.starfarer.combat.entities.terrain.Cloud
import com.fs.starfarer.combat.entities.terrain.A
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.plus
import org.lazywizard.lazylib.ext.plusAssign
import org.lwjgl.util.vector.Vector
import org.lwjgl.util.vector.Vector2f
import java.lang.IllegalArgumentException
import kotlin.math.roundToInt

object MCTE_nebulaUtils {
    @Transient
    private var caughtNaN: Boolean = false

    private const val failuresTilDecision = 900
    private const val incrementValue = 1

   /* fun getCellCentroid(nebulaHandler: CombatNebulaAPI, nebulaCell: MutableSet<Cloud>, ourCoordinates: Vector2f? = null): Vector2f? {
        if (nebulaHandler is A) {
            var ourCoordinates = ourCoordinates
            if (ourCoordinates == null) {
                ourCoordinates = nebulaCell.firstOrNull()?.location ?: return null
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

    fun getTopCoord(nebulaHandler: A, nebulaCell: MutableSet<CloudAPI>, rayCastCoordinate: Vector2f): Vector2f {
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
    } */

    fun digToSeeIfCloudInCell(nebulaHandler: A, nebulaCell: MutableSet<CloudAPI>, cloud: Cloud?): Boolean {
        if (cloud == null) return false

        var possibleCellInhabitant = cloud.flowDest
        while (possibleCellInhabitant != null) {
            if (nebulaCell.contains(possibleCellInhabitant)) {
                return true
            }
            possibleCellInhabitant = possibleCellInhabitant.flowDest
        }
        return false
    }

    fun getRadiusOfCell(cell: Set<CloudAPI>, coreTile: IntArray, nebula: CombatNebulaAPI): Float {
        var currentCloud: CloudAPI? = nebula.getCloud(coreTile[0], coreTile[1]) ?: return 0f
        val center = currentCloud!!.location
        if (currentCloud !in cell) return 0f

        val currentTile = coreTile.copyOf()

        while (true) {
            currentCloud = nebula.getCloud(currentTile[0] + 1, currentTile[1])
            if (currentCloud == null || currentCloud !in cell) {
                break
            }
            currentTile[0]++
        }
        var rightBound = getCoordinatesFromNebulaTile(currentTile) ?: Vector2f(0f, 0f)
        currentTile[0] = coreTile[0]
        currentTile[1] = coreTile[1]
        while (true) {
            currentCloud = nebula.getCloud(currentTile[0] - 1, currentTile[1])
            if (currentCloud == null || currentCloud !in cell) {
                break
            }
            currentTile[0]--
        }
        val leftBound = getCoordinatesFromNebulaTile(currentTile) ?: Vector2f(0f, 0f)
        currentTile[0] = coreTile[0]
        currentTile[1] = coreTile[1]
        while (true) {
            currentCloud = nebula.getCloud(currentTile[0], currentTile[1] + 1)
            if (currentCloud == null || currentCloud !in cell) {
                break
            }
            currentTile[1]++
        }
        val upperBound = getCoordinatesFromNebulaTile(currentTile) ?: Vector2f(0f, 0f)
        currentTile[0] = coreTile[0]
        currentTile[1] = coreTile[1]
        while (true) {
            currentCloud = nebula.getCloud(currentTile[0], currentTile[1] - 1)
            if (currentCloud == null || currentCloud !in cell) {
                break
            }
            currentTile[1]--
        }
        val lowerBound = getCoordinatesFromNebulaTile(currentTile) ?: Vector2f(0f, 0f)
        currentTile[0] = coreTile[0]
        currentTile[1] = coreTile[1]

        return (MathUtils.getDistance(upperBound, center) +
                MathUtils.getDistance(lowerBound, center) +
                MathUtils.getDistance(rightBound, center) +
                MathUtils.getDistance(leftBound, center))/4
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

    fun getCoordinatesFromNebulaTile(tile: IntArray): Vector2f? {
        val engine = Global.getCombatEngine() ?: return null
        val nebula = engine.nebula ?: return null
        //val height: Float = engine.mapHeight + (MCTE_shipUtils.NEBULA_BUFFER_SIZE * 2f)
        //val width: Float = engine.mapWidth + (MCTE_shipUtils.NEBULA_BUFFER_SIZE * 2f)

        val cellSize: Float = nebula.tileSizeInPixels
        if (cellSize == 0f) return null // why can this happen

        var modifiedX = 0f
        var modifiedY = 0f

        modifiedX = (tile[0] * cellSize)
        modifiedY = (tile[1] * cellSize)

        return Vector2f(modifiedX, modifiedY)
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
        if (cellSize == 0f) return null // why can this happen

        val x = location.x
        val y = location.y

        if (x.isNaN() || y.isNaN()) return null //not sure why this happens, but it does

        var modifiedX = 0
        var modifiedY = 0
        try {
            modifiedX = (((x) + width / 2) / cellSize).roundToInt()
            modifiedY = (((y) + height / 2) / cellSize).roundToInt()
        } catch (ex: IllegalArgumentException) {
            if (!caughtNaN) {
                MCTE_debugUtils.displayError("NaN error caught before game crashed")
                MCTE_debugUtils.log.debug(
                    "Debug variable info of getNebulaTile:" +
                            "engine variable: $engine, nebula: $nebula, height: $height, width: $width, cellSize: $cellSize," +
                            "x: $x, y: $y, modifiedX: $modifiedX, modifiedY: $modifiedY", ex
                )
                caughtNaN = true
            }
            return null
        }

        return intArrayOf(modifiedX, modifiedY)
    }
}
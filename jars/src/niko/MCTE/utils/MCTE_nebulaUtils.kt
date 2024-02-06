package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BoundsAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.combat.entities.terrain.Cloud
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.plusAssign
import org.lwjgl.util.vector.Vector2f
import java.lang.IllegalArgumentException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

object MCTE_nebulaUtils {
    @Transient
    private var caughtNaN: Boolean = false

    private const val failuresTilDecision = 900
    private const val incrementValue = 1

    fun Cloud.getCloudsInRadius(radiusInTiles: Int, nebulaHandler: CombatNebulaAPI): MutableSet<Cloud> {
        val coreTile = getNebulaTile(location) ?: return HashSet()
        val cloudsToReturn = HashSet<Cloud>()

        val halvedRadius: Int = (ceil(radiusInTiles/2f)).toInt()

        val startingX = (coreTile[0] - halvedRadius)
        val startingY = (coreTile[1] - halvedRadius)

        val endingX = (coreTile[0] + halvedRadius)
        val endingY = (coreTile[1] + halvedRadius)

        var currX = startingX
        var currY = startingY

        while (true) {
            while(currY++ < endingY) {
                if (abs(coreTile[1] - currY) > radiusInTiles)
                    break
                val cloud = nebulaHandler.getCloud(currX, currY) as? Cloud ?: continue
                cloudsToReturn += cloud
            }
            currX++
            if (abs(coreTile[0] - currX) > radiusInTiles)
                break
            val cloud = nebulaHandler.getCloud(currX, currY) as? Cloud
            if (cloud != null) cloudsToReturn += cloud
            if (currX >= endingX) break
            currY = startingY
        }
        return cloudsToReturn
    }

    fun getCellCentroid(nebulaHandler: CombatNebulaAPI, nebulaCell: MutableSet<Cloud>, ourCoordinates: Vector2f? = null): Vector2f? {
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

    fun getTopCoord(nebulaHandler: CombatNebulaAPI, nebulaCell: MutableSet<Cloud>, rayCastCoordinate: Vector2f): Vector2f {
        var failureIndex = 0

        val cachedRayCastCoordinate = Vector2f(rayCastCoordinate)
        while (failureIndex < failuresTilDecision) {
            rayCastCoordinate.y += incrementValue
            val cloud = nebulaHandler.getCloud(rayCastCoordinate.x, rayCastCoordinate.y) as? Cloud
            if (!nebulaCell.contains(cloud)) {
                if (digToSeeIfCloudInCell(nebulaHandler, nebulaCell, cloud)) {
                    nebulaCell += cloud!!
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

    fun getBottomCoord(nebulaHandler: CombatNebulaAPI, nebulaCell: MutableSet<Cloud>, rayCastCoordinate: Vector2f): Vector2f {
        var failureIndex = 0

        val cachedRayCastCoordinate = Vector2f(rayCastCoordinate)
        while (failureIndex < failuresTilDecision) {
            rayCastCoordinate.y -= incrementValue
            val cloud = nebulaHandler.getCloud(rayCastCoordinate.x, rayCastCoordinate.y) as? Cloud
            if (!nebulaCell.contains(cloud)) {
                if (digToSeeIfCloudInCell(nebulaHandler, nebulaCell, cloud)) {
                    nebulaCell += cloud!!
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

    fun getLeftCoord(nebulaHandler: CombatNebulaAPI, nebulaCell: MutableSet<Cloud>, rayCastCoordinate: Vector2f): Vector2f {
        var failureIndex = 0

        val cachedRayCastCoordinate = Vector2f(rayCastCoordinate)
        while (failureIndex < failuresTilDecision) {
            rayCastCoordinate.x -= incrementValue
            val cloud = nebulaHandler.getCloud(rayCastCoordinate.x, rayCastCoordinate.y) as? Cloud
            if (!nebulaCell.contains(cloud)) {
                if (digToSeeIfCloudInCell(nebulaHandler, nebulaCell, cloud)) {
                    nebulaCell += cloud!!
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

    fun getRightCoord(nebulaHandler: CombatNebulaAPI, nebulaCell: MutableSet<Cloud>, rayCastCoordinate: Vector2f): Vector2f {
        var failureIndex = 0

        val cachedRayCastCoordinate = Vector2f(rayCastCoordinate)
        while (failureIndex < failuresTilDecision) {
            rayCastCoordinate.x += incrementValue
            val cloud = nebulaHandler.getCloud(rayCastCoordinate.x, rayCastCoordinate.y) as? Cloud
            if (!nebulaCell.contains(cloud)) {
                if (digToSeeIfCloudInCell(nebulaHandler, nebulaCell, cloud)) {
                    nebulaCell += cloud!!
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

    fun digToSeeIfCloudInCell(nebulaHandler: CombatNebulaAPI, nebulaCell: MutableSet<Cloud>, cloud: Cloud?): Boolean {
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

    fun getRadiusOfCell(cell: MutableSet<Cloud>, nebula: CombatNebulaAPI, centroid: Vector2f): Float {
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

        return combinedValue

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
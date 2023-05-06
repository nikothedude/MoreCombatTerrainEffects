package niko.MCTE.utils

import kotlin.math.round

object MCTE_mathUtils {

    fun Double.roundTo(decimalPoints: Int): Double {
        var multiplier = 1.0
        repeat(decimalPoints) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    fun Float.roundTo(decimalPoints: Int): Float {
        return this.toDouble().roundTo(decimalPoints).toFloat()
    }

    fun IntArray.expandInRadius(tileRadius: Int): MutableSet<IntArray> {
        val toAddX: Int = ((tileRadius * 2))
        val toAddY: Int = ((tileRadius * 2))

        val intArrays: MutableSet<IntArray> = HashSet()
        var startTile = intArrayOf(this[0], this[1])
        startTile[0] -= (toAddX)/2
        startTile[1] -= (toAddY)/2

        intArrays += startTile

        var xAddsLeft = toAddX
        var yAddsLeft = toAddY

        var defaultX = (startTile[0])
        var defaultY = (startTile[1])

        var currX: Int = defaultX
        var currY: Int = defaultY

        while (true) {
            while (yAddsLeft > 0) {
                currY++
                yAddsLeft--
                intArrays += intArrayOf(currX, currY)
                println("$currX, $currY")
            }
            if (xAddsLeft <= 0) break
            xAddsLeft--
            currX++
            yAddsLeft = toAddY
            intArrays += intArrayOf(currX, currY)
            currY = defaultY
        }

        return intArrays
    }

}
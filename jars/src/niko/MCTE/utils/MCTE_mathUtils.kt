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

}
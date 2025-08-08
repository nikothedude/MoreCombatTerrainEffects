package niko.MCTE.utils

import org.lazywizard.lazylib.MathUtils
import java.util.Random
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

    fun Float.trimHangingZero(): Number {
        if (this % 1 == 0f) return this.toInt()
        return this
    }

    fun prob(chance: Int, random: Random = MathUtils.getRandom()): Boolean {
        return prob(chance.toDouble(), random)
    }

    fun prob(chance: Float, random: Random = MathUtils.getRandom()): Boolean {
        return prob(chance.toDouble(), random)
    }

    fun prob(chance: Double, random: Random = MathUtils.getRandom()): Boolean {
        return (random.nextFloat() * 100f < chance)
    }
}
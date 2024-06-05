package niko.MCTE.utils

object MCTE_stringUtils {
    fun toPercent(num: Float): String {
        return String.format("%.0f", num * 100) + "%"
    }
}
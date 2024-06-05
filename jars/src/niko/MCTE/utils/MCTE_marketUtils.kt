package niko.MCTE.utils

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries

object MCTE_marketUtils {
    fun MarketAPI.protectedByShield(): Boolean {
        for (industry in industries) {
            if (industry.id == Industries.PLANETARYSHIELD && (industry.isFunctional)) return true
        }
        return false
    }
}
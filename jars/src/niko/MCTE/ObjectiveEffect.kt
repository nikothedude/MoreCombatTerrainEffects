package niko.MCTE

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.impl.campaign.CampaignObjective
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.utils.MCTE_mathUtils.trimHangingZero
import org.lazywizard.lazylib.MathUtils
import kotlin.math.exp

enum class ObjectiveEffect {
    COMMS_RELAY() {
        override fun getMaxDistance(): Float {
            return MCTE_settings.COMMS_RELAY_MAX_DISTANCE
        }

        override fun getMinDistance(): Float {
            return MCTE_settings.COMMS_RELAY_MIN_DISTANCE
        }

        override fun getBaseTerrainName(): String {
            return "Comms Relay"
        }

        override fun getOfferingText(): String {
            return "enhanced CP regeneration"
        }

        override fun getBaseStrength(): Float {
            return MCTE_settings.COMMS_BASE_CP_RATE
        }
    },
    SENSOR_ARRAY {
        override fun getMaxDistance(): Float {
            return MCTE_settings.SENSOR_ARRAY_MAX_DISTANCE
        }

        override fun getMinDistance(): Float {
            return MCTE_settings.SENSOR_ARRAY_MIN_DISTANCE
        }

        override fun getBaseTerrainName(): String {
            return "Sensor Array"
        }

        override fun getOfferingText(): String {
            return "weapon range"
        }

        override fun getBaseStrength(): Float {
            return MCTE_settings.SENSOR_ARRAY_BASE_WEAPON_RANGE_INCREMENT
        }
    },
    NAV_BUOY {
        override fun getMaxDistance(): Float {
            return MCTE_settings.NAV_BUOY_MAX_DISTANCE
        }

        override fun getMinDistance(): Float {
            return MCTE_settings.NAV_BUOY_MIN_DISTANCE
        }

        override fun getBaseTerrainName(): String {
            return "Nav Buoy"
        }

        override fun getOfferingText(): String {
            return "top speed"
        }

        override fun getBaseStrength(): Float {
            return MCTE_settings.NAV_BUOY_BASE_COORD
        }
    };

    /** @return 0-1. */
    open fun getPercentEffectiveness(fleet: CampaignFleetAPI, objective: CustomCampaignEntityAPI): Float {
        val fleetCoordinates = fleet.location

        val distance = MathUtils.getDistance(fleetCoordinates, objective.location)
        val minDist = getMinDistance()
        val adjustedMin = minDist.coerceAtLeast(objective.radius)
        var maxDist = getMaxDistance()
        if (!objective.hasTag(Tags.MAKESHIFT)) {
            maxDist *= MCTE_settings.PRISTINE_OBJECTIVE_EFFECT_MULT
        }
        val adjustedDist = (distance - adjustedMin).coerceAtLeast(0f)
        if (adjustedDist > maxDist) return 0f

        var mult = (1 - (1 / (maxDist / adjustedDist)))
        return mult
    }

    fun getTerrainName(fleet: CampaignFleetAPI, objective: CustomCampaignEntityAPI): String {
        return ("${getBaseTerrainName()} (${(getPercentEffectiveness(fleet, objective) * 100f).toInt()}%)")
    }

    abstract fun getMaxDistance(): Float
    abstract fun getMinDistance(): Float

    abstract fun getBaseTerrainName(): String
    open fun createTerrainTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, fleet: CampaignFleetAPI, objective: CustomCampaignEntityAPI) {
        tooltip.addTitle(getBaseTerrainName())
        addFirstTerrainTooltip(tooltip, expanded, fleet, objective)

        if (!wantToAssist(fleet, objective)) {
            tooltip.addPara(
                "The ${objective.name} is %s to your fleet, meaning you cannot reap it's benefits in combat.",
                5f,
                Misc.getNegativeHighlightColor(),
                "not friendly"
            )
            return
        }

        addTerrainTooltipEffectiveness(tooltip, expanded, fleet, objective)
    }

    open fun wantToAssist(fleet: CampaignFleetAPI, objective: CustomCampaignEntityAPI): Boolean {
        val fleetFaction = fleet.faction
        val ourFaction = objective.faction

        if ((objective.customPlugin as? CampaignObjective)?.isHacked == true && fleet.faction.id == Factions.PLAYER) return true
        if (ourFaction.id == Factions.NEUTRAL || ourFaction.isHostileTo(fleetFaction)) return false

        return true
    }

    open fun addFirstTerrainTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, fleet: CampaignFleetAPI, objective: CustomCampaignEntityAPI) {
        tooltip.addPara(
            "Your fleet is in range of %s, which offers %s to any allied fleets in combat.",
            5f,
            Misc.getHighlightColor(),
            objective.name, getOfferingText()
        ).setHighlightColors(
            objective.faction.baseUIColor,
            Misc.getHighlightColor()
        )
        if (!objective.hasTag(Tags.MAKESHIFT)) {
            tooltip.addPara(
                "Due to it's pristine nature, %s has %s more strength and range than makeshift variants of it's kind.",
                5f,
                Misc.getPositiveHighlightColor(),
                objective.name, "${((((MCTE_settings.PRISTINE_OBJECTIVE_EFFECT_MULT)) - 1) * 100f).roundTo(2).trimHangingZero()}%"
            ).setHighlightColors(
                objective.faction.baseUIColor,
                Misc.getPositiveHighlightColor()
            )
        }
    }

    abstract fun getOfferingText(): String

    protected fun addTerrainTooltipEffectiveness(tooltip: TooltipMakerAPI, expanded: Boolean, fleet: CampaignFleetAPI, objective: CustomCampaignEntityAPI) {
        tooltip.addPara(
            "Based on your fleet's current distance to ${objective.name}, you would receive " +
            "%s of it's in-combat bonuses should you engage in combat.",
            5f,
            Misc.getHighlightColor(),
            "${(getPercentEffectiveness(fleet, objective) * 100f).roundTo(1).trimHangingZero()}%"
        )
    }

    abstract fun getBaseStrength(): Float
}
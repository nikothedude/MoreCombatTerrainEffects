package niko.MCTE.codex

import com.fs.starfarer.api.impl.campaign.NavBuoyEntityPlugin
import com.fs.starfarer.api.impl.campaign.SensorArrayEntityPlugin
import com.fs.starfarer.api.impl.campaign.econ.CommRelayCondition
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.COMMS_BASE_CP_RATE
import niko.MCTE.settings.MCTE_settings.NAV_BUOY_BASE_COORD
import niko.MCTE.settings.MCTE_settings.SENSOR_ARRAY_BASE_WEAPON_RANGE_INCREMENT
import niko.MCTE.utils.MCTE_mathUtils.trimHangingZero

enum class ObjectiveEntry(
    val title: String,
    val iconName: String
) {
    COMMS_RELAY("Comms Relay", "graphics/ships/com_relay.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("A common technology used primarily on the frontier, this is a hyperwave communications array which transmits and receives data between star systems at faster-than-light speeds. The rapidly pulsing hyperwaves suitable for FTL data transmission have been shown to damage DNA so these relays are always stationed away from habitats.", 0f)
        }

        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara("%s/%s stability for same-faction colonies in system", 0f, Misc.getHighlightColor(), "+${CommRelayCondition.MAKESHIFT_COMM_RELAY_BONUS.trimHangingZero()}", "+${CommRelayCondition.COMM_RELAY_BONUS.trimHangingZero()}").setHighlightColors(
                Misc.getHighlightColor(),
                Misc.getPositiveHighlightColor()
            )
        }

        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("A comms relay offers %s/%s faster %s.",
                5f,
                Misc.getHighlightColor(),
                "${(COMMS_BASE_CP_RATE * 100f).trimHangingZero()}%", "${((COMMS_BASE_CP_RATE * 100f) * MCTE_settings.PRISTINE_OBJECTIVE_EFFECT_MULT).trimHangingZero()}%",
                "CP regeneration"
            ).setHighlightColors(
                Misc.getHighlightColor(), Misc.getPositiveHighlightColor(),
                Misc.getHighlightColor()
            )
        }
    },
    NAV_BUOY("Nav Buoy", "graphics/ships/nav_buoy.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("A common technology, a nav buoy monitors the in-system hyperfield and is capable of transmitting its findings to friendly fleets in-system. The data readings are pristine and of great use in configuring drive fields.", 0f)
        }

        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara("%s/%s burn level for all same-faction fleets in system", 0f, Misc.getHighlightColor(), "+${NavBuoyEntityPlugin.NAV_BONUS_MAKESHIFT.trimHangingZero()}", "+${NavBuoyEntityPlugin.NAV_BONUS.trimHangingZero()}").setHighlightColors(
                Misc.getHighlightColor(),
                Misc.getPositiveHighlightColor()
            )
        }

        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("A nav buoy offers %s/%s %s.",
                5f,
                Misc.getHighlightColor(),
                "+${(NAV_BUOY_BASE_COORD).trimHangingZero()}%", "+${((NAV_BUOY_BASE_COORD) * MCTE_settings.PRISTINE_OBJECTIVE_EFFECT_MULT).trimHangingZero()}%",
                "nav rating"
            ).setHighlightColors(
                Misc.getHighlightColor(), Misc.getPositiveHighlightColor(),
                Misc.getHighlightColor()
            )
        }
    },
    SENSOR_ARRAY("Sensor Array", "graphics/ships/sensor_array.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("A common technology, this is a super-high-resolution passive monitoring array with a transmitter capable of real-time, faster-than-light data transmission within the star system. The data is used to supplement the regular sensor readings made by fleets.", 0f)
        }

        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara("%s/%s sensor range for all same-faction fleets in system", 0f, Misc.getHighlightColor(), "+${SensorArrayEntityPlugin.SENSOR_BONUS_MAKESHIFT.trimHangingZero()}", "+${SensorArrayEntityPlugin.SENSOR_BONUS.trimHangingZero()}").setHighlightColors(
                Misc.getHighlightColor(),
                Misc.getPositiveHighlightColor()
            )
        }

        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("A sensor array offers %s/%s %s.",
                5f,
                Misc.getHighlightColor(),
                "+${(SENSOR_ARRAY_BASE_WEAPON_RANGE_INCREMENT).trimHangingZero()}", "+${((SENSOR_ARRAY_BASE_WEAPON_RANGE_INCREMENT) * MCTE_settings.PRISTINE_OBJECTIVE_EFFECT_MULT).trimHangingZero()}",
                "non-missile non-PD weapon range"
            ).setHighlightColors(
                Misc.getHighlightColor(), Misc.getPositiveHighlightColor(),
                Misc.getHighlightColor()
            )
        }
    };

    abstract fun createDesc(info: TooltipMakerAPI)
    open fun hasTacticalDesc(): Boolean = false
    open fun createTacticalDesc(info: TooltipMakerAPI) {}
    open fun createCampaignDesc(info: TooltipMakerAPI) {}
    open fun createCombatDesc(info: TooltipMakerAPI) {}
    open fun createGroundBattleDesc(info: TooltipMakerAPI) {}

    open fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = HashSet()

    open fun isAvailableInCodex(): Boolean = true
    open fun getTags(): MutableSet<String> = HashSet()

    companion object {
        const val HAS_CAMPAIGN_EFFECTS = "Campaign"
        const val HAS_COMBAT_EFFECTS = "Combat"
        const val HAS_GROUND_BATTLE_EFFECTS = "Ground Battle"
    }
}
package niko.MCTE.codex

import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.codex.CodexDataV2
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin
import com.fs.starfarer.api.impl.codex.CodexEntryV2
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

enum class TerrainEntry(
    val title: String,
    val iconName: String
) {
    CORONA("Corona", "graphics/icons/terrain/corona.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara(
                "A halo of high temperature plasma which surrounds a star; most ships are not built to withstand prolonged exposure to the extreme radiation bombardment from the corona and will take damage if they remain within it.",
                0f
            )
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara("Reduces the combat readiness of all ships in the corona at a steady pace.", 0f)
            info.addPara("The corona's heavy winds also blow fleets away from it's source.", 5f)
            info.addPara("Occasional solar flare activity takes these effects to even more dangerous levels.", 5f)
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("Reduces the peak performance time of ships and increases the rate of combat readiness degradation in protracted engagements.", 0f)
        }
        override fun hasAfterDesc(): Boolean = true
        override fun createAfterDesc(info: TooltipMakerAPI) {
            info.addPara("%s is effective as resisting both the campaign and combat effects of this terrain.", 0f, Misc.getHighlightColor(), "Solar Shielding").color = Misc.getGrayColor()
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = hashSetOf(CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.SOLAR_SHIELDING)))
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS)
    },
    MAGFIELD("Magnetic Field", "graphics/icons/terrain/magnetic_storm.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("A strong magnetic field which traps high energy charged particles. It's extremely difficult to get any sensor readings on a fleet lurking within.", 0f)
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara("Reduces detected-at range of fleets within by %s.", 0f, Misc.getHighlightColor(), "50%")
            info.addPara("Occasionally, massive storms wrack the field, creating large spikes of magnetic activity.", 5f)
            info.addPara("Fleets within these storms have their sensor strength reduced by %s, but are rendered %s to other fleets (save for %s).", 5f, Misc.getHighlightColor(), "90%", "nearly invisible", "visual contact").setHighlightColors(Misc.getNegativeHighlightColor(), Misc.getPositiveHighlightColor(), Misc.getHighlightColor())
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("Reduces the peak performance time of ships and increases the rate of combat readiness degradation in protracted engagements.", 0f)
        }
        override fun hasAfterDesc(): Boolean = true
        override fun createAfterDesc(info: TooltipMakerAPI) {
            info.addPara("An %s counters the in-combat effect of this terrain.", 0f, Misc.getHighlightColor(), "ECCM package").color = Misc.getGrayColor()
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = hashSetOf(CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.ECCM)))
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS)
    };

    abstract fun createDesc(info: TooltipMakerAPI)
    open fun hasAfterDesc(): Boolean = false
    open fun createAfterDesc(info: TooltipMakerAPI) {}
    open fun createCampaignDesc(info: TooltipMakerAPI) {}
    open fun createCombatDesc(info: TooltipMakerAPI) {}

    open fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = HashSet()

    open fun isAvailableInCodex(): Boolean = true
    open fun getTags(): MutableSet<String> = HashSet()

    companion object {
        const val HAS_CAMPAIGN_EFFECTS = "Affects Campaign"
        const val HAS_COMBAT_EFFECTS = "Affects Combat"
    }
}
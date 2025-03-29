package niko.MCTE.codex

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import com.fs.starfarer.api.impl.codex.CodexDataV2
import com.fs.starfarer.api.impl.codex.CodexDialogAPI
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin.ListMode
import com.fs.starfarer.api.impl.codex.CodexEntryV2
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_ids
import niko_SA.scripts.effectApplierScript
import kotlin.math.max

object CodexData {

    const val MCTE_MASTER = "MCTE_master"
    const val TERRAIN = "MCTE_terrain"
    const val OBJECTIVE = "MCTE_objective"

    fun addCodexInfo() {
        Global.getSettings().loadTexture("graphics/icons/intel/events.png")
        val MCTEcat = object : CodexEntryV2(MCTE_MASTER, "MCTE", "graphics/icons/intel/events.png") {
            override fun hasTagDisplay(): Boolean {
                return false
            }

            override fun hasCustomDetailPanel(): Boolean = true

            override fun createCustomDetail(
                panel: CustomPanelAPI,
                relatedEntries: UIPanelAPI?,
                codex: CodexDialogAPI?
            ) {
                val opad = 10f
                val width = panel.position.width
                val horzBoxPad = 30f
                // the right width for a tooltip wrapped in a box to fit next to relatedEntries
                val tw = width - 290f - opad - horzBoxPad + 10f

                val text = panel.createUIElement(tw, 0f, false)
                text.setParaSmallInsignia()

                text.addPara("TEST", 0f)

                panel.updateUIElementSizeAndMakeItProcessInput(text)
                val box = panel.wrapTooltipWithBox(text)
                panel.addComponent(box).inTL(0f, 0f)
                if (relatedEntries != null) {
                    panel.addComponent(relatedEntries).inTR(0f, 0f)
                }

                var height = box.position.height
                if (relatedEntries != null) {
                    height = max(height.toDouble(), relatedEntries.position.height.toDouble()).toFloat()
                }
            }

            override fun getSourceMod(): ModSpecAPI? {
                return Global.getSettings().modManager.getModSpec(MCTE_ids.modId)
            }
        }
        CodexDataV2.ROOT.addChild(MCTEcat)
        val terrainCat = object : CodexEntryV2(TERRAIN, "Terrain", CodexDataV2.getIcon(TERRAIN)) {
            override fun hasTagDisplay(): Boolean {
                return true
            }

            override fun configureTagDisplay(tags: TagDisplayAPI) {
                var combat = 0
                var campaign = 0
                var groundbattle = 0
                var total = 0

                for (curr in getChildren()) {
                    if (curr.param !is TerrainEntry) continue
                    val castedParam = (curr.param as TerrainEntry)
                    if (!curr.isVisible || curr.isLocked || curr.skipForTags()) continue

                    if (castedParam.getTags().contains(TerrainEntry.HAS_CAMPAIGN_EFFECTS)) {
                        campaign++
                    }
                    if (castedParam.getTags().contains(TerrainEntry.HAS_COMBAT_EFFECTS)) {
                        combat++
                    }
                    if (MCTE_debugUtils.nexEnabled && castedParam.getTags().contains(TerrainEntry.HAS_GROUND_BATTLE_EFFECTS)) {
                        groundbattle++
                    }
                    total++
                }

                val opad = 10f

                tags.beginGroup(false, CodexDataV2.ALL_TYPES, 120f)
                tags.addTag(TerrainEntry.HAS_CAMPAIGN_EFFECTS, campaign)
                tags.addTag(TerrainEntry.HAS_COMBAT_EFFECTS, combat)
                tags.addTag(TerrainEntry.HAS_GROUND_BATTLE_EFFECTS, groundbattle)
                tags.setTotalOverrideForCurrentGroup(total)
                tags.addGroup(opad)

                tags.checkAll()
            }

            override fun getSourceMod(): ModSpecAPI? {
                return Global.getSettings().modManager.getModSpec(MCTE_ids.modId)
            }
        }
        MCTEcat.addChild(terrainCat)
        Global.getSettings().loadTexture("graphics/icons/intel/comm_sniffer.png")
        val objectiveCat = object : CodexEntryV2(OBJECTIVE, "Objectives", "graphics/icons/intel/comm_sniffer.png") {
            /*override fun hasTagDisplay(): Boolean {
                return true
            }

            override fun configureTagDisplay(tags: TagDisplayAPI) {
                var combat = 0
                var campaign = 0
                var groundbattle = 0
                var total = 0

                for (curr in getChildren()) {
                    if (curr.param !is ObjectiveEntry) continue
                    val castedParam = (curr.param as ObjectiveEntry)
                    if (!curr.isVisible || curr.isLocked || curr.skipForTags()) continue

                    if (castedParam.getTags().contains(ObjectiveEntry.HAS_CAMPAIGN_EFFECTS)) {
                        campaign++
                    }
                    if (castedParam.getTags().contains(ObjectiveEntry.HAS_COMBAT_EFFECTS)) {
                        combat++
                    }
                    if (MCTE_debugUtils.nexEnabled && castedParam.getTags().contains(ObjectiveEntry.HAS_GROUND_BATTLE_EFFECTS)) {
                        groundbattle++
                    }
                    total++
                }

                val opad = 10f

                tags.beginGroup(false, CodexDataV2.ALL_TYPES, 120f)
                tags.addTag(ObjectiveEntry.HAS_CAMPAIGN_EFFECTS, campaign)
                tags.addTag(ObjectiveEntry.HAS_COMBAT_EFFECTS, combat)
                tags.addTag(ObjectiveEntry.HAS_GROUND_BATTLE_EFFECTS, groundbattle)
                tags.setTotalOverrideForCurrentGroup(total)
                tags.addGroup(opad)

                tags.checkAll()
            }*/
        }
        MCTEcat.addChild(objectiveCat)
        /*Global.getSettings().loadTexture("graphics/icons/intel/news.png")
        val terrainBase = object : CodexEntryV2("MCTE_terrainDisclaimer", "DISCLAIMER!", "graphics/icons/intel/news.png") {
            override fun hasCustomDetailPanel(): Boolean = true

            override fun createCustomDetail(
                panel: CustomPanelAPI,
                relatedEntries: UIPanelAPI?,
                codex: CodexDialogAPI?
            ) {
                val opad = 10f
                val width = panel.getPosition().getWidth()
                val horzBoxPad = 30f
                // the right width for a tooltip wrapped in a box to fit next to relatedEntries
                val tw = width - 290f - opad - horzBoxPad + 10f

                val text = panel.createUIElement(tw, 0f, false)
                text.setParaSmallInsignia()

                text.addPara("TEST", 0f)

                panel.updateUIElementSizeAndMakeItProcessInput(text)
                val box = panel.wrapTooltipWithBox(text)
                panel.addComponent(box).inTL(0f, 0f)
                if (relatedEntries != null) {
                    panel.addComponent(relatedEntries).inTR(0f, 0f)
                }

                var height = box.getPosition().getHeight()
                if (relatedEntries != null) {
                    height = max(height.toDouble(), relatedEntries.getPosition().getHeight().toDouble()).toFloat()
                }
            }

            override fun getSourceMod(): ModSpecAPI? {
                return Global.getSettings().modManager.getModSpec(MCTE_ids.modId)
            }
        }
        terrainCat.addChild(terrainBase)*/

        for (effect in TerrainEntry.values()) {
            if (!effect.isAvailableInCodex()) {
                continue
            }
            val curr = object : CodexEntryV2(effect.name, effect.title, effect.iconName, effect) {
                override fun matchesTags(tags: Set<String>): Boolean {
                    if (tags.contains(TERRAIN)) return false
                    if (effect.getTags().any { tags.contains(it) } == true) return true
                    return false
                }

                override fun createTitleForList(info: TooltipMakerAPI, width: Float, mode: ListMode?) {
                    info.addPara(effect.title, Misc.getBasePlayerColor(), 0f)
                    if (mode == ListMode.RELATED_ENTRIES) {
                        info.addPara("Terrain", Misc.getGrayColor(), 0f)
                    }
                }

                override fun isVignetteIcon(): Boolean {
                    return true
                }

                override fun hasCustomDetailPanel(): Boolean = true

                override fun createCustomDetail(
                    panel: CustomPanelAPI,
                    relatedEntries: UIPanelAPI?,
                    codex: CodexDialogAPI?
                ) {
                    val opad = 10f
                    val width = panel.position.width
                    val horzBoxPad = 30f
                    // the right width for a tooltip wrapped in a box to fit next to relatedEntries
                    val tw = width - 290f - opad - horzBoxPad + 10f

                    val text = panel.createUIElement(tw, 0f, false)
                    //text.setParaSmallInsignia()
                    effect.createDesc(text)
                    if (effect.getTags().contains(TerrainEntry.HAS_CAMPAIGN_EFFECTS)) {
                        text.addSectionHeading("Campaign", Alignment.MID, 5f)
                        effect.createCampaignDesc(text)
                    }
                    if (effect.getTags().contains(TerrainEntry.HAS_COMBAT_EFFECTS)) {
                        text.addSectionHeading("Combat", Alignment.MID, 5f)
                        effect.createCombatDesc(text)
                    }
                    if (MCTE_debugUtils.nexEnabled && effect.getTags().contains(TerrainEntry.HAS_GROUND_BATTLE_EFFECTS)) {
                        text.addSectionHeading("Ground Battle", Alignment.MID, 5f)
                        effect.createGroundBattleDesc(text)
                    }
                    if (effect.hasTacticalDesc()) {
                        text.addSectionHeading("Tactical", Alignment.MID, 5f)
                        effect.createTacticalDesc(text)
                    }
                    panel.updateUIElementSizeAndMakeItProcessInput(text)
                    val box = panel.wrapTooltipWithBox(text)
                    panel.addComponent(box).inTL(0f, 0f)
                    if (relatedEntries != null) {
                        panel.addComponent(relatedEntries).inTR(0f, 0f)
                    }

                    var height = box.position.height
                    if (relatedEntries != null) {
                        height = max(height.toDouble(), relatedEntries.position.height.toDouble()).toFloat()
                    }
                }

                override fun getSourceMod(): ModSpecAPI? {
                    return Global.getSettings().modManager.getModSpec(MCTE_ids.modId)
                }
            }

            for (entry in effect.getRelatedEntries()) {
                curr.addRelatedEntry(entry)
            }

            Global.getSettings().loadTexture(effect.iconName)
            terrainCat.addChild(curr)
        }

        for (effect in ObjectiveEntry.values()) {
            if (!effect.isAvailableInCodex()) {
                continue
            }
            val curr = object : CodexEntryV2(effect.name, effect.title, effect.iconName, effect) {
                override fun matchesTags(tags: Set<String>): Boolean {
                    if (tags.contains(TERRAIN)) return false
                    if (effect.getTags().any { tags.contains(it) } == true) return true
                    return false
                }

                override fun createTitleForList(info: TooltipMakerAPI, width: Float, mode: ListMode?) {
                    info.addPara(effect.title, Misc.getBasePlayerColor(), 0f)
                    if (mode == ListMode.RELATED_ENTRIES) {
                        info.addPara("Objective", Misc.getGrayColor(), 0f)
                    }
                }

                override fun hasCustomDetailPanel(): Boolean = true

                override fun createCustomDetail(
                    panel: CustomPanelAPI,
                    relatedEntries: UIPanelAPI?,
                    codex: CodexDialogAPI?
                ) {
                    val opad = 10f
                    val width = panel.position.width
                    val horzBoxPad = 30f
                    // the right width for a tooltip wrapped in a box to fit next to relatedEntries
                    val tw = width - 290f - opad - horzBoxPad + 10f

                    val text = panel.createUIElement(tw, 0f, false)
                    //text.setParaSmallInsignia()
                    effect.createDesc(text)
                    text.addSectionHeading("Campaign", Alignment.MID, 5f)
                    effect.createCampaignDesc(text)
                    text.addSectionHeading("Combat", Alignment.MID, 5f)
                    effect.createCombatDesc(text)

                    panel.updateUIElementSizeAndMakeItProcessInput(text)
                    val box = panel.wrapTooltipWithBox(text)
                    panel.addComponent(box).inTL(0f, 0f)
                    if (relatedEntries != null) {
                        panel.addComponent(relatedEntries).inTR(0f, 0f)
                    }

                    var height = box.position.height
                    if (relatedEntries != null) {
                        height = max(height.toDouble(), relatedEntries.position.height.toDouble()).toFloat()
                    }
                }

                override fun getSourceMod(): ModSpecAPI? {
                    return Global.getSettings().modManager.getModSpec(MCTE_ids.modId)
                }
            }

            for (entry in effect.getRelatedEntries()) {
                curr.addRelatedEntry(entry)
            }

            Global.getSettings().loadTexture(effect.iconName)
            objectiveCat.addChild(curr)
        }
    }

    fun linkCodexInfo() {
        val terrainCat = CodexDataV2.getEntry(TERRAIN)
        for (child in terrainCat.children) {
            val effect = child.param as? TerrainEntry ?: continue

            for (entry in effect.getRelatedEntries()) {
                child.addRelatedEntry(entry)
                entry.addRelatedEntry(child)
            }
        }
    }

}
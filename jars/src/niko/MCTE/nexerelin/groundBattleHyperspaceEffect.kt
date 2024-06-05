package niko.MCTE.nexerelin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import exerelin.campaign.intel.groundbattle.*
import exerelin.utilities.NexUtilsGUI
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_NEX_BASE_DISORGANIZE_CHANCE
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_NEX_BASE_DISORGANIZE_TURNS
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_NEX_ENABLED
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_NEX_HOLDER_DAMAGE_MULT
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_NEX_HOLDER_DISORGANIZE_CHANCE_MULT
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_NEX_MAX_INDUSTRIES_TO_TARGET
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_NEX_MIN_INDUSTRIES_TO_TARGET
import niko.MCTE.settings.MCTE_settings.HYPERSTORM_NEX_STRIKE_BASE_DAMAGE
import niko.MCTE.utils.MCTE_marketUtils.protectedByShield
import niko.MCTE.utils.MCTE_stringUtils
import org.lazywizard.lazylib.MathUtils

class groundBattleHyperspaceEffect: groundBattleTerrainEffect() {

    private val troopsToReorganize: MutableMap<GroundUnit, Int> = HashMap()

    companion object {
        const val EXTRA_CELL_DETECTION_RADIUS = 50f // arbitrary

        const val BASE_INDUSTRY_WEIGHT = 50f

        const val INDUSTRIES_HIT_PARAMS_ID = "MCTE_industriesHit"
        const val MOVEMENT_COST_ID = "MCTE_hyperspaceMovementCost"
    }

    override fun terrainIsAffecting(terrain: CampaignTerrainAPI): Boolean {
        if (!HYPERSTORM_NEX_ENABLED) return false
        val plugin = terrain.plugin as? HyperspaceTerrainPlugin ?: return false
        val entity = getEntity() ?: return false

        return (plugin.getCellAt(entity, EXTRA_CELL_DETECTION_RADIUS) != null && plugin.containsEntity(entity))
    }

    /*override fun reportUnitMoved(unit: GroundUnit?, lastLoc: IndustryForBattle?) {
        if (unit == null) return
        val side = intel.getSide(unit.isAttacker)
        //val cost = getMovementCostMult(side) * unit.deployCost

        side.movementPointsSpent.modifyFlat(MOVEMENT_COST_ID, cost)
    }*/

    override fun beforeTurnResolve(turn: Int) {
        super.beforeTurnResolve(turn)

        if (shouldLightningStrike()) doLightningStrike()
    }

    override fun afterTurnResolve(turn: Int) {
        super.afterTurnResolve(turn)

        for (entry in troopsToReorganize) {
            val unit = entry.key
            val rounds = entry.value

            if (unit.location == null) continue
            unit.reorganize(rounds)
        }
        troopsToReorganize.clear()
    }

    private fun shouldLightningStrike(applicableTerrain: MutableSet<CampaignTerrainAPI>? = null): Boolean {
        if (marketProtected()) return false // before we get affecting terrain for optimization
        var applicableTerrain = applicableTerrain
        if (applicableTerrain == null) applicableTerrain = getAffectingTerrain()

        val stormingTerrain = getStormingTerrain(applicableTerrain)

        if (stormingTerrain.isEmpty()) return false
        val chance = getStrikeChance(stormingTerrain)
        val randFloat = MathUtils.getRandom().nextFloat()
        if (chance >= randFloat) return true
        return false
    }

    private fun marketProtected(): Boolean {
        val market = intel.market
        return market.protectedByShield()
    }

    private fun getStrikeChance(applicableTerrain: MutableSet<CampaignTerrainAPI> = getAffectingTerrain()): Float {
        return ((MCTE_settings.HYPERSTORM_NEX_CHANCE_PER_ROUND/100) * applicableTerrain.size)
    }

    private fun doLightningStrike() {
        val targetIndustries = getTargetIndustries()

        val resolve = GroundBattleRoundResolve(intel)
        for (industry in targetIndustries) {
            strikeIndustry(industry, resolve)
        }

        val log = createLogInstance()

        log.params[INDUSTRIES_HIT_PARAMS_ID] = targetIndustries

        intel.addLogEvent(log)

        if (intel.isPlayerAttacker != null) { // involved
            val industryString = getIndustriesString(targetIndustries)
            val message = "Lightning strike! $industryString struck by lightning"
            val intelPlugin = MessageIntel(message, Misc.getHighlightColor(), arrayOf(industryString))
            intelPlugin.icon = Global.getSettings().getSpriteName("intel", "MCTE_hyperStorm")
            Global.getSector().campaignUI.addMessage(intelPlugin)
            Global.getSoundPlayer().playUISound("MCTE_hyperStormArcSoundStereo", 1f, 0.19f)
        }
        //Global.getSoundPlayer().playUISound("terrain_hyperspace_lightning", 1f, 2.3f) // doesnt work :(
    }

    private fun strikeIndustry(industry: IndustryForBattle, resolve: GroundBattleRoundResolve) {
        val holdingSide = industry.holdingSide
        for (troop in industry.units) {
            var damage = HYPERSTORM_NEX_STRIKE_BASE_DAMAGE
            var disorganizeChance = HYPERSTORM_NEX_BASE_DISORGANIZE_CHANCE

            val side = intel.getSide(troop.isAttacker)
            val troopHolder = (holdingSide == side)

            if (troopHolder) {
                damage *= HYPERSTORM_NEX_HOLDER_DAMAGE_MULT
                disorganizeChance *= HYPERSTORM_NEX_HOLDER_DISORGANIZE_CHANCE_MULT
            }

            val randFloat = MathUtils.getRandom().nextFloat()
            if (disorganizeChance >= randFloat) {
                troopsToReorganize[troop] = HYPERSTORM_NEX_BASE_DISORGANIZE_TURNS
            }
            resolve.damageUnit(troop, damage)
        }
    }

    private fun getIndustriesToTarget(): Int {
        return MathUtils.getRandomNumberInRange(HYPERSTORM_NEX_MIN_INDUSTRIES_TO_TARGET, HYPERSTORM_NEX_MAX_INDUSTRIES_TO_TARGET)
    }

    private fun getTargetIndustries(): MutableSet<IndustryForBattle> {
        val targetIndustries: MutableSet<IndustryForBattle> = HashSet()

        val industries = intel.industries
        val picker = WeightedRandomPicker<IndustryForBattle>()

        for (industry in industries) {
            picker.add(industry, getWeightForIndustry(industry))
        }

        for (i in 1..getIndustriesToTarget()) {
            targetIndustries += picker.pickAndRemove()
            if (picker.isEmpty) break
        }

        return targetIndustries
    }

    override fun addModifierEntry(
        info: TooltipMakerAPI?,
        outer: CustomPanelAPI?,
        width: Float,
        pad: Float,
        isAttacker: Boolean?
    ) {
        if (info == null || outer == null || isAttacker != null) return

        val stormingTerrain = getStormingTerrain()
        if (stormingTerrain.isEmpty()) return
//        val isStorming = stormingTerrain.isNotEmpty()

        //val name = if (isStorming) "Hyperspace Storm" else "Deep Hyperspace"
        val name = "Hyperspace Storm"
        val iconString = "graphics/icons/terrain/hyperspace_storm.png"

        val gen = NexUtilsGUI.addPanelWithFixedWidthImage(
            outer, null, width, MODIFIER_ENTRY_HEIGHT, name, width - MODIFIER_ENTRY_HEIGHT - 8,
            8f, iconString, MODIFIER_ENTRY_HEIGHT, 3f, Misc.getNegativeHighlightColor(), true, modifierTooltip
        )
        info.addCustom(gen.panel, pad)
    }

    override fun processTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
        if (tooltip == null) return

        /*val applicableTerrain = getAffectingTerrain()
        if (applicableTerrain.isEmpty()) return*/

        val protected = marketProtected()

        val stormingTerrain = getStormingTerrain()
        if (stormingTerrain.isEmpty()) return
        //val isStorming = stormingTerrain.isNotEmpty()

        //val firstDesc = "Each troop movement incurs a movement point penalty (TODO: ELABORATE)"
        //val firstLabel = tooltip.addPara(firstDesc, 0f)

        val chance = getStrikeChance(stormingTerrain) * 100

        val timesToPick = "${HYPERSTORM_NEX_MIN_INDUSTRIES_TO_TARGET}-${HYPERSTORM_NEX_MAX_INDUSTRIES_TO_TARGET}"

        val holderDamageMultPercent = MCTE_stringUtils.toPercent(1 - HYPERSTORM_NEX_HOLDER_DAMAGE_MULT)
        val holderDisorganizeChanceMult = MCTE_stringUtils.toPercent(1 - HYPERSTORM_NEX_HOLDER_DISORGANIZE_CHANCE_MULT)

        val desc = "Every turn, there is a %s for %s to be struck by lightning, dealing " +
                "%s to all units with a %s to disorganize for %s." +
                "\n\n" +
                "Units holding the industry receive %s less strike damage, and are %s less likely to disorganize."

        val label = tooltip.addPara(
            desc,
            10f,
            Misc.getHighlightColor(),
            "${chance.toInt()}% chance",
            "$timesToPick industries",
            "${HYPERSTORM_NEX_STRIKE_BASE_DAMAGE.toInt()} damage",
            "${HYPERSTORM_NEX_BASE_DISORGANIZE_CHANCE.toInt()}% chance",
            "$HYPERSTORM_NEX_BASE_DISORGANIZE_TURNS turn(s)",
            holderDamageMultPercent,
            holderDisorganizeChanceMult
        )
        label.setHighlightColors(
            Misc.getHighlightColor(),
            Misc.getHighlightColor(),
            Misc.getNegativeHighlightColor(),
            Misc.getHighlightColor(),
            Misc.getNegativeHighlightColor(),
            Misc.getPositiveHighlightColor(),
            Misc.getPositiveHighlightColor()
        )

        if (protected) {
            val marketName = intel.market.name
            val secondLabel = tooltip.addPara(
                "$marketName is protected by a %s, preventing all of the above effects from occurring.",
                5f,
                Misc.getHighlightColor(),
                "planetary shield"
            )
        }
    }

    private fun entityHasAtmosphere(): Boolean {
        val entity = getEntity() ?: return false
        return (entity is PlanetAPI && entity.spec.atmosphereThickness > 0)

    }

    override fun writeLog(tooltip: TooltipMakerAPI, log: TerrainGroundBattleLog) {
        super.writeLog(tooltip, log)

        val industriesHit = log.params[INDUSTRIES_HIT_PARAMS_ID] as MutableSet<IndustryForBattle>
        if (industriesHit.isEmpty()) return

        val industriesString = getIndustriesString(industriesHit)

        val desc = "%s! Units on %s %s and %s."
        val label = tooltip.addPara(desc, GroundBattleLog.LOG_PADDING, Misc.getHighlightColor(), "Lightning strike", industriesString, "damaged", "disorganized")
        label.setHighlightColors(java.awt.Color.MAGENTA, Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor())
    }

    private fun getIndustriesString(industriesHit: MutableSet<IndustryForBattle>): String {
        var string = ""

        for (industry in industriesHit) {
            val index = industriesHit.indexOf(industry)
            val name = industry.name

            string += name
            if (industriesHit.size > index + 1) string += ", "
        }

        return string
    }

    private fun getStormingTerrain(terrain: MutableSet<CampaignTerrainAPI> = getAffectingTerrain()): MutableSet<CampaignTerrainAPI> {
        val stormingTerrain: MutableSet<CampaignTerrainAPI> = HashSet()
        val entity = getEntity() ?: return stormingTerrain
        for (iterTerrain in terrain) {
            val plugin = iterTerrain.plugin as HyperspaceTerrainPlugin
            val cell = plugin.getCellAt(entity, EXTRA_CELL_DETECTION_RADIUS) ?: continue
            if (cell.isStorming) stormingTerrain += iterTerrain
        }
        return stormingTerrain
    }

    private fun getWeightForIndustry(industry: IndustryForBattle): Float {
        var weight = BASE_INDUSTRY_WEIGHT
        //val realIndustry = industry.industry

        return weight
    }

    /*private fun getMovementCostMult(side: GroundBattleSide): Float {
        val hasAtmosphere = entityHasAtmosphere()

        var cost = MCTE_settings.DEEP_HYPERSPACE_NEX_MOVEMENT_COST
        if (hasAtmosphere) cost *= MCTE_settings.DEEP_HYPERSPACE_NEX_MOVEMENT_COST_ATMOS_MULT
        return cost
    }*/


    override fun getSortOrder(): Float {
        return -700f
    }
}
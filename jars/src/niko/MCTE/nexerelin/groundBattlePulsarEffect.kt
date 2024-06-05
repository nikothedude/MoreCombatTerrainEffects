package niko.MCTE.nexerelin

import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.intel.groundbattle.GroundBattleRoundResolve
import exerelin.campaign.intel.groundbattle.GroundUnit
import exerelin.campaign.intel.groundbattle.IndustryForBattle
import exerelin.utilities.NexUtilsGUI
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.PULSAR_NEX_ENABLED
import niko.MCTE.utils.MCTE_marketUtils.protectedByShield
import niko.MCTE.utils.MCTE_reflectionUtils.get
import niko.MCTE.utils.MCTE_stringUtils

class groundBattlePulsarEffect: groundBattleTerrainEffect() {

    private val troopsToReorganize: MutableMap<GroundUnit, Int> = HashMap()

    override fun terrainIsAffecting(terrain: CampaignTerrainAPI): Boolean {
        if (!PULSAR_NEX_ENABLED) return false
        val plugin = terrain.plugin as? PulsarBeamTerrainPlugin ?: return false
        val entity = getEntity() ?: return false

        val angle = get("pulsarAngle", plugin) as Float // copied from PulsarBeamTerrainPlugin containsPoint because its blocker fucks things up

        if (!Misc.isInArc(angle, PulsarBeamTerrainPlugin.PULSAR_ARC, plugin.entity.location, entity.location) &&
            !Misc.isInArc(angle + 180f, PulsarBeamTerrainPlugin.PULSAR_ARC, plugin.entity.location, entity.location)
        ) {
            return false
        }

        val dist: Float = Misc.getDistance(plugin.entity.location, entity.location)
        return dist >= plugin.pulsarInnerRadius
    }

    override fun beforeTurnResolve(turn: Int) {
        val affectingTerrain = getAffectingTerrain()
        if (shouldApplyEffects(affectingTerrain)) applyEffects(affectingTerrain)
    }

    override fun reportUnitMoved(unit: GroundUnit, lastLoc: IndustryForBattle?) {
        if (unit.location == null || lastLoc != null || !shouldApplyEffects(getAffectingTerrain())) return // on deployment
        tryToReorganizeTroop(unit, 1)
    }

    override fun afterTurnResolve(turn: Int) {
        super.afterTurnResolve(turn)

        for (entry in troopsToReorganize) {
            val unit = entry.key
            val rounds = entry.value

            // modified so reorganizing units dont reorganize again
            tryToReorganizeTroop(unit, rounds)
        }
        troopsToReorganize.clear()
    }

    private fun tryToReorganizeTroop(unit: GroundUnit, rounds: Int) {
        if (unit.location == null || unit.isReorganizing) return
        unit.reorganize(rounds)
    }

    private fun shouldApplyEffects(affectingTerrain: MutableSet<CampaignTerrainAPI>): Boolean {
        if (intel.market.protectedByShield()) return false

        return affectingTerrain.isNotEmpty()
    }

    private fun applyEffects(terrain: MutableSet<CampaignTerrainAPI>) {
        val resolve = GroundBattleRoundResolve(intel)
        for (unit in intel.allUnits) {
            if (unit.location == null) continue
            troopsToReorganize[unit] = 1

            damageTroop(unit, resolve, mult = terrain.size.toFloat())
        }
    }

    private fun damageTroop(unit: GroundUnit, resolve: GroundBattleRoundResolve, mult: Float) {
        val damage = getDamage(unit) * mult
        resolve.damageUnit(unit, damage)
    }

    private fun getDamage(unit: GroundUnit): Float {
        var damage = MCTE_settings.PULSAR_NEX_BASE_DAMAGE

        val holding = (unit.location.heldByAttacker && unit.isAttacker) || (!unit.location.heldByAttacker && !unit.isAttacker)
        if (holding) damage *= MCTE_settings.PULSAR_NEX_HOLDING_DAMAGE_MULT

        return damage
    }

    override fun addModifierEntry(
        info: TooltipMakerAPI?,
        outer: CustomPanelAPI?,
        width: Float,
        pad: Float,
        isAttacker: Boolean?
    ) {
        if (info == null || outer == null || isAttacker != null) return

        val terrain = getAffectingTerrain()
        if (terrain.isEmpty()) return

        val name = "Pulsar"
        val iconString = "graphics/icons/terrain/corona.png"

        val gen = NexUtilsGUI.addPanelWithFixedWidthImage(
            outer, null, width, MODIFIER_ENTRY_HEIGHT, name, width - MODIFIER_ENTRY_HEIGHT - 8,
            8f, iconString, MODIFIER_ENTRY_HEIGHT, 3f, Misc.getNegativeHighlightColor(), true, modifierTooltip
        )
        info.addCustom(gen.panel, pad)
    }

    override fun processTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
        if (tooltip == null) return

        val protected = intel.market.protectedByShield()
        val damage = MCTE_settings.PULSAR_NEX_BASE_DAMAGE.toInt()

        val desc = "All units are %s and take %s damage every turn, applied individually." +
                "\n\n" +
                "Units holding an industry receive %s less damage."

        val label = tooltip.addPara(desc, 10f, Misc.getHighlightColor(), "disorganized", "$damage",
            MCTE_stringUtils.toPercent(1 - MCTE_settings.PULSAR_NEX_HOLDING_DAMAGE_MULT)
        )
        label.setHighlightColors(Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getPositiveHighlightColor())

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
    override fun getSortOrder(): Float {
        return -701f
    }
}
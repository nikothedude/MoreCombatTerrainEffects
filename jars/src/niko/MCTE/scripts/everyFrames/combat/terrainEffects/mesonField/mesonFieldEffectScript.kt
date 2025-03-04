package niko.MCTE.scripts.everyFrames.combat.terrainEffects.mesonField

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.utils.MCTE_mathUtils.trimHangingZero
import niko.MCTE.utils.terrainCombatEffectIds.mesonFieldEffect

class mesonFieldEffectScript(
    var isStorm: Boolean = false,
    var weaponRangeIncrement: Float = 0f,
    var systemRangeMult: Float = 1f,
    var fighterRangeIncrement: Float = 0f,
    var visionMult: Float = 1f
): baseTerrainEffectScript() {

    override var effectPrototype: combatEffectTypes? = combatEffectTypes.MESONFIELD
    companion object {
        fun modifyTerrainTooltip(tooltip: TooltipMakerAPI, nextpad: Float, isStorm: Boolean) {
            if (isStorm) {
                tooltip.addPara(
                    "Significantly increases %s, %s, %s and %s of all ships in the battlespace.",
                    nextpad,
                    Misc.getHighlightColor(),
                    "weapon range", "wing range", "system range", "vision range"
                )
            } else {
                tooltip.addPara(
                    "Somewhat increases %s and %s of all ships in the battlespace.",
                    nextpad,
                    Misc.getHighlightColor(),
                    "non-missile weapon range", "vision range"
                )
            }
        }
    }

    val timer: IntervalUtil = IntervalUtil(0.15f, 0.15f)

    override fun applyEffects(amount: Float) {
        timer.advance(amount)
        if (timer.intervalElapsed()) {
            for (ship in engine.ships) {
                val mutableStats = ship.mutableStats

                mutableStats.ballisticWeaponRangeBonus.modifyFlat(mesonFieldEffect, getRangeIncrementForShip(ship))
                mutableStats.energyWeaponRangeBonus.modifyFlat(mesonFieldEffect, getRangeIncrementForShip(ship))

                mutableStats.sightRadiusMod.modifyMult(mesonFieldEffect, getVisionMultForShip(ship))

                if (!isStorm) continue
                mutableStats.missileWeaponRangeBonus.modifyFlat(mesonFieldEffect, getRangeIncrementForShip(ship))
                mutableStats.fighterWingRange.modifyMult(mesonFieldEffect, getWingRangeIncrementForShip(ship))
                mutableStats.systemRangeBonus.modifyMult(mesonFieldEffect, getSystemRangeMultForShip(ship))
            }
        }

    }

    private fun getSystemRangeMultForShip(ship: ShipAPI): Float {
        return systemRangeMult
    }

    private fun getVisionMultForShip(ship: ShipAPI): Float {
        return visionMult
    }

    private fun getWingRangeIncrementForShip(ship: ShipAPI): Float {
        return fighterRangeIncrement
    }

    private fun getRangeIncrementForShip(ship: ShipAPI): Float {
        return weaponRangeIncrement
    }

    override fun handleSounds(amount: Float) {
        if (isStorm) {
            Global.getSoundPlayer().playUILoop("terrain_corona_am_flare", 1f, 0.4f)
        } else {
            Global.getSoundPlayer().playUILoop("terrain_corona_am", 1f, 0.2f)
        }
    }

   override fun handleNotification(amount: Float): Boolean {
        if (!super.handleNotification(amount)) return false
        val ship = engine.playerShip ?: return false
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
        val stormOrNot = if (isStorm) "storm" else "field"
        val missileOrNot = if (isStorm) "" else "non-missile "
        engine.maintainStatusForPlayerShip(
            "niko_MCTE_mesonField1",
            icon,
            "Meson $stormOrNot",
            "${((getVisionMultForShip(ship))*100).roundTo(2).trimHangingZero()}% vision",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCTE_mesonField2",
            icon,
            "Meson $stormOrNot",
            "+${getRangeIncrementForShip(ship).trimHangingZero()} ${missileOrNot}weapon range",
            true)
        if (!isStorm) return true
        engine.maintainStatusForPlayerShip(
            "niko_MCTE_mesonField3",
            icon,
            "Meson $stormOrNot",
            "+${getWingRangeIncrementForShip(ship).trimHangingZero()} fighter range",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCTE_mesonField4",
            icon,
            "Meson $stormOrNot",
            "${((getSystemRangeMultForShip(ship))*100).roundTo(2).trimHangingZero()}% system range",
            true)
        return true
    }
}
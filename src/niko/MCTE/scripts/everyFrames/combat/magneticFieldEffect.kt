package niko.MCTE.scripts.everyFrames.combat

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import niko.MCTE.utils.terrainCombatEffectIds

class magneticFieldEffect(
    val engine: CombatEngineAPI = Global.getCombatEngine(),
    val visionMod: Float,
    val missileMod: Float,
    val rangeMod: Float,
    val eccmChanceMod: Float,

): BaseEveryFrameCombatPlugin() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (Global.getCurrentState() != GameState.COMBAT) return
        super.advance(amount, events)

        for (ship: ShipAPI in engine.ships) {
            if (ship.mutableStats.sightRadiusMod.getMultBonus(terrainCombatEffectIds.magneticField) == null) {
                ship.mutableStats.sightRadiusMod.modifyMult(terrainCombatEffectIds.magneticField, visionMod)

                ship.mutableStats.missileGuidance.modifyMult(terrainCombatEffectIds.magneticField, missileMod)
                ship.mutableStats.missileMaxTurnRateBonus.modifyMult(terrainCombatEffectIds.magneticField, missileMod)

                ship.mutableStats.ballisticWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, rangeMod)
                ship.mutableStats.energyWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, rangeMod)
                ship.mutableStats.missileWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, rangeMod)
                ship.mutableStats.eccmChance.modifyMult(terrainCombatEffectIds.magneticField, eccmChanceMod)
            }
        }
        //for (missile: MissileAPI in engine.missiles) { }
    }
}
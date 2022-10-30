package niko.MCTE.scripts.everyFrames.combat

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.input.InputEventAPI
import niko.MCTE.utils.terrainCombatEffectIds
import org.lazywizard.lazylib.MathUtils

class magneticFieldEffect(
    val engine: CombatEngineAPI = Global.getCombatEngine(),
    val isStorm: Boolean,
    val visionMod: Float,
    val missileMod: Float,
    val rangeMod: Float,
    val eccmChanceMod: Float,
    var missileBreakLockBaseChance: Float,

    ): BaseEveryFrameCombatPlugin() {
    val random = MathUtils.getRandom()
    var deltaTime = 0f

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (Global.getCurrentState() != GameState.COMBAT) return
        super.advance(amount, events)
        deltaTime += amount

        for (ship: ShipAPI in engine.ships) {
            if (ship.mutableStats.sightRadiusMod.getMultBonus(terrainCombatEffectIds.magneticField) == null) {
                val ecmMult = ship.mutableStats.dynamic.getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifiedValue
                ship.mutableStats.sightRadiusMod.modifyMult(terrainCombatEffectIds.magneticField, visionMod*ecmMult)

                ship.mutableStats.missileGuidance.modifyMult(terrainCombatEffectIds.magneticField, missileMod*ecmMult)
                ship.mutableStats.missileMaxTurnRateBonus.modifyMult(terrainCombatEffectIds.magneticField, missileMod*ecmMult)

                ship.mutableStats.ballisticWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, rangeMod*ecmMult)
                ship.mutableStats.energyWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, rangeMod*ecmMult)
                ship.mutableStats.missileWeaponRangeBonus.modifyMult(terrainCombatEffectIds.magneticField, rangeMod*ecmMult)
                ship.mutableStats.eccmChance.modifyMult(terrainCombatEffectIds.magneticField, eccmChanceMod)
            }
        }

        val thresholdForMissileIterate = 1f
        if (deltaTime >= thresholdForMissileIterate) {
            deltaTime = 0f
            for (missile: MissileAPI in engine.missiles) {
                if (missile !is GuidedMissileAI) continue
                val source: ShipAPI? = missile.source
                var missileBreakLockChance = missileBreakLockBaseChance

                if (source != null) {
                    val stats = source.mutableStats
                    missileBreakLockChance *= stats.dynamic.getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifiedValue
                }

                missileBreakLockBaseChance = missileBreakLockChance.coerceAtMost(1f)
                val randomFloatVal = random.nextInt()
                if (random.)
            }
        }
    }
}
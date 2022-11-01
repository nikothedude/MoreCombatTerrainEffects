package niko.MCTE.scripts.everyFrames.combat.terrainEffects.nebula

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.combat.ShipAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.MCPE_settings
import niko.MCTE.utils.MCPE_settings.NEBULA_DISABLE_ZERO_FLUX_BOOST
import niko.MCTE.utils.MCPE_settings.NEBULA_RANGE_MULT
import niko.MCTE.utils.MCPE_settings.NEBULA_SPEED_DECREMENT
import niko.MCTE.utils.MCPE_settings.NEBULA_VISION_MULT
import niko.MCTE.utils.MCPE_shipUtils.isAffectedByNebulaSecondary
import niko.MCTE.utils.terrainCombatEffectIds
import kotlin.collections.HashMap

class nebulaEffectScript: baseTerrainEffectScript() {

    lateinit var nebulaHandler: CombatNebulaAPI

    val thresholdForAdvancement: Float = 1.2f

    val visionMult = NEBULA_VISION_MULT
    val rangeMult = NEBULA_RANGE_MULT
    val speedDecrement = NEBULA_SPEED_DECREMENT
    val disableZeroFluxBoost = NEBULA_DISABLE_ZERO_FLUX_BOOST

    protected val affectedShips: MutableMap<ShipAPI, Boolean> = HashMap()

    val amountOfTimeElapsedOutsideOfNebula: HashMap<ShipAPI, Float> = HashMap()

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        if (Global.getCurrentState() != GameState.COMBAT) return
        if (!MCPE_settings.EXTRA_NEBULA_EFFECTS_ENABLED) {
            this.engine.removePlugin(this)
            return
        }
        nebulaHandler = this.engine.nebula
    }

    override fun applyEffects(amount: Float) {
        handleShips(amount)
    }

    private fun handleShips(amount: Float) {
        if (engine.isPaused) return
        for (ship: ShipAPI in engine.ships) {
            val mutableStats = ship.mutableStats
            if (affectedShips[ship] == null) {
                if (ship.isAffectedByNebulaSecondary(nebulaHandler)) {
                    mutableStats.ballisticWeaponRangeBonus.modifyMult(terrainCombatEffectIds.nebulaEffect, rangeMult)
                    mutableStats.energyWeaponRangeBonus.modifyMult(terrainCombatEffectIds.nebulaEffect, rangeMult)
                    mutableStats.missileWeaponRangeBonus.modifyMult(terrainCombatEffectIds.nebulaEffect, rangeMult)

                    mutableStats.sightRadiusMod.modifyMult(terrainCombatEffectIds.nebulaEffect, visionMult)

                    mutableStats.maxSpeed.modifyFlat(terrainCombatEffectIds.nebulaEffect, speedDecrement)
                    mutableStats.acceleration.modifyFlat(terrainCombatEffectIds.nebulaEffect, speedDecrement)
                    mutableStats.deceleration.modifyFlat(terrainCombatEffectIds.nebulaEffect, speedDecrement)

                    if (disableZeroFluxBoost) mutableStats.zeroFluxMinimumFluxLevel.modifyFlat(terrainCombatEffectIds.nebulaEffect, -50f)

                    affectedShips[ship] = true
                    amountOfTimeElapsedOutsideOfNebula[ship] = 0f
                }
            } else if (!ship.isAffectedByNebulaSecondary(nebulaHandler)) {
                amountOfTimeElapsedOutsideOfNebula[ship] = amountOfTimeElapsedOutsideOfNebula[ship]!! + amount
                if (amountOfTimeElapsedOutsideOfNebula[ship]!! >= thresholdForAdvancement) {
                    mutableStats.ballisticWeaponRangeBonus.unmodifyMult(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.energyWeaponRangeBonus.unmodifyMult(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.missileWeaponRangeBonus.unmodifyMult(terrainCombatEffectIds.nebulaEffect)

                    mutableStats.sightRadiusMod.unmodifyMult(terrainCombatEffectIds.nebulaEffect)

                    mutableStats.maxSpeed.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.acceleration.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.deceleration.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)

                    mutableStats.zeroFluxMinimumFluxLevel.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)

                    affectedShips -= ship
                    amountOfTimeElapsedOutsideOfNebula -= ship
                }
            } else {
                amountOfTimeElapsedOutsideOfNebula[ship] = 0f
                if (!engine.isEntityInPlay(ship)) affectedShips -= ship
             }
        }
    }

    override fun handleNotification(amount: Float) {
        val playerShip = engine.playerShip
        if (playerShip.isAffectedByNebulaSecondary(nebulaHandler) ||
            (amountOfTimeElapsedOutsideOfNebula[playerShip] != null &&
            amountOfTimeElapsedOutsideOfNebula[playerShip]!! < thresholdForAdvancement)) {
            val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_nebulaEffect1",
                icon,
                "Nebula",
                "Zero-flux boost disabled",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_nebulaEffect2",
                icon,
                "Nebula",
                "Vision range reduced by ${100-visionMult*100}%",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_nebulaEffect3",
                icon,
                "Nebula",
                "Weapon range reduced by ${100-rangeMult*100}%",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_nebulaEffect4",
                icon,
                "Nebula",
                "Speed reduced by ${-speedDecrement} Su",
                true)
        }
    }

    override fun handleSounds(amount: Float) {
        val playerShip = engine.playerShip
        if (playerShip.isAffectedByNebulaSecondary(nebulaHandler)) {
            Global.getSoundPlayer().playUILoop("terrain_nebula", 1f, 1f)
        }
    }
}

package niko.MCTE.scripts.everyFrames.combat.terrainEffects.nebula

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.util.Misc
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.utils.MCTE_settings
import niko.MCTE.utils.MCTE_settings.NEBULA_DISABLE_ZERO_FLUX_BOOST
import niko.MCTE.utils.MCTE_settings.NEBULA_RANGE_MULT
import niko.MCTE.utils.MCTE_settings.NEBULA_SPEED_DECREMENT
import niko.MCTE.utils.MCTE_settings.NEBULA_VISION_MULT
import niko.MCTE.utils.MCTE_shipUtils.isAffectedByNebulaSecondary
import niko.MCTE.utils.terrainCombatEffectIds
import kotlin.collections.HashMap

class nebulaEffectScript: baseTerrainEffectScript() {

    var nebulaHandler: CombatNebulaAPI? = engine?.nebula

    val thresholdForNebulaAdvancement: Float = 1.4f

    val visionMult = NEBULA_VISION_MULT
    val rangeMult = NEBULA_RANGE_MULT
    val speedDecrement = NEBULA_SPEED_DECREMENT
    val disableZeroFluxBoost = NEBULA_DISABLE_ZERO_FLUX_BOOST

    protected val affectedEntities: MutableMap<CombatEntityAPI, Boolean> = HashMap()

    val amountOfTimeElapsedOutsideOfNebula: HashMap<CombatEntityAPI, Float> = HashMap()

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        if (Global.getCurrentState() != GameState.COMBAT) return
        if (!MCTE_settings.EXTRA_NEBULA_EFFECTS_ENABLED) {
            this.engine.removePlugin(this)
            return
        }
        nebulaHandler = this.engine.nebula
        if (nebulaHandler == null) {
            this.engine.removePlugin(this)
        }
    }

    override fun applyEffects(amount: Float) {
        handleShips(amount)
        //handleMissiles(amount)
    }

    private fun handleShips(amount: Float) {
        if (nebulaHandler == null) return
        if (engine.isPaused) return
        for (ship: ShipAPI in engine.ships) {
            val mutableStats = ship.mutableStats
            if (affectedEntities[ship] == null) {
                if (ship.isAffectedByNebulaSecondary(nebulaHandler!!)) {
                    mutableStats.ballisticWeaponRangeBonus.modifyMult(terrainCombatEffectIds.nebulaEffect, rangeMult)
                    mutableStats.energyWeaponRangeBonus.modifyMult(terrainCombatEffectIds.nebulaEffect, rangeMult)
                    mutableStats.missileWeaponRangeBonus.modifyMult(terrainCombatEffectIds.nebulaEffect, rangeMult)

                    mutableStats.sightRadiusMod.modifyMult(terrainCombatEffectIds.nebulaEffect, visionMult)

                    mutableStats.maxSpeed.modifyFlat(terrainCombatEffectIds.nebulaEffect, speedDecrement)
                    mutableStats.acceleration.modifyFlat(terrainCombatEffectIds.nebulaEffect, speedDecrement)
                    mutableStats.deceleration.modifyFlat(terrainCombatEffectIds.nebulaEffect, speedDecrement)

                    if (disableZeroFluxBoost) mutableStats.zeroFluxMinimumFluxLevel.modifyFlat(terrainCombatEffectIds.nebulaEffect, -50f)

                    affectedEntities[ship] = true
                    amountOfTimeElapsedOutsideOfNebula[ship] = 0f
                }
            } else if (!ship.isAffectedByNebulaSecondary(nebulaHandler!!)) {
                amountOfTimeElapsedOutsideOfNebula[ship] = amountOfTimeElapsedOutsideOfNebula[ship]!! + amount
                if (amountOfTimeElapsedOutsideOfNebula[ship]!! >= thresholdForNebulaAdvancement) {
                    mutableStats.ballisticWeaponRangeBonus.unmodifyMult(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.energyWeaponRangeBonus.unmodifyMult(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.missileWeaponRangeBonus.unmodifyMult(terrainCombatEffectIds.nebulaEffect)

                    mutableStats.sightRadiusMod.unmodifyMult(terrainCombatEffectIds.nebulaEffect)

                    mutableStats.maxSpeed.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.acceleration.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.deceleration.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)

                    mutableStats.zeroFluxMinimumFluxLevel.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)

                    affectedEntities -= ship
                    amountOfTimeElapsedOutsideOfNebula -= ship
                }
            } else {
                amountOfTimeElapsedOutsideOfNebula[ship] = 0f
                if (!engine.isEntityInPlay(ship)) affectedEntities -= ship
             }
        }
    }

    private fun handleMissiles(amount: Float) {
        for (missile in engine.missiles) {
            val mutableStats = missile.damage.stats
            if (affectedEntities[missile] == null) {
                if (missile.isAffectedByNebulaSecondary(nebulaHandler!!)) {

                    mutableStats.maxSpeed.modifyFlat(terrainCombatEffectIds.nebulaEffect, 999f)
                    mutableStats.acceleration.modifyFlat(terrainCombatEffectIds.nebulaEffect, 999f)
                    mutableStats.deceleration.modifyFlat(terrainCombatEffectIds.nebulaEffect, speedDecrement)

                    affectedEntities[missile] = true
                    amountOfTimeElapsedOutsideOfNebula[missile] = 0f
                }
            } else if (!missile.isAffectedByNebulaSecondary(nebulaHandler!!)) {
                amountOfTimeElapsedOutsideOfNebula[missile] = amountOfTimeElapsedOutsideOfNebula[missile]!! + amount
                if (amountOfTimeElapsedOutsideOfNebula[missile]!! >= thresholdForNebulaAdvancement) {
                    mutableStats.maxSpeed.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.acceleration.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.deceleration.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)

                    affectedEntities -= missile
                    amountOfTimeElapsedOutsideOfNebula -= missile
                }
            }
        }
    }

    override fun handleNotification(amount: Float) {
        if (nebulaHandler == null) return
        val playerShip = engine.playerShip
        if (playerShip.isAffectedByNebulaSecondary(nebulaHandler!!) ||
            (amountOfTimeElapsedOutsideOfNebula[playerShip] != null &&
            amountOfTimeElapsedOutsideOfNebula[playerShip]!! < thresholdForNebulaAdvancement)) {
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
                "Vision range reduced by ${(100-visionMult*100).roundTo(2)}%",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_nebulaEffect3",
                icon,
                "Nebula",
                "Weapon range reduced by ${(100-rangeMult*100).roundTo(2)}%",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_nebulaEffect4",
                icon,
                "Nebula",
                "Speed reduced by ${-speedDecrement.roundTo(2)} Su",
                true)
        }
    }

    override fun handleSounds(amount: Float) {
        if (nebulaHandler == null) {
            MCTE_debugUtils.log.debug("null nebula handler")
            return
        }
        val playerShip = engine.playerShip
        if (playerShip.isAffectedByNebulaSecondary(nebulaHandler!!)) {
            Global.getSoundPlayer().playUILoop("terrain_nebula", 1f, 1f)
        }
    }
}

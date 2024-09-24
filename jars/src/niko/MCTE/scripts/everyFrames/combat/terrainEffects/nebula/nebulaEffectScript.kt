package niko.MCTE.scripts.everyFrames.combat.terrainEffects.nebula

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.CombatNebulaAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.sun.org.apache.xpath.internal.operations.Bool
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.NEBULA_DISABLE_ZERO_FLUX_BOOST
import niko.MCTE.settings.MCTE_settings.NEBULA_RANGE_MULT
import niko.MCTE.settings.MCTE_settings.NEBULA_SPEED_DECREMENT
import niko.MCTE.settings.MCTE_settings.NEBULA_VISION_MULT
import niko.MCTE.utils.MCTE_shipUtils.hasInsulatedEngines
import niko.MCTE.utils.MCTE_shipUtils.isInsideNebulaAuxillary
import niko.MCTE.utils.terrainCombatEffectIds
import kotlin.collections.HashMap

class nebulaEffectScript: baseTerrainEffectScript() {

    override var effectPrototype: combatEffectTypes? = null

    var nebulaHandler: CombatNebulaAPI? = engine?.nebula

    val thresholdForNebulaAdvancement: Float = 1.4f

    val visionMult = NEBULA_VISION_MULT
    val rangeMult = NEBULA_RANGE_MULT
    val speedDecrement = NEBULA_SPEED_DECREMENT
    val disableZeroFluxBoost = NEBULA_DISABLE_ZERO_FLUX_BOOST

    protected val affectedEntities: MutableMap<CombatEntityAPI, Boolean> = HashMap()

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
                if (ship.isInsideNebulaAuxillary()) {
                    val modifiedRangeMult = getRangeMultForShip(ship)
                    
                    mutableStats.ballisticWeaponRangeBonus.modifyMult(terrainCombatEffectIds.nebulaEffect, modifiedRangeMult)
                    mutableStats.energyWeaponRangeBonus.modifyMult(terrainCombatEffectIds.nebulaEffect, modifiedRangeMult)
                    mutableStats.missileWeaponRangeBonus.modifyMult(terrainCombatEffectIds.nebulaEffect, modifiedRangeMult)

                    val modifiedVisionMult = getVisionMultForShip(ship)
                    mutableStats.sightRadiusMod.modifyMult(terrainCombatEffectIds.nebulaEffect, modifiedVisionMult)

                    val modifiedSpeedDecrement = getSpeedDecrementForShip(ship)
                    mutableStats.maxSpeed.modifyFlat(terrainCombatEffectIds.nebulaEffect, modifiedSpeedDecrement)
                    mutableStats.acceleration.modifyFlat(terrainCombatEffectIds.nebulaEffect, modifiedSpeedDecrement)
                    mutableStats.deceleration.modifyFlat(terrainCombatEffectIds.nebulaEffect, modifiedSpeedDecrement)

                    if (shouldDisableZeroFluxBoost(ship)) mutableStats.zeroFluxMinimumFluxLevel.modifyFlat(terrainCombatEffectIds.nebulaEffect, -50f)

                    affectedEntities[ship] = true
                    //amountOfTimeElapsedOutsideOfNebula[ship] = 0f
                }
            } else {
                if (!engine.isEntityInPlay(ship)){
                    affectedEntities -= ship
                    continue
                }
                if (!ship.isInsideNebulaAuxillary()) {
                    mutableStats.ballisticWeaponRangeBonus.unmodifyMult(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.energyWeaponRangeBonus.unmodifyMult(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.missileWeaponRangeBonus.unmodifyMult(terrainCombatEffectIds.nebulaEffect)

                    mutableStats.sightRadiusMod.unmodifyMult(terrainCombatEffectIds.nebulaEffect)

                    mutableStats.maxSpeed.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.acceleration.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)
                    mutableStats.deceleration.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)

                    mutableStats.zeroFluxMinimumFluxLevel.unmodifyFlat(terrainCombatEffectIds.nebulaEffect)

                    affectedEntities -= ship
                }
            }
        }
    }

    private fun shouldDisableZeroFluxBoost(ship: ShipAPI): Boolean {
        if (!disableZeroFluxBoost || ship.hasInsulatedEngines()) return false
        return true
    }

    private fun getSpeedDecrementForShip(ship: ShipAPI): Float {
        if (ship.hasInsulatedEngines()) return 0f
        var decrement = speedDecrement

        return decrement
    }

    private fun getVisionMultForShip(ship: ShipAPI): Float {
        return visionMult
    }

    private fun getRangeMultForShip(ship: ShipAPI): Float {
        return rangeMult
    }

    override fun handleNotification(amount: Float): Boolean {
        if (!super.handleNotification(amount)) return false
        if (nebulaHandler == null) return false
        val playerShip = engine.playerShip ?: return false
        if (playerShip.isInsideNebulaAuxillary()) {
            val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_nebula")
            if (shouldDisableZeroFluxBoost(playerShip)) {
                val zeroFluxString = if (playerShip.hasInsulatedEngines()) {
                    "Insulated engines preventing loss of zero-flux boost and mitigating speed loss"
                } else {
                    "Zero-flux boost disabled"
                }
                engine.maintainStatusForPlayerShip(
                    "niko_MCPE_nebulaEffect1",
                    icon,
                    "Nebula",
                    zeroFluxString,
                    true
                )
            }
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_nebulaEffect2",
                icon,
                "Nebula",
                "Vision range reduced by ${(100-(getVisionMultForShip(playerShip))*100).toInt()}%",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_nebulaEffect3",
                icon,
                "Nebula",
                "Weapon range reduced by ${(100-(getRangeMultForShip(playerShip))*100).toInt()}%",
                true)
            engine.maintainStatusForPlayerShip(
                "niko_MCPE_nebulaEffect4",
                icon,
                "Nebula",
                "Speed reduced by ${(-getSpeedDecrementForShip(playerShip)).toInt()} Su",
                true)
        }
        return true
    }

    override fun handleSounds(amount: Float) {
        if (nebulaHandler == null) {
            MCTE_debugUtils.log.debug("null nebula handler")
            return
        }
        val playerShip = engine.playerShip ?: return
        if (playerShip.isInsideNebulaAuxillary()) {
            Global.getSoundPlayer().playUILoop("terrain_nebula", 1f, 1f)
        }
    }
}

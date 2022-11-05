package niko.MCTE.scripts.everyFrames.combat.terrainEffects.debrisField

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles
import com.fs.starfarer.api.loading.RoleEntryAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_miscUtils.getGlobalDebrisFieldShipSourcePicker
import org.lwjgl.util.vector.Vector2f

class debrisFieldEffectScript(
    val density: Double,
    val specialSalvage: HashSet<Any>,
): baseTerrainEffectScript() {
    var isDone = false

    //ship ids are the same across the master ship and its fragments
    val shipIdToPieces: MutableMap<String, MutableSet<ShipAPI>> = HashMap()

    val timesToSplitPicker = WeightedRandomPicker<Float>()

    val effectiveDensityDecrement = 0.1

    init {
        addWeightsToPicker()
    }

    private fun addWeightsToPicker() {
        timesToSplitPicker.add(0f, 20f)
        timesToSplitPicker.add(1f, 15f)
        timesToSplitPicker.add(2f, 10f)
        timesToSplitPicker.add(3f, 5f)
        timesToSplitPicker.add(4f, 5f)
        timesToSplitPicker.add(5f, 1f)
        timesToSplitPicker.add(6f, 0.1f)
    }

    override fun applyEffects(amount: Float) {
        if (isDone) return

        var effectiveDensity = getEffectiveDensity()
        while (effectiveDensity > 0f) {
            effectiveDensity -= effectiveDensityDecrement
            val hulk = createHulk() ?: return //just abort
            val hulkId = hulk.id
            shipIdToPieces[hulkId] = splitUpHulk(hulk)
        }

        delete()
    }

    private fun splitUpHulk(hulk: ShipAPI, timesSplit: Float = 0f): MutableSet<ShipAPI> {
        var timesSplit = timesSplit
        var failedSplits = 0f
        val amountOfFailedSplitsTilDone = 10f
        val piecesOfHulk = HashSet<ShipAPI>()
        piecesOfHulk += hulk
        val timesToSplit = (timesToSplitPicker.pick() - timesSplit)

        while (timesSplit < timesToSplit) {
            val splitResult = hulk.splitShip()
            if (splitResult == null) {
                failedSplits++
                MCTE_debugUtils.log.info("null splitship result in splituphulk, incrementing failedsplits")
                if (failedSplits >= amountOfFailedSplitsTilDone) {
                    MCTE_debugUtils.log.info("failedsplits $failedSplits exceeded or met $amountOfFailedSplitsTilDone, exiting loop")
                    break
                }
                continue
            } else failedSplits = 0f
            timesSplit++
            piecesOfHulk += splitUpHulk(splitResult, timesSplit)
        }
        return piecesOfHulk
    }

    private fun createHulk(): ShipAPI? {
        val fleetManager = engine.getFleetManager(100)
        val variant = getVariantForHulk() ?: return null
        val hulk = fleetManager.spawnShipOrWing(variant, getLocationForInitialHulk(), getFacingForInitialHulk(), 0f)
        if (hulk == null) {
            MCTE_debugUtils.displayError("null hulk during createHulk(), aborting")
            return null
        }
        hulkifyShip(hulk)
        return hulk
    }

    private fun hulkifyShip(hulk: ShipAPI) {
        hulk.collisionClass = CollisionClass.FIGHTER
        hulk.hitpoints = 0.0000000001f
        while (hulk.isAlive) {
            engine!!.applyDamage(hulk, hulk.location, 1f, DamageType.ENERGY, 0f, true, false, null, false)
        }
        hulk.collisionClass = CollisionClass.SHIP
    }

    private fun getVariantForHulk(): String? {
        return getGlobalDebrisFieldShipSourcePicker().pick()
    }

    private fun getLocationForInitialHulk(): Vector2f {
        return Vector2f(0f, 0f)
    }

    private fun getFacingForInitialHulk(): Float {
        return 0f
    }

    private fun getEffectiveDensity(): Double {
        return density*10
    }

    override fun handleNotification(amount: Float) {
        return
    }

    override fun handleSounds(amount: Float) {
        return
    }

    private fun delete() {
        isDone = true
        engine.removePlugin(this)
    }
}

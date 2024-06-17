package niko.MCTE.UNGP.rules

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import exerelin.campaign.intel.groundbattle.plugins.MagneticCrustPlugin
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.blackHole.blackHoleEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.deepHyperspaceEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField.magneticFieldEffect
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.mesonField.mesonFieldEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.pulsarBeam.pulsarEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.slipstream.SlipstreamEffectScript
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.terrainEffectCreationLogic
import niko.MCTE.utils.terrainScriptsTracker
import org.lazywizard.lazylib.MathUtils
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings

class randomTerrainEffect: UNGPterrainEffect() {
    var effectMult = 1f

    companion object {
        val chanceToStorm = hashMapOf(Pair(combatEffectTypes.MAGFIELD, 20f), Pair(combatEffectTypes.MESONFIELD, 20f))
    }

    override var classOfScript: Class<out baseTerrainEffectScript>? = null

    override fun resetTicksToWait() {
        ticksToWait = 3
    }

    override fun updateDifficultyCache(difficulty: UNGP_SpecialistSettings.Difficulty?) {
        if (difficulty == null) return

        effectMult = difficulty.getLinearValue(1f, 1f)
        super.updateDifficultyCache(difficulty)
    }

    override fun modifyScript(script: baseTerrainEffectScript) {
        return
    }

    private fun getEffectWeCanUse(): combatEffectTypes? {
        val effectsWeCanUse = hashSetOf(
            combatEffectTypes.MAGFIELD,
            combatEffectTypes.HYPERSPACE,
            combatEffectTypes.SLIPSTREAM,
            combatEffectTypes.PULSAR,
            combatEffectTypes.BLACKHOLE
        )

        if (MCTE_debugUtils.MPCenabled) effectsWeCanUse += combatEffectTypes.MESONFIELD
        for (entry in terrainScriptsTracker.terrainScripts) {
            val list = entry.value

            if (list.isNotEmpty()) {
                effectsWeCanUse -= list.random().effectPrototype ?: continue
                continue
            }
        }
        return effectsWeCanUse.randomOrNull()
    }

    override fun createNewScriptInstance(engine: CombatEngineAPI): baseTerrainEffectScript? {
        val usableEffect = getEffectWeCanUse() ?: return null
        when (usableEffect) {
            combatEffectTypes.SLIPSTREAM -> return combatEffectTypes.SLIPSTREAM.createInformedEffectInstance(effectMult)
            combatEffectTypes.MAGFIELD -> {
                val stormList = ArrayList<Boolean>()
                stormList += shouldDoStorm(combatEffectTypes.MAGFIELD)
                return combatEffectTypes.MAGFIELD.createInformedEffectInstance(stormList, effectMult)
            }
            combatEffectTypes.MESONFIELD -> {
                val stormList = ArrayList<Boolean>()
                stormList += shouldDoStorm(combatEffectTypes.MESONFIELD)
                return combatEffectTypes.MESONFIELD.createInformedEffectInstance(stormList, effectMult)
            }
            combatEffectTypes.HYPERSPACE -> return combatEffectTypes.HYPERSPACE.createInformedEffectInstance(combatEffectTypes.instantiateHyperstormCells(engine, effectMult, true))
            combatEffectTypes.PULSAR -> return combatEffectTypes.PULSAR.createInformedEffectInstance(hashMapOf(Pair(MathUtils.getRandomNumberInRange(0f, 360f), MCTE_settings.PULSAR_BASE_FORCE * 20f)), effectMult)
            combatEffectTypes.BLACKHOLE -> return combatEffectTypes.BLACKHOLE.createInformedEffectInstance(hashMapOf(Pair(MathUtils.getRandomNumberInRange(0f, 360f), MCTE_settings.BLACKHOLE_BASE_GRAVITY * 20f)), effectMult)
        }
        MCTE_debugUtils.log.info("$usableEffect does not match any implemented random terrain effects, skipping random effect gen")
        return null
    }

    private fun shouldDoStorm(type: combatEffectTypes): Boolean {
        val weight = chanceToStorm[type] ?: return false

        val picker = WeightedRandomPicker<Boolean>()
        picker.add(false, 100f)
        picker.add(true, weight)
        return picker.pick()
    }

    override fun getDescriptionParams(index: Int, difficulty: UNGP_SpecialistSettings.Difficulty?): String {
        when (index) {
            0 -> return "$effectMult"
            else -> return ""
        }
    }
}
package niko.MCTE.UNGP.rules

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.util.Misc
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
import niko.MCTE.utils.terrainScriptsTracker
import org.lazywizard.lazylib.MathUtils
import ungp.api.rules.tags.UNGP_CampaignTag
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.asJavaRandom

class randomTerrainEffect: UNGPterrainEffect(), UNGP_CampaignTag {
    var effectMult = 1f
    var needsToAnnounceNextEffect = false
        get() {
            if (field == null) field = false
            return field
        }
    var random: Random = Random(MathUtils.getRandom().nextInt())
        get() {
            if (field == null) field = Random(MathUtils.getRandom().nextInt())
            return field
        }

    companion object {
        val chanceToStorm = hashMapOf(Pair(combatEffectTypes.MAGFIELD, 20f), Pair(combatEffectTypes.MESONFIELD, 20f))
        val effectsWeCanUse = hashMapOf(
            Pair(combatEffectTypes.MAGFIELD, 10f),
            Pair(combatEffectTypes.HYPERSPACE, 5f),
            Pair(combatEffectTypes.SLIPSTREAM, 10f),
            Pair(combatEffectTypes.PULSAR, 1f),
            Pair(combatEffectTypes.BLACKHOLE, 1f)
        )
    }

    init {
        if (MCTE_debugUtils.MPCenabled) effectsWeCanUse += Pair(combatEffectTypes.MESONFIELD, 10f)
        if (MCTE_debugUtils.indEvoEnabled) effectsWeCanUse += Pair(combatEffectTypes.MINEFIELD, 5f)
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
        val localEffects = effectsWeCanUse.toMutableMap()
        for (entry in terrainScriptsTracker.terrainScripts) {
            val list = entry.value

            if (list.isNotEmpty()) {
                localEffects -= list.random().effectPrototype ?: continue
                continue
            }
        }

        val picker = WeightedRandomPicker<combatEffectTypes>()
        localEffects.forEach { if (it.key.isEnabled()) picker.add(it.key, it.value) }
        picker.random = random.asJavaRandom()
        if (picker.isEmpty) return null
        return picker.pick()
    }

    override fun createNewScriptInstance(engine: CombatEngineAPI): baseTerrainEffectScript? {
        val usableEffect = getNextEffect(true) ?: return null
        for (entry in terrainScriptsTracker.terrainScripts) {
            val list = entry.value

            if (list.isNotEmpty() && list.any { it.effectPrototype == usableEffect }) {
                return null
            }
        }

        val baseMult = MCTE_settings.UNGP_EFFECT_BASE_MULT
        val effectMult = (effectMult * baseMult)

        when (usableEffect) {
            combatEffectTypes.SLIPSTREAM -> return combatEffectTypes.SLIPSTREAM.createInformedEffectInstance(effectMult)
            combatEffectTypes.MAGFIELD -> {
                val stormList = ArrayList<Boolean>()
                stormList += shouldDoStorm(combatEffectTypes.MAGFIELD)
                return combatEffectTypes.MAGFIELD.createInformedEffectInstance(
                    stormList,
                    effectMult,
                    ArrayList<MagneticFieldTerrainPlugin>()
                )
            }
            combatEffectTypes.MESONFIELD -> {
                val stormList = ArrayList<Boolean>()
                stormList += shouldDoStorm(combatEffectTypes.MESONFIELD)
                return combatEffectTypes.MESONFIELD.createInformedEffectInstance(stormList, effectMult)
            }
            combatEffectTypes.HYPERSPACE -> return combatEffectTypes.HYPERSPACE.createInformedEffectInstance(combatEffectTypes.instantiateHyperstormCells(engine, effectMult, true))
            combatEffectTypes.PULSAR -> return combatEffectTypes.PULSAR.createInformedEffectInstance(hashMapOf(Pair(MathUtils.getRandomNumberInRange(0f, 360f), 0.6f)), effectMult * 0.4f)
            combatEffectTypes.BLACKHOLE -> return combatEffectTypes.BLACKHOLE.createInformedEffectInstance(hashMapOf(Pair(MathUtils.getRandomNumberInRange(0f, 360f), 0.4f)), effectMult * 0.5f)
            combatEffectTypes.MINEFIELD -> {
                val targettingPlayer = (MathUtils.getRandomNumberInRange(0, 1) * (effectMult * 0.25f)).roundToInt()
                val targettingEnemy = (MathUtils.getRandomNumberInRange(0, 1) * (effectMult * 0.25f)).roundToInt()
                return combatEffectTypes.MINEFIELD.createInformedEffectInstance(hashMapOf(Pair(0, targettingPlayer), Pair(1, targettingEnemy)))
            }
            else -> {
                MCTE_debugUtils.log.info("$usableEffect does not match any implemented random terrain effects, skipping random effect gen")
                return null
            }
        }
    }

    private fun shouldDoStorm(type: combatEffectTypes): Boolean {
        val weight = chanceToStorm[type] ?: return false

        val picker = WeightedRandomPicker<Boolean>()
        picker.add(false, 100f)
        picker.add(true, weight)
        return picker.pick()
    }

    override fun getDescriptionParams(index: Int, difficulty: UNGP_SpecialistSettings.Difficulty?): String {
        updateDifficultyCache(difficulty)
        when (index) {
            0 -> return "${effectMult}x"
            1 -> return if (getNextEffect() != null) getNextEffect()!!.frontEndName else "Not initialized!"
            else -> return ""
        }
    }

    fun getNextEffect(withClear: Boolean = false): combatEffectTypes? {
        var next = Global.getSector()?.memoryWithoutUpdate?.get("\$MCTE_UNGPnextEffect") as? combatEffectTypes
        if (withClear) {
            updateNextEffect()
        }
        return next
    }

    fun updateNextEffect() {
        val usable = getEffectWeCanUse() ?: return
        Global.getSector()?.memoryWithoutUpdate?.set("\$MCTE_UNGPnextEffect", usable)
        needsToAnnounceNextEffect = true
    }

    override fun advanceInCampaign(
        amount: Float,
        params: UNGP_CampaignPlugin.TempCampaignParams?
    ) {
        if (needsToAnnounceNextEffect) {
            needsToAnnounceNextEffect = false
            val effect = getNextEffect() ?: return
            val campaignUI = Global.getSector().campaignUI ?: return
            campaignUI.addMessage(
                "Next effect: ${effect.frontEndName}!",
                Misc.getNegativeHighlightColor()
            )
        } else if (Global.getSector()?.memoryWithoutUpdate?.get("\$MCTE_UNGPnextEffect") == null) {
            updateNextEffect()
        }
    }
}
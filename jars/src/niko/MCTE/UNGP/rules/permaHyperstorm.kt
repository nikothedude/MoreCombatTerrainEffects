package niko.MCTE.UNGP.rules

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.deepHyperspace.deepHyperspaceEffectScript
import niko.MCTE.settings.MCTE_settings
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings

class permaHyperstorm: UNGPterrainEffect() {
    override var classOfScript: Class<out baseTerrainEffectScript>? = deepHyperspaceEffectScript::class.java

    var sizeMult = 1f

    override fun updateDifficultyCache(difficulty: UNGP_SpecialistSettings.Difficulty?) {
        if (difficulty == null) return

        sizeMult = difficulty.getLinearValue(1f, 0.5f)
        super.updateDifficultyCache(difficulty)
    }

    override fun modifyScript(script: baseTerrainEffectScript) {
        if (script !is deepHyperspaceEffectScript) return
        script.stormingCells.addAll(combatEffectTypes.instantiateHyperstormCells(Global.getCombatEngine(), sizeMult, true))
    }

    override fun createNewScriptInstance(engine: CombatEngineAPI): baseTerrainEffectScript {
        return combatEffectTypes.HYPERSPACE.createInformedEffectInstance(combatEffectTypes.instantiateHyperstormCells(engine, sizeMult, true))
    }

    override fun getDescriptionParams(index: Int, difficulty: UNGP_SpecialistSettings.Difficulty?): String {
        updateDifficultyCache(difficulty)
        val minClouds = MCTE_settings.MIN_HYPERCLOUDS_TO_ADD_PER_CELL * sizeMult
        val maxClouds = MCTE_settings.MAX_HYPERCLOUDS_TO_ADD_PER_CELL * sizeMult

        return when (index) {
            0 -> minClouds.toString()
            1 -> maxClouds.toString()
            2 -> "storming hyperclouds"
            3 -> "strike nearby ships with lightning"
            else -> ""
        }
    }

}
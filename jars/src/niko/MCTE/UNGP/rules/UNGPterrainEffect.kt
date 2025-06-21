package niko.MCTE.UNGP.rules

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.terrainScriptsTracker
import ungp.api.rules.UNGP_BaseRuleEffect
import ungp.api.rules.tags.UNGP_CampaignTag
import ungp.api.rules.tags.UNGP_CombatInitTag
import ungp.api.rules.tags.UNGP_CombatTag
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings

abstract class UNGPterrainEffect: UNGP_BaseRuleEffect(), UNGP_CombatTag, UNGP_CombatInitTag {
    abstract var classOfScript: Class<out baseTerrainEffectScript>?
    var applied = false
    var ticksToWait = 2 // this value doesnt matter, check init()

    override fun init(engine: CombatEngineAPI?) {
        applied = false
        resetTicksToWait() // reset, just in case
    }

    open fun resetTicksToWait() {
        ticksToWait = 2
    }

    override fun applyEnemyShipInCombat(amount: Float, enemy: ShipAPI?) {
        return
    }

    override fun applyPlayerShipInCombat(amount: Float, engine: CombatEngineAPI?, ship: ShipAPI?) {
        return
    }

    override fun advanceInCombat(engine: CombatEngineAPI?, amount: Float) {
        if (engine == null) return
        if (engine.isSimulation) return
        if (applied) return
        if (ticksToWait-- > 0) return

        applyToCombat(engine)
        applied = true
    }

    open fun applyToCombat(engine: CombatEngineAPI) {
        if (modifyExistingScript(engine)) return
        createNewScript(engine)
    }

    open fun modifyExistingScript(engine: CombatEngineAPI): Boolean {
        val existingScript = getExistingScript(engine) ?: return false
        modifyScript(existingScript)
        return true
    }

    abstract fun modifyScript(script: baseTerrainEffectScript)
    open fun getExistingScript(engine: CombatEngineAPI): baseTerrainEffectScript? {
        return terrainScriptsTracker.terrainScripts[classOfScript]?.randomOrNull()
    }
    abstract fun createNewScriptInstance(engine: CombatEngineAPI): baseTerrainEffectScript?

    open fun createNewScript(engine: CombatEngineAPI): baseTerrainEffectScript? {
        val script = createNewScriptInstance(engine)
        script?.start()
        return script
    }
}
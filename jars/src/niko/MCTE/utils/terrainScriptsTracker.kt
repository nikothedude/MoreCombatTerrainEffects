package niko.MCTE.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript

object terrainScriptsTracker {
    val terrainScripts: MutableMap<Class<out baseTerrainEffectScript>, MutableSet<baseTerrainEffectScript>> = HashMap()

    fun addScript(script: baseTerrainEffectScript) {

        var listOfScript = terrainScripts[script.javaClass]
        if (listOfScript == null) {
            terrainScripts[script.javaClass] = HashSet()
            listOfScript = terrainScripts[script.javaClass]
        }
        listOfScript!!.add(script)
    }

    fun removeScript(script: baseTerrainEffectScript) {
        val listOfScript = terrainScripts[script.javaClass] ?: return
        listOfScript.remove(script)
    }
}
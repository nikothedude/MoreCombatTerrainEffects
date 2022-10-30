package niko.MCTE.scripts.everyFrames.combat

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.input.InputEventAPI

class terrainEffectScriptAdder: BaseEveryFrameCombatPlugin() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (Global.getCurrentState() != GameState.COMBAT) return
        super.advance(amount, events)

        val engine = Global.getCombatEngine() ?: return
        val playerFleet = Global.getSector().playerFleet ?: return
        val playerLocation = playerFleet.containingLocation ?: return
        val playerCoordinates = playerFleet.location ?: return

        addMagneticFieldScript(engine, playerFleet, playerLocation)
        engine.removePlugin(this)
    }

    private fun addMagneticFieldScript(engine: CombatEngineAPI, playerFleet: CampaignFleetAPI, playerLocation: LocationAPI) {

        var isStorm = false
        var visionMod = 1f
        var missileMod = 1f
        var rangeMod = 1f
        var eccmChanceMod = 1f
        var missileBreakLockBaseChance = 0f
        var canAddPlugin = false
        for (terrain: CampaignTerrainAPI in playerLocation.terrainCopy) {
            if (terrain is MagneticFieldTerrainPlugin) {
                if (!terrain.containsEntity(playerFleet)) continue
                val isInFlare = (terrain.terrainName == "Magnetic Storm")
                if (isInFlare) isStorm = true

                visionMod *= if (isInFlare) 0.1f else 0.5f
                missileMod *= if (isInFlare) 0.1f else 0.7f
                rangeMod *= if (isInFlare) 0.3f else 0.8f
                eccmChanceMod *= if (isInFlare) 0.2f else 0.7f
                missileBreakLockBaseChance += if (isInFlare) 60f else 10f
                canAddPlugin = true
            }
        }
        if (canAddPlugin) {
            if (isStorm) {
                Global.getSoundPlayer().playUILoop("terrain_magstorm", 1f, 0.1f)
            } else {
                Global.getSoundPlayer().playUILoop("terrain_magfield", 1f, 0.1f)
            }
            engine.addPlugin(magneticFieldEffect(engine, isStorm, visionMod, missileMod, rangeMod, eccmChanceMod, missileBreakLockBaseChance))
        }
    }
}
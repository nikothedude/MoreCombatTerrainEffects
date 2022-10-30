package niko.MCTE.scripts.everyFrames.combat

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.input.InputEventAPI

class magneticFieldEffectAdder: BaseEveryFrameCombatPlugin() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (Global.getCurrentState() != GameState.COMBAT) return
        super.advance(amount, events)

        val engine = Global.getCombatEngine() ?: return
        val playerFleet = Global.getSector().playerFleet ?: return
        val playerLocation = playerFleet.containingLocation ?: return
        val playerCoordinates = playerFleet.location ?: return

        var visionMod = 1f
        var missileMod = 1f
        var rangeMod = 1f
        var eccmChanceMod = 1f
        var canAddPlugin = false
        for (terrain: CampaignTerrainAPI in playerLocation.terrainCopy) {
            if (terrain is MagneticFieldTerrainPlugin) {
                if (!terrain.containsEntity(playerFleet)) continue
                val isInFlare = (terrain.terrainName == "Magnetic Storm")

                visionMod -= if (isInFlare) visionMod*0.1f else visionMod*0.5f
                missileMod -= if (isInFlare) missileMod*0.1f else missileMod*0.7f
                rangeMod -= if (isInFlare) rangeMod*0.3f else rangeMod*0.8f
                eccmChanceMod = if (isInFlare) eccmChanceMod*0.2f else eccmChanceMod*0.7f
                canAddPlugin = true
            }
        }
        if (canAddPlugin) engine.addPlugin(magneticFieldEffect(engine, visionMod, missileMod, rangeMod, eccmChanceMod))
        engine.removePlugin(this)
    }
}
package niko.MCTE.scripts.everyFrames.combat.terrainEffects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatLayeredRenderingPlugin
import com.fs.starfarer.api.input.InputEventAPI

abstract class renderableEffect: baseTerrainEffectScript() {

    val renderingPlugins: MutableSet<CombatLayeredRenderingPlugin> = HashSet()

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        for (renderingPlugin in renderingPlugins) this.engine.addLayeredRenderingPlugin(renderingPlugin)
    }
}
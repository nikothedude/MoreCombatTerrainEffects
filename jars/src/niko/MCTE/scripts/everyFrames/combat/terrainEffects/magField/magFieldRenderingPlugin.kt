package niko.MCTE.scripts.everyFrames.combat.terrainEffects.magField

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.RangeBlockerUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class magFieldRenderingPlugin(
    val plugins: MutableSet<MagneticFieldTerrainPlugin>
): BaseCombatLayeredRenderingPlugin() {
    val engine = Global.getCombatEngine()
    val playerFleet: CampaignFleetAPI? = Global.getSector().playerFleet

    val pluginToPlayerfleetAngle: MutableMap<MagneticFieldTerrainPlugin, Float> = HashMap()

    val pluginToRenderPoint: HashMap<MagneticFieldTerrainPlugin, Float> = HashMap()

    init {
        val maxHeight = engine.mapHeight
        val maxWidth = engine.mapWidth
        for (plugin in plugins) {
            var angle = 5f
            if (playerFleet != null && playerFleet.location != null) {
                angle = VectorUtils.getAngle(plugin.auroraCenterLoc, playerFleet.location)
            }
            pluginToPlayerfleetAngle[plugin] = angle

            var renderPoint: Vector2f
            renderPoint = ()
        }
    }

    protected val layers: EnumSet<CombatEngineLayers> = EnumSet.of(CombatEngineLayers.ABOVE_PLANETS)

    override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
        return layers
    }

    var phaseAngle = 10f
    override fun advance(amount: Float) {
        super.advance(amount)
        val days = Global.getSector().clock.convertToDays(amount)
        val phaseMult = 0.1f
        phaseAngle += days * 360f * phaseMult
        phaseAngle = Misc.normalizeAngle(phaseAngle)
    }

    override fun getRenderRadius(): Float {
        return 9999999f
    }

    override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {
        // waht the fuck

        val alphaMult = viewport?.alphaMult
        if (alphaMult == null || alphaMult <= 0) return
        for (plugin in plugins) {

            val bandWidthInTexture: Float = plugin.auroraBandWidthInTexture
            var bandIndex: Float

            val radStart: Float = 0f //controls inner radius of the aurora
            var radEnd: Float = 40000f //vice versa

            if (radEnd < radStart + 10f) radEnd = radStart + 10f

            val circ = ((Math.PI * 2f * (radStart + radEnd) / 2f).toFloat())
            val pixelsPerSegment = 50f
            val segments = ((circ / pixelsPerSegment).roundToInt().toFloat())

            val startRad = Math.toRadians(180.0).toFloat()
            val endRad = Math.toRadians(360.0).toFloat()
            val spanRad = abs(endRad - startRad)
            val anglePerSegment = (spanRad / segments)

            val loc = Vector2f(0f, -engine.mapHeight)
            val x = loc.x
            val y = loc.y

            GL11.glPushMatrix()
            GL11.glTranslatef(x, y, 0f) //translation, controls the spatial center

            GL11.glEnable(GL11.GL_TEXTURE_2D)

            plugin.auroraTexture.bindTexture()

            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)

            val thickness = (radEnd - radStart) * 1f

            var texProgress = 0f
            val texHeight: Float = plugin.auroraTexture.textureHeight
            val imageHeight: Float = plugin.auroraTexture.height
            var texPerSegment = pixelsPerSegment * texHeight / imageHeight * bandWidthInTexture / thickness

            texPerSegment *= plugin.auroraTexPerSegmentMult*3

            val totalTex = Math.max(1f, (texPerSegment * segments).roundToInt().toFloat())
            texPerSegment = totalTex / segments

            val texWidth: Float = plugin.auroraTexture.textureWidth
            val imageWidth: Float = plugin.auroraTexture.width

            val blocker: RangeBlockerUtil? = plugin.auroraBlocker

            for (iter in 0..1) {
                bandIndex = if (iter == 0) {
                    1f
                } else {
                    0f
                }
                val leftTX = (bandIndex * texWidth * bandWidthInTexture / imageWidth)
                val rightTX = (bandIndex + 1f) * texWidth * bandWidthInTexture / imageWidth - 0.001f
                GL11.glBegin(GL11.GL_QUAD_STRIP)
                var i = 0f
                while (i < (segments + 1)) {
                    val segIndex = i % segments.toInt()

                    //float phaseAngleRad = (float) Math.toRadians(phaseAngle + segIndex * 10) + (segIndex * anglePerSegment * 10f);
                    val phaseAngleRad: Float = if (iter == 0) {
                        Math.toRadians(phaseAngle.toDouble()).toFloat() + segIndex * anglePerSegment * 10f
                    } else { //if (iter == 1) {
                        Math.toRadians(-phaseAngle.toDouble()).toFloat() + segIndex * anglePerSegment * 5f
                    }
                    var angle = Math.toDegrees((segIndex * anglePerSegment).toDouble()).toFloat()
                    if (iter == 1) angle += 180f
                    var blockerMax = 100000f
                    if (blocker != null) {
                        blockerMax = blocker.getCurrMaxAt(angle)
                        //blockerMax *= 1.5f;
                        blockerMax *= 0.75f
                        if (blockerMax > blocker.maxRange) {
                            blockerMax = blocker.maxRange
                        }
                        //blockerMax += 1500f;
                    }
                    val pulseSin = sin(phaseAngleRad.toDouble()).toFloat()
                    //if (plugin instanceof PulsarBeamTerrainPlugin) pulseSin += 1f;
                    var pulseMax: Float = thickness * plugin.getAuroraShortenMult(angle)
                    //				if (pulseMax < 0) {
//					pulseMax = -pulseMax;
//					//pulseSin += 1f;
//				}
                    if (pulseMax > blockerMax * 0.5f) {
                        pulseMax = blockerMax * 0.5f
                    }
                    val pulseAmount = pulseSin * pulseMax
                    var pulseInner = pulseAmount * 0.1f
                    pulseInner *= plugin.getAuroraInnerOffsetMult(angle)
                    //pulseInner *= Math.max(0, pulseSin - 0.5f);
                    //pulseInner *= 0f;
                    val thicknessMult: Float = plugin.getAuroraThicknessMult(angle) // thickness
                    val thicknessFlat: Float = plugin.getAuroraThicknessFlat(angle) // also thickness
                    val theta = anglePerSegment * segIndex
                    val cos = cos(theta.toDouble()).toFloat()
                    val sin = sin(theta.toDouble()).toFloat()
                    var rInner = radStart - pulseInner
                    if (rInner < radStart * 0.9f) rInner = radStart * 0.9f
                    var rOuter = radStart + thickness * thicknessMult - pulseAmount + thicknessFlat
                    if (blocker != null) {
                        if (rOuter > blockerMax - pulseAmount) {
//						float fraction = rOuter / (r + thickness * thicknessMult + thicknessFlat);
//						rOuter = blockerMax * fraction;
                            rOuter = blockerMax - pulseAmount
                            //rOuter = blockerMax;
                            if (rOuter < radStart) rOuter = radStart
                        }
                        if (rInner > rOuter) {
                            rInner = rOuter
                        }
                    }
                    val x1 = cos * rInner
                    val y1 = sin * rInner
                    var x2 = cos * rOuter
                    var y2 = sin * rOuter
                    x2 += (cos(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()
                    y2 += (sin(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()
                    val color: Color = plugin.getAuroraColorForAngle(angle)
                    var alpha: Float = plugin.getAuroraAlphaMultForAngle(angle)
                    if (blocker != null) {
                        alpha *= blocker.getAlphaAt(angle)
                    }
                    GL11.glColor4ub(
                        color.red.toByte(),
                        color.green.toByte(),
                        color.blue.toByte(),
                        (color.alpha.toFloat() * alphaMult * alpha).toInt().toByte()
                    )
                    GL11.glTexCoord2f(leftTX, texProgress)
                    GL11.glVertex2f(x1, y1)
                    GL11.glTexCoord2f(rightTX, texProgress)
                    GL11.glVertex2f(x2, y2) //these 2 methods control the size
                    texProgress += texPerSegment * 1f
                    i++
                }
                GL11.glEnd()
                //GL11.glRotatef(180f, 0f, 0f, 1f)
            }
            GL11.glPopMatrix()

//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }
    }
}
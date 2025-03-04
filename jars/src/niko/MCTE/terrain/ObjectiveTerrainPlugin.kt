package niko.MCTE.terrain

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.CampaignObjective
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko.MCTE.ObjectiveEffect
import niko.MCTE.utils.MCTE_ids
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*

class ObjectiveTerrainPlugin: BaseRingTerrain() {
    lateinit var ourParams: ObjectiveTerrainParams
    val UID = Misc.genUID()

    class ObjectiveTerrainParams(
        bandWidthInEngine: Float,
        middleRadius: Float,
        relatedEntity: SectorEntityToken,
        var effect: ObjectiveEffect
    ): RingParams(bandWidthInEngine, middleRadius, relatedEntity, null)

    var interval = IntervalUtil(1.1f, 1.2f) // days
        get() {
            if (field == null) field = IntervalUtil(1.1f, 1.2f)
            return field
        }

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any) {
        super.init(terrainId, entity, param)

        ourParams = param as ObjectiveTerrainParams
        name = "error"
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        interval.advance(Misc.getDays(amount))
        if (interval.intervalElapsed()) {
            if (!ourParams.relatedEntity.isAlive) {
                entity?.containingLocation?.removeEntity(entity)
                ourParams.relatedEntity.memoryWithoutUpdate[MCTE_ids.OBJECTIVE_TERRAIN_MEMID] = null
            }
        }
    }

    override fun getTerrainName(): String {
        return nameForTooltip
    }

    override fun getTerrainId(): String {
        return UID // TODO: this is almost definitely a bad idea, but it lets the terrain tooltips stack
    }

    override fun getNameForTooltip(): String {
        val playerFleet = Global.getSector().playerFleet
        val objective = ourParams.relatedEntity

        return ourParams.effect.getTerrainName(playerFleet, objective as CustomCampaignEntityAPI)
    }

    override fun getNameColor(): Color {
        val playerFleet = Global.getSector().playerFleet
        val objective = ourParams.relatedEntity as? CustomCampaignEntityAPI ?: return super.getNameColor()

        if (ourParams.effect.wantToAssist(playerFleet, objective)) {
            return Misc.getPositiveHighlightColor()
        } else {
            if (objective.faction.relToPlayer.isHostile) {
                return Misc.getNegativeHighlightColor()
            }
            return Misc.getGrayColor()
        }
    }

    override fun hasTooltip(): Boolean {
        return true
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        if (tooltip == null) return
        val playerFleet = Global.getSector().playerFleet
        val objective = ourParams.relatedEntity
        ourParams.effect.createTerrainTooltip(tooltip, expanded, playerFleet, objective as CustomCampaignEntityAPI)
    }

    override fun containsEntity(other: SectorEntityToken?): Boolean {
        if (other == null) return false

        //val objective = ourParams.relatedEntity
        //if (objective.faction.id == Factions.NEUTRAL) return false

        return super.containsEntity(other)
    }

    override fun getEffectCategory(): String? {
        return null // can stack with other terrain
    }

    override fun getRenderRange(): Float {
        return 0f
    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers> {
        return EnumSet.of(CampaignEngineLayers.TERRAIN_1) // arbitrary
    }

    override fun canPlayerHoldStationIn(): Boolean {
        return false
    }

    /*override fun getMinEffectRadius(locFrom: Vector2f?): Float {
        return 0f
    }*/

}
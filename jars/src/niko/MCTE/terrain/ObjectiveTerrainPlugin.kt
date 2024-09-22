package niko.MCTE.terrain

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain

class ObjectiveTerrainPlugin: BaseRingTerrain() {
    class ObjectiveTerrainParams(
        bandWidthInEngine: Float,
        middleRadius: Float,
        relatedEntity: SectorEntityToken,
        var innerRadius: Float,
        var outerRadius: Float,

    ): RingParams(bandWidthInEngine, middleRadius, relatedEntity, null)
}
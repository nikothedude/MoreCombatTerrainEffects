package niko.MCTE.scripts.everyFrames.combat.terrainEffects.pulsarBeam

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.utils.MCTE_miscUtils
import niko.MCTE.utils.MCTE_miscUtils.getAllObjects
import niko.MCTE.utils.MCTE_settings.PULSAR_BASE_FORCE
import niko.MCTE.utils.MCTE_settings.PULSAR_FORCE_ENABLED
import niko.MCTE.utils.MCTE_settings.PULSAR_PPT_COMPENSATION
import niko.MCTE.utils.MCTE_shipUtils.isTangible
import niko.MCTE.utils.terrainCombatEffectIds
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class pulsarEffectScript(
    val plugins: MutableSet<PulsarBeamTerrainPlugin>,
    val pluginsToIntensity: MutableMap<PulsarBeamTerrainPlugin, Float>,
    val pluginsToAngle: MutableMap<PulsarBeamTerrainPlugin, Float>,
    val hardFluxGenerationPerFrame: Float,
    val bonusEMPDamageForWeapons: Float,
    val shieldDestabilziationMult: Float,
    val EMPChancePerFrame: Float,
    val EMPdamage: Float,
    val energyDamage: Float,
): baseTerrainEffectScript(), usesDeltaTime, DamageDealtModifier {
    private val timesToDoThingPerSecond = 60f
    override var deltaTime: Float = 0f

    override val thresholdForAdvancement: Float = (1/timesToDoThingPerSecond)
    private val color = Color(159, 255, 231, 255)

    protected val affectedShips: MutableMap<ShipAPI, Boolean> = HashMap()
    private val chargedProjectiles: MutableMap<DamagingProjectileAPI, Float> = HashMap()
    private val chargedWeapons: MutableMap<WeaponAPI, Float> = HashMap()

    var overallIntensity: Float = 0f

    init {
        for (intensity in pluginsToIntensity.values) {
            overallIntensity += intensity
        }
    }

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)

        this.engine.listenerManager.addListener(this)
    }

    override fun applyEffects(amount: Float) {
        applyStats()
        applyVisuals()

        if (!canAdvance(amount)) return

        applyForce()
        generateFlux()
        applyEMP()
    }

    private fun applyStats() {
        for (ship: ShipAPI in engine.ships) {
            if (affectedShips[ship] == null) {

                val effectMult: Float = getEffectMultForShip(ship)
                val modifiedShieldMult = getShieldMultForShip(ship)

                val mutableStats = ship.mutableStats

                mutableStats.shieldDamageTakenMult.modifyMult(terrainCombatEffectIds.pulsarEffect, modifiedShieldMult)
                mutableStats.shieldUpkeepMult.modifyMult(terrainCombatEffectIds.pulsarEffect, modifiedShieldMult)
                mutableStats.dynamic.getStat(Stats.SHIELD_PIERCED_MULT).modifyMult(terrainCombatEffectIds.pulsarEffect, 1/modifiedShieldMult)

                replaceExistingEffect(ship, mutableStats)
                chargeWeapons(ship, mutableStats)
                affectedShips[ship] = true
            }
        }
        for (projectile: DamagingProjectileAPI in engine.projectiles) {
            if (isChargedProjectile(projectile)) return
            chargeProjectile(projectile)
            return
        }
        val iterator = chargedProjectiles.keys.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            if (!engine.isEntityInPlay(projectile)) {
                iterator.remove()
                continue
            }
        }
    }

    private fun getShieldMultForShip(ship: ShipAPI): Float {
        val effectMult = getEffectMultForShip(ship)
        return (shieldDestabilziationMult * effectMult).coerceAtLeast(1f)
    }

    private fun isChargedProjectile(projectile: DamagingProjectileAPI): Boolean {
        if (chargedProjectiles[projectile] != null) return true
        val weapon: WeaponAPI? = projectile.weapon
        if (weapon != null && chargedWeapons[weapon] != null) return true
        return false
    }

    private fun chargeProjectile(projectile: DamagingProjectileAPI) {
        val bonusDamage = getChargeForProjectile(projectile)
        val damage = projectile.damage
        damage.fluxComponent += (bonusDamage)
        chargedProjectiles[projectile] = bonusDamage
    }

    private fun getChargeForProjectile(projectile: DamagingProjectileAPI): Float {
        val weapon: WeaponAPI? = projectile.weapon
        if (weapon != null) {
            if (chargedWeapons[weapon] != null) {
                return chargedWeapons[weapon]!!
            }
        } else {
            val source: ShipAPI? = projectile.source
            if (source != null) {
                val effectMult = getEffectMultForShip(source)
                return bonusEMPDamageForWeapons*(effectMult.coerceAtLeast(1f))
            }
        }
        return bonusEMPDamageForWeapons
    }

    private fun chargeWeapons(ship: ShipAPI, mutableStats: MutableShipStatsAPI) {
        for (weapon: WeaponAPI in ship.allWeapons) {
            chargeWeapon(ship, weapon)
        }
    }

    private fun chargeWeapon(ship: ShipAPI, weapon: WeaponAPI) {
        val empAmount: Float = getChargeForWeapon(ship, weapon)
        weapon.damage.fluxComponent += empAmount
        chargedWeapons[weapon] = empAmount
    }

    private fun getChargeForWeapon(ship: ShipAPI, weapon: WeaponAPI): Float {
        val shipCharge = getChargeForShip(ship)
        return shipCharge
    }

    private fun getChargeForShip(ship: ShipAPI): Float {
        val effectMult = getEffectMultForShip(ship)
        return bonusEMPDamageForWeapons*(effectMult.coerceAtLeast(1f))
    }

    private fun replaceExistingEffect(ship: ShipAPI, mutableStats: MutableShipStatsAPI) {
        val effectMult = getEffectMultForShip(ship)
        var compensation = (PULSAR_PPT_COMPENSATION/(overallIntensity+1)/effectMult).coerceAtMost(1f)
        for (PPTmod in mutableStats.peakCRDuration.multBonuses) {
            if (PPTmod.key.contains("pulsar_beam_stat_mod_", true)) {
                val value = PPTmod.value.value
                if (compensation == 1f) compensation += 0.000000000000001f //since not doing this doesnt cause a recompute
                mutableStats.peakCRDuration.modifyMult(PPTmod.key, (compensation + value) - (compensation * value))
            }
        }
        for (CRLossMod in mutableStats.crLossPerSecondPercent.multBonuses) {
            if (CRLossMod.key.contains("pulsar_beam_stat_mod_", true)) {
                val value = CRLossMod.value.value
                if (compensation == 1f) compensation += 0.00000000000001f
                mutableStats.crLossPerSecondPercent.modifyMult(CRLossMod.key, (compensation + value) - (compensation * value))
            }
        }
    }

    private fun applyVisuals() {
       return
    }

    private fun applyForce() {
        if (engine.isPaused) return
        if (!PULSAR_FORCE_ENABLED) return
        for (entry in pluginsToIntensity.entries) {
            val plugin = entry.key
            val intensity = entry.value

            for (entity: CombatEntityAPI in engine.getAllObjects()) {
                if (!engine.isInPlay(entity)) continue
                if (!entity.isTangible()) continue
                var mass = entity.mass
                if (mass == 0f) {
                    if (entity is DamagingProjectileAPI) {
                        mass = (entity.damageAmount)/100
                    }
                }
                val pushForce = getGravityForceForEntity(entity, intensity)
                val angle = pluginsToAngle[plugin]!!
                MCTE_miscUtils.applyForceWithSuppliedMass(
                    entity,
                    mass,
                    MathUtils.getPointOnCircumference(Vector2f(0f, 0f), 1f, angle),
                    pushForce
                )
                //entity.applyForce(angle, pushForce)
            }
        }
    }

    private fun getGravityForceForEntity(entity: CombatEntityAPI, baseIntensity: Float): Float {
        var effectMult = 1f
        var timeMult = 1f
        val engineMult: Float = engine.timeMult.modifiedValue
        if (entity is ShipAPI) {
            timeMult = entity.mutableStats.timeMult.modifiedValue
            effectMult = getEffectMultForShip(entity)
        }
        val adjustedIntensity = baseIntensity * PULSAR_BASE_FORCE
        val totalTimeMult = timeMult + engineMult-1
        val mult = if (entity is DamagingProjectileAPI) 0.3f else 1f
        return (((adjustedIntensity)*effectMult)*totalTimeMult)*mult
    }

    private fun generateFlux() {
        if (engine.isPaused) return
        val iterator = affectedShips.keys.iterator()
        while (iterator.hasNext()) {
            val ship = iterator.next()
            if (!engine.isEntityInPlay(ship)) {
                iterator.remove()
                continue
            }
            if (!ship.isTangible()) continue
            if (ship.isFighter) continue
            val maxFlux = ship.maxFlux
            if (maxFlux <= hardFluxGenerationPerFrame) continue
            val timeMult: Float = ship.mutableStats.timeMult.modifiedValue
            val engineMult: Float = engine.timeMult.modifiedValue
            val totalMult = timeMult + engineMult-1
            ship.fluxTracker.increaseFlux(((getHardFluxGenForShip(ship))*totalMult), true)
        }
    }

    private fun getHardFluxGenForShip(ship: ShipAPI): Float {
        val effectMult = getEffectMultForShip(ship)

        return hardFluxGenerationPerFrame*effectMult
    }

    private fun applyEMP() {
        if (engine.isPaused) return
        for (ship: ShipAPI in affectedShips.keys) {
            if (!shouldEMPship(ship)) continue
            val empAmount = getEMPDamage(ship)
            val damageAmount = getEnergyDamage(ship)

            val effectMult = getEffectMultForShip(ship)
            val soundId = if (!ship.isFighter) "tachyon_lance_emp_impact" else null
            var randomLocation = ship.location
            val bounds = ship.exactBounds
            if (bounds != null) {
                val randomFloat = random.nextFloat()
                val threshold = 0.3f

                if (randomFloat > threshold) {
                    bounds.update(ship.location, ship.facing)
                    val randSegment = bounds.segments.randomOrNull()
                    if (randSegment != null) {
                        val loc = randSegment.p1 ?: randSegment.p2 ?: ship.location ?: continue
                        randomLocation = loc
                    }
                }
            }

            engine.spawnEmpArc(ship, randomLocation, ship, ship,
                DamageType.ENERGY,
                damageAmount,
                empAmount,
                Float.MAX_VALUE,
                soundId,
                (20f)*effectMult,
                Color(25, 100, 155, 255),
                Color(255, 255, 255, 255)
            )
        }
    }

    private fun shouldEMPship(ship: ShipAPI): Boolean {

        val chance: Float = getEMPChanceForShip(ship)
        val randomFloat = random.nextFloat()

        return (chance > randomFloat)
    }

    private fun getEMPChanceForShip(ship: ShipAPI): Float {
        if (!engine.isEntityInPlay(ship)) return 0f
        if (!ship.isTangible()) return 0f
        val shield: ShieldAPI? = ship.shield
        if (shield != null) {
            val shieldArc = shield.activeArc
            if (shieldArc >= 360) return 0f
        }

        val timeMult: Float = ship.mutableStats.timeMult.modifiedValue
        val engineMult: Float = engine.timeMult.modifiedValue
        val totalMult = timeMult + engineMult-1

        return EMPChancePerFrame*totalMult
    }

    private fun getEnergyDamage(ship: ShipAPI): Float {
        val effectMult = getEffectMultForShip(ship)
        return energyDamage*effectMult
    }

    private fun getEMPDamage(ship: ShipAPI): Float {
        val effectMult = getEffectMultForShip(ship)
        return EMPdamage*effectMult
    }

    private fun getEffectMultForShip(ship: ShipAPI): Float {
        val coronaMult = ship.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifiedValue

        return coronaMult
    }

    override fun handleNotification(amount: Float) {
        if (engine.playerShip == null) return
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")

        val ship = engine.playerShip

        val modifiedShieldMult = getShieldMultForShip(ship)
        val empChance = getEMPChanceForShip(ship)
        val empDamage = getEMPDamage(ship)

        engine.maintainStatusForPlayerShip(
            "niko_MCPE_pulsar1",
            icon,
            "Pulsar Beam",
            "Shield efficiency, upkeep efficiency, EMP pierce resist reduced by ${((modifiedShieldMult-1)*100).roundTo(2)}%",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_pulsar3",
            icon,
            "Pulsar Beam",
            "${(getChargeForShip(ship)).roundTo(2)} EMP damage applied to all projectiles",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_pulsar4",
            icon,
            "Pulsar Beam",
            "${empChance.roundTo(2)}% per frame for ship plating to polarize and EMP self for ${empDamage.roundTo(2)} EMP damage",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_pulsar2",
            icon,
            "Pulsar Beam",
            "Generating hardflux at rate of ${calculateFluxGeneratedPerSecond(ship).roundTo(2)} per second",
            true)
        engine.maintainStatusForPlayerShip(
            "niko_MCPE_pulsar5",
            icon,
            "Pulsar Beam",
            "Solar winds interfering with battlespace",
            true)
    }

    private fun calculateFluxGeneratedPerSecond(ship: ShipAPI): Float {
        val hardFluxPerFrame = getHardFluxGenForShip(ship)
        return (hardFluxPerFrame*timesToDoThingPerSecond)
    }

    override fun handleSounds(amount: Float) {
        Global.getSoundPlayer().playUILoop("terrain_corona_am", 1f, 1f)
    }

    private fun getCachedCharge(projectile: DamagingProjectileAPI): Float {
        if (chargedProjectiles[projectile] != null) return chargedProjectiles[projectile]!!
        val weapon = projectile.weapon
        if (weapon != null) {
            if (chargedWeapons[weapon] != null) return chargedWeapons[weapon]!!
        }
        return 0f
    }

    override fun modifyDamageDealt(
        param: Any?,
        target: CombatEntityAPI?,
        damage: DamageAPI?,
        point: Vector2f?,
        shieldHit: Boolean
    ): String? {

        if (param == null || target == null) return null
        if (param !is DamagingProjectileAPI) return null
        if (target !is ShipAPI) return null
        if (shieldHit) return null

        val randomFloat = random.nextFloat()
        if (randomFloat <= 0.75f) return null

        val projectile = param
        if (!isChargedProjectile(projectile)) return null

        val bonusEMP = getCachedCharge(projectile)

        val projectileSource: ShipAPI? = projectile.source
        val sourceForArc = projectileSource ?: target

        engine.spawnEmpArc(
            sourceForArc, point, target, target, DamageType.ENERGY, 0f, bonusEMP,
            10000000000f,
            "tachyon_lance_emp_impact",
            bonusEMP/25,
            Color(25, 100, 155, 255),
            Color(255, 255, 255, 255)
        )
        return null
    }
}
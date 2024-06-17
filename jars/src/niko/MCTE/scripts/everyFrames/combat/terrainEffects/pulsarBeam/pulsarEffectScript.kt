package niko.MCTE.scripts.everyFrames.combat.terrainEffects.pulsarBeam

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Stats.CORONA_EFFECT_MULT
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.util.IntervalUtil
import niko.MCTE.combatEffectTypes
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.baseTerrainEffectScript
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.usesDeltaTime
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import niko.MCTE.utils.MCTE_miscUtils
import niko.MCTE.utils.MCTE_miscUtils.getAllObjects
import niko.MCTE.settings.MCTE_settings.PULSAR_BASE_FORCE
import niko.MCTE.settings.MCTE_settings.PULSAR_FORCE_ENABLED
import niko.MCTE.settings.MCTE_settings.PULSAR_PPT_COMPENSATION
import niko.MCTE.settings.MCTE_settings.SOLAR_SHIELDING_EFFECT_MULT
import niko.MCTE.utils.MCTE_miscUtils.replaceExistingEffect
import niko.MCTE.utils.MCTE_shipUtils.isTangible
import niko.MCTE.utils.terrainCombatEffectIds
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class pulsarEffectScript(
    val anglesToIntensity: MutableMap<Float, Float> = HashMap(),
    var hardFluxGenerationPerFrame: Float = 0f,
    var bonusEMPDamageForWeapons: Float = 0f,
    var shieldDestabilziationMult: Float = 1f,
    var EMPChancePerFrame: Float = 0f,
    var EMPdamage: Float = 0f,
    var energyDamage: Float = 0f,
): baseTerrainEffectScript(), usesDeltaTime, DamageDealtModifier {

    override var effectPrototype: combatEffectTypes? = combatEffectTypes.PULSAR

    private val timesToDoThingPerSecond = 60f
    override var deltaTime: Float = 0f
    override val thresholdForAdvancement: Float = (1/timesToDoThingPerSecond)

    private val color = Color(159, 255, 231, 255)

    private val chargedProjectiles: MutableMap<DamagingProjectileAPI, Float> = HashMap()
    private val chargedWeapons: MutableMap<DamageAPI, Float> = HashMap()
    var overallIntensity: Float = 0f

    val timer: IntervalUtil = IntervalUtil(0.15f, 0.15f)

    private val originalValues: MutableMap<ShipAPI, MutableMap<StatBonus, MutableMap<String, MCTE_miscUtils.originalTerrainValue>>> = HashMap()

    init {
        refreshOverallIntensity()
    }

    fun refreshOverallIntensity() {
        overallIntensity = 0f
        for (intensity in anglesToIntensity.values) {
            overallIntensity += intensity
        }
    }

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)

        this.engine.listenerManager.addListener(this)
        Global.getSector().addScript(pulsarEffectRemovalScript(chargedWeapons))
    }

    class pulsarEffectRemovalScript(
        val chargedWeapons: MutableMap<DamageAPI, Float>
    ): EveryFrameScript {
        var done: Boolean = false
        override fun isDone(): Boolean {
            return done
        }

        override fun runWhilePaused(): Boolean {
            return true
        }

        override fun advance(amount: Float) {
            if (Global.getCurrentState() == GameState.COMBAT) return
            for (damage in chargedWeapons.keys) {
                val fluxDamage = chargedWeapons[damage] ?: continue
                damage.fluxComponent -= fluxDamage
            }
            stop()
        }

        private fun stop() {
            Global.getSector().removeScript(this)
            done = true
        }
    }

    override fun applyEffects(amount: Float) {
        timer.advance(amount)
        if (timer.intervalElapsed()) {
            applyStats()
            applyVisuals()
        }

        if (!canAdvance(amount)) return

        applyForce()
        generateFlux()
        applyEMP()
    }

    private fun applyStats() {
        for (ship: ShipAPI in engine.ships) {

            val modifiedShieldMult = getShieldMultForShip(ship)

            val mutableStats = ship.mutableStats

            mutableStats.shieldDamageTakenMult.modifyMult(terrainCombatEffectIds.pulsarEffect, modifiedShieldMult)
            mutableStats.shieldUpkeepMult.modifyMult(terrainCombatEffectIds.pulsarEffect, modifiedShieldMult)
            mutableStats.dynamic.getStat(Stats.SHIELD_PIERCED_MULT).modifyMult(terrainCombatEffectIds.pulsarEffect, 1/modifiedShieldMult)

            replaceExistingEffect(originalValues, getPPTCompensation(ship), "pulsar_beam_stat_mod_1", "pulsar_beam_stat_mod_2", ship, mutableStats)
            chargeWeapons(ship, mutableStats)
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
        if (!ship.isTangible()) return 1f
        val effectMult = getEffectMultForShip(ship)
        return (shieldDestabilziationMult * effectMult).coerceAtLeast(1f)
    }

    private fun isChargedProjectile(projectile: DamagingProjectileAPI): Boolean {
        if (chargedProjectiles[projectile] != null) return true
        val weapon: WeaponAPI? = projectile.weapon
        if (weapon != null && chargedWeapons[weapon.damage] != null) return true
        return false
    }

    private fun chargeProjectile(projectile: DamagingProjectileAPI) {
        if (chargedProjectiles[projectile] != null) return
        val bonusDamage = getChargeForProjectile(projectile)
        val damage = projectile.damage
        damage.fluxComponent += (bonusDamage)
        chargedProjectiles[projectile] = bonusDamage
    }

    private fun getChargeForProjectile(projectile: DamagingProjectileAPI): Float {
        val weapon: WeaponAPI? = projectile.weapon
        if (weapon != null) {
            if (chargedWeapons[weapon.damage] != null) {
                return chargedWeapons[weapon.damage]!!
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
            if (chargedWeapons[weapon.damage] == null) {
                chargeWeapon(ship, weapon)
            }
        }
    }

    private fun chargeWeapon(ship: ShipAPI, weapon: WeaponAPI) {
        val empAmount: Float = getChargeForWeapon(ship, weapon)
        weapon.damage.fluxComponent += empAmount
        chargedWeapons[weapon.damage] = empAmount
    }

    private fun getChargeForWeapon(ship: ShipAPI, weapon: WeaponAPI): Float {
        val shipCharge = getChargeForShip(ship)
        return shipCharge
    }

    private fun getChargeForShip(ship: ShipAPI): Float {
        val effectMult = getEffectMultForShip(ship)
        return bonusEMPDamageForWeapons*(effectMult.coerceAtLeast(1f))
    }

    private fun getPPTCompensation(ship: ShipAPI): Float {
        //val coronaEffect = (ship.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifiedValue).coerceAtLeast(1f)
        return (PULSAR_PPT_COMPENSATION/(overallIntensity+1))///coronaEffect)
    }

    private fun applyVisuals() {
       return
    }

    private fun applyForce() {
        if (engine.isPaused) return
        if (!PULSAR_FORCE_ENABLED) return
        for (entry in anglesToIntensity.entries) {
            val angle = entry.key
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
        val totalTimeMult = engineMult
        val mult = if (entity is DamagingProjectileAPI) 0.3f else 1f
        return (((adjustedIntensity)*effectMult)*totalTimeMult)*mult
    }

    private fun generateFlux() {
        if (engine.isPaused) return
        for (ship in engine.ships) {
            if (!ship.isTangible()) continue
            if (ship.isFighter) continue
            val maxFlux = ship.maxFlux
            if (maxFlux <= hardFluxGenerationPerFrame) continue
            ship.fluxTracker.increaseFlux(((getHardFluxGenForShip(ship))), true)
        }
    }

    private fun getHardFluxGenForShip(ship: ShipAPI): Float {
        val effectMult = getEffectMultForShip(ship)
        val engineMult: Float = engine.timeMult.modifiedValue

        return (hardFluxGenerationPerFrame*effectMult)*engineMult
    }

    private fun applyEMP() {
        if (engine.isPaused) return
        for (ship: ShipAPI in engine.ships) {
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
        /*val shield: ShieldAPI? = ship.shield
        if (shield != null) {
            val shieldArc = shield.activeArc
            if (shieldArc >= 360) return 0f
        }*/

        //val timeMult: Float = ship.mutableStats.timeMult.modifiedValue
        val engineMult: Float = engine.timeMult.modifiedValue
        val totalMult = engineMult

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

        val baseEffect = (ship.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).baseValue)
        val currentEffect = (ship.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifiedValue)
        val solarShieldingEffect = (baseEffect - currentEffect) * SOLAR_SHIELDING_EFFECT_MULT
        val adjustedEffect = (baseEffect - solarShieldingEffect)

        return adjustedEffect
    }

    override fun handleNotification(amount: Float): Boolean {
        if (!super.handleNotification(amount)) return false
        if (engine.playerShip == null) return false
        val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty")

        val ship = engine.playerShip

        val modifiedShieldMult = getShieldMultForShip(ship)
        val empChance = getEMPChanceForShip(ship)
        val empActualDamage = getEnergyDamage(ship)
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
        var randomDamageData = ""
        if (empActualDamage > 0f) {
            randomDamageData = "and ${empActualDamage.roundTo(2)} energy damage"
        }
        val randomEMPData = "${empChance.roundTo(2)}% per frame for ship plating to polarize and EMP self for ${empDamage.roundTo(2)} EMP damage $randomDamageData"
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
        return true
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
            if (chargedWeapons[weapon.damage] != null) return chargedWeapons[weapon.damage]!!
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
package niko.MCTE.codex

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.StarTypes
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.RingSystemTerrainPlugin
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.impl.codex.CodexDataV2
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Pair
import data.scripts.campaign.terrain.niko_MPC_mesonField.Companion.NORMAL_DETECTED_MULT
import data.scripts.campaign.terrain.niko_MPC_mesonField.Companion.STORM_DETECTED_MULT
import data.scripts.campaign.terrain.niko_MPC_mesonField.Companion.STORM_SENSOR_MULT
import indevo.exploration.minefields.MineBeltTerrainPlugin
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.MCTE_debugUtils
import niko.MCTE.utils.MCTE_mathUtils.trimHangingZero
import niko.MCTE.utils.MCTE_stringUtils
import java.awt.Color
import kotlin.math.max

enum class TerrainEntry(
    val title: String,
    val iconName: String
) {
    CORONA("Corona", "graphics/icons/terrain/corona.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara(
                "A halo of high temperature plasma which surrounds a star; most ships are not built to withstand prolonged exposure to the extreme radiation bombardment from the corona and will take damage if they remain within it.",
                0f
            )
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara("Reduces the combat readiness of all ships in the corona at a steady pace.", 0f)
            info.addPara("The corona's heavy winds also blow fleets away from it's source.", 5f)
            info.addPara("Occasional solar flare activity takes these effects to even more dangerous levels.", 5f)
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("Reduces the peak performance time of ships and increases the rate of combat readiness degradation in protracted engagements.", 0f)
        }
        override fun hasTacticalDesc(): Boolean = true
        override fun createTacticalDesc(info: TooltipMakerAPI) {
            info.addPara("%s is effective at resisting both the campaign and combat effects of this terrain.", 0f, Misc.getHighlightColor(), "Solar Shielding").color = Misc.getGrayColor()
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = hashSetOf(CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.SOLAR_SHIELDING)))
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS)
    },
    MAGFIELD("Magnetic Field", "graphics/icons/terrain/magnetic_storm.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("A strong magnetic field which traps high energy charged particles. It's extremely difficult to get any sensor readings on a fleet lurking within.", 0f)
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara("Reduces detected-at range of fleets within by %s.", 0f, Misc.getHighlightColor(), "50%")
            info.addPara("Occasionally, massive storms wrack the field, creating large spikes of magnetic activity.", 5f)
            info.addPara("Fleets within these storms have their sensor strength reduced by %s, but are rendered %s to other fleets (save for %s).", 5f, Misc.getHighlightColor(), "90%", "nearly invisible", "visual contact").setHighlightColors(Misc.getNegativeHighlightColor(), Misc.getPositiveHighlightColor(), Misc.getHighlightColor())
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("Reduces weapon, vision, and fighter range.", 0f)
            info.addPara("Guided missiles have a chance to have their guidance scrambled.", 5f)
            val label = info.addPara("%s have significantly heightened effects, making most guided munitions %s.", 5f, Misc.getHighlightColor(), "Magnetic storms", "useless")
            label.color = Misc.getGrayColor()
            label.setHighlightColors(Misc.getHighlightColor(), Misc.getNegativeHighlightColor())
        }
        override fun hasTacticalDesc(): Boolean = true
        override fun createTacticalDesc(info: TooltipMakerAPI) {
            info.addPara("An %s counters the in-combat effect of this terrain.", 0f, Misc.getHighlightColor(), "ECCM package").color = Misc.getGrayColor()
            info.addPara("In combat, this terrain typically favors %s, %s and %s while making %s significantly less effective.", 5f, Misc.getHighlightColor(), "fast knife-fighters", "carrier groups", "rockets", "guided missiles").setHighlightColors(
                Misc.getPositiveHighlightColor(),
                Misc.getPositiveHighlightColor(),
                Misc.getPositiveHighlightColor(),
                Misc.getNegativeHighlightColor()
            )
            info.setBulletedListMode(BaseIntelPlugin.BULLET)
            info.addPara("However, with a %s ECCM package, %s become %s (if not more).", 0f, Misc.getHighlightColor(), "s-modded", "guided munitions", "normally effective").setHighlightColors(
                Misc.getStoryOptionColor(),
                Misc.getHighlightColor(),
                Misc.getPositiveHighlightColor()
            )
            info.setBulletedListMode(null)
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = hashSetOf(CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.ECCM)))
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS)
    },
    NEBULA("Nebula", "graphics/icons/terrain/nebula.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("Interstellar clouds of hydrogen, helium, and dust that cause significant sensor noise that may aid a fleet hiding within. The (relatively) thick medium also interferes with drive field operation.", 0f)
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara(
                "Reduces the range at which fleets inside can be detected by %s.", 0f,
                Misc.getHighlightColor(),
                "" + ((1f - NebulaTerrainPlugin.VISIBLITY_MULT) * 100).toInt() + "%"
            )
            info.addPara(
                "Reduces the travel speed of fleets inside by up to %s. Larger fleets are slowed down more.",
                5f,
                Misc.getHighlightColor(),
                "" + ((Misc.BURN_PENALTY_MULT) * 100f).toInt() + "%"
            )

            val playerFleet = Global.getSector()?.playerFleet ?: return
            val penalty = Misc.getBurnMultForTerrain(playerFleet)
            info.addPara(
                "Your fleet's speed is reduced by %s.", 5f,
                Misc.getHighlightColor(),
                "" + Math.round((1f - penalty) * 100) + "%" //Strings.X + penaltyStr
            )
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("Nebula, and nebula-like structures (such as hyperclouds) create nebulae in-combat.", 0f)
            info.addPara(
                "Ships within a nebula will suffer %s less weapon & vision range, as well as a flat speed reduction of %s.",
                5f,
                Misc.getNegativeHighlightColor(),
                MCTE_stringUtils.toPercent(1 - MCTE_settings.NEBULA_RANGE_MULT),
                "${(-MCTE_settings.NEBULA_SPEED_DECREMENT).trimHangingZero()} SU"
            )
        }
        override fun hasTacticalDesc(): Boolean = MCTE_settings.EXTRA_NEBULA_EFFECTS_ENABLED
        override fun createTacticalDesc(info: TooltipMakerAPI) {
            if (MCTE_settings.EXTRA_NEBULA_EFFECTS_ENABLED) {
                info.addPara(
                    "A %s prevents the speed loss from nebula clouds.",
                    0f,
                    Misc.getHighlightColor(),
                    "insulated engine assembly"
                ).color = Misc.getGrayColor()
                info.addPara(
                    "It's best to set up defensive lines behind nebulae, to lure enemies into them and obtain %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "range superiority"
                )
            }
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> {
            if (MCTE_settings.EXTRA_NEBULA_EFFECTS_ENABLED) return hashSetOf(CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.INSULATEDENGINE)))
            return HashSet()
        }
        override fun getTags(): MutableSet<String> {
            val tags = mutableSetOf(HAS_CAMPAIGN_EFFECTS)
            if (MCTE_settings.EXTRA_NEBULA_EFFECTS_ENABLED) tags += HAS_COMBAT_EFFECTS
            return tags
        }
    },
    HYPERSTORM("Hyperspace Storm", "graphics/icons/terrain/hyperspace_storm.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("A volume of hyperspace undergoing chaotic phase shifts leading to wildly fluctuating resonance cascades. Ancient and dire warnings of prolonged exposure to so-called \"hyperspace storms\" have been passed down through generations of spacers.", 0f)
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            val highlight = Misc.getHighlightColor()
            val nextPad = 5f
            val pad = 5f
            info.addPara(
                "Reduces the range at which fleets inside can be detected by %s.",
                0f,
                highlight,
                "" + Math.round((1f - HyperspaceTerrainPlugin.VISIBLITY_MULT) * 100f) + "%"
            )

            info.addPara(
                "Reduces the speed of fleets inside by up to %s. Larger fleets are slowed down more.",
                nextPad,
                highlight,
                "" + Math.round((Misc.BURN_PENALTY_MULT) * 100f) + "%"
            )

            val playerFleet = Global.getSector()?.playerFleet
            if (playerFleet != null) {
                val penalty = Misc.getBurnMultForTerrain(playerFleet)
                info.addPara(
                    "Your fleet's speed is reduced by %s.", pad,
                    highlight,
                    "" + Math.round((1f - penalty) * 100f) + "%" //Strings.X + penaltyStr
                )
            }

            info.addPara(
                "Being caught in a storm causes storm strikes to damage ships " +
                        "and reduce their combat readiness. " +
                        "Larger fleets attract more damaging strikes.", 10f
            )

            info.addPara(
                "In addition, storm strikes toss the fleet's drive bubble about " +
                        "with great violence, often causing a loss of control. " +
                        "Some commanders are known to use these to gain additional " +
                        "speed, and to save fuel - a practice known as \"storm riding\".", Misc.getTextColor(), pad
            )

            info.addPara("\"Slow-moving\" fleets do not attract storm strikes.", Misc.getTextColor(), pad)
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("Clouds of deep hyperspace will periodically strike nearby ships/missiles with lightning, dealing %s and %s damage.", 0f, Misc.getNegativeHighlightColor(), "massive EMP", "slight energy")
            info.addPara("%s and %s targets are prioritized first.", 5f, Misc.getHighlightColor(), "Fast-moving", "massive")
            info.setBulletedListMode(BaseIntelPlugin.BULLET)
            info.addPara("\"Slow-moving\" ships and missiles (below %s) will not be targeted.", 0f, Misc.getHighlightColor(), "${MCTE_settings.HYPERSTORM_SPEED_THRESHOLD.trimHangingZero()}su")
            info.setBulletedListMode(null)
        }
        override fun createGroundBattleDesc(info: TooltipMakerAPI) {
            info.addPara("Has a small chance to %s, %s all troops inside and dealing %s.", 0f, Misc.getNegativeHighlightColor(), "strike an industry with lightning", "disorganizing", "significant damage")
        }
        override fun hasTacticalDesc(): Boolean = true
        override fun createTacticalDesc(info: TooltipMakerAPI) {
            info.addPara("%s is effective at resisting both the campaign and combat effects of this terrain.", 0f, Misc.getHighlightColor(), "Solar Shielding").color = Misc.getGrayColor()
            info.addPara("Defending ones ship against a lightning strike is part of the standard spaceflight curriculum - allies and enemies are quite adept at blocking incoming lightning.", 5f).color = Misc.getGrayColor()

            info.addPara("While strikes against fighters and missiles are instant, larger ships are warned of an impending attack by a series of %s. It is wise to %s of that area before the strike finalizes.", 5f, Misc.getHighlightColor(), "smaller lightning bolts from the strike location", "ensure total shield coverage")
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = hashSetOf(CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.SOLAR_SHIELDING)), CodexDataV2.getEntry(NEBULA.name))
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS, HAS_GROUND_BATTLE_EFFECTS)
    },
    PULSAR("Pulsar Stream", "graphics/augments/pulsar_augment_icon.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("A relativistic, magnetized jet combined with intense radiation. Commonly emitted by neutron stars.", 0f)
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara("Reduces the combat readiness of all ships caught in the pulsar beam at a rapid pace, and blows the fleet off-course.", 0f)
            info.addPara("The magnitude of the effect drops off rapidly with distance from the source.", 5f)
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara(
                "Reduces the peak performance time of ships and increases the rate of combat readiness degradation in protracted engagements.",
                0f
            )
            info.addPara("Solar winds blow ships and projectiles away from the pulsar.", 5f)
            info.addPara("Reduces shield efficiency, increases shield upkeep, and increases chance for piercing weapons such as the ion beam to pierce shields.", 5f)
            info.addPara("Generates hardflux on all ships at a rate of %s per second.", 5f, Misc.getHighlightColor(), "${(MCTE_settings.PULSAR_HARDFLUX_GEN_INCREMENT * 60f).trimHangingZero()}")
            info.addPara("Additionally, periodically EMPs ships and fighters caught within the beam.", 5f)
            info.setBulletedListMode(BaseIntelPlugin.BULLET)
            info.addPara("Your chance to resist an EMP is equal to %s", 0f, Misc.getHighlightColor(), "your shield arc divided by 360")
            info.setBulletedListMode(null)
            info.addPara("Finally, all projectiles receive a flat bonus of %s EMP damage.", 5f, Misc.getHighlightColor(), "${MCTE_settings.PULSAR_EMP_DAMAGE_BONUS_FOR_WEAPONS_INCREMENT.trimHangingZero()}")
        }
        override fun hasTacticalDesc(): Boolean = true
        override fun createTacticalDesc(info: TooltipMakerAPI) {
            info.addPara("%s is effective at resisting both the campaign and combat effects of this terrain.", 0f, Misc.getHighlightColor(), "Solar Shielding").color = Misc.getGrayColor()
            info.setBulletedListMode(BaseIntelPlugin.BULLET)
            info.addPara("%s does not, however, reduce the damage bonus on fired projectiles, making the hullmod highly desirable in pulsars.", 0f, Misc.getHighlightColor(), "Solar Shielding").color = Misc.getGrayColor()
            info.setBulletedListMode(null)
            info.addPara("The extremely hostile environment of a pulsar makes any battles inside %s. They should be avoided %s.", 5f, Misc.getHighlightColor(), "extremely costly and unpredictable", "if possible").setHighlightColors(
                Misc.getNegativeHighlightColor(),
                Misc.getHighlightColor()
            )
            info.addPara("The projectile-charging effect of the beam heavily favors %s weapons.", 5f, Misc.getPositiveHighlightColor(), "rapid-fire")
            info.addPara("Low-mass and slow missiles (such as the pilum) become nigh-useless due to the solar winds.", 5f)
            info.addPara("EMP resistance is an absolute must, as shields are weakened to the point they cannot be completely relied upon", 5f)
            info.addPara("%s is highly recommended.", 5f, Misc.getHighlightColor(), "Solar shielding")
        }

        override fun createGroundBattleDesc(info: TooltipMakerAPI) {
            info.addPara("%s all troops, preventing %s and reducing %s.", 0f, Misc.getHighlightColor(), "Disorganizes", "movement", "outgoing damage")
            info.addPara("Additionally deals %s to all troops.", 0f, Misc.getHighlightColor(), "low, persistent damage")
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = hashSetOf(
            CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.SOLAR_SHIELDING)),
            CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.FLUXBREAKERS)),
            CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.HARDENED_SHIELDS)),
            CodexDataV2.getEntry(CodexDataV2.getPlanetEntryId(StarTypes.NEUTRON_STAR))
        )
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS, HAS_GROUND_BATTLE_EFFECTS)
    },
    SLIPSTREAM("Slipstream", "graphics/icons/campaign/slipstream_fuel.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("A flow in hyperspace, akin to currents navigated by ancient seafarers back on Old Earth.", 0f)
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara(
                "Most slipstreams are temporary, and in recent memory their ebb and flow has been "
                        + "unusually synchronized with the standard Domain cycle.", 0f
            )

            info.addPara(
                "Fleets traveling inside a slipstream use %s less fuel for the distance covered.",
                5f, Misc.getHighlightColor(),
                "" + Math.round((1f - SlipstreamTerrainPlugin2.FUEL_USE_MULT) * 100f) + "%"
            )

            info.addPara(
                ("In addition, traveling at burn levels above %s is even more fuel-efficient. "
                        + "For example, a fleet traveling at burn %s will consume half as much fuel "
                        + "for the distance it covers."),
                5f,
                Misc.getHighlightColor(), "20", "40", "half"
            )

            info.addPara("These fuel use reductions are not reflected by the fuel range indicator on the map.", 5f)
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("Forces %s onto all %s, even if they can't normally have it.", 0f, Misc.getHighlightColor(), "safety overrides", "ships, fighters, and missiles").setHighlightColors(
                Misc.getNegativeHighlightColor(),
                Misc.getHighlightColor()
            )
            info.setBulletedListMode(BaseIntelPlugin.BULLET)
            info.addPara("For %s, manifests as a massive speed and maneuverability buff", 0f, Misc.getHighlightColor(), "missiles")
            info.setBulletedListMode(null)

            info.addPara("If the ship/fighter already has safety overrides, %s the zero-flux boost and vent rate while %s peak performance time.", 5f, Misc.getHighlightColor(), "massively increases", "massively decreasing").setHighlightColors(
                Misc.getPositiveHighlightColor(),
                Misc.getNegativeHighlightColor()
            )
        }
        override fun hasTacticalDesc(): Boolean = true
        override fun createTacticalDesc(info: TooltipMakerAPI) {
            info.addPara("This terrain heavily favors %s, %s, and %s.", 0f, Misc.getPositiveHighlightColor(), "knife-fighters", "carrier groups", "missiles")
            info.setBulletedListMode(BaseIntelPlugin.BULLET)
            info.addPara("Ships with %s are great candidates due to not losing any more range, but be careful of their horrible %s", 0f, Misc.getHighlightColor(), "safety overrides", "PPT").setHighlightColors(
                Misc.getHighlightColor(),
                Misc.getNegativeHighlightColor()
            )
            info.addPara("Flux-limited fighters (such as the %s) are disproportionately improved by this terrain", 0f, Misc.getHighlightColor(), "broadsword")
            info.setBulletedListMode(null)

            info.addPara("%s can be given safety overrides by this terrain, to great effect.", 5f, Misc.getHighlightColor(), "Capital ships")
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = hashSetOf(
            CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.SAFETYOVERRIDES)),
            CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.HARDENED_SUBSYSTEMS))
        )
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS)
    },
    EVENT_HORIZON("Event Horizon", "graphics/augments/black_hole_augment_icon.png") {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("The gravitational flux near the event horizon of a black hole is extreme.", 0f)
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            info.addPara(
                "Reduces the combat readiness of " +
                        "all ships near the event horizon at a steady pace.", 0f
            )
            info.addPara(
                "The drive field is also disrupted, making getting away from the event horizon more difficult.",
                5f
            )
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara(
                "Reduces the peak performance time of ships and increases the rate of combat readiness degradation in protracted engagements.",
                0f
            )
            info.addPara("The extreme gravity of the black hole %s, massively increasing timeflow for all ships and fighters.", 5f, Misc.getHighlightColor(), "disrupts relativity")
            info.addPara("Additionally, the gravity pulls ships and projectiles towards the black hole, disrupting travel and firing trajectories.", 5f)
        }
        override fun hasTacticalDesc(): Boolean = true
        override fun createTacticalDesc(info: TooltipMakerAPI) {
            val label = info.addPara("While %s diminishes the PPT drain, it actually %s, making the hullmod a double-edged sword.", 0f, Misc.getHighlightColor(), "solar shielding", "negates the timeflow increase")
            label.color = Misc.getGrayColor()
            label.setHighlightColors(
                Misc.getHighlightColor(),
                Misc.getNegativeHighlightColor()
            )

            info.addPara("This terrain heavily favors %s and %s - the reduced projectile speed making projectile-based stand-off tactics %s.", 5f, Misc.getPositiveHighlightColor(), "knife-fighters", "beams", "unviable").setHighlightColors(
                Misc.getPositiveHighlightColor(),
                Misc.getPositiveHighlightColor(),
                Misc.getNegativeHighlightColor()
            )
            info.addPara("Low-mass and slow missiles (such as the pilum) become nigh-usless due to the gravitic forces.", 5f)
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = hashSetOf(
            CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(HullMods.SOLAR_SHIELDING)),
            CodexDataV2.getEntry(CodexDataV2.getPlanetEntryId(StarTypes.BLACK_HOLE))
        )
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS)
    },

    // modded
    MESON_FIELD("Meson Field", "graphics/MCTE_mesonField.png") {
        override fun isAvailableInCodex(): Boolean {
            return MCTE_debugUtils.MPCenabled
        }

        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("This data-pak is from W.WL.B 231! Tri-Tachyon thanks you for your patronage.", 0f).color = Misc.getGrayColor()

            info.addPara("A thick field of meson particles. Due to their signature enhancing nature, it's very easy to get a clear sensor reading on a fleet passing through. The effect is inverted, however, once meson concentration surpasses the Burivil threshold, and instead fleets lurking within find their sensors massively amplified and their own signature hidden.", 5f)
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            val highlight = Misc.getHighlightColor()

            info.addPara(
                "Increases the range at which fleets inside can be detected by %s.",
                0f,
                highlight,
                "${(NORMAL_DETECTED_MULT * 100).toInt()}%"
            )

            info.addPara("Meson fields are volatile and tend to \"storm\", creating massive spikes of mesons that are enough to surpass the Burivil threshold.", 5f)
            info.addPara(
                "Areas thick enough to pass the aformentioned threshold boost sensor range of " +
                        "fleets in it by %s, and decrease detected-at range of fleets inside by %s.",
                5f,
                highlight,
                "${(STORM_SENSOR_MULT * 100).toInt()}%", "${(STORM_DETECTED_MULT * 100).toInt()}%"
            )
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("Normal meson fields increase weapon and vision range by %s and %s, respectively.", 0f, Misc.getHighlightColor(),
                "${MCTE_settings.MESON_FIELD_WEAPON_RANGE_INCREMENT.trimHangingZero()}",
                MCTE_stringUtils.toPercent((MCTE_settings.MESON_FIELD_VISION_MULT))
            )
            info.addPara("%s massively amplify the above affects, and additionally increase fighter engagement range and system range by %s and %s, respectively.",
                5f,
                Misc.getHighlightColor(),
                "Meson storms",
                "${MCTE_settings.MESON_STORM_WING_RANGE_INCREMENT.trimHangingZero()}",
                MCTE_stringUtils.toPercent((MCTE_settings.MESON_STORM_SYSTEM_RANGE_MULT))
            )
        }
        override fun hasTacticalDesc(): Boolean = true
        override fun createTacticalDesc(info: TooltipMakerAPI) {
            info.addPara("While both versions of this terrain benefit %s weapons, the %s does so in a far greater intensity, as well as vastly improving the efficacy of %s and %s.",
                0f,
                Misc.getPositiveHighlightColor(),
                "short-ranged",
                "meson storm",
                "fighter wings",
                "short-ranged systems"
            )
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = HashSet()
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS)
    },
    MINEFIELD("Minefield", "graphics/icons/markets/stealth_mines.png") {
        override fun isAvailableInCodex(): Boolean {
            return MCTE_debugUtils.indEvoEnabled
        }

        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara("This data-pak is from Ind.Evo 231! Tri-Tachyon thanks you for your patronage.", 0f).color = Misc.getGrayColor()

            info.addPara("A semi-autonomous, self replicating minefield. Spent mines get replaced automatically, allowing the field to persist hundreds of years without loss of efficiency.", 5f)
        }
        override fun createCampaignDesc(info: TooltipMakerAPI) {
            val highlight = Misc.getHighlightColor()

            info.addPara(
                "Extremely dangerous - If hostile to the controlling entity or running without a transponder, " +
                        "there is a high chance of mine explosions " +
                        "that knock the fleet off course and deal high damage to multiple ships.", 0f
            )

            info.addPara(
                "Smaller and and slow-moving fleets have a better chance to make it through unscathed.", 5f,
                highlight,
                "Smaller", "slow-moving"
            )

            val fleet = Global.getSector().playerFleet
            if (fleet != null) {
                val fleetCompBaseHitChance = max(
                    (MineBeltTerrainPlugin.getBaseFleetHitChance(fleet) - MineBeltTerrainPlugin.MAX_FLEET_SIZE_BEFORE_MALUS).toDouble(),
                    0.0
                ).toFloat()
                val chanceColourMap: MutableMap<Float, Pair<String, Color>> = LinkedHashMap()
                chanceColourMap[0.7f] = Pair("high", Color.ORANGE)
                chanceColourMap[0.4f] = Pair("medium", Color.YELLOW)
                chanceColourMap[0.1f] = Pair("low", Color.GREEN)

                var p = Pair("extreme", Color.RED)
                for ((key, value) in chanceColourMap) {
                    if (fleetCompBaseHitChance < key) p = value
                }

                info.addPara(
                    "Risk due to your fleet size: %s", 5f,
                    p.two,
                    p.one
                )
            }

            info.addPara(
                "Reduces the range at which stationary or slow-moving* fleets inside it can be detected by %s.",
                5f,
                highlight,
                "" + ((1f - RingSystemTerrainPlugin.getVisibilityMult(Global.getSector().playerFleet)) * 100).toInt() + "%"
            )

            info.addPara("Can be %s to locally disable a number of mines.", 5f, Misc.getHighlightColor(), "interdicted")
        }
        override fun createCombatDesc(info: TooltipMakerAPI) {
            info.addPara("Ships unfriendly to the minefield's owner will have stealth mines %s near them.", 0f, Misc.getNegativeHighlightColor(), "uncloak and detonate")
            info.setBulletedListMode(BaseIntelPlugin.BULLET)
            info.addPara("Unclaimed minefields will spawn mines on %s", 0f, Misc.getNegativeHighlightColor(), "both sides")
            info.setBulletedListMode(null)
        }
        override fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = HashSet()
        override fun getTags(): MutableSet<String> = mutableSetOf(HAS_CAMPAIGN_EFFECTS, HAS_COMBAT_EFFECTS)
    };

    abstract fun createDesc(info: TooltipMakerAPI)
    open fun hasTacticalDesc(): Boolean = false
    open fun createTacticalDesc(info: TooltipMakerAPI) {}
    open fun createCampaignDesc(info: TooltipMakerAPI) {}
    open fun createCombatDesc(info: TooltipMakerAPI) {}
    open fun createGroundBattleDesc(info: TooltipMakerAPI) {}

    open fun getRelatedEntries(): MutableSet<CodexEntryPlugin> = HashSet()

    open fun isAvailableInCodex(): Boolean = true
    open fun getTags(): MutableSet<String> = HashSet()

    companion object {
        const val HAS_CAMPAIGN_EFFECTS = "Campaign"
        const val HAS_COMBAT_EFFECTS = "Combat"
        const val HAS_GROUND_BATTLE_EFFECTS = "Ground Battle"
    }
}
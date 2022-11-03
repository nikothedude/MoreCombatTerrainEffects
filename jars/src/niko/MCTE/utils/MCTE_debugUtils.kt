package niko.MCTE.utils

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.lwjgl.Sys
import java.awt.Color

object MCTE_debugUtils {
    val systemName = System.getProperty("os.name").lowercase()
    val log: Logger = Global.getLogger(MCTE_debugUtils::class.java)

    init {
        log.level = Level.ALL
    }

    /**
     * Displays the given errorCode in whatever game layer the game is currently in. Also logs it, as well as prints
     * the stack trace in log.
     * @param errorCode The errorCode to display and log.
     * @param highPriority If true, a very loud and visible message will be displayed to the user.
     * @param crash If true, crashes the game.
     * @throws RuntimeException
     */
    @Throws(RuntimeException::class)
    @JvmStatic
    @JvmOverloads
    fun displayError(errorCode: String = "Unimplemented errorcode", highPriority: Boolean = false, crash: Boolean = false,
                     logType: Level = Level.ERROR) {

        if (MCTE_settings.SHOW_ERRORS_IN_GAME) {
            when (val gameState = Global.getCurrentState()) {
                GameState.CAMPAIGN -> displayErrorToCampaign(errorCode, highPriority)
                GameState.COMBAT -> displayErrorToCombat(errorCode, highPriority)
                GameState.TITLE -> displayErrorToTitle(errorCode, highPriority)
                else -> log.warn("Non-standard gamestate value during displayError, gamestate: $gameState")
            }
        }
        log.log(logType, "Error code:", java.lang.RuntimeException(errorCode))
        if (crash) {
            throw RuntimeException(
                "A critical error has occurred in More Planetary Conditions, and for one reason" +
                        " or another, the mod author has decided that this error is severe enough to warrant a crash." +
                        " Error code: " + errorCode
            )
        }
    }

    @JvmStatic
    private fun displayErrorToCampaign(errorCode: String, highPriority: Boolean) {
        val campaignUI = Global.getSector().campaignUI
        campaignUI.addMessage("###### MORE PLANETARY CONDITIONS ERROR ######")
        campaignUI.addMessage(errorCode)
        if (highPriority) {
            Global.getSoundPlayer().playUISound("cr_playership_critical", 1f, 1f)
            campaignUI.addMessage(
                "The above error has been deemed high priority by the mod author. It is likely" +
                        " that it's occurrence will interfere with your gameplay, and/or it is considered to be a \"critical\" error,"
                        + " one that interferes with crucial functionality. You may want to save your game and come to the mod author.",
                Color.RED
            )
        }
        campaignUI.addMessage("Please provide the mod author of more planetary conditions a copy of your logs. These messages can be disabled in the niko_MPC_settings.json file in the MPC mod folder.")
    }

    @JvmStatic
    private fun displayErrorToCombat(errorCode: String, highPriority: Boolean) {
        val engine = Global.getCombatEngine()
        val combatUI = engine.combatUI
        combatUI.addMessage(
            1,
            "Please provide the mod author of more planetary conditions with a copy of your logs. These messages can be disabled in the niko_MPC_settings.json file in the MPC mod folder."
        )
        if (highPriority) {
            Global.getSoundPlayer().playUISound("cr_playership_critical", 1f, 1f)
            combatUI.addMessage(
                1, "The above error has been deemed high priority by the mod author. It is likely" +
                        " that it's occurrence will interfere with your gameplay, and/or it is considered to be a \"critical\" error,"
                        + " one that interferes with crucial functionality. You may want to save your game and come to the mod author.",
                Color.RED
            )
        }
        combatUI.addMessage(1, errorCode)
        combatUI.addMessage(1, "###### MORE PLANETARY CONDITIONS ERROR ######")
    }

    @JvmStatic
    private fun displayErrorToTitle(errorCode: String, highPriority: Boolean) {
        return
    }

    @JvmStatic
    fun isDebugMode(): Boolean {
        return (Global.getSettings().isDevMode)
    }

    @JvmStatic
    inline fun <reified T> memKeyHasIncorrectType(hasMemory: HasMemory, key: String): Boolean {
        return memKeyHasIncorrectType<T>(hasMemory.memoryWithoutUpdate, key)
    }

    @JvmStatic
    inline fun <reified T> memKeyHasIncorrectType(memory: MemoryAPI?, key: String): Boolean {
        if (memory == null) return false
        val cachedValue = memory[key]
        if (cachedValue !is T) {
            if (cachedValue != null) displayError(
                "Non-null invalid value in $this memory, key: $key." +
                        "Expected value: ${T::class.simpleName} Value: $cachedValue", true)
            return true
        }
        return false
    }

    fun isMacOS(): Boolean {
        return (systemName == "mac")
    }
}

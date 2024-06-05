package niko.MCTE.console.commands

import niko.MCTE.settings.MCTE_settings
import niko.MCTE.settings.MCTE_settings.loadAllSettings
import org.apache.log4j.Level
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.BaseCommand.CommandResult
import org.lazywizard.console.Console

class MCTE_reloadSettings : BaseCommand {

    override fun runCommand(args: String, context: CommandContext): CommandResult {
        try {
            loadAllSettings()
        } catch (ex: Exception) {
            val errorCode = "runCommand failed due to thrown exception: $ex, ${ex.cause}"
            Console.showMessage(errorCode, Level.ERROR)
            return CommandResult.ERROR
        }
        Console.showMessage("Success! Settings have been reloaded.")
        return CommandResult.SUCCESS
    }
}
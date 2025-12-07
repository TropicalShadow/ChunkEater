package club.tesseract.sustain.commands;

import club.tesseract.sustain.Sustain;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;


/**
 * An example command.
 */
@CommandAlias("control")
public class ControlCommand extends BaseCommand {

    @Dependency
    private Sustain plugin;


    @Subcommand("start")
    void startCommand(CommandIssuer issuer) {
        plugin.getContext().resume();
        issuer.sendMessage("Game has resumed");
    }
    @Subcommand("stop")
    void stopCommand(CommandIssuer issuer) {
        plugin.getContext().pause();
        issuer.sendMessage("Game has paused");
    }
    @Subcommand("reset")
    void resetCommand(CommandIssuer issuer) {
        plugin.getContext().resetPoints(true);
        plugin.getContext().resetPoints(false);
        issuer.sendMessage("Game has resumed");
    }

}

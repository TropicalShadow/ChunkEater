package club.tesseract.sustain.commands;

import club.tesseract.sustain.Sustain;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;

/**
 * An example command.
 */
@CommandAlias("debug")
public class DebugCommand extends BaseCommand {

    @Dependency
    private Sustain plugin;


    @Subcommand("default")
    @Default
    public void onDefault(CommandIssuer sender) {
        sender.sendMessage("Hello, Minecraft!");
    }

}

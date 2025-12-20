package club.tesseract.sustain.commands;

import club.tesseract.sustain.Sustain;
import club.tesseract.sustain.SustainContext;
import club.tesseract.sustain.TeamManager;
import club.tesseract.sustain.events.GamePauseEvent;
import club.tesseract.sustain.events.GameStartEvent;
import club.tesseract.sustain.points.PointActor;
import club.tesseract.sustain.util.MessageFx;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static co.aikar.commands.ACFBukkitUtil.sendMsg;


/**
 * An example command.
 */
@CommandAlias("control")
public class ControlCommand extends BaseCommand {

    @Dependency
    private Sustain plugin;


    @CommandPermission("sustain.admin")
    @Subcommand("start")
    void startCommand(CommandIssuer issuer) {
        boolean isDirty = plugin.getContext().isDirty();
        plugin.getContext().resume();
        Bukkit.getServer().getPluginManager().callEvent(
                new GameStartEvent(isDirty)
        );
        MessageFx.issuerInfo(issuer, "Game has resumed");
    }
    @CommandPermission("sustain.admin")
    @Subcommand("stop")
    void stopCommand(CommandIssuer issuer) {
        plugin.getContext().pause();
        Bukkit.getServer().getPluginManager().callEvent(
                new GamePauseEvent()
        );
        MessageFx.issuerInfo(issuer, "Game has paused");
    }

    @CommandPermission("sustain.admin")
    @Subcommand("reset")
    void resetCommand(CommandIssuer issuer) {
        if(plugin.getContext().isDirty()){
            plugin.getContext().forceEndGame();
            MessageFx.issuerInfo(issuer, "Game has been reset.");
            return;
        }
        MessageFx.issuerInfo(issuer, "Game is not dirty, no need to reset.");
    }

    @CommandPermission("sustain.admin")
    @Subcommand("tickrate")
    @Syntax("<ticks>")
    void tickRateCommand(CommandIssuer issuer, int ticks) {
        plugin.getContext().setTickRate(ticks);
        MessageFx.issuerInfo(issuer, "Tick rate set to " + ticks + " seconds.");
    }

    @CommandPermission("sustain.admin")
    @Subcommand("addtime")
    @Syntax("<team> <time>")
    void addTimeCommand(CommandIssuer issuer, TeamManager.Team team, long time) {
        PointActor actor;
        if (issuer.getIssuer() instanceof Player p) {
            actor = PointActor.player(p);
        } else {
            actor = PointActor.console();
        }
        plugin.getContext().increasePoint(time, team, actor, "ADMIN_ADD");
        MessageFx.issuerInfo(issuer, "Added " + time + " seconds to " + team + " team.");
    }

    @CommandPermission("sustain.admin")
    @Subcommand("removetime")
    @Syntax("<team> <time>")
    void removeTimeCommand(CommandIssuer issuer, TeamManager.Team team, long time) {
        PointActor actor;
        if (issuer.getIssuer() instanceof Player p) {
            actor = PointActor.player(p);
        } else {
            actor = PointActor.console();
        }
        plugin.getContext().decreasePoint(time, team, actor, "ADMIN_REMOVE");
        MessageFx.issuerInfo(issuer, "Removed " + time + " seconds to " + team + " team.");
    }


    @Subcommand("team")
    void getTeamCommand(CommandIssuer issuer) {
        if(!(issuer.getIssuer() instanceof Player player)) {
            MessageFx.issuerInfo(issuer, "This command can only be used by players.");
            return;
        }
        var team = plugin.getContext().getPlayerTeam(player);
        MessageFx.info(player, Component.textOfChildren(
                Component.text("Your team is: ", net.kyori.adventure.text.format.NamedTextColor.YELLOW),
                team.displayName()
        ));
    }

    @Subcommand("team")
    @Syntax("<player>")
    void getTeamCommand(CommandIssuer issuer, OnlinePlayer target) {
        var team = plugin.getContext().getPlayerTeam(target.getPlayer());
        MessageFx.info(target.getPlayer(), Component.textOfChildren(
                Component.text("Your team is: ", NamedTextColor.YELLOW),
                team.displayName()
        ));
    }

    @CommandPermission("sustain.admin")
    @Subcommand("setteam")
    @Syntax("<player> <team>")
    void setTeamCommand(CommandIssuer issuer, OnlinePlayer target, TeamManager.Team team) {
        if(plugin.getContext().isDirty() && !plugin.getContext().getTeamManager().isParticipating(team)){
            MessageFx.issuerInfo(issuer, "Cannot assign players to non-participating teams while the game is active.");
            return;
        }

        plugin.getContext().addPlayerToTeam(target.getPlayer(), team);
        if(!(issuer.getIssuer() instanceof Audience audience)){
            MessageFx.issuerInfo(issuer, "Team of " + target.getPlayer().getName() + " set to " + team.name());
            return;
        }

        audience.sendMessage(Component.textOfChildren(
                Component.text("Team of "),
                Component.text(target.getPlayer().getName(), NamedTextColor.AQUA),
                Component.text(" set to "),
                team.displayName()
        ));
    }

    @CommandPermission("sustain.admin")
    @Subcommand("times")
    void timesCommand(CommandIssuer issuer) {
        List<TeamManager.Team> teams = plugin.getContext().getTeamManager().getActiveTeams();
        Component message = Component.text("Team Times", NamedTextColor.GRAY, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("---------------------", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                ;
        for (TeamManager.Team team : teams) {
            long time = plugin.getContext().getPoints(team);
            message = message.append(
                    Component.textOfChildren(
                            Component.newline(),
                            team.displayName(),
                            Component.text(": "),
                            Component.text(SustainContext.toTimeString(time), plugin.getContext().timeToColour(time)),
                            Component.space(),
                            Component.text("seconds", NamedTextColor.YELLOW)
                    )
            );
        }

        if(!(issuer.getIssuer() instanceof CommandSender commandSender)){
            issuer.sendMessage("unsupported issuer");
            return;
        }
        commandSender.sendMessage(message);
    }

    @CommandPermission("sustain.admin")
    @Subcommand("exportledger")
    void exportLedger(CommandIssuer issuer) {
        try {
            java.io.File exportsDir = new java.io.File(plugin.getDataFolder(), "exports");
            String fileName = club.tesseract.sustain.points.PointLedger.defaultJsonFileName(() -> new java.util.Date());
            java.io.File out = new java.io.File(exportsDir, fileName);
            plugin.getContext().getPointLedger().writeJson(out);
            MessageFx.issuerInfo(issuer, "Ledger exported to: " + out.getAbsolutePath());
        } catch (Exception ex) {
            MessageFx.issuerInfo(issuer, "Failed to export ledger JSON: " + ex.getMessage());
        }
    }

    @CommandPermission("sustain.admin")
    @Subcommand("forcespectator")
    public void forceSpectator(CommandIssuer issuer, OnlinePlayer target) {
        target.getPlayer().setGameMode(GameMode.SPECTATOR);
        MessageFx.issuerInfo(issuer, "Set " + target.getPlayer().getName() + " to spectator mode.");
    }

    @HelpCommand
    public void doHelp(CommandSender sender, CommandHelp help) {
        sendMsg(sender, "Sustain Help Menu:");
        help.showHelp();
    }
}

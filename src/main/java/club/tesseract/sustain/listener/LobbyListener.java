package club.tesseract.sustain.listener;

import club.tesseract.sustain.Sustain;
import club.tesseract.sustain.TeamManager;
import club.tesseract.sustain.util.GameModeHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class LobbyListener implements Listener {

    private final Component WELCOME_MESSAGE = Component.text("Welcome to the Sustain Lobby!")
            .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
    private final Component INFO_MESSAGE = Component.text("Step on a colour to pick your team.")
            .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW);

    private final Sustain plugin;

    public LobbyListener(Sustain plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Scoreboard scoreboard = plugin.getContext().getTeamManager().getScoreboard();
        player.setScoreboard(scoreboard);

        Team team = scoreboard.getPlayerTeam(player);
        if (team != null) {
            // Player is already on a team, add them to the team in context
            TeamManager.Team t = TeamManager.Team.valueOf(team.getName());
            plugin.getContext().addPlayerToTeam(player, t);
        }

        if(!plugin.getContext().isDirty() && plugin.getContext().isPaused()){
            // Assume game hasn't started nor had started yet
            club.tesseract.sustain.util.MessageFx.info(
                    player,
                    Component.join(JoinConfiguration.newlines(),
                            WELCOME_MESSAGE,
                            INFO_MESSAGE
                    )
            );
            player.teleport(player.getWorld().getSpawnLocation());
            GameModeHelper.setGameMode(player,GameMode.ADVENTURE);
        }

        if(!plugin.getContext().isPaused()){
            plugin.getContext().getActionBarScheduler().setFallbackMessageSupplier(
                    player, plugin.getContext().actionBar()
            );
        }
    }

    @EventHandler
    public void onGameModeSwitch(PlayerGameModeChangeEvent event) {
        if(event.getCause().equals(PlayerGameModeChangeEvent.Cause.PLUGIN))return;
        if(event.getCause().equals(PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE))return;

        Player player = event.getPlayer();
        GameMode newGameMode = event.getNewGameMode();
        GameModeHelper.setGameMode(player, newGameMode);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerTeamSelector(PlayerMoveEvent event) {
        if (!plugin.getContext().isPaused()) return;
        if (plugin.getContext().isDirty())return;
        if (!event.hasChangedBlock()) return;
        final Player player = event.getPlayer();
        final Location from = event.getFrom();
        final Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        // get block underplayer;
        var block = to.clone().subtract(0, 1, 0).getBlock();

        TeamManager.Team team = TeamManager.Team.fromBlock(block);
        if (team == null) return;
        TeamManager.Team playersActiveTeam = plugin.getContext().getPlayerTeam(player);
        if(team == playersActiveTeam)return;


        plugin.getContext().addPlayerToTeam(player, team);

        plugin.getContext().getActionBarScheduler().putActionBarMessage(
                player,
                Component.textOfChildren(
                        Component.text("You have joined the ", NamedTextColor.WHITE),
                        team.displayName()
                )
        );
        club.tesseract.sustain.util.MessageFx.info(player, Component.textOfChildren(
                Component.text("You have joined the ", NamedTextColor.GRAY),
                team.displayName(),
                Component.text(" team!", NamedTextColor.GRAY)
        ));
    }


}

package club.tesseract.sustain;

import club.tesseract.sustain.events.GameFinishEvent;
import club.tesseract.sustain.points.PointActor;
import club.tesseract.sustain.points.PointHoarder;
import club.tesseract.sustain.points.PointLedger;
import club.tesseract.sustain.scheduler.ActionBarQueue.DefaultFallbackMessage;
import club.tesseract.sustain.scheduler.ActionBarScheduler;
import club.tesseract.sustain.scheduler.RespawnScheduler;
import club.tesseract.sustain.ticker.GlobalBukkitTicker;
import club.tesseract.sustain.util.GameModeHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SustainContext {

    public final static long DEFAULT_TIME = 10 * 60;

    public final AtomicInteger TICK_RATE = new AtomicInteger(1);
    private final PointHoarder pointHoarder = new PointHoarder(DEFAULT_TIME);

    private final AtomicBoolean GAME_PAUSED = new AtomicBoolean(true);
    private final AtomicBoolean GAME_DIRTY = new AtomicBoolean(false);

    private final HashMap<Player, Integer> deadPlayers = new HashMap<>();
    private TeamManager teamManager = new TeamManager();
    private ActionBarScheduler actionBarScheduler = new ActionBarScheduler();
    private RespawnScheduler respawnScheduler = new RespawnScheduler();
    // Ledger is managed by PointHoarder; keep accessor for external reads

    public SustainContext() {
        // Allow TeamManager to query current points for active team calculations
        teamManager.setPointsProvider(pointHoarder::getPoints);
    }

    public void forceEndGame() {
        GAME_PAUSED.set(true);
        GAME_DIRTY.set(false);
        resetPoints(TeamManager.Team.BLUE, PointActor.console(), "FORCE_END_RESET");
        resetPoints(TeamManager.Team.RED, PointActor.console(), "FORCE_END_RESET");
        // Clear participating teams so next round can re-lock
        teamManager.clearParticipatingTeams();
        Bukkit.getServer().getPluginManager().callEvent(new GameFinishEvent(GameFinishEvent.FINISH_STATE.FORCE_END, null));
    }

    @Nullable
    public synchronized GameFinishEvent isFinished() {
        // Build collections of alive/active teams (excluding spectators)
        java.util.List<TeamManager.Team> aliveByPoints = new java.util.ArrayList<>();
        for (TeamManager.Team t : getTeamManager().getParticipatingTeams()) {
            if (t.isSpectator()) continue;
            long pts = pointHoarder.getPoints(t);
            if (pts > 0) aliveByPoints.add(t);
        }
        java.util.List<TeamManager.Team> activeByRule = teamManager.getActiveTeams();

        // If only one team remains alive by points -> they win
        if (aliveByPoints.size() == 1) {
            GAME_PAUSED.set(true);
            return new GameFinishEvent(GameFinishEvent.FINISH_STATE.TEAM_WIN, aliveByPoints.getFirst());
        }

        // If none alive by points -> tie
        if (aliveByPoints.isEmpty()) {
            GAME_PAUSED.set(true);
            return new GameFinishEvent(GameFinishEvent.FINISH_STATE.TIE, null);
        }

        // If exactly two teams are active (points>0 or have online players), treat zero as instant finish
        if (activeByRule.size() == 2) {
            TeamManager.Team a = activeByRule.get(0);
            TeamManager.Team b = activeByRule.get(1);
            long pa = pointHoarder.getPoints(a);
            long pb = pointHoarder.getPoints(b);
            if (pa <= 0 && pb <= 0) {
                GAME_PAUSED.set(true);
                return new GameFinishEvent(GameFinishEvent.FINISH_STATE.TIE, null);
            }
            if (pa <= 0) {
                GAME_PAUSED.set(true);
                return new GameFinishEvent(GameFinishEvent.FINISH_STATE.TEAM_WIN, b);
            }
            if (pb <= 0) {
                GAME_PAUSED.set(true);
                return new GameFinishEvent(GameFinishEvent.FINISH_STATE.TEAM_WIN, a);
            }
        }

        return null;
    }

    public synchronized long increasePoint(long point, TeamManager.Team team, PointActor actor, String reason) {
        return pointHoarder.increase(point, team, actor, reason);
    }

    public synchronized long decreasePoint(long point, TeamManager.Team team, PointActor actor, String reason) {
        return pointHoarder.decrease(point, team, actor, reason);
    }

    public synchronized long resetPoints(TeamManager.Team team, PointActor actor, String reason) {
        return pointHoarder.reset(team, actor, reason);
    }


    public long getPoints(TeamManager.Team team) {
        if(team.isSpectator()) return -1;
        if(!teamManager.isParticipating(team)) return -1;
        return pointHoarder.getPoints(team);
    }

    public boolean isPaused() {
        return GAME_PAUSED.get();
    }

    public void pause() {
        GAME_DIRTY.set(true);
        GAME_PAUSED.set(true);
    }

    public void resume() {
        GAME_DIRTY.set(true);
        GAME_PAUSED.set(false);
    }

    public void resetDirty() {
        GAME_DIRTY.set(false);
    }

    public boolean isDirty() {
        return GAME_DIRTY.get();
    }

    public void setTickRate(int ticks) {
        TICK_RATE.set(ticks);
    }

    public int getTickRate() {
        return TICK_RATE.get();
    }

    public void onPlayerDeath(Player player){
        player.setInvulnerable(true);
        GameModeHelper.setGameMode(player, GameMode.SPECTATOR);
        deadPlayers.put(player, GlobalBukkitTicker.getActiveTicks());
    }

    public void clearPlayerDeath(Player player){
        deadPlayers.remove(player);
    }

    public boolean isPlayerDead(Player player){
        return deadPlayers.containsKey(player);
    }

    public HashMap<Player, Integer> getDeadPlayers(){
        return deadPlayers;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public ActionBarScheduler getActionBarScheduler() {
        return actionBarScheduler;
    }

    public RespawnScheduler getRespawnScheduler(){
        return respawnScheduler;
    }
    
    public PointLedger getPointLedger() {
        return pointHoarder.getPointLedger();
    }

    public static String toTimeString(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secondsLeft = seconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secondsLeft);
        }

        return String.format("%02d:%02d", minutes, secondsLeft);
    }

    public void addPlayerToTeam(Player player, TeamManager.Team team) {
        teamManager.setPlayerTeam(player, team);
    }

    public DefaultFallbackMessage actionBar() {
        return (player) -> {
            TeamManager.Team team = getPlayerTeam(player);
            if (team.isSpectator()) {
                return Component.text("Spectating", NamedTextColor.GRAY);
            }

            if(!isDirty()){
                return null;
            }


            long points = getPoints(team);
            Component liveComponet = Component.textOfChildren(
                    Component.text("⏱", NamedTextColor.GRAY, TextDecoration.BOLD),
                    Component.space(),
                    Component.text(toTimeString(points), timeToColour(points))
            );
            if (isPaused()) {
                return Component.textOfChildren(
                        Component.text("[PAUSED] ", NamedTextColor.RED, TextDecoration.BOLD),
                        liveComponet
                );
            }
            return liveComponet;
        };
    }

    public TextColor timeToColour(long time) {
        if (isPaused()) return NamedTextColor.GRAY;
        if (time > 300) {
            return NamedTextColor.GREEN;
        }
        if (time > 120) {
            return TextColor.lerp((time - 120) / 180f, NamedTextColor.YELLOW, NamedTextColor.GREEN);
        }
        return TextColor.lerp(time / 120f, NamedTextColor.RED, NamedTextColor.YELLOW);
    }

    @NotNull
    public TeamManager.Team getPlayerTeam(Player player) {
        for (TeamManager.Team team : TeamManager.Team.values()) {
            var bukkitTeam = teamManager.getScoreboard().getTeam(team.name());
            if (bukkitTeam != null && bukkitTeam.hasPlayer(player)) {
                return team;
            }
        }
        return TeamManager.Team.SPECTATOR;
    }

    
}

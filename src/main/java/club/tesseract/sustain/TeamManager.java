package club.tesseract.sustain;


import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.function.ToLongFunction;

public class TeamManager {

    private Scoreboard scoreboard = null;
    /**
     * Unified per-team data holder.
     */
    public record TeamData(org.bukkit.scoreboard.Team bukkitTeam, @Nullable Location spawn, BossBar bossBar) {}

    private final EnumMap<Team, TeamData> teams = new EnumMap<>(Team.class);
    private ToLongFunction<Team> pointsProvider = null;
    // Teams that are locked in as participants for the current round (exclude SPECTATOR)
    private final Set<Team> participating = EnumSet.noneOf(Team.class);

    public TeamManager(){
        for (Team value : Team.values()) {
            BossBar bossBar = BossBar.bossBar(
                    value.displayName(),
                    1.0f,
                    value.getBossBarColor(),
                    BossBar.Overlay.PROGRESS
            );
            teams.put(value, new TeamData(null, null, bossBar));
        }
    }

    /**
     * Provide a function to retrieve current points for a given team.
     * This allows TeamManager to compute active teams without owning the points storage.
     */
    public void setPointsProvider(ToLongFunction<Team> provider) {
        this.pointsProvider = provider;
    }

    /**
     * Lock the set of teams participating in the current round.
     * Spectator will be ignored even if provided.
     */
    public void setParticipatingTeams(Set<Team> teams) {
        this.participating.clear();
        if (teams != null) {
            for (Team t : teams) {
                if (t != null && !t.isSpectator()) this.participating.add(t);
            }
        }
    }

    /**
     * Clear the participating teams (e.g., when round ends).
     */
    public void clearParticipatingTeams() {
        this.participating.clear();
    }

    /**
     * Current locked-in participating teams (unmodifiable copy).
     */
    public Set<Team> getParticipatingTeams() {
        return java.util.Collections.unmodifiableSet(this.participating);
    }

    /**
     * Whether the team is part of the current round.
     */
    public boolean isParticipating(Team team) {
        return !team.isSpectator() && (participating.isEmpty() || participating.contains(team));
    }

    public Scoreboard getScoreboard() {
        @Nullable ScoreboardManager manager = Bukkit.getScoreboardManager(); // stupid paper says NotNull but it can be null
        if(manager == null){
            return null;
        }
        this.scoreboard = manager.getMainScoreboard();
        return scoreboard;
    }

    private void generateTeams(){
        Scoreboard scoreboard = getScoreboard();
        if (scoreboard == null) return;
        for (Team team : Team.values()){
            org.bukkit.scoreboard.Team bukkitTeam = scoreboard.getTeam(team.name());
            if(bukkitTeam == null){
                bukkitTeam = scoreboard.registerNewTeam(team.name());
            }
            bukkitTeam.color(team.getColour());
            bukkitTeam.displayName(team.displayName());
            bukkitTeam.setAllowFriendlyFire(false);
            bukkitTeam.prefix(team.teamPrefix());
            if(team.isSpectator()){
                bukkitTeam.setCanSeeFriendlyInvisibles(true);
                bukkitTeam.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
                bukkitTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.FOR_OWN_TEAM);
            }else{
                bukkitTeam.setCanSeeFriendlyInvisibles(false);
                bukkitTeam.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.FOR_OTHER_TEAMS);
                bukkitTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
            }

            TeamData existing = teams.get(team);
            BossBar bossBar = existing != null ? existing.bossBar() : BossBar.bossBar(team.displayName(), 1.0f, team.getBossBarColor(), BossBar.Overlay.PROGRESS);
            Location spawn = existing != null ? existing.spawn() : null;
            teams.put(team, new TeamData(bukkitTeam, spawn, bossBar));
        }
    }

    void setPlayerTeam(Player player, Team team){
        if(teams.isEmpty()){
            generateTeams();
        }
        TeamData data = teams.get(team);
        if(data == null || data.bukkitTeam() == null) {
            generateTeams();
            data = teams.get(team);
        }
        if (data == null || data.bukkitTeam() == null) {
            throw new IllegalStateException("Team " + team.name() + " not found in scoreboard.");
        }
        data.bukkitTeam().addPlayer(player);

        // Hide all boss bars first
        for (TeamData td : teams.values()) {
            if (td != null && td.bossBar() != null) player.hideBossBar(td.bossBar());
        }
        BossBar bossBar = data.bossBar();
        if(bossBar == null){
            Sustain.getPlugin().getLogger().warning("BossBar for team " + team.name() + " not found.");
            return;
        }
        player.showBossBar(bossBar);
    }

    @Nullable
    public Team getTeamFromCampfire(org.bukkit.block.Block block){
        for (Map.Entry<Team, TeamData> e : teams.entrySet()) {
            Team team = e.getKey();
            Location location = e.getValue() != null ? e.getValue().spawn() : null;
            if(location != null && location.getBlock().equals(block)){
                return team;
            }
        }
        return null;
    }

    public org.bukkit.scoreboard.Team getTeam(Team team){
        TeamData data = teams.get(team);
        return data == null ? null : data.bukkitTeam();
    }

    @UnknownNullability
    public TeamData getTeamData(Team team){
        return teams.get(team);
    }

    /**
     * Checks if the specified team has at least one online player.
     */
    public boolean hasOnlinePlayers(Team team) {
        org.bukkit.scoreboard.Team bt = getTeam(team);
        if (bt == null) return false;
        for (OfflinePlayer op : bt.getPlayers()) {
            if (op.isOnline()) return true;
        }
        return false;
    }

    /**
     * Returns the list of active teams (excluding spectators).
     * A team is considered active if it has points > 0 OR has at least one online player.
     */
    public List<Team> getActiveTeams() {
        List<Team> active = new ArrayList<>();
        for (Team t : Team.values()) {
            if (t.isSpectator()) continue;
            // If participating set is defined (non-empty), ignore non-participating teams for this round
            if (!participating.isEmpty() && !participating.contains(t)) continue;
            long pts = 0;
            if (pointsProvider != null) {
                try {
                    pts = Math.max(0, pointsProvider.applyAsLong(t));
                } catch (Throwable ignored) {
                    pts = 0;
                }
            }
            if (pts > 0 || hasOnlinePlayers(t)) {
                active.add(t);
            }
        }
        return active;
    }

    public void setTeamSpawn(Team team, Location location){
        TeamData data = teams.get(team);
        if (data == null) {
            // ensure bossbar exists
            BossBar bossBar = BossBar.bossBar(team.displayName(), 1.0f, team.getBossBarColor(), BossBar.Overlay.PROGRESS);
            teams.put(team, new TeamData(null, location, bossBar));
        } else {
            teams.put(team, new TeamData(data.bukkitTeam(), location, data.bossBar()));
        }
    }

    @NotNull
    public Optional<Location> getTeamSpawn(Team team){
        TeamData teamData = teams.get(team);
        if(teamData == null)return Optional.empty();
        return Optional.ofNullable(teamData.spawn());
    }

    public HashMap<Team, @Nullable Location> getTeamSpawns(){
        HashMap<Team, Location> out = new HashMap<>();
        for (Map.Entry<Team, TeamData> e : teams.entrySet()) {
            out.put(e.getKey(), e.getValue() == null ? null : e.getValue().spawn());
        }
        return out;
    }

    public enum Team{
        RED(NamedTextColor.RED, Material.RED_CONCRETE, false),
        BLUE(NamedTextColor.BLUE, Material.BLUE_CONCRETE, false),
        GREEN(NamedTextColor.GREEN, Material.GREEN_CONCRETE, false),
        YELLOW(NamedTextColor.YELLOW, Material.YELLOW_CONCRETE, false),
        PURPLE(NamedTextColor.DARK_PURPLE, Material.PURPLE_CONCRETE, false),
        PINK(NamedTextColor.LIGHT_PURPLE, Material.PINK_CONCRETE, false),

        SPECTATOR(NamedTextColor.GRAY, Material.GRAY_CONCRETE, true)
        ;
        private final NamedTextColor colour;
        private final Material material;
        private final boolean isSpectator;

        Team(NamedTextColor colour, Material material, boolean isSpectator){
            this.colour = colour;
            this.material = material;
            this.isSpectator = isSpectator;
        }

        public Component displayName() {
            return Component.text(this.name()).color(this.colour).decoration(TextDecoration.BOLD, isSpectator);
        }

        public Component teamPrefix(){
            return Component.textOfChildren(
                    Component.text("[", NamedTextColor.DARK_GRAY),
                    displayName(),
                    Component.text("]", NamedTextColor.DARK_GRAY),
                    Component.space()
            );
        }

        public NamedTextColor getColour() {
            return colour;
        }

        public Color getBukkitColour(){
            return Color.fromBGR(colour.blue(), colour.green(), colour.red());
        }

        public BossBar.Color getBossBarColor(){
            if(this == RED) return BossBar.Color.RED;
            if(this == BLUE) return BossBar.Color.BLUE;
            if(this == GREEN) return BossBar.Color.GREEN;
            if(this == YELLOW) return BossBar.Color.YELLOW;
            if(this == PURPLE) return BossBar.Color.PURPLE;
            if(this == PINK) return BossBar.Color.PINK;
            return BossBar.Color.WHITE; // Spectator
        }

        public Material getMaterial() {
            return material;
        }

        public boolean isSpectator() {
            return isSpectator;
        }

        @Nullable
        public static Team fromBlock(org.bukkit.block.Block block){
            Material type = block.getType();
            for(Team team : Team.values()){
                if(team.getMaterial() == type){
                    return team;
                }
            }
            return null;
        }
    }
}
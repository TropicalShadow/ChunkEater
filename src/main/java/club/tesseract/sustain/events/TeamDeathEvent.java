package club.tesseract.sustain.events;

import club.tesseract.sustain.TeamManager;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TeamDeathEvent extends Event {

    public enum Reason {
        ZERO,
        ADMIN
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final TeamManager.Team team;
    private final Reason reason;

    public TeamDeathEvent(TeamManager.Team deadTeam, Reason reason) {
        this.team = deadTeam;
        this.reason = reason;
    }

    public TeamManager.Team getTeam() {
        return team;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}

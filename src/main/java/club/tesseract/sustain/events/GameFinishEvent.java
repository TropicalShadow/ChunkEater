package club.tesseract.sustain.events;

import club.tesseract.sustain.TeamManager;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GameFinishEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final FINISH_STATE state;
    private final @Nullable TeamManager.Team winningTeam;

    public GameFinishEvent(FINISH_STATE state, @Nullable TeamManager.Team winningTeam) {
        this.state = state;
        this.winningTeam = winningTeam;
    }

    public FINISH_STATE getState() {
        return state;
    }

    @Nullable
    public TeamManager.Team getWinningTeam() {
        return winningTeam;
    }


    public enum FINISH_STATE{
        TEAM_WIN,
        TIE,
        FORCE_END,
        ;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}

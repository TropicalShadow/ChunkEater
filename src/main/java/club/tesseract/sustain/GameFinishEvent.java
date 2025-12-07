package club.tesseract.sustain;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameFinishEvent extends Event {


    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final FINISH_STATE state;
    private final Player[] winners;

    public GameFinishEvent(FINISH_STATE state, Player... winners) {
        this.state = state;
        this.winners = winners;
    }

    public FINISH_STATE getState() {
        return state;
    }

    public Player[] getWinners() {
        return winners;
    }


    public enum FINISH_STATE{
        BLUE_WIN,
        RED_WIN,
        TIE;
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

package club.tesseract.sustain.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GamePauseEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();


    public GamePauseEvent() {

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

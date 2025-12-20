package club.tesseract.sustain.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameStartEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final boolean isResume;

    public GameStartEvent(boolean isResume) {
        this.isResume = isResume;
    }

    public boolean isResume() {
        return isResume;
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

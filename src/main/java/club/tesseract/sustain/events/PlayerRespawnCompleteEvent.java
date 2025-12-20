package club.tesseract.sustain.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerRespawnCompleteEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public PlayerRespawnCompleteEvent(@NotNull Player player) {
        super(player);
    }


    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

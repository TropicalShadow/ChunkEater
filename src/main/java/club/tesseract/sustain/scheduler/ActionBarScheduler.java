package club.tesseract.sustain.scheduler;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.WeakHashMap;

public final class ActionBarScheduler implements Listener {

    private final WeakHashMap<Player, ActionBarQueue> playerQueues = new WeakHashMap<>();

    @EventHandler
    public void onTickEvent(ServerTickEndEvent event) {
        tick();
    }

    public void putActionBarMessage(Player player, Component message) {
        ActionBarQueue queue = playerQueues.computeIfAbsent(player, ActionBarQueue::new);
        queue.enqueueMessage(message);
    }

    public void setFallbackMessageSupplier(Player player, ActionBarQueue.DefaultFallbackMessage supplier) {
        ActionBarQueue queue = playerQueues.computeIfAbsent(player, ActionBarQueue::new);
        queue.setFallbackMessageSupplier(supplier);
    }

    void tick(){
        for (ActionBarQueue queue : playerQueues.values()) {
            queue.tick();
        }
    }

}

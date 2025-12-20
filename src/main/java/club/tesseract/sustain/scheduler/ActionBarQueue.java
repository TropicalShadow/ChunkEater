package club.tesseract.sustain.scheduler;

import club.tesseract.sustain.ticker.GlobalBukkitTicker;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

public class ActionBarQueue {
    private final Player player;

    private final Queue<Component> messages = new LinkedList<>();
    private DefaultFallbackMessage fallbackMessage = (ignored) -> null;
    private int lastMessageSentTick = 0;

    public static final int QUEUE_LIMIT = 250;

    public ActionBarQueue(Player player) {
        this.player = player;
    }

    public void enqueueMessage(Component message) {
        if (messages.size() >= QUEUE_LIMIT) {
            player.sendActionBar(message);
            return; // Discard new messages if the queue is full
        }
        messages.add(message);
    }

    public void setFallbackMessageSupplier(@Nullable DefaultFallbackMessage fallbackMessage) {
        if(fallbackMessage == null){
            this.fallbackMessage = (ignored) -> null;
            return;
        }
        this.fallbackMessage = fallbackMessage;
    }

    public void tick(){
        int currentTick = GlobalBukkitTicker.getTick();
        int sinceLastMsg = currentTick - lastMessageSentTick;
        int betweenMsg = computeTicksBetweenMessages();
        if( sinceLastMsg < betweenMsg){
            return;
        }
        Component message = messages.poll();
        if(message == null){
            message = fallbackMessage.apply(player);
            if(message == null){
                return;
            }
        }

        lastMessageSentTick = currentTick;

        player.sendActionBar(message);
    }

    private int computeTicksBetweenMessages() {
        int count = messages.size();
        if (count <= 3) {
            return 22;
        }
        if (count <= 6) {
            return 19;
        }
        if (count <= 10) {
            return 17;
        }
        if (count <= 15) {
            return 11;
        }
        if (count <= 25) {
            return 6;
        }
        return 1;
    }


    @FunctionalInterface
    public interface DefaultFallbackMessage extends Function<@NotNull Player, @Nullable Component> {

    }
}

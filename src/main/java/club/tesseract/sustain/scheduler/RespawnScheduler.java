package club.tesseract.sustain.scheduler;

import club.tesseract.sustain.Sustain;
import club.tesseract.sustain.events.PlayerRespawnCompleteEvent;
import club.tesseract.sustain.ticker.GlobalBukkitTicker;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;


public class RespawnScheduler implements Listener {

    private static final long DEATH_DURATION = 5;

    private final Sustain plugin = Sustain.getPlugin();


    @EventHandler
    public void onTickEvent(ServerTickEndEvent event) {
        tick();
    }


    void tick() {
        long activeTick = GlobalBukkitTicker.getActiveTicks();

        if (plugin.getContext().isPaused()) {
            plugin.getContext().getDeadPlayers().forEach((p, ignored) -> {
                plugin.getContext().getActionBarScheduler().putActionBarMessage(p, Component.text("[PAUSED]").color(NamedTextColor.RED));
            });
        }

        plugin.getContext().getDeadPlayers().entrySet().removeIf(entry -> {
            Player player = entry.getKey();
            long deathTime = entry.getValue();
            long tickDelta = activeTick - deathTime;
            if ((tickDelta / 20) >= DEATH_DURATION) {
                // Respawn player
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.showTitle(Title.title(
                                Component.empty(),
                                Component.text("Respawning...").color(NamedTextColor.RED),
                                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                        ));
                        PlayerRespawnCompleteEvent event = new PlayerRespawnCompleteEvent(player);
                        Bukkit.getServer().getPluginManager().callEvent(event);
                    }
                });
                return true; // Remove from map
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    long secondsLeft = DEATH_DURATION - (tickDelta / 20);
                    player.showTitle(Title.title(
                            Component.empty(),
                            Component.text("Respawning in " + secondsLeft + "s")
                                    .color(NamedTextColor.RED),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
                    ));
                }
            });
            return false; // Keep in map
        });
    }

}

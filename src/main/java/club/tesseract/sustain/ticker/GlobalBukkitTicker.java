package club.tesseract.sustain.ticker;

import club.tesseract.sustain.Sustain;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class GlobalBukkitTicker implements Listener {
    private static final GlobalBukkitTicker INSTANCE = new GlobalBukkitTicker();

    private int tick;
    private int activeGameTicking;

    public static void register(Sustain plugin){
        Bukkit.getServer().getPluginManager().registerEvents(INSTANCE, plugin);
    }


    @EventHandler
    public void onTick(ServerTickEndEvent event) {
        tick++;
        if(!Sustain.getPlugin().getContext().isPaused()){
            activeGameTicking++;
        }
    }

    public int tick() {
        return tick;
    }

    public int getActiveGameTicking(){
        return activeGameTicking;
    }

    public static GlobalBukkitTicker getInstance() {
        return INSTANCE;
    }

    public static int getTick() {
        return INSTANCE.tick();
    }

    public static int getActiveTicks(){
        return INSTANCE.getActiveGameTicking();
    }

    public static boolean every(int period, Entity entity){
        int within = Math.abs(entity.getUniqueId().hashCode()) % period;
        return entity.getTicksLived() % period == within;
    }
}

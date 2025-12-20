package club.tesseract.sustain.listener;

import club.tesseract.sustain.Sustain;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BrandingListener implements Listener {

    private final Sustain plugin;

    public BrandingListener(Sustain plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
    }

}

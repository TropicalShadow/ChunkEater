package club.tesseract.sustain.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerSpawnListener implements Listener {


    @EventHandler
    public void onPlayerSpawn(PlayerJoinEvent event){
        event.getPlayer().sendMessage("Hello, World!");
    }

}

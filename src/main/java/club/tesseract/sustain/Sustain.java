package club.tesseract.sustain;

import club.tesseract.sustain.commands.DebugCommand;
import club.tesseract.sustain.listener.ItemDropEvent;
import club.tesseract.sustain.listener.PlayerSpawnListener;
import club.tesseract.sustain.utils.PluginMetaUtils;
import co.aikar.commands.PaperCommandManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main class of the plugin.
 */
public final class Sustain extends JavaPlugin {

    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        // Plugin startup logic

        Integer pluginId = PluginMetaUtils.getMeta().bstatsPluginId();
        if (pluginId != null) {
            new Metrics(this, pluginId);
            getLogger().info("bStats metrics enabled!");
        }

        commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new DebugCommand());

        getServer().getPluginManager().registerEvents(new PlayerSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new ItemDropEvent(this), this);

        commandManager.setDefaultExceptionHandler((command, registeredCommand, sender, args, t) -> {
            sender.sendMessage("An error occurred while executing the command.");
            getLogger().warning("Error occured while executing command " + command.getName());
            getLogger().severe(t.getMessage());
            return true; // mark as handled to prevent further handlers from being called.
        });
        this.getLogger().info("Plugin enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        this.getLogger().info("Plugin disabled!");
    }

    public static Sustain getPlugin() {
        return Sustain.getPlugin(Sustain.class);
    }
}

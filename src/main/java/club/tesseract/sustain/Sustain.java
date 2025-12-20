package club.tesseract.sustain;

import club.tesseract.sustain.commands.ControlCommand;
import club.tesseract.sustain.listener.GameRuntimeListener;
import club.tesseract.sustain.listener.LobbyListener;
import club.tesseract.sustain.listener.WorldReaderListener;
import club.tesseract.sustain.ticker.GlobalBukkitTicker;
import club.tesseract.sustain.util.PluginMetaUtils;
import club.tesseract.sustain.util.WorldUtils;
import co.aikar.commands.PaperCommandManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class of the plugin.
 */
public final class Sustain extends JavaPlugin {

    private static final Logger log = LoggerFactory.getLogger(Sustain.class);
    private PaperCommandManager commandManager;
    private SustainContext context;

    @Override
    public void onLoad() {
        log.info("Attempting to load world...");
        WorldUtils.createFromTemplate(this,"world");
    }

    @Override
    public void onEnable() {
        GlobalBukkitTicker.register(this);
        // Plugin startup logic
        context = new SustainContext();
        Integer pluginId = PluginMetaUtils.getMeta().bstatsPluginId();
        if (pluginId != null) {
            new Metrics(this, pluginId);
            getLogger().info("bStats metrics enabled!");
        }

        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.registerCommand(new ControlCommand());

        Bukkit.getServer().getPluginManager().registerEvents(new WorldReaderListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        getServer().getPluginManager().registerEvents(new GameRuntimeListener(this), this);
        getServer().getPluginManager().registerEvents(context.getActionBarScheduler(), this);
        getServer().getPluginManager().registerEvents(context.getRespawnScheduler(), this);

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

    public SustainContext getContext() {
        return context;
    }
}

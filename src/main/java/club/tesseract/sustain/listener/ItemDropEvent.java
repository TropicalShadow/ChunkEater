package club.tesseract.sustain.listener;

import club.tesseract.sustain.Sustain;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.concurrent.atomic.AtomicLong;

public class ItemDropEvent implements Listener {

    private final NamespacedKey amount = NamespacedKey.fromString("sustain:fuckyou");
    private final NamespacedKey isRed = NamespacedKey.fromString("sustain:fuckyoutwo");

    private final AtomicLong bluePoints = new AtomicLong(60 * 10);
    private final AtomicLong redPoints = new AtomicLong(60 * 10);

    private Scoreboard scoreboard = null;
    private Objective objective = null;
    private final Sustain plugin;

    public ItemDropEvent(Sustain plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getScheduler().runTask(plugin, this::getScoreboard);
        this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if(objective == null)return;
            bluePoints.updateAndGet(i -> Math.max(0, i - 1));
            redPoints.updateAndGet(i -> Math.max(0, i - 1));
            objective.getScore("blue").customName(getScoreComponent(true));
            objective.getScore("red").customName(getScoreComponent(false));
        }, 60, 20L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getAndSetScoreboard(player);
    }

    void getAndSetScoreboard(Player player) {
        getScoreboard();
        player.setScoreboard(scoreboard);
        scoreboard.getTeam("red").addPlayer(player);
    }

    Component getScoreComponent(boolean isBlue) {
        if (isBlue) {
            return Component.textOfChildren(
                    Component.text("BLUE", NamedTextColor.BLUE),
                    Component.space(),
                    Component.text(toTimeString(bluePoints.get()))
            );
        } else {
            return Component.textOfChildren(
                    Component.text("RED", NamedTextColor.RED),
                    Component.space(),
                    Component.text(toTimeString(redPoints.get()))
            );
        }
    }

    Scoreboard getScoreboard() {
        if (scoreboard == null) {
            this.scoreboard = this.plugin.getServer().getScoreboardManager().getNewScoreboard();
            this.objective = scoreboard.registerNewObjective("sustain", "dummy");
            this.objective.displayName(Component.text("Sustain", NamedTextColor.GOLD));
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            this.objective.getScore("red").numberFormat(NumberFormat.blank());
            this.objective.getScore("red").customName(getScoreComponent(false));
            this.objective.getScore("blue").numberFormat(NumberFormat.blank());
            this.objective.getScore("blue").customName(getScoreComponent(true));
            this.scoreboard.registerNewTeam("red").addEntry("red");
            this.scoreboard.registerNewTeam("blue").addEntry("blue");
        }
        return scoreboard;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType().equals(BlockType.CAMPFIRE)) {
            block.getChunk().getPersistentDataContainer().set(isRed, PersistentDataType.BOOLEAN, true);
        } else if (block.getType().equals(BlockType.SOUL_CAMPFIRE)) {
            block.getChunk().getPersistentDataContainer().set(isRed, PersistentDataType.BOOLEAN, false);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItemDrop();
        if (!item.getItemStack().getType().isBlock()) return;
        item.getScheduler().runAtFixedRate(plugin, (task) -> {
            if (item.isOnGround()) {
                if (item.getItemStack().getType().equals(Material.BARRIER)) {
                    item.getChunk().getPersistentDataContainer().set(amount, PersistentDataType.LONG, 0L);
                    player.sendMessage("Points reset");
                    objective.getScore("blue").customName(getScoreComponent(true));
                    objective.getScore("red").customName(getScoreComponent(false));
                    item.remove();
                    return;
                }

                // get block under item
                Location loc = item.getLocation();
                World world = item.getWorld();
                Block block = world.getBlockAt(loc);
                Block blockUnder = world.getBlockAt(loc.subtract(0, 1, 0));

                boolean isBlue = false;

                if (block.getType().equals(Material.CAMPFIRE) || blockUnder.getType().equals(Material.CAMPFIRE)) {
                    isBlue = false;
                } else if (block.getType().equals(Material.SOUL_CAMPFIRE) || blockUnder.getType().equals(Material.SOUL_CAMPFIRE)) {
                    isBlue = true;
                } else {
                    return;
                }

                // TODO - check if block below is campfire, if so, consume and do shit
                // TODO - get item value multiply by item qty

                long f = blockToValue(item.getItemStack().getType()) * item.getItemStack().getAmount();
                long result;
                if (isBlue) {
                    result = bluePoints.updateAndGet(i -> i + f);
                } else {
                    result = redPoints.updateAndGet(i -> i + f);
                }

                if (isBlue)
                    objective.getScore("blue").customName(getScoreComponent(true));
                else
                    objective.getScore("red").customName(getScoreComponent(false));

                player.sendMessage("You now have " + result + " sustain points");
                item.getWorld().spawnParticle(Particle.EXPLOSION, item.getLocation(), 10);
                item.remove();
                task.cancel();
            }
        }, null, 20L, 10);
    }

    public static String toTimeString(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secondsLeft = seconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secondsLeft);
        }

        return String.format("%02d:%02d", minutes, secondsLeft);
    }

    /**
     * get block duration in seconds
     *
     * @param material block material
     * @return block duration
     */
    long blockToValue(Material material) {
        plugin.getLogger().info(">" + material.getHardness() + " " + material.name());
        if (material.equals(Material.AIR)) return 0;
        if (material.equals(Material.BARRIER)) return 0;
        float hardness = material.getHardness();
        if (hardness == -1) return 0;
        if (hardness <= 1) {
            hardness += 1;
            return (long) (hardness * 60);
        }
        if (hardness <= 6) return (long) (hardness * 60);
        if (hardness == 22) return 1200; // 20 minutes

        return (long) (material.getHardness() * 1.5);
    }

}

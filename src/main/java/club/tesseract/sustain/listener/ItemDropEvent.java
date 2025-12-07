package club.tesseract.sustain.listener;

import club.tesseract.sustain.GameFinishEvent;
import club.tesseract.sustain.Sustain;
import club.tesseract.sustain.SustainContext;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.block.Campfire;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.time.format.TextStyle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ItemDropEvent implements Listener {

    private final Sustain plugin;

    public ItemDropEvent(Sustain plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if(plugin.getContext().isPaused()) return;
            plugin.getContext().decreasePoint(1,true);
            plugin.getContext().decreasePoint(1,false);
            GameFinishEvent event = plugin.getContext().isFinished();
            if(event != null){
                event.callEvent();
            }
        }, 60, 20L);
    }

    @EventHandler
    public void gameFinish(GameFinishEvent e){
        Player[] winners = e.getWinners();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isWinner = false;
            for (Player winner : winners) {
                if (winner != null && winner.getUniqueId().equals(player.getUniqueId())) {
                    isWinner = true;
                    break;
                }
            }

            if (e.getState() == GameFinishEvent.FINISH_STATE.TIE) {
                player.sendMessage(Component.text("It's a tie!", NamedTextColor.YELLOW));
                continue;
            }

            if (isWinner) {
                player.sendMessage(Component.text("You won!", NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
            } else {
                player.sendMessage(Component.text("You lost!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByBlockEvent e){
        Block damager = e.getDamager();

        // Cancel damage and set it to 0 when the damager is a campfire or soul campfire
        Material type = damager.getType();
        if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
            e.setDamage(0);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockAgainst();
        if(!(block.getType() == Material.CAMPFIRE && block.getType() == Material.SOUL_CAMPFIRE)){
            return;
        }
        Player player = event.getPlayer();

        boolean isBlue = false;

        if (block.getType().equals(Material.CAMPFIRE)) {
            isBlue = false;
        } else if (block.getType().equals(Material.SOUL_CAMPFIRE)) {
            isBlue = true;
        } else {
            return;
        }

        event.setCancelled(false);
        event.setBuild(false);
        ItemStack itemInHand = event.getItemInHand();
        Campfire campfire = (Campfire) block;
        campfire.setItem(1, itemInHand);
        player.getInventory().remove(itemInHand);
        long duration = blockToValue(itemInHand.getType()) * itemInHand.getAmount();
        plugin.getContext().increasePoint(duration,isBlue);
        player.sendMessage("Points gained " + SustainContext.toTimeString(duration));
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItemDrop();
        if (!item.getItemStack().getType().isBlock()) return;
        item.getScheduler().runAtFixedRate(plugin, (task) -> {
            if (item.isOnGround()) {

                // get block under item
                Location loc = item.getLocation();
                World world = item.getWorld();
                Block block = world.getBlockAt(loc);
                Block blockUnder = world.getBlockAt(loc.subtract(0, 1, 0));

                boolean isBlue;

                if (block.getType().equals(Material.CAMPFIRE) || blockUnder.getType().equals(Material.CAMPFIRE)) {
                    isBlue = false;
                } else if (block.getType().equals(Material.SOUL_CAMPFIRE) || blockUnder.getType().equals(Material.SOUL_CAMPFIRE)) {
                    isBlue = true;
                } else {
                    return;
                }

                if (item.getItemStack().getType().equals(Material.BARRIER)) {
                    plugin.getContext().resetPoints(isBlue);
                    player.sendMessage("Points reset");
                    item.remove();
                    return;
                }

                long f = blockToValue(item.getItemStack().getType()) * item.getItemStack().getAmount();
                long result = plugin.getContext().increasePoint(f, isBlue);
                player.sendMessage("You now have " + SustainContext.toTimeString(result) + " sustain points");
                item.getWorld().spawnParticle(Particle.EXPLOSION, item.getLocation(), 10);
                item.remove();
                task.cancel();
            }
        }, null, 20L, 10);
    }

    /**
     * get block duration in seconds
     *
     * @param material block material
     * @return block duration
     */
    long blockToValue(Material material) {
        // TODO - rewrite this shit
        plugin.getLogger().info(">" + material.getHardness() + " " + material.name());
        if (material.equals(Material.AIR)) return 0;
        if (material.equals(Material.BARRIER)) return 0;
        float hardness = material.getHardness();
        if (hardness == -1) return 0;
        if (hardness <= 1) {
            hardness += 1;
            return (long) hardness;
        }
        return (long) hardness;
    }

}

package club.tesseract.sustain.listener;

import club.tesseract.sustain.Sustain;
import club.tesseract.sustain.TeamManager;
import org.bukkit.Chunk;
import org.bukkit.GameRules;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class WorldReaderListener implements Listener {

    private final HashMap<TeamManager.Team, Block> blocks = new HashMap<>();
    private final Sustain plugin;

    public WorldReaderListener(Sustain plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event){
        World world = event.getWorld();
        world.setAutoSave(false);
        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
    }

    @EventHandler
    public void onWorldLoad(ChunkLoadEvent event){
        World world = event.getWorld();

        Chunk chunk = event.getChunk();
        // TODO - use either items contains or block under to specify what team camp belongs to
        boolean containsCampfire = chunk.contains(Material.CAMPFIRE.createBlockData());
        boolean containsSoulFire = chunk.contains(Material.SOUL_CAMPFIRE.createBlockData()); // TODO - this is fucked and strict about, facing, lit, signal_fire, waterlogged

        if(containsCampfire || containsSoulFire){
            // iterate through all blocks in chunk
            for(int x = 0; x < 16; x++){
                for(int y = 0; y < world.getMaxHeight(); y++) {
                    for (int z = 0; z < 16; z++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE) {
                            JavaPlugin.getPlugin(Sustain.class).getComponentLogger().info("Found {} at {}", block.getType(), block.getLocation().toString());
                            if(block.getType() == Material.CAMPFIRE){
                                blocks.put(TeamManager.Team.RED, block);
                                plugin.getContext().getTeamManager().setTeamSpawn(TeamManager.Team.RED, block.getLocation());
                            } else if(block.getType() == Material.SOUL_CAMPFIRE){
                                blocks.put(TeamManager.Team.BLUE, block);
                                plugin.getContext().getTeamManager().setTeamSpawn(TeamManager.Team.BLUE, block.getLocation());
                            }else{
                                JavaPlugin.getPlugin(Sustain.class).getComponentLogger().warn("Unknown campfire type found: {}", block.getType());
                            }
                        }
                    }
                }
            }
        }
    }



}

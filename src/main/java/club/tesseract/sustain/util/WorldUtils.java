package club.tesseract.sustain.util;

import club.tesseract.sustain.Sustain;
import club.tesseract.sustain.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class WorldUtils {

    public static void reloadChunks(Sustain plugin){
        Bukkit.getWorlds().forEach(world -> {
            Set<Long> resetChunks = new HashSet<>();
            Set<TeamManager.Team> activeTeams = plugin.getContext().getTeamManager().getParticipatingTeams();
            activeTeams.forEach(team ->{
                TeamManager.TeamData data = plugin.getContext().getTeamManager().getTeamData(team);
                if(data.spawn() == null) return;
                Location spawn = data.spawn();
                Chunk originChunk = spawn.getChunk();
                int radius = 1;
                for(int x = -radius; x <= radius; x++){
                    for(int z = -radius; z <= radius; z++){
                        Chunk chunk = world.getChunkAt(originChunk.getX() + x, originChunk.getZ() + z);
                        if(resetChunks.contains(chunk.getChunkKey())){
                            continue;
                        }
                        resetChunks.add(chunk.getChunkKey());
                    }
                }
            });
            resetChunks.forEach(chunkKey -> {
                Chunk chunk = world.getChunkAt(chunkKey);
                boolean success = world.unloadChunk(chunk.getX(), chunk.getZ(), false);
                if(!success){
                    plugin.getLogger().warning("Failed to unload chunk at " + chunk.getX() + "," + chunk.getZ() + " in world " + world.getName());
                    return;
                }
                world.getChunkAtAsync(chunk.getX(), chunk.getZ());
            });
        });
    }

    public static void createFromTemplate(Sustain plugin, String worldName) {
        File file = Bukkit.getWorldContainer();
        Path worldPath = file.toPath().resolve(worldName);

        if(Files.exists(worldPath)){
            plugin.getLogger().info("World " + worldName + " already exists, skipping creation.");
            return;
        }

        // find folder called template_world
        File templateWorld = new File(file, "template_world");
        if (!templateWorld.exists() || !templateWorld.isDirectory()) {
            plugin.getLogger().warning("Template world folder not found: " + templateWorld.getAbsolutePath());
            return;
        }

        try {
            // copy template_world to worldName
            Path templatePath = templateWorld.toPath();
            Files.walk(templatePath)
                    .forEach(source -> {
                        try {
                            Path destination = worldPath.resolve(templatePath.relativize(source));
                            if (Files.isDirectory(source)) {
                                if (!Files.exists(destination)) {
                                    Files.createDirectory(destination);
                                }
                            } else {
                                Files.copy(source, destination);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error copying world file: " + e.getMessage());
                        }
                    });
            plugin.getLogger().info("World " + worldName + " created from template.");
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating world from template: " + e.getMessage());
        }
    }

}

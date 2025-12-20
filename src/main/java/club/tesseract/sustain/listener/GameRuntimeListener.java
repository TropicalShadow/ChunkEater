package club.tesseract.sustain.listener;

import club.tesseract.sustain.Sustain;
import club.tesseract.sustain.SustainContext;
import club.tesseract.sustain.TeamManager;
import club.tesseract.sustain.events.*;
import club.tesseract.sustain.points.PointActor;
import club.tesseract.sustain.points.PointLedger;
import club.tesseract.sustain.util.GameModeHelper;
import club.tesseract.sustain.util.MessageFx;
import club.tesseract.sustain.util.WorldUtils;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.papermc.paper.datacomponent.DataComponentHolder;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class GameRuntimeListener implements Listener {

    private static final int VOID_HEIGHT = 40;

    private final Sustain plugin;
    private final AtomicInteger dustTicker = new AtomicInteger(0);
    private final Set<TeamManager.Team> announcedDead = ConcurrentHashMap.newKeySet();

    public GameRuntimeListener(Sustain plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (plugin.getContext().isPaused()) return;

            for (TeamManager.Team value : plugin.getContext().getTeamManager().getParticipatingTeams()) {
                if (value.isSpectator()) continue;
                plugin.getContext().decreasePoint(
                        plugin.getContext().getTickRate(),
                        value,
                        PointActor.tick(System.currentTimeMillis()),
                        "TICK_DECAY"
                );
            }

            for (TeamManager.Team t : plugin.getContext().getTeamManager().getParticipatingTeams()) {
                if (t.isSpectator()) continue;
                long pts = plugin.getContext().getPoints(t);
                if (pts <= 0 && !announcedDead.contains(t)) {
                    TeamDeathEvent ev = new TeamDeathEvent(t, TeamDeathEvent.Reason.ZERO);
                    announcedDead.add(t);
                    plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                        ev.callEvent();
                        return null;
                    });
                }
            }

            GameFinishEvent event = plugin.getContext().isFinished();
            if (event != null) {
                plugin.getServer().getScheduler().callSyncMethod(
                        plugin,
                        event::callEvent
                );
            }
        }, 60, 20L);

        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            boolean isPaused = plugin.getContext().isPaused();
            HashMap<TeamManager.Team, Location> spawns = this.plugin.getContext().getTeamManager().getTeamSpawns();
            spawns.forEach((team, location) -> {
                if (location == null) return;
                Color colour = team.getBukkitColour();
                if (isPaused) {
                    colour = Color.GRAY;
                }
                int tick = dustTicker.getAndIncrement();
                int offset = tick % 10;
                if (dustTicker.get() > 10) {
                    dustTicker.set(0);
                }
                location.getWorld().spawnParticle(
                        Particle.DUST,
                        location.toCenterLocation(),
                        2,
                        0.0,
                        offset,
                        0.0,
                        0.0,
                        new Particle.DustOptions(colour, 10.0f)
                );
            });
        }, 20L, 5L);
    }


    @EventHandler
    public void onGameStart(GameStartEvent event) {
        if (!event.isResume()) {
            announcedDead.clear();
            // Lock in participating teams for this round: teams that have at least one assigned player now (exclude SPECTATOR)
            java.util.Set<TeamManager.Team> participating = new java.util.HashSet<>();
            for (TeamManager.Team t : TeamManager.Team.values()) {
                if (t.isSpectator()) continue;
                org.bukkit.scoreboard.Team bt = plugin.getContext().getTeamManager().getTeam(t);
                if (bt != null && !bt.getPlayers().isEmpty()) {
                    participating.add(t);
                }
            }
            plugin.getContext().getTeamManager().setParticipatingTeams(participating);
        }
        Bukkit.getOnlinePlayers().forEach(player -> {
            plugin.getContext().getActionBarScheduler()
                    .setFallbackMessageSupplier(player, plugin.getContext().actionBar());
        });
        if (!event.isResume()) {
            // Teleport players to their team spawns
            HashMap<TeamManager.Team, Location> spawns = plugin.getContext().getTeamManager().getTeamSpawns();
            for (Player player : Bukkit.getOnlinePlayers()) {
                TeamManager.Team team = plugin.getContext().getPlayerTeam(player);
                if (team.equals(TeamManager.Team.SPECTATOR)) {
                    Location loc = spawns.values().stream().filter(Objects::nonNull).findFirst().orElse(player.getWorld().getSpawnLocation());
                    player.getInventory().clear();
                    player.teleport(loc);
                    player.setRespawnLocation(loc);
                } else if (spawns.containsKey(team)) {
                    Location spawnLocation = spawns.get(team);
                    player.getInventory().clear();
                    player.teleport(spawnLocation);
                    player.setRespawnLocation(spawnLocation);
                    player.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_TELEPORT, Sound.Source.PLAYER, 1, 1), Sound.Emitter.self());
                    // send start title
                    player.showTitle(Title.title(Component.text("GO!", NamedTextColor.GREEN).decorate(TextDecoration.BOLD), Component.empty()));
                } else {
                    plugin.getLogger().severe("No spawn location found for team " + team.name() + ". Teleporting to world spawn.");
                    player.getInventory().clear();
                    player.setRespawnLocation(player.getWorld().getSpawnLocation());
                    player.teleport(player.getWorld().getSpawnLocation());
                }
            }
        }
        Bukkit.getWorlds().forEach(world -> {
            world.setGameRule(GameRules.PVP, true);
        });
        Bukkit.getOnlinePlayers().forEach(player -> {
            // set survival mode
            TeamManager.Team team = plugin.getContext().getPlayerTeam(player);
            MessageFx.info(player, Component.text("Game has started.", NamedTextColor.GRAY));
            if (team.isSpectator()) {
                GameModeHelper.setGameMode(player, GameMode.SPECTATOR);
                return;
            }

            if (plugin.getContext().isPlayerDead(player)) {
                return;
            }
            GameModeHelper.setGameMode(player, GameMode.SURVIVAL);
        });
    }

    @EventHandler
    public void onPlayerRevive(PlayerRespawnCompleteEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getContext().isDirty()) return;
        TeamManager.Team team = plugin.getContext().getPlayerTeam(player);
        Optional<Location> respawnPoint = plugin.getContext().getTeamManager().getTeamSpawn(team);
        Location location = respawnPoint.orElse(player.getWorld().getSpawnLocation());
        player.setInvulnerable(false);
        player.teleport(location);
        GameModeHelper.setGameMode(player, GameMode.SURVIVAL);
    }

    @EventHandler
    public void onGamePause(GamePauseEvent event) {
        // disable pvp
        Bukkit.getWorlds().forEach(world -> {
            world.setGameRule(GameRules.PVP, false);
        });
        Bukkit.getOnlinePlayers().forEach(player -> {
            // set adventure mode
            TeamManager.Team team = plugin.getContext().getPlayerTeam(player);
            MessageFx.warn(player, Component.text("Game has been paused.", NamedTextColor.GRAY));
            if (team.isSpectator()) {
                return;
            }
            if (player.getGameMode() != GameMode.ADVENTURE) {
                GameModeHelper.setGameMode(player, GameMode.ADVENTURE);
            }
        });
    }

    @EventHandler
    public void gameFinish(GameFinishEvent e) {

        for (Player player : Bukkit.getOnlinePlayers()) {
            GameModeHelper.setGameMode(player, GameMode.SPECTATOR);
        }
        Bukkit.getWorlds().forEach(world -> {
            world.setGameRule(GameRules.PVP, false);
        });

        TeamManager.Team team = e.getWinningTeam();

        if (e.getState() == GameFinishEvent.FINISH_STATE.TEAM_WIN) {
            if (team == null) {
                plugin.getLogger().severe("GameFinishEvent state is TEAM_WIN but winning team is null!");
                return;
            }
            Team bukkitTeam = plugin.getContext().getTeamManager().getTeam(team);
            Set<OfflinePlayer> players = bukkitTeam.getPlayers();
            List<? extends Player> losers = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !players.contains(p))
                    .toList();
            List<? extends Player> winners = Bukkit.getOnlinePlayers().stream()
                    .filter(players::contains)
                    .toList();
            Component winnerTitle = Component.textOfChildren(
                    team.displayName().color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                    Component.space(),
                    Component.text("Team", NamedTextColor.YELLOW),
                    Component.space(),
                    Component.text("Wins!", NamedTextColor.YELLOW)
            );
            Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(Component.empty(), winnerTitle));

            MessageFx.successEach(winners, Component.text("Congratulations! You have won the game!", NamedTextColor.GOLD));
            MessageFx.warnEach(losers, Component.text("Better luck next time! The " + team.name() + " team has won the game.", NamedTextColor.RED));
        } else if (e.getState() == GameFinishEvent.FINISH_STATE.TIE) {
            Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(
                    Component.text("Draw!", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                    Component.text("No team has won the game.", NamedTextColor.GRAY)
            ));
            MessageFx.audienceInfo(Audience.audience(Bukkit.getOnlinePlayers()), Component.text("The game has ended in a draw!", NamedTextColor.YELLOW));
        } else if (e.getState() == GameFinishEvent.FINISH_STATE.FORCE_END) {
            Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(
                    Component.text("Game Ended!", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    Component.text("The game was ended by an administrator.", NamedTextColor.GRAY)
            ));
            MessageFx.audienceInfo(Audience.audience(Bukkit.getOnlinePlayers()), Component.text("The game was ended by an administrator.", NamedTextColor.RED));
        }

        MessageFx.audienceInfo(Audience.audience(Bukkit.getOnlinePlayers()), Component.text("Returning to lobby in 10 seconds...", NamedTextColor.GRAY));
        // Export ledger to JSON before we reset
        try {
            File exportsDir = new File(plugin.getDataFolder(), "exports");
            String fileName = PointLedger.defaultJsonFileName(Date::new);
            File out = new File(exportsDir, fileName);
            plugin.getContext().getPointLedger().writeJson(out);
            plugin.getLogger().info("Ledger exported to: " + out.getAbsolutePath());
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to export ledger JSON: " + ex.getMessage());
        }
        plugin.getContext().getPointLedger().reset();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Teleport players to world spawn, clear their teams, gamemode to adventure
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(player.getWorld().getSpawnLocation());
                GameModeHelper.setGameMode(player, GameMode.ADVENTURE);
                player.getInventory().clear();
                player.heal(player.getAttribute(Attribute.MAX_HEALTH).getDefaultValue());
                plugin.getContext().addPlayerToTeam(player, TeamManager.Team.SPECTATOR);
                MessageFx.info(player, Component.text("You have been returned to the lobby.", NamedTextColor.GRAY));
            }
            plugin.getContext().getTeamManager().getTeamSpawns().values().forEach(loc ->{
                if(loc == null)return;
                Block block = loc.getBlock();
                if (!(block.getBlockData() instanceof Campfire campfire)) return;
                campfire.setLit(true);
            });

            announcedDead.clear();
            // Clear participating teams for next round setup
            plugin.getContext().getTeamManager().clearParticipatingTeams();
            plugin.getContext().resetDirty();
        }, 20 * 10L); // 10 seconds delay
    }


    @EventHandler
    public void onhunger(FoodLevelChangeEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerVoid(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();

        if (player.getLocation().getY() > VOID_HEIGHT) return;
        if (!plugin.getContext().isDirty()) return; // game hasn't started yet

        if (plugin.getContext().isPaused()) {
            player.setFallDistance(0);
            player.teleport(player.getWorld().getSpawnLocation());
            player.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_TELEPORT, Sound.Source.PLAYER, 1, 1), Sound.Emitter.self());
            return;
        }

        TeamManager.Team team = plugin.getContext().getPlayerTeam(player);
        HashMap<TeamManager.Team, Location> spawns = plugin.getContext().getTeamManager().getTeamSpawns();

        if (team.isSpectator()) {
            // teleport to random spawn or world spawn
            player.setFallDistance(0);
            player.teleport(spawns.values().stream().findAny().orElse(player.getWorld().getSpawnLocation()));
            player.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_TELEPORT, Sound.Source.PLAYER, 1, 1), Sound.Emitter.self());
            return;
        }


        if (spawns.containsKey(team)) {
            player.setFallDistance(0);
            player.damage(100, DamageSource.builder(DamageType.OUT_OF_WORLD).build());
            MessageFx.info(player, Component.text("You have been teleported back to your spawn point.", NamedTextColor.GRAY));
        } else {
            player.setFallDistance(0);
            player.damage(100, DamageSource.builder(DamageType.OUT_OF_WORLD).build());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType().equals(Material.CAMPFIRE) || block.getType().equals(Material.SOUL_CAMPFIRE)) {
            MessageFx.warn(event.getPlayer(), Component.text("You cannot break campfires!", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onOpenInventory(InventoryOpenEvent event){
        HumanEntity human = event.getPlayer();
        if(!(human instanceof Player player)) return;
        if(plugin.getContext().isPlayerDead(player) || plugin.getContext().getPlayerTeam(player).isSpectator()){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event){
        Player player = event.getPlayer();
        if(plugin.getContext().isPlayerDead(player) || plugin.getContext().getPlayerTeam(player).isSpectator()){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupEXP(PlayerPickupExperienceEvent event){
        Player player = event.getPlayer();
        if(plugin.getContext().isPlayerDead(player) || plugin.getContext().getPlayerTeam(player).isSpectator()){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickup(PlayerAttemptPickupItemEvent event){
        Player player = event.getPlayer();
        if(plugin.getContext().isPlayerDead(player) || plugin.getContext().getPlayerTeam(player).isSpectator()){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        if (!plugin.getContext().isDirty()) return;
        if (plugin.getContext().isPaused()) return;

        for (Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext(); ) {
            ItemStack drop = iterator.next();
            if (keepItemOnDeath(drop)) {
                iterator.remove();
                event.getItemsToKeep().add(drop);
            }
        }
    }

    @EventHandler
    public void respawnLocationEvent(PlayerRespawnEvent event){
        if (!plugin.getContext().isDirty()) return;
        if (plugin.getContext().isPaused()) return;
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH) return;
        Player player = event.getPlayer();

        event.setRespawnLocation(player.getLocation());
    }

    @EventHandler
    public void onPlayerRespawnEvent(PlayerPostRespawnEvent event) {
        if (!plugin.getContext().isDirty()) return;
        if (plugin.getContext().isPaused()) return;
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH) return;
        Player player = event.getPlayer();
        // spectate mode
        plugin.getContext().onPlayerDeath(player);
        TeamManager.Team team = plugin.getContext().getPlayerTeam(player);
        Optional<Location> loc = plugin.getContext().getTeamManager().getTeamSpawn(team);
        EntityDamageEvent cause = player.getLastDamageCause();
        if(cause == null)return;
        if(cause.getDamageSource().getDamageType().equals(DamageType.OUT_OF_WORLD)){
            loc.ifPresent(player::teleport);
        }
    }

    private boolean keepItemOnDeath(ItemStack item) {
        Material type = item.getType();
        if (type.isBlock()) return false;
        if (type.getEquipmentSlot().isArmor()) return true;
        return isTool(item);
    }

    public boolean isTool(ItemStack item) {
        if (item == null) return false;

        Material type = item.getType();
        return switch (type) {
            case WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE,
                 WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL,
                 WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE,
                 WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE, DIAMOND_HOE, NETHERITE_HOE,
                 WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD,
                 BOW, CROSSBOW, MACE, TRIDENT,
                 WOODEN_SPEAR, STONE_SPEAR, IRON_SPEAR, GOLDEN_SPEAR, DIAMOND_SPEAR, NETHERITE_SPEAR,
                 SHEARS, FLINT_AND_STEEL, SHIELD -> true;
            default -> false;
        };
    }


    @EventHandler
    public void onTeamDeath(TeamDeathEvent e) {
        TeamManager.Team team = e.getTeam();
        TeamManager.TeamData data = plugin.getContext().getTeamManager().getTeamData(team);
        if(data == null) {
            throw new RuntimeException("No data found for team " + team.name());
        }
        Team bukkitTeam = data.bukkitTeam();
        if (bukkitTeam == null) return;
        for (OfflinePlayer op : bukkitTeam.getPlayers()) {
            if (op.isOnline()) {
                Player player = (Player) op;
                player.getInventory().clear();
                GameModeHelper.setGameMode(player, GameMode.SPECTATOR);
                MessageFx.error(player, Component.text("Your team has been eliminated!", NamedTextColor.RED));
            }
        }
        Bukkit.broadcast(Component.textOfChildren(
                team.displayName(),
                Component.space(),
                Component.text("has been eliminated from the game!", NamedTextColor.GRAY)
        ));
        if (data.spawn() == null) return;// goofy bug safety check
        Block campfireBlock = data.spawn().getBlock();
        if (!(campfireBlock.getBlockData() instanceof Campfire campfire)) return;
        campfire.setLit(false);
        campfireBlock.setBlockData(campfire);
    }


    @EventHandler
    public void onRejoin(PlayerJoinEvent e) {
        if (!plugin.getContext().isDirty()) return; // game hasn't started yet
        Player player = e.getPlayer();
        plugin.getContext().getActionBarScheduler()
                .setFallbackMessageSupplier(player, plugin.getContext().actionBar());
        // get player's team check if they are supposed to be spectator
        TeamManager.Team team = plugin.getContext().getPlayerTeam(player);
        if (team.isSpectator()) {
            player.getInventory().clear();
            GameModeHelper.setGameMode(player, GameMode.SPECTATOR);
            e.joinMessage(null);
            return;
        }

        if (announcedDead.contains(team)) {
            player.getInventory().clear();
            GameModeHelper.setGameMode(player, GameMode.SPECTATOR);
            e.joinMessage(null);
            return;
        }

        if (plugin.getContext().isPlayerDead(player)) {
            GameModeHelper.setGameMode(player, GameMode.SPECTATOR);
            return;
        }

        Optional<Location> location = plugin.getContext().getTeamManager().getTeamSpawn(team);
        if (!plugin.getContext().isPaused()) {
            GameModeHelper.setGameMode(player, GameMode.SURVIVAL);
            player.teleport(location.orElse(player.getWorld().getSpawnLocation()));
        } else {
            player.teleport(location.orElse(player.getWorld().getSpawnLocation()));
            GameModeHelper.setGameMode(player, GameMode.ADVENTURE);
        }

    }

    @EventHandler
    public void onDamage(EntityDamageByBlockEvent e) {
        Block damager = e.getDamager();
        if (damager == null) return; // most likely void dmg

        // Cancel damage and set it to 0 when the damager is a campfire or soul campfire
        Material type = damager.getType();
        if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
            e.setDamage(0);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpectatorDamaging(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager)) return;
        TeamManager.Team team = plugin.getContext().getPlayerTeam(damager);
        if (!team.isSpectator() && !plugin.getContext().isPlayerDead(damager)) return;
        e.setDamage(0);
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block blockAgainst = event.getBlockAgainst();
        if (blockAgainst.getType() != Material.CAMPFIRE && blockAgainst.getType() != Material.SOUL_CAMPFIRE) {
            return;
        }
        if (plugin.getContext().isPaused()) return;
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();
        if (!itemInHand.getType().isBlock()) return;
        if (player.isSneaking()) return;

        TeamManager.Team team;

        if (blockAgainst.getType().equals(Material.CAMPFIRE) || blockAgainst.getType().equals(Material.SOUL_CAMPFIRE)) {
            team = plugin.getContext().getTeamManager().getTeamFromCampfire(blockAgainst);
        } else {
            return;
        }

        event.setCancelled(false);
        event.setBuild(false);
        player.getInventory().remove(itemInHand);
        long duration = blockToValue(itemInHand.getType()) * itemInHand.getAmount();
        plugin.getContext().increasePoint(
                duration,
                team,
                PointActor.player(event.getPlayer()),
                "CAMPFIRE_FEED"
        );
        MessageFx.success(player, Component.text("Points gained " + SustainContext.toTimeString(duration), NamedTextColor.GREEN));
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {

        // TODO - combine with block place event to reduce code duplication?
        // TODO - stop eating blocks if game is paused or team is dead
        Player player = event.getPlayer();
        Item item = event.getItemDrop();
        if (!item.getItemStack().getType().isBlock()) return;
        if (plugin.getContext().isPaused()) return;
        item.getScheduler().runAtFixedRate(plugin, (task) -> {
            if (item.isOnGround()) {

                // get block under item
                Location loc = item.getLocation();
                World world = item.getWorld();
                Block block = world.getBlockAt(loc);
                Block blockUnder = world.getBlockAt(loc.subtract(0, 1, 0));

                TeamManager.Team team = null;

                if (block.getType().equals(Material.CAMPFIRE) || blockUnder.getType().equals(Material.CAMPFIRE)) {
                    team = plugin.getContext().getTeamManager().getTeamFromCampfire(block);
                } else if (block.getType().equals(Material.SOUL_CAMPFIRE) || blockUnder.getType().equals(Material.SOUL_CAMPFIRE)) {
                    team = plugin.getContext().getTeamManager().getTeamFromCampfire(block);
                } else {
                    return;
                }
                if (team == null) {
                    return;
                }

                if (item.getItemStack().getType().equals(Material.BARRIER)) {
                    plugin.getContext().resetPoints(team, PointActor.player(player), "RESET_BY_ITEM");
                    MessageFx.error(player, Component.text("Points reset", NamedTextColor.RED));
                    item.remove();
                    return;
                }
                if (item.getItemStack().getType().equals(Material.COMMAND_BLOCK)) {
                    plugin.getContext().decreasePoint(10, team, PointActor.player(player), "COMMAND_BLOCK_PENALTY");
                    MessageFx.warn(player, Component.text("10 points deducted", NamedTextColor.YELLOW));
                    item.remove();
                    return;
                }

                long f = blockToValue(item.getItemStack().getType()) * item.getItemStack().getAmount();
                long result = plugin.getContext().increasePoint(
                        f,
                        team,
                        PointActor.player(player),
                        "ITEM_FEED"
                );
                world.playSound(block.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                MessageFx.success(player, Component.text("You now have " + SustainContext.toTimeString(result) + " sustain points", NamedTextColor.GREEN));
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
    public static long blockToValue(Material material) {
        Sustain.getPlugin().getLogger().info(">" + material.getHardness() + " " + material.name());
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

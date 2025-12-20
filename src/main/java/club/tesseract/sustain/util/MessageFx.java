package club.tesseract.sustain.util;

import co.aikar.commands.CommandIssuer;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Small helper for sending messages paired with a subtle sound and optional particles.
 * Keeps UX consistent and avoids duplicating boilerplate in listeners/commands.
 */
public final class MessageFx {

    private MessageFx() {}

    // Preset sounds
    private static final Sound INFO_SND = Sound.sound(SoundEventKeys.UI_BUTTON_CLICK, Sound.Source.PLAYER, 0.7f, 1.2f);
    private static final Sound SUCCESS_SND = Sound.sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP, Sound.Source.PLAYER, 0.8f, 1.1f);
    private static final Sound WARN_SND = Sound.sound(SoundEventKeys.BLOCK_NOTE_BLOCK_BASS, Sound.Source.PLAYER, 0.6f, 0.8f);
    private static final Sound ERROR_SND = Sound.sound(SoundEventKeys.BLOCK_ANVIL_LAND, Sound.Source.PLAYER, 0.4f, 1.0f);

    public static void info(Player player, Component msg) {
        to(player, msg, INFO_SND, null);
    }

    public static void success(Player player, Component msg) {
        to(player, msg, SUCCESS_SND, new Particle.DustOptions(Color.LIME, 1.0f));
    }

    public static void warn(Player player, Component msg) {
        to(player, msg, WARN_SND, new Particle.DustOptions(Color.YELLOW, 1.0f));
    }

    public static void error(Player player, Component msg) {
        to(player, msg, ERROR_SND, new Particle.DustOptions(Color.RED, 1.0f));
    }

    public static void to(Player player, Component msg, @Nullable Sound sound, @Nullable Particle.DustOptions dust) {
        if (player == null) return;
        player.sendMessage(msg);
        if (sound != null) {
            player.playSound(sound, net.kyori.adventure.sound.Sound.Emitter.self());
        }
        if (dust != null) {
            // Small burst above the player head
            var loc = player.getLocation().clone().add(0, 1.6, 0);
            player.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.15, 0.15, 0.15, 0.0, dust);
        }
    }

    public static void toEach(Collection<? extends Player> players, Component msg, @Nullable Sound sound, @Nullable Particle.DustOptions dust) {
        if (players == null) return;
        for (Player p : players) {
            to(p, msg, sound, dust);
        }
    }

    public static void audienceInfo(Audience audience, Component msg) {
        if (audience == null) return;
        audience.sendMessage(msg);
        // Also play a subtle click for all online players (audience could be broadcast)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(INFO_SND, Sound.Emitter.self());
        }
    }

    /**
     * Send a message to a CommandIssuer, playing a subtle info sound if issuer is a player.
     */
    public static void issuerInfo(CommandIssuer issuer, Component msg) {
        if (issuer == null) return;
        Object raw = issuer.getIssuer();
        if (raw instanceof Audience aud) {
            aud.sendMessage(msg);
        } else if (raw instanceof org.bukkit.command.CommandSender cs) {
            cs.sendMessage(msg);
        } else {
            issuer.sendMessage(msg.toString());
        }
        if (raw instanceof Player p) {
            p.playSound(INFO_SND, Sound.Emitter.self());
        }
    }

    public static void issuerInfo(CommandIssuer issuer, String msg) {
        issuerInfo(issuer, Component.text(msg));
    }

    // Convenience: batch variants with presets
    public static void infoEach(Collection<? extends Player> players, Component msg) {
        toEach(players, msg, INFO_SND, null);
    }

    public static void successEach(Collection<? extends Player> players, Component msg) {
        toEach(players, msg, SUCCESS_SND, new Particle.DustOptions(Color.LIME, 1.0f));
    }

    public static void warnEach(Collection<? extends Player> players, Component msg) {
        toEach(players, msg, WARN_SND, new Particle.DustOptions(Color.YELLOW, 1.0f));
    }

    public static void errorEach(Collection<? extends Player> players, Component msg) {
        toEach(players, msg, ERROR_SND, new Particle.DustOptions(Color.RED, 1.0f));
    }
}

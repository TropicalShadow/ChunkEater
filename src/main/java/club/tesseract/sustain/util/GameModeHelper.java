package club.tesseract.sustain.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.WeakHashMap;

public class GameModeHelper {

    private static final WeakHashMap<Player, ItemStack[]> savedInventories = new WeakHashMap<>();

    public static void setGameMode(Player player, GameMode gameMode) {
        if (gameMode == GameMode.SPECTATOR) {
            if(!player.getInventory().isEmpty() && !savedInventories.containsKey(player)){
                savedInventories.put(player, player.getInventory().getContents());
                player.getInventory().clear();
            }

            player.setGameMode(GameMode.ADVENTURE);

            player.setAllowFlight(true);
            player.setFlying(true);
            player.setInvisible(true);
            player.setInvulnerable(true);

            player.heal(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getDefaultValue());
            player.setStarvationRate(0);
            player.setFoodLevel(20);
            player.sendMessage(Component.textOfChildren(
                    Component.text("You are now in ", NamedTextColor.GRAY),
                    Component.text("SPECTATOR", NamedTextColor.AQUA, TextDecoration.BOLD),
                    Component.text(" mode.", NamedTextColor.GRAY)
            ));

            return;
        }
        player.setAllowFlight(gameMode == GameMode.CREATIVE);
        player.setFlying(gameMode == GameMode.CREATIVE && player.isFlying());
        player.setInvisible(false);
        player.setInvulnerable(gameMode.isInvulnerable());
        player.setStarvationRate(80);

        if (savedInventories.containsKey(player)) {
            ItemStack[] contents = savedInventories.get(player);
            player.getInventory().setContents(contents);
            savedInventories.remove(player);
        }

        player.setGameMode(gameMode);
        player.sendMessage(Component.textOfChildren(
                Component.text("Your game mode has been set to ", NamedTextColor.GRAY),
                Component.text(gameMode.name(), NamedTextColor.AQUA, TextDecoration.BOLD),
                Component.text(".", NamedTextColor.GRAY)
        ));

    }

}

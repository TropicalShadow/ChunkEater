package club.tesseract.sustain;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PapiExtension extends PlaceholderExpansion {

    private final Sustain plugin;

    public PapiExtension(Sustain plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "chunk-eater";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TropicalShadow";
    }

    @Override
    public @NotNull String getVersion() {
        return "0.0.1";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase()) {
            case "red_time":
                return SustainContext.toTimeString(plugin.getContext().getPoints(TeamManager.Team.RED));
            case "blue_time":
                return SustainContext.toTimeString(plugin.getContext().getPoints(TeamManager.Team.BLUE));
            default:
                return null;
        }
    }
}

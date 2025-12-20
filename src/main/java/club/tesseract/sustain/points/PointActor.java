package club.tesseract.sustain.points;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * Sealed hierarchy for the actor responsible for a point change.
 */
public sealed interface PointActor permits PointActor.PlayerActor, PointActor.ConsoleActor, PointActor.TickActor {

    String type();

    /**
     * A stable identifier for the actor when applicable (e.g., player UUID). May be null for console.
     */
    String id();

    /**
     * A human-friendly display name for the actor.
     */
    String displayName();

    static PlayerActor player(Player player) {
        Objects.requireNonNull(player, "player");
        return new PlayerActor(player.getUniqueId(), player.getName());
    }

    static ConsoleActor console() {
        return ConsoleActor.INSTANCE;
    }

    static TickActor tick(long tickTag) {
        return new TickActor(tickTag);
    }

    /**
     * Player actor wrapper holding UUID and name snapshot.
     */
    record PlayerActor(UUID uuid, String name) implements PointActor {
        public PlayerActor {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(name, "name");
        }

        @Override
        public String type() { return "PLAYER"; }

        @Override
        public String id() { return uuid.toString(); }

        @Override
        public String displayName() { return name; }
    }

    /**
     * Console/system actor singleton.
     */
    enum ConsoleActor implements PointActor {
        INSTANCE;
        @Override
        public String type() { return "CONSOLE"; }
        @Override
        public String id() { return null; }
        @Override
        public String displayName() { return "CONSOLE"; }
    }

    /**
     * Actor representing automatic tick-based decay or similar.
     */
    record TickActor(long tag) implements PointActor {
        @Override
        public String type() { return "TICK"; }
        @Override
        public String id() { return Long.toString(tag); }
        @Override
        public String displayName() { return "TICK#" + tag; }
    }
}

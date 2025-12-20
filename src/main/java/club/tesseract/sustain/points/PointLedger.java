package club.tesseract.sustain.points;

import club.tesseract.sustain.TeamManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

/**
 * Basic per-team points ledger that tracks who caused a points change,
 * the delta (+/-), timestamp, reason, and resulting total.
 */
public class PointLedger {

    private final Map<TeamManager.Team, Deque<Entry>> teamEntries = new EnumMap<>(TeamManager.Team.class);
    private final int maxEntriesPerTeam;

    public PointLedger() {
        this(256); // keep a reasonable recent history by default
    }

    public PointLedger(int maxEntriesPerTeam) {
        this.maxEntriesPerTeam = Math.max(1, maxEntriesPerTeam);
        for (TeamManager.Team team : TeamManager.Team.values()) {
            teamEntries.put(team, new ArrayDeque<>());
        }
    }

    /**
     * Add a ledger entry for a team.
     *
     * @param team           team affected
     * @param actor          actor who caused the change (player/console/tick)
     * @param delta          positive for increase, negative for decrease
     * @param reason         short reason label
     * @param resultingTotal the team's resulting points total after applying the delta
     */
    public synchronized void addEntry(TeamManager.Team team, PointActor actor, long delta, String reason, long resultingTotal) {
        if (team == null) return;
        if (actor == null) actor = PointActor.ConsoleActor.INSTANCE;
        Entry entry = Entry.from(team, actor, delta, reason, resultingTotal);
        Deque<Entry> deque = teamEntries.get(team);
        if (deque == null) return;
        deque.addFirst(entry);
        // trim
        while (deque.size() > maxEntriesPerTeam) {
            deque.removeLast();
        }
    }

    /**
     * Get an immutable snapshot of entries for a team, most-recent first.
     */
    public synchronized List<Entry> getEntries(TeamManager.Team team) {
        Deque<Entry> deque = teamEntries.get(team);
        if (deque == null) return Collections.emptyList();
        return List.copyOf(deque);
    }

    /**
     * Get up to {@code limit} most-recent entries for a team.
     */
    public synchronized List<Entry> getRecentEntries(TeamManager.Team team, int limit) {
        if (limit <= 0) return Collections.emptyList();
        Deque<Entry> deque = teamEntries.get(team);
        if (deque == null || deque.isEmpty()) return Collections.emptyList();
        List<Entry> out = new ArrayList<>(Math.min(limit, deque.size()));
        int i = 0;
        for (Entry e : deque) {
            if (i++ >= limit) break;
            out.add(e);
        }
        return out;
    }
    
    public synchronized void reset(){
        teamEntries.clear();
    }
    
    public synchronized Object snapshot(){
        Map<TeamManager.Team, List<Entry>> snapshot = new EnumMap<>(TeamManager.Team.class);
        for(TeamManager.Team team : TeamManager.Team.values()){
            Deque<Entry> deque = teamEntries.get(team);
            if(deque == null || deque.isEmpty()){
                snapshot.put(team, Collections.emptyList());
            } else {
                snapshot.put(team, List.copyOf(deque));
            }
        }
        return snapshot;
    }

    /**
     * Snapshot as a strongly typed map (used for JSON export).
     */
    public synchronized Map<TeamManager.Team, List<Entry>> snapshotMap() {
        Map<TeamManager.Team, List<Entry>> snapshot = new EnumMap<>(TeamManager.Team.class);
        for (TeamManager.Team team : TeamManager.Team.values()) {
            Deque<Entry> deque = teamEntries.get(team);
            if (deque == null || deque.isEmpty()) {
                snapshot.put(team, Collections.emptyList());
            } else {
                snapshot.put(team, List.copyOf(deque));
            }
        }
        return snapshot;
    }

    /**
     * Returns a pretty-printed JSON string of the current snapshot.
     */
    public synchronized String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(snapshotMap());
    }

    /**
     * Writes the current snapshot to the given file as JSON (pretty printed).
     */
    public synchronized void writeJson(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            // noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(snapshotMap(), writer);
        }
    }

    /**
     * Utility to generate a default filename with timestamp: ledger-YYYYMMDD-HHMMSS.json
     */
    public static String defaultJsonFileName(Supplier<Date> clock) {
        Date now = clock == null ? new Date() : clock.get();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return "ledger-" + fmt.format(now) + ".json";
    }

    /**
     * Immutable ledger entry.
     */
    public static final class Entry {
        public final long timestampMillis;
        public final TeamManager.Team team;
        public final long delta;
        public final long resultingTotal;
        public final String reason;
        public final String actorType;
        public final String actorId; // may be null for console
        public final String actorName;

        private Entry(long timestampMillis,
                      TeamManager.Team team,
                      long delta,
                      long resultingTotal,
                      String reason,
                      String actorType,
                      String actorId,
                      String actorName) {
            this.timestampMillis = timestampMillis;
            this.team = team;
            this.delta = delta;
            this.resultingTotal = resultingTotal;
            this.reason = reason == null ? "" : reason;
            this.actorType = actorType;
            this.actorId = actorId;
            this.actorName = actorName;
        }

        public static Entry from(TeamManager.Team team, PointActor actor, long delta, String reason, long resultingTotal) {
            String id = actor.id();
            String name = actor.displayName();
            String type = actor.type();
            return new Entry(System.currentTimeMillis(), team, delta, resultingTotal, reason, type, id, name);
        }
    }
}

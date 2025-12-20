package club.tesseract.sustain.points;

import club.tesseract.sustain.TeamManager;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Encapsulates per-team sustain points and logging via PointLedger.
 * All mutations are logged with explicit reasons and actors.
 */
public class PointHoarder {

    private final long initialPoints; // default starting points (seconds)
    private final Map<TeamManager.Team, AtomicLong> pointsMap = new EnumMap<>(TeamManager.Team.class);
    private final PointLedger ledger = new PointLedger();

    public PointHoarder() {
        this(10 * 60); // 10 minutes in seconds by default
    }

    public PointHoarder(long initialPoints) {
        this.initialPoints = Math.max(0, initialPoints);
    }

    public synchronized long getPoints(TeamManager.Team team) {
        return pointsMap.computeIfAbsent(team, k -> new AtomicLong(initialPoints)).get();
    }

    public synchronized void setPoints(TeamManager.Team team, long points) {
        pointsMap.computeIfAbsent(team, k -> new AtomicLong(initialPoints)).set(Math.max(0, points));
    }

    public synchronized long increase(long delta, TeamManager.Team team, PointActor actor, String reason) {
        long newTotal = pointsMap
                .computeIfAbsent(team, k -> new AtomicLong(initialPoints))
                .addAndGet(Math.max(0, delta));
        ledger.addEntry(team, actor, Math.max(0, delta), reason, newTotal);
        return newTotal;
    }

    public synchronized long decrease(long delta, TeamManager.Team team, PointActor actor, String reason) {
        long nonNeg = Math.max(0, delta);
        AtomicLong counter = pointsMap.computeIfAbsent(team, k -> new AtomicLong(initialPoints));
        long newTotal = counter.updateAndGet(cur -> Math.max(cur - nonNeg, 0));
        ledger.addEntry(team, actor, -nonNeg, reason, newTotal);
        return newTotal;
    }

    public synchronized long reset(TeamManager.Team team, PointActor actor, String reason) {
        AtomicLong counter = pointsMap.computeIfAbsent(team, k -> new AtomicLong(initialPoints));
        long before = counter.getAndSet(initialPoints);
        long delta = initialPoints - before;
        ledger.addEntry(team, actor, delta, reason, initialPoints);
        return initialPoints;
    }

    public PointLedger getPointLedger() {
        return ledger;
    }

    public List<PointLedger.Entry> getRecentEntries(TeamManager.Team team, int limit) {
        return ledger.getRecentEntries(team, limit);
    }
}

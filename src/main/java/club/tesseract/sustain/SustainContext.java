package club.tesseract.sustain;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SustainContext {

    public final static long DEFAULT_TIME = 10 * 60;

    private final AtomicLong BLUE_POINTS = new AtomicLong(DEFAULT_TIME);
    private final AtomicLong RED_POINTS = new AtomicLong(DEFAULT_TIME);
    private final AtomicBoolean GAME_PAUSED = new AtomicBoolean(true);

    public SustainContext() {

    }

    @Nullable
    public synchronized GameFinishEvent isFinished() {
        long bluePoints = BLUE_POINTS.get();
        long redPoints = RED_POINTS.get();
        if (bluePoints <= 0 || redPoints <= 0) {
            GameFinishEvent.FINISH_STATE state = GameFinishEvent.FINISH_STATE.TIE;
            if (bluePoints <= 0 && redPoints <= 0) {
                state = GameFinishEvent.FINISH_STATE.TIE;
            } else if (bluePoints <= 0) {
                state = GameFinishEvent.FINISH_STATE.BLUE_WIN;
            } else {
                state = GameFinishEvent.FINISH_STATE.RED_WIN;
            }
            return new GameFinishEvent(state);
        }
        return null;
    }

    public synchronized long increasePoint(long point, boolean isBlue) {
        if (isBlue) {
            return BLUE_POINTS.addAndGet(point);
        }
        return RED_POINTS.addAndGet(point);
    }

    public synchronized long decreasePoint(long point, boolean isBlue) {
        if (isBlue) {
            return BLUE_POINTS.updateAndGet((num) -> Math.max(num - 1, 0));
        }
        return RED_POINTS.updateAndGet((num) -> Math.max(num - 1, 0));
    }

    public synchronized long resetPoints(boolean isBlue) {
        if (isBlue) {
            BLUE_POINTS.set(DEFAULT_TIME);
        } else {
            RED_POINTS.set(DEFAULT_TIME);
        }
        return DEFAULT_TIME;
    }

    public long getPoints(boolean isBlue) {
        return isBlue ? BLUE_POINTS.get() : RED_POINTS.get();
    }

    public boolean isPaused() {
        return GAME_PAUSED.get();
    }

    public void pause() {
        GAME_PAUSED.set(true);
    }

    public void resume() {
        GAME_PAUSED.set(false);
    }

    public static String toTimeString(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secondsLeft = seconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secondsLeft);
        }

        return String.format("%02d:%02d", minutes, secondsLeft);
    }

}

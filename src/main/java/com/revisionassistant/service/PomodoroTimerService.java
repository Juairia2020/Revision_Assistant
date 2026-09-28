package com.revisionassistant.service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

import java.util.function.IntConsumer;

/**
 * Application-wide Pomodoro timer. The timer state lives here, not in a
 * view controller, so it keeps counting while the user moves between pages
 * and only stops when the app closes (or the user resets/logs out).
 * Must be used from the JavaFX application thread.
 */
public final class PomodoroTimerService {

    public enum Mode { WORK, SHORT_BREAK, LONG_BREAK }

    private static final int SESSIONS_BEFORE_LONG_BREAK = 4;
    private static final PomodoroTimerService INSTANCE = new PomodoroTimerService();

    public static PomodoroTimerService getInstance() {
        return INSTANCE;
    }

    private final IntegerProperty secondsRemaining = new SimpleIntegerProperty();
    private final IntegerProperty totalSeconds = new SimpleIntegerProperty();
    private final IntegerProperty sessionCount = new SimpleIntegerProperty(0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.WORK);
    private final StringProperty status = new SimpleStringProperty("Ready to focus?");

    private int workMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;

    private final Timeline ticker;
    private long endNanos;
    private IntConsumer onWorkSessionComplete;

    private PomodoroTimerService() {
        ticker = new Timeline(new KeyFrame(Duration.millis(250), e -> tick()));
        ticker.setCycleCount(Timeline.INDEFINITE);
        loadModeDuration();
    }

    // ----- observable state ---------------------------------------------
    public IntegerProperty secondsRemainingProperty() { return secondsRemaining; }
    public IntegerProperty totalSecondsProperty() { return totalSeconds; }
    public IntegerProperty sessionCountProperty() { return sessionCount; }
    public BooleanProperty runningProperty() { return running; }
    public ObjectProperty<Mode> modeProperty() { return mode; }
    public StringProperty statusProperty() { return status; }

    public boolean isRunning() { return running.get(); }
    public Mode getMode() { return mode.get(); }
    public int getWorkMinutes() { return workMinutes; }
    public int getShortBreakMinutes() { return shortBreakMinutes; }
    public int getLongBreakMinutes() { return longBreakMinutes; }
    public void setStatus(String text) { status.set(text); }

    /** Called (on the FX thread) with the length of every finished work session. */
    public void setOnWorkSessionComplete(IntConsumer handler) { this.onWorkSessionComplete = handler; }

    /** True while a focus session is counting down, or paused part-way through. */
    public boolean isFocusSessionActive() {
        return mode.get() == Mode.WORK
                && (running.get() || secondsRemaining.get() < totalSeconds.get());
    }

    // ----- commands -----------------------------------------------------
    public void startPause() {
        if (running.get()) {
            secondsRemaining.set(remainingFromClock());
            ticker.pause();
            running.set(false);
            status.set("Paused");
        } else {
            if (secondsRemaining.get() <= 0) loadModeDuration();
            endNanos = System.nanoTime() + secondsRemaining.get() * 1_000_000_000L;
            running.set(true);
            status.set(mode.get() == Mode.WORK ? "Focus time!" : "Break time!");
            ticker.play();
        }
    }

    public void reset() {
        ticker.stop();
        running.set(false);
        sessionCount.set(0);
        mode.set(Mode.WORK);
        loadModeDuration();
        status.set("Ready to focus?");
    }

    /** Stops everything (used on logout and app shutdown). */
    public void shutdown() {
        ticker.stop();
        running.set(false);
    }

    /** Applies new lengths; the running interval is left alone, an untouched one is refreshed. */
    public void setDurations(int work, int shortBreak, int longBreak) {
        workMinutes = Math.max(1, work);
        shortBreakMinutes = Math.max(1, shortBreak);
        longBreakMinutes = Math.max(1, longBreak);
        if (!running.get() && secondsRemaining.get() == totalSeconds.get()) loadModeDuration();
    }

    // ----- internals ----------------------------------------------------
    private void tick() {
        int left = remainingFromClock();
        secondsRemaining.set(left);
        if (left <= 0) {
            ticker.stop();
            running.set(false);
            onIntervalComplete();
        }
    }

    private int remainingFromClock() {
        long nanosLeft = endNanos - System.nanoTime();
        return (int) Math.max(0, Math.ceil(nanosLeft / 1_000_000_000.0));
    }

    private void onIntervalComplete() {
        boolean workFinished = mode.get() == Mode.WORK;
        int finishedMinutes = totalSeconds.get() / 60;

        if (workFinished) {
            sessionCount.set(sessionCount.get() + 1);
            if (sessionCount.get() % SESSIONS_BEFORE_LONG_BREAK == 0) {
                mode.set(Mode.LONG_BREAK);
                status.set("Great work! Time for a long break.");
            } else {
                mode.set(Mode.SHORT_BREAK);
                status.set("Nice! Take a short break.");
            }
        } else {
            mode.set(Mode.WORK);
            status.set("Break done! Ready for another session?");
        }
        loadModeDuration();

        if (workFinished && finishedMinutes > 0 && onWorkSessionComplete != null) {
            // Never open a dialog from inside an animation pulse.
            Platform.runLater(() -> onWorkSessionComplete.accept(finishedMinutes));
        }
    }

    private void loadModeDuration() {
        int seconds = switch (mode.get()) {
            case WORK -> workMinutes * 60;
            case SHORT_BREAK -> shortBreakMinutes * 60;
            case LONG_BREAK -> longBreakMinutes * 60;
        };
        totalSeconds.set(seconds);
        secondsRemaining.set(seconds);
    }
}

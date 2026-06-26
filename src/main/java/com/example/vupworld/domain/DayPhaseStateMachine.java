package com.example.vupworld.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DayPhaseStateMachine {
    public static final int FINAL_DAY = 30;

    private static final Map<DayPhase, List<NextStep>> NEXT_STEPS_BY_PHASE = buildNextStepsByPhase();

    private DayPhaseStateMachine() {
    }

    public enum NextStep {
        SUBMIT_NON_STREAM_ACTION(DayPhase.READY, DayPhase.ACTION_RESOLVED),
        START_STREAM_PLAN(DayPhase.READY, DayPhase.NEED_TITLE),
        CHOOSE_STREAM_TITLE(DayPhase.NEED_TITLE, DayPhase.ACTION_RESOLVED),
        REROLL_STREAM_TITLES(DayPhase.NEED_TITLE, DayPhase.NEED_TITLE),
        CANCEL_STREAM_PLAN(DayPhase.NEED_TITLE, DayPhase.READY),
        START_OFF_STREAM(DayPhase.ACTION_RESOLVED, DayPhase.OFF_STREAM_READY),
        SUBMIT_OFF_STREAM_ACTION(DayPhase.OFF_STREAM_READY, DayPhase.OFF_STREAM_RESOLVED),
        SKIP_OFF_STREAM(DayPhase.OFF_STREAM_READY, DayPhase.OFF_STREAM_RESOLVED),
        QUEUE_INTERACTION_CHOICE(DayPhase.OFF_STREAM_RESOLVED, DayPhase.NEED_INTERACTION_CHOICE),
        QUEUE_FORMAL_EVENT(DayPhase.OFF_STREAM_RESOLVED, DayPhase.NEED_EVENT_CHOICE),
        COMPLETE_ACTION_REPORT(DayPhase.OFF_STREAM_RESOLVED, DayPhase.REPORT_READY),
        COMPLETE_FINAL_ACTION_REPORT(DayPhase.OFF_STREAM_RESOLVED, DayPhase.ENDING_READY),
        // Backward-compatible direct transitions from ACTION_RESOLVED
        QUEUE_INTERACTION_CHOICE_DIRECT(DayPhase.ACTION_RESOLVED, DayPhase.NEED_INTERACTION_CHOICE),
        QUEUE_FORMAL_EVENT_DIRECT(DayPhase.ACTION_RESOLVED, DayPhase.NEED_EVENT_CHOICE),
        COMPLETE_ACTION_REPORT_DIRECT(DayPhase.ACTION_RESOLVED, DayPhase.REPORT_READY),
        COMPLETE_FINAL_ACTION_REPORT_DIRECT(DayPhase.ACTION_RESOLVED, DayPhase.ENDING_READY),
        COMPLETE_INTERACTION_TO_EVENT(DayPhase.NEED_INTERACTION_CHOICE, DayPhase.NEED_EVENT_CHOICE),
        COMPLETE_INTERACTION_REPORT(DayPhase.NEED_INTERACTION_CHOICE, DayPhase.REPORT_READY),
        COMPLETE_FINAL_INTERACTION_REPORT(DayPhase.NEED_INTERACTION_CHOICE, DayPhase.ENDING_READY),
        COMPLETE_EVENT_REPORT(DayPhase.NEED_EVENT_CHOICE, DayPhase.REPORT_READY),
        COMPLETE_FINAL_EVENT_REPORT(DayPhase.NEED_EVENT_CHOICE, DayPhase.ENDING_READY),
        START_NEXT_DAY(DayPhase.REPORT_READY, DayPhase.READY);

        private final DayPhase from;
        private final DayPhase to;

        NextStep(DayPhase from, DayPhase to) {
            this.from = from;
            this.to = to;
        }

        public DayPhase from() {
            return from;
        }

        public DayPhase to() {
            return to;
        }
    }

    public static List<NextStep> allowedNextSteps(DayPhase phase) {
        requirePhase(phase, "phase");
        return NEXT_STEPS_BY_PHASE.getOrDefault(phase, List.of());
    }

    public static Set<DayPhase> allowedNextPhases(DayPhase phase) {
        EnumSet<DayPhase> phases = EnumSet.noneOf(DayPhase.class);
        for (NextStep step : allowedNextSteps(phase)) {
            phases.add(step.to());
        }
        return Collections.unmodifiableSet(phases);
    }

    public static boolean allows(DayPhase from, DayPhase to) {
        requirePhase(from, "from");
        requirePhase(to, "to");
        return allowedNextPhases(from).contains(to);
    }

    public static boolean isTransient(DayPhase phase) {
        requirePhase(phase, "phase");
        return phase == DayPhase.ACTION_RESOLVED;
    }

    public static DayPhase afterReport(int day) {
        return afterReport(day, FINAL_DAY);
    }

    public static DayPhase afterReport(int day, int finalDay) {
        if (day < 1) {
            throw new IllegalArgumentException("day must be positive");
        }
        requireFinalDay(finalDay);
        return day >= finalDay ? DayPhase.ENDING_READY : DayPhase.REPORT_READY;
    }

    public static boolean canStartNextDay(DayPhase phase, int day) {
        return canStartNextDay(phase, day, FINAL_DAY);
    }

    public static boolean canStartNextDay(DayPhase phase, int day, int finalDay) {
        requirePhase(phase, "phase");
        requireFinalDay(finalDay);
        return phase == DayPhase.REPORT_READY && day > 0 && day < finalDay;
    }

    private static Map<DayPhase, List<NextStep>> buildNextStepsByPhase() {
        EnumMap<DayPhase, List<NextStep>> stepsByPhase = new EnumMap<>(DayPhase.class);
        for (DayPhase phase : DayPhase.values()) {
            stepsByPhase.put(phase, new ArrayList<>());
        }
        for (NextStep step : NextStep.values()) {
            stepsByPhase.get(step.from()).add(step);
        }
        stepsByPhase.replaceAll((phase, steps) -> List.copyOf(steps));
        return Collections.unmodifiableMap(stepsByPhase);
    }

    private static void requirePhase(DayPhase phase, String argumentName) {
        if (phase == null) {
            throw new IllegalArgumentException(argumentName + " is required");
        }
    }

    private static void requireFinalDay(int finalDay) {
        if (finalDay < 1) {
            throw new IllegalArgumentException("finalDay must be positive");
        }
    }
}

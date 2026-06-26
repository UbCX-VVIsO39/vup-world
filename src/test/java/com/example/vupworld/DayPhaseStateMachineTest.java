package com.example.vupworld;

import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.DayPhaseStateMachine;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static com.example.vupworld.domain.DayPhase.ACTION_RESOLVED;
import static com.example.vupworld.domain.DayPhase.ENDING_READY;
import static com.example.vupworld.domain.DayPhase.NEED_EVENT_CHOICE;
import static com.example.vupworld.domain.DayPhase.NEED_INTERACTION_CHOICE;
import static com.example.vupworld.domain.DayPhase.NEED_TITLE;
import static com.example.vupworld.domain.DayPhase.OFF_STREAM_READY;
import static com.example.vupworld.domain.DayPhase.OFF_STREAM_RESOLVED;
import static com.example.vupworld.domain.DayPhase.READY;
import static com.example.vupworld.domain.DayPhase.REPORT_READY;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.CANCEL_STREAM_PLAN;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.CHOOSE_STREAM_TITLE;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.COMPLETE_ACTION_REPORT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.COMPLETE_ACTION_REPORT_DIRECT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.COMPLETE_EVENT_REPORT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.COMPLETE_FINAL_ACTION_REPORT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.COMPLETE_FINAL_ACTION_REPORT_DIRECT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.COMPLETE_FINAL_EVENT_REPORT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.COMPLETE_FINAL_INTERACTION_REPORT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.COMPLETE_INTERACTION_REPORT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.COMPLETE_INTERACTION_TO_EVENT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.QUEUE_FORMAL_EVENT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.QUEUE_FORMAL_EVENT_DIRECT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.QUEUE_INTERACTION_CHOICE;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.QUEUE_INTERACTION_CHOICE_DIRECT;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.REROLL_STREAM_TITLES;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.SKIP_OFF_STREAM;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.START_NEXT_DAY;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.START_OFF_STREAM;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.START_STREAM_PLAN;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.SUBMIT_NON_STREAM_ACTION;
import static com.example.vupworld.domain.DayPhaseStateMachine.NextStep.SUBMIT_OFF_STREAM_ACTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayPhaseStateMachineTest {

    @Test
    void fixesAllowedPhaseTransitionMatrix() {
        assertAllowed(READY, ACTION_RESOLVED, NEED_TITLE);
        assertAllowed(NEED_TITLE, ACTION_RESOLVED, NEED_TITLE, READY);
        assertAllowed(ACTION_RESOLVED, OFF_STREAM_READY, NEED_INTERACTION_CHOICE, NEED_EVENT_CHOICE, REPORT_READY, ENDING_READY);
        assertAllowed(OFF_STREAM_READY, OFF_STREAM_RESOLVED);
        assertAllowed(OFF_STREAM_RESOLVED, NEED_INTERACTION_CHOICE, NEED_EVENT_CHOICE, REPORT_READY, ENDING_READY);
        assertAllowed(NEED_INTERACTION_CHOICE, NEED_EVENT_CHOICE, REPORT_READY, ENDING_READY);
        assertAllowed(NEED_EVENT_CHOICE, REPORT_READY, ENDING_READY);
        assertAllowed(REPORT_READY, READY);
        assertAllowed(ENDING_READY);
    }

    @Test
    void fixesNextStepSemantics() {
        assertEquals(Set.of(SUBMIT_NON_STREAM_ACTION, START_STREAM_PLAN), Set.copyOf(DayPhaseStateMachine.allowedNextSteps(READY)));
        assertEquals(Set.of(CHOOSE_STREAM_TITLE, REROLL_STREAM_TITLES, CANCEL_STREAM_PLAN), Set.copyOf(DayPhaseStateMachine.allowedNextSteps(NEED_TITLE)));
        assertEquals(Set.of(START_OFF_STREAM, QUEUE_INTERACTION_CHOICE_DIRECT, QUEUE_FORMAL_EVENT_DIRECT, COMPLETE_ACTION_REPORT_DIRECT, COMPLETE_FINAL_ACTION_REPORT_DIRECT), Set.copyOf(DayPhaseStateMachine.allowedNextSteps(ACTION_RESOLVED)));
        assertEquals(Set.of(SUBMIT_OFF_STREAM_ACTION, SKIP_OFF_STREAM), Set.copyOf(DayPhaseStateMachine.allowedNextSteps(OFF_STREAM_READY)));
        assertEquals(Set.of(QUEUE_INTERACTION_CHOICE, QUEUE_FORMAL_EVENT, COMPLETE_ACTION_REPORT, COMPLETE_FINAL_ACTION_REPORT), Set.copyOf(DayPhaseStateMachine.allowedNextSteps(OFF_STREAM_RESOLVED)));
        assertEquals(Set.of(COMPLETE_INTERACTION_TO_EVENT, COMPLETE_INTERACTION_REPORT, COMPLETE_FINAL_INTERACTION_REPORT), Set.copyOf(DayPhaseStateMachine.allowedNextSteps(NEED_INTERACTION_CHOICE)));
        assertEquals(Set.of(COMPLETE_EVENT_REPORT, COMPLETE_FINAL_EVENT_REPORT), Set.copyOf(DayPhaseStateMachine.allowedNextSteps(NEED_EVENT_CHOICE)));
        assertEquals(Set.of(START_NEXT_DAY), Set.copyOf(DayPhaseStateMachine.allowedNextSteps(REPORT_READY)));
        assertTrue(DayPhaseStateMachine.allowedNextSteps(ENDING_READY).isEmpty());
    }

    @Test
    void marksActionResolvedAsOnlyTransientPhase() {
        for (DayPhase phase : DayPhase.values()) {
            assertEquals(phase == ACTION_RESOLVED, DayPhaseStateMachine.isTransient(phase));
        }
    }

    @Test
    void resolvesReportCompletionPhaseByDay() {
        assertEquals(REPORT_READY, DayPhaseStateMachine.afterReport(1));
        assertEquals(REPORT_READY, DayPhaseStateMachine.afterReport(29));
        assertEquals(ENDING_READY, DayPhaseStateMachine.afterReport(30));
        assertEquals(ENDING_READY, DayPhaseStateMachine.afterReport(31));
        assertThrows(IllegalArgumentException.class, () -> DayPhaseStateMachine.afterReport(0));
    }

    @Test
    void onlyReportReadyBeforeFinalDayCanStartNextDay() {
        assertTrue(DayPhaseStateMachine.canStartNextDay(REPORT_READY, 1));
        assertTrue(DayPhaseStateMachine.canStartNextDay(REPORT_READY, 29));
        assertFalse(DayPhaseStateMachine.canStartNextDay(REPORT_READY, 30));
        assertFalse(DayPhaseStateMachine.canStartNextDay(ENDING_READY, 30));
        assertFalse(DayPhaseStateMachine.canStartNextDay(NEED_TITLE, 12));
    }

    @Test
    void rejectsNullPhaseArguments() {
        assertThrows(IllegalArgumentException.class, () -> DayPhaseStateMachine.allowedNextSteps(null));
        assertThrows(IllegalArgumentException.class, () -> DayPhaseStateMachine.allows(null, READY));
        assertThrows(IllegalArgumentException.class, () -> DayPhaseStateMachine.allows(READY, null));
        assertThrows(IllegalArgumentException.class, () -> DayPhaseStateMachine.canStartNextDay(null, 1));
    }

    private static void assertAllowed(DayPhase phase, DayPhase... expectedNextPhases) {
        EnumSet<DayPhase> expected = expectedNextPhases.length == 0
                ? EnumSet.noneOf(DayPhase.class)
                : EnumSet.copyOf(Set.of(expectedNextPhases));
        assertEquals(expected, DayPhaseStateMachine.allowedNextPhases(phase));
        for (DayPhase candidate : DayPhase.values()) {
            assertEquals(expected.contains(candidate), DayPhaseStateMachine.allows(phase, candidate));
        }
    }
}

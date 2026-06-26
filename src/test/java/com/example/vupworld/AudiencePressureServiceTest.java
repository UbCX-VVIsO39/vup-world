package com.example.vupworld;

import com.example.vupworld.domain.RouteType;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.fan.AudiencePressureService;
import com.example.vupworld.service.core.DayFlowService;
import com.example.vupworld.service.infra.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AudiencePressureServiceTest {
    private final BusinessLogMapper businessLogMapper = mock(BusinessLogMapper.class);
    private final DayFlowService dayFlowService = mock(DayFlowService.class);
    private final AudiencePressureService service = new AudiencePressureService(
            new JsonService(new ObjectMapper()),
            dayFlowService,
            businessLogMapper
    );

    AudiencePressureServiceTest() {
        when(dayFlowService.routeLabel(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void lockedExpectationPunishesOffRouteAction() {
        Vup vup = vupWithRouteScore(RouteType.SINGING_IDOL.name(), 12);

        var pressure = service.pressureForRoute(vup, 8, RouteType.SLICE_SAINT.name());

        assertTrue(pressure.active());
        assertEquals(2, pressure.level());
        assertEquals(70, service.fanKeepPercent(pressure));
        assertEquals(-2, service.reputationPenalty(pressure));
        assertEquals(3, service.watchHeatGain(pressure));
    }

    @Test
    void previousTalkReviewReducesLockedExpectationPressure() {
        Vup vup = vupWithRouteScore(RouteType.SINGING_IDOL.name(), 12);
        BusinessLog previous = new BusinessLog();
        previous.setAction("TRAIN_TALK");
        when(businessLogMapper.findLatestByVupIdAndDay(vup.getId(), 7)).thenReturn(previous);

        var pressure = service.pressureForRoute(vup, 8, RouteType.SLICE_SAINT.name());

        assertTrue(pressure.active());
        assertEquals(1, pressure.level());
        assertEquals(85, service.fanKeepPercent(pressure));
    }

    private Vup vupWithRouteScore(String route, int score) {
        Vup vup = new Vup();
        vup.setId(100L);
        vup.setRouteScoreJson(new JsonService(new ObjectMapper()).write(Map.of(route, score)));
        vup.setExpectationJson("{}");
        return vup;
    }
}

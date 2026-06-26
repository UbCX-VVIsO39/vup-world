package com.example.vupworld.service;

import com.example.vupworld.model.Vup;
import com.example.vupworld.service.fan.RivalProgressService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RivalProgressServiceTest {
    private final RivalProgressService service = new RivalProgressService();

    @Test
    void returnsThreeRivals() {
        Vup vup = new Vup();
        vup.setDayCount(10);
        vup.setFans(100);
        var rivals = service.getRivals(vup);
        assertEquals(3, rivals.size());
    }

    @Test
    void eachRivalHasNonNullNameRouteAndStyle() {
        Vup vup = new Vup();
        vup.setDayCount(10);
        vup.setFans(100);
        var rivals = service.getRivals(vup);
        for (int i = 0; i < rivals.size(); i++) {
            var rival = rivals.get(i);
            assertNotNull(rival.name, "rival[" + i + "].name should not be null");
            assertNotNull(rival.route, "rival[" + i + "].route should not be null");
            assertNotNull(rival.style, "rival[" + i + "].style should not be null");
        }
    }

    @Test
    void rivalFansArePositiveNumbers() {
        Vup vup = new Vup();
        vup.setDayCount(10);
        vup.setFans(100);
        var rivals = service.getRivals(vup);
        for (int i = 0; i < rivals.size(); i++) {
            assertTrue(rivals.get(i).fans > 0,
                    "rival[" + i + "].fans should be positive, got " + rivals.get(i).fans);
        }
    }

    @Test
    void rivalHeatIsPositive() {
        Vup vup = new Vup();
        vup.setDayCount(10);
        vup.setFans(100);
        var rivals = service.getRivals(vup);
        for (int i = 0; i < rivals.size(); i++) {
            assertTrue(rivals.get(i).heat > 0,
                    "rival[" + i + "].heat should be positive, got " + rivals.get(i).heat);
        }
    }

    @Test
    void rivalFansGrowWithDayCount() {
        Vup earlyVup = new Vup();
        earlyVup.setDayCount(1);
        earlyVup.setFans(100);

        Vup lateVup = new Vup();
        lateVup.setDayCount(20);
        lateVup.setFans(100);

        var earlyRivals = service.getRivals(earlyVup);
        var lateRivals = service.getRivals(lateVup);

        for (int i = 0; i < 3; i++) {
            assertTrue(lateRivals.get(i).fans > earlyRivals.get(i).fans,
                    "rival[" + i + "] should have more fans at day 20 than day 1");
        }
    }
}

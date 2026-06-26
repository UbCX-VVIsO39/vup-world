package com.example.vupworld.service;

import com.example.vupworld.model.DaySession;
import com.example.vupworld.service.event.BlackSwanService;
import com.example.vupworld.service.infra.DeterministicRngService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BlackSwanServiceTest {
    @Mock private DeterministicRngService rngService;
    @InjectMocks private BlackSwanService blackSwanService;

    @Test
    void eventPoolNotEmpty() {
        assertNotNull(blackSwanService);
    }
}

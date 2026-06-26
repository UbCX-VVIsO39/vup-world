package com.example.vupworld.service.content;

import com.example.vupworld.domain.RouteType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteContentMappingServiceTest {

    private final RouteContentMappingService mappingService = new RouteContentMappingService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exposesSevenBaseRoutesElevenPlayerIdentitiesAndNineFormalEndings() {
        assertEquals(
                Arrays.stream(RouteType.values()).map(RouteType::name).toList(),
                mappingService.baseRouteTypes()
        );
        assertEquals(7, mappingService.baseRouteTypes().size());
        assertEquals(11, mappingService.playerRouteTypes().size());
        assertEquals(9, mappingService.formalEndingKeys().size());

        assertEquals("SLICE_SAINT", mappingService.scoreRouteType("DANCE_MEME"));
        assertEquals("SOCIAL_COLLAB", mappingService.scoreRouteType("DD_BUS_STOP"));
        assertEquals("BLACK_RED_MAIN_STAGE", mappingService.scoreRouteType("MAIN_STAGE_KING"));
        assertEquals("ELECTRONIC_PICKLE", mappingService.scoreRouteType("CYBER_GIRLFRIEND"));
        assertEquals("ELECTRONIC_PICKLE", mappingService.scoreRouteType("GLORIOUS_GRADUATION"));
    }

    @Test
    void routeIdentityJsonUsesTheSamePlayerRoutesAndStrategiesAsTheMappingTable() throws IOException {
        IdentityPack pack = readJson("content/route-identity.json", IdentityPack.class);
        Map<String, RouteIdentity> identities = pack.routeIdentities().stream()
                .collect(Collectors.toMap(RouteIdentity::routeType, Function.identity()));

        assertEquals(Set.copyOf(mappingService.playerRouteTypes()), identities.keySet());
        for (String routeType : mappingService.playerRouteTypes()) {
            assertEquals(
                    mappingService.mappingFor(routeType).identityStrategy(),
                    identities.get(routeType).strategy(),
                    routeType + " should keep route-identity strategy aligned"
            );
        }
    }

    @Test
    void midgameAndCommercialPacksCoverEveryPlayerRouteThroughTheUnifiedRouteGroups() throws IOException {
        MidgamePack midgame = readJson("content/midgame-events.json", MidgamePack.class);
        CommercialPack commercial = readJson("content/commercial-route-content.json", CommercialPack.class);

        assertRouteWindowCoverage("midgame", midgame.windows(), midgame.events());
        assertRouteWindowCoverage("commercial", commercial.lateGameWindows(), commercial.lateGameEvents());
    }

    @Test
    void acceptanceStrategiesResolveToKnownRouteIdentitiesAndFormalEndingKeys() {
        assertEquals(List.of(
                "steady",
                "clip",
                "black_red",
                "social",
                "singing",
                "cyber_girlfriend",
                "main_stage_king",
                "glorious_graduation",
                "idle",
                "defense",
                "random"
        ), mappingService.acceptanceStrategyKeys());

        for (String strategy : mappingService.acceptanceStrategyKeys()) {
            RouteContentMappingService.AcceptanceStrategyMapping acceptance = mappingService
                    .acceptanceStrategy(strategy)
                    .orElseThrow();
            if ("random".equals(strategy)) {
                assertEquals("", acceptance.expectedEndingKey());
                continue;
            }
            assertTrue(mappingService.playerRouteTypes().contains(acceptance.routeIdentityType()));
            assertTrue(mappingService.formalEndingKeys().contains(acceptance.expectedEndingKey()));
            assertEquals(
                    mappingService.mappingFor(acceptance.routeIdentityType()).routeGroup(),
                    acceptance.routeGroup()
            );
        }
    }

    private void assertRouteWindowCoverage(String packName, List<WindowDef> windows, List<ContentEvent> events) {
        for (String routeType : mappingService.playerRouteTypes()) {
            String expectedGroup = mappingService.routeGroupFor(routeType);
            for (WindowDef window : windows) {
                boolean covered = events.stream()
                        .filter(event -> window.key().equals(event.window()))
                        .anyMatch(event -> expectedGroup.equals(event.routeGroup())
                                && event.routeTypes() != null
                                && event.routeTypes().contains(routeType));
                assertTrue(
                        covered,
                        () -> packName + " missing " + routeType + " in " + window.key()
                                + " via routeGroup " + expectedGroup
                );
            }
        }
    }

    private <T> T readJson(String path, Class<T> type) throws IOException {
        String json = new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json, type);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record IdentityPack(List<RouteIdentity> routeIdentities) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RouteIdentity(String routeType, String strategy) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MidgamePack(List<WindowDef> windows, List<ContentEvent> events) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CommercialPack(List<WindowDef> lateGameWindows, List<ContentEvent> lateGameEvents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WindowDef(String key) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentEvent(String window, String routeGroup, List<String> routeTypes) {
    }
}

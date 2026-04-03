package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.Route;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:route_isolation_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=update"
})
@Transactional
class RouteIsolationTest {

    @Autowired
    private RouteService routeService;

    @Test
    void shouldIsolateRoutesByCardKeyForQueryAndMutation() {
        String cardKeyA = "CARD_A";
        String cardKeyB = "CARD_B";

        Route a1 = routeService.createRoute(cardKeyA, "A-线路1", "a-value-1");
        Route b1 = routeService.createRoute(cardKeyB, "B-线路1", "b-value-1");

        List<Route> routesA = routeService.getAllRoutes(cardKeyA);
        List<Route> routesB = routeService.getAllRoutes(cardKeyB);
        assertEquals(1, routesA.size());
        assertEquals(1, routesB.size());
        assertEquals(a1.getId(), routesA.get(0).getId());
        assertEquals(b1.getId(), routesB.get(0).getId());

        Optional<Route> shouldNotSeeBFromA = routeService.getRouteById(b1.getId(), cardKeyA);
        assertFalse(shouldNotSeeBFromA.isPresent());

        RuntimeException updateException = assertThrows(RuntimeException.class, () ->
                routeService.updateRoute(cardKeyA, b1.getId(), "B-线路1-改", "b-value-1-改"));
        assertTrue(updateException.getMessage().contains("线路不存在"));

        routeService.deleteRoute(cardKeyA, b1.getId());
        assertTrue(routeService.getRouteById(b1.getId(), cardKeyB).isPresent());

        routeService.updateRouteStatus(cardKeyB, b1.getId(), false);
        assertEquals(0, routeService.getRoutesByStatus(true, cardKeyB).size());
        assertEquals(1, routeService.getRoutesByStatus(false, cardKeyB).size());
    }
}


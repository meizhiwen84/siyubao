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
        "spring.datasource.url=jdbc:sqlite:file:route_isolation_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=cn.laobayou.siyubao.config.SQLiteDialect",
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=update"
})
@Transactional
class RouteIsolationTest {

    @Autowired
    private RouteService routeService;

    @Test
    void shouldIsolateRoutesByUserIdForQueryAndMutation() {
        Long userIdA = 1001L;
        Long userIdB = 2002L;

        Route a1 = routeService.createRoute(userIdA, "A-线路1", "a-value-1");
        Route b1 = routeService.createRoute(userIdB, "B-线路1", "b-value-1");

        List<Route> routesA = routeService.getAllRoutes(userIdA);
        List<Route> routesB = routeService.getAllRoutes(userIdB);
        assertEquals(1, routesA.size());
        assertEquals(1, routesB.size());
        assertEquals(a1.getId(), routesA.get(0).getId());
        assertEquals(b1.getId(), routesB.get(0).getId());

        Optional<Route> shouldNotSeeBFromA = routeService.getRouteById(b1.getId(), userIdA);
        assertFalse(shouldNotSeeBFromA.isPresent());

        RuntimeException updateException = assertThrows(RuntimeException.class, () ->
                routeService.updateRoute(userIdA, b1.getId(), "B-线路1-改", "b-value-1-改"));
        assertTrue(updateException.getMessage().contains("线路不存在"));

        routeService.deleteRoute(userIdA, b1.getId());
        assertTrue(routeService.getRouteById(b1.getId(), userIdB).isPresent());

        routeService.updateRouteStatus(userIdB, b1.getId(), false);
        assertEquals(0, routeService.getRoutesByStatus(true, userIdB).size());
        assertEquals(1, routeService.getRoutesByStatus(false, userIdB).size());
    }
}

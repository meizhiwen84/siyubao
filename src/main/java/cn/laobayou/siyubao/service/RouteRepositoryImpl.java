package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.Route;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 线路数据访问实现类（内存存储）
 */
@Repository
public class RouteRepositoryImpl implements RouteRepository {
    
    private final Map<Long, Route> routes = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public Route save(Route route) {
        if (route.getId() == null) {
            // 新增
            route.setId(idGenerator.getAndIncrement());
            route.setCreateTime(LocalDateTime.now());
        }
        route.setUpdateTime(LocalDateTime.now());
        routes.put(route.getId(), route);
        return route;
    }
    
    @Override
    public Optional<Route> findByIdAndCardKey(Long id, String cardKey) {
        Route route = routes.get(id);
        if (route == null) return Optional.empty();
        return Objects.equals(route.getCardKey(), cardKey) ? Optional.of(route) : Optional.empty();
    }
    
    @Override
    public List<Route> findAllByCardKey(String cardKey) {
        return routes.values().stream()
                .filter(route -> Objects.equals(route.getCardKey(), cardKey))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Route> findByStatusAndCardKey(Boolean status, String cardKey) {
        return routes.values().stream()
                .filter(route -> Objects.equals(route.getCardKey(), cardKey))
                .filter(route -> Objects.equals(route.getStatus(), status))
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Route> findByRouteNameAndCardKey(String routeName, String cardKey) {
        return routes.values().stream()
                .filter(route -> Objects.equals(route.getCardKey(), cardKey))
                .filter(route -> Objects.equals(route.getRouteName(), routeName))
                .findFirst();
    }

    @Override
    public Optional<Route> findByRouteValueAndCardKey(String routeValue, String cardKey) {
        return routes.values().stream()
                .filter(route -> Objects.equals(route.getCardKey(), cardKey))
                .filter(route -> Objects.equals(route.getRouteValue(), routeValue))
                .max(Comparator.comparing(Route::getUpdateTime));
    }
    
    @Override
    public void deleteByIdAndCardKey(Long id, String cardKey) {
        findByIdAndCardKey(id, cardKey).ifPresent(route -> routes.remove(route.getId()));
    }
    
    @Override
    public boolean existsByRouteNameAndCardKey(String routeName, String cardKey) {
        return routes.values().stream()
                .filter(route -> Objects.equals(route.getCardKey(), cardKey))
                .anyMatch(route -> Objects.equals(route.getRouteName(), routeName));
    }
}

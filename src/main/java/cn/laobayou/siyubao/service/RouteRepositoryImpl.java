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
    public Optional<Route> findById(Long id) {
        return Optional.ofNullable(routes.get(id));
    }
    
    @Override
    public List<Route> findAll() {
        return new ArrayList<>(routes.values());
    }
    
    @Override
    public List<Route> findByStatus(Boolean status) {
        return routes.values().stream()
                .filter(route -> Objects.equals(route.getStatus(), status))
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Route> findByRouteName(String routeName) {
        return routes.values().stream()
                .filter(route -> Objects.equals(route.getRouteName(), routeName))
                .findFirst();
    }
    
    @Override
    public void deleteById(Long id) {
        routes.remove(id);
    }
    
    @Override
    public boolean existsByRouteName(String routeName) {
        return routes.values().stream()
                .anyMatch(route -> Objects.equals(route.getRouteName(), routeName));
    }
}
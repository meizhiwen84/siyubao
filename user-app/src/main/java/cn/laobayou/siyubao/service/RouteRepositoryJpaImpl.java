package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.Route;
import cn.laobayou.siyubao.repository.RouteJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 线路数据访问实现类（JPA持久化存储）
 */
@Repository
@Primary
@Transactional
public class RouteRepositoryJpaImpl implements RouteRepository {
    
    @Autowired
    private RouteJpaRepository jpaRepository;
    
    @Override
    public Route save(Route route) {
        if (route.getId() == null) {
            // 新增时设置创建时间
            route.setCreateTime(LocalDateTime.now());
        }
        route.setUpdateTime(LocalDateTime.now());
        return jpaRepository.save(route);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Route> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Route> findAllByUserId(Long userId) {
        return jpaRepository.findAllOrderByUpdateTimeDesc(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Route> findByStatusAndUserId(Boolean status, Long userId) {
        return jpaRepository.findByStatusAndUserId(status, userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Route> findByRouteNameAndUserId(String routeName, Long userId) {
        return jpaRepository.findByRouteNameAndUserId(routeName, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Route> findByRouteValueAndUserId(String routeValue, Long userId) {
        return jpaRepository.findFirstByRouteValueAndUserIdOrderByUpdateTimeDesc(routeValue, userId);
    }
    
    @Override
    public void deleteByIdAndUserId(Long id, Long userId) {
        jpaRepository.findByIdAndUserId(id, userId).ifPresent(jpaRepository::delete);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByRouteNameAndUserId(String routeName, Long userId) {
        return jpaRepository.existsByRouteNameAndUserId(routeName, userId);
    }
}

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
    public Optional<Route> findByIdAndCardKey(Long id, String cardKey) {
        return jpaRepository.findByIdAndCardKey(id, cardKey);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Route> findAllByCardKey(String cardKey) {
        return jpaRepository.findAllOrderByUpdateTimeDesc(cardKey);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Route> findByStatusAndCardKey(Boolean status, String cardKey) {
        return jpaRepository.findByStatusAndCardKey(status, cardKey);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Route> findByRouteNameAndCardKey(String routeName, String cardKey) {
        return jpaRepository.findByRouteNameAndCardKey(routeName, cardKey);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Route> findByRouteValueAndCardKey(String routeValue, String cardKey) {
        return jpaRepository.findFirstByRouteValueAndCardKeyOrderByUpdateTimeDesc(routeValue, cardKey);
    }
    
    @Override
    public void deleteByIdAndCardKey(Long id, String cardKey) {
        jpaRepository.findByIdAndCardKey(id, cardKey).ifPresent(jpaRepository::delete);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByRouteNameAndCardKey(String routeName, String cardKey) {
        return jpaRepository.existsByRouteNameAndCardKey(routeName, cardKey);
    }
}

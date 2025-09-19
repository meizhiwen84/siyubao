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
    public Optional<Route> findById(Long id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Route> findAll() {
        return jpaRepository.findAllOrderByUpdateTimeDesc();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Route> findByStatus(Boolean status) {
        return jpaRepository.findByStatus(status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Route> findByRouteName(String routeName) {
        return jpaRepository.findByRouteName(routeName);
    }
    
    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByRouteName(String routeName) {
        return jpaRepository.existsByRouteName(routeName);
    }
}
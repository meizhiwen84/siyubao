package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 线路JPA Repository接口
 * 提供基于JPA的数据访问功能
 */
@Repository
public interface RouteJpaRepository extends JpaRepository<Route, Long> {
    
    /**
     * 根据状态查找线路
     */
    List<Route> findByStatus(Boolean status);
    
    /**
     * 根据线路名称查找
     */
    Optional<Route> findByRouteName(String routeName);
    
    /**
     * 检查线路名称是否存在
     */
    boolean existsByRouteName(String routeName);
    
    /**
     * 根据线路名称查找（忽略大小写）
     */
    @Query("SELECT r FROM Route r WHERE LOWER(r.routeName) = LOWER(:routeName)")
    Optional<Route> findByRouteNameIgnoreCase(@Param("routeName") String routeName);
    
    /**
     * 查找所有启用的线路，按创建时间降序排列
     */
    @Query("SELECT r FROM Route r WHERE r.status = true ORDER BY r.createTime DESC")
    List<Route> findActiveRoutesOrderByCreateTimeDesc();
    
    /**
     * 查找所有线路，按更新时间降序排列
     */
    @Query("SELECT r FROM Route r ORDER BY r.updateTime DESC")
    List<Route> findAllOrderByUpdateTimeDesc();
}
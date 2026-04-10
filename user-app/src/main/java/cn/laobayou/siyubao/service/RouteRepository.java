package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.Route;
import java.util.List;
import java.util.Optional;

/**
 * 线路数据访问接口
 */
public interface RouteRepository {
    
    /**
     * 保存线路
     */
    Route save(Route route);
    
    /**
     * 根据ID查找线路
     */
    Optional<Route> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 查找所有线路
     */
    List<Route> findAllByUserId(Long userId);
    
    /**
     * 根据状态查找线路
     */
    List<Route> findByStatusAndUserId(Boolean status, Long userId);
    
    /**
     * 根据线路名称查找
     */
    Optional<Route> findByRouteNameAndUserId(String routeName, Long userId);

    Optional<Route> findByRouteValueAndUserId(String routeValue, Long userId);
    
    /**
     * 删除线路
     */
    void deleteByIdAndUserId(Long id, Long userId);
    
    /**
     * 检查线路名称是否存在
     */
    boolean existsByRouteNameAndUserId(String routeName, Long userId);
}

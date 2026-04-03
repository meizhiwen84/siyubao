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
    Optional<Route> findByIdAndCardKey(Long id, String cardKey);
    
    /**
     * 查找所有线路
     */
    List<Route> findAllByCardKey(String cardKey);
    
    /**
     * 根据状态查找线路
     */
    List<Route> findByStatusAndCardKey(Boolean status, String cardKey);
    
    /**
     * 根据线路名称查找
     */
    Optional<Route> findByRouteNameAndCardKey(String routeName, String cardKey);

    Optional<Route> findByRouteValueAndCardKey(String routeValue, String cardKey);
    
    /**
     * 删除线路
     */
    void deleteByIdAndCardKey(Long id, String cardKey);
    
    /**
     * 检查线路名称是否存在
     */
    boolean existsByRouteNameAndCardKey(String routeName, String cardKey);
}

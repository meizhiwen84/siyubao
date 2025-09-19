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
    Optional<Route> findById(Long id);
    
    /**
     * 查找所有线路
     */
    List<Route> findAll();
    
    /**
     * 根据状态查找线路
     */
    List<Route> findByStatus(Boolean status);
    
    /**
     * 根据线路名称查找
     */
    Optional<Route> findByRouteName(String routeName);
    
    /**
     * 删除线路
     */
    void deleteById(Long id);
    
    /**
     * 检查线路名称是否存在
     */
    boolean existsByRouteName(String routeName);
}
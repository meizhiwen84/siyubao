package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    @Query("SELECT s FROM UserSubscription s WHERE s.userId = :userId AND s.status = 'ACTIVE' ORDER BY CASE WHEN s.endTime IS NULL THEN 0 ELSE 1 END, s.endTime DESC, s.id DESC")
    Optional<UserSubscription> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM UserSubscription s WHERE s.userId IN :userIds AND s.status = 'ACTIVE' ORDER BY s.userId ASC, s.endTime DESC, s.id DESC")
    List<UserSubscription> findActiveByUserIds(@Param("userIds") List<Long> userIds);
}

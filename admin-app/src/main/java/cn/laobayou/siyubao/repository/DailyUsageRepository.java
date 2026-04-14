package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.DailyUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyUsageRepository extends JpaRepository<DailyUsage, Long> {
    Optional<DailyUsage> findByUserIdAndDay(Long userId, LocalDate day);

    Page<DailyUsage> findByUserId(Long userId, Pageable pageable);

    @Modifying
    @Query(value = "INSERT INTO daily_usage(user_id, day, count) VALUES(:userId, :day, 1) ON DUPLICATE KEY UPDATE count = count + 1", nativeQuery = true)
    void increment(@Param("userId") Long userId, @Param("day") LocalDate day);
}


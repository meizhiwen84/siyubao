package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.DailyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyUsageRepository extends JpaRepository<DailyUsage, Long> {
    Optional<DailyUsage> findByUserIdAndDay(Long userId, LocalDate day);

    @Modifying
    @Query(value = "INSERT INTO daily_usage(user_id, day, count) VALUES(:userId, :day, 1) ON CONFLICT(user_id, day) DO UPDATE SET count = count + 1", nativeQuery = true)
    void increment(@Param("userId") Long userId, @Param("day") String day);
}


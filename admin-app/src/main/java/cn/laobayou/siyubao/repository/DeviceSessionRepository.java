package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.DeviceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {
    Optional<DeviceSession> findByToken(String token);

    List<DeviceSession> findAllByUserIdAndRevokedFalseOrderByLastSeenTimeDescIdDesc(Long userId);

    List<DeviceSession> findAllByUserIdOrderByLastSeenTimeDescIdDesc(Long userId);

    List<DeviceSession> findAllByUserIdInOrderByLastSeenTimeDescIdDesc(List<Long> userIds);

    Page<DeviceSession> findAllByUserId(Long userId, Pageable pageable);

    Page<DeviceSession> findAllByUserIdIn(List<Long> userIds, Pageable pageable);

    @Modifying
    @Query("UPDATE DeviceSession s SET s.lastSeenTime = :t WHERE s.id = :id")
    void touch(@Param("id") Long id, @Param("t") LocalDateTime t);

    @Modifying
    @Query("UPDATE DeviceSession s SET s.revoked = true WHERE s.id IN :ids")
    void revokeIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE DeviceSession s SET s.revoked = true WHERE s.id = :id")
    void revokeId(@Param("id") Long id);

    @Modifying
    @Query("UPDATE DeviceSession s SET s.revoked = true WHERE s.userId = :userId AND s.id <> :keepId")
    void revokeOthers(@Param("userId") Long userId, @Param("keepId") Long keepId);

    @Modifying
    @Query("UPDATE DeviceSession s SET s.revoked = true WHERE s.userId = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);
}

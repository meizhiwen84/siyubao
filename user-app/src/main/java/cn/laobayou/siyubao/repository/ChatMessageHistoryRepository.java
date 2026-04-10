package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.ChatMessageHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatMessageHistoryRepository extends JpaRepository<ChatMessageHistory, Long> {
    @Query("SELECT m FROM ChatMessageHistory m WHERE (:userId IS NULL OR m.userId = :userId) AND (:line IS NULL OR m.line = :line) AND (:platform IS NULL OR m.platform = :platform) AND (:phone IS NULL OR m.phone = :phone) ORDER BY m.createTime DESC")
    Page<ChatMessageHistory> search(
            @Param("userId") Long userId,
            @Param("line") String line,
            @Param("platform") String platform,
            @Param("phone") String phone,
            Pageable pageable);

    Optional<ChatMessageHistory> findByIdAndUserId(Long id, Long userId);
}

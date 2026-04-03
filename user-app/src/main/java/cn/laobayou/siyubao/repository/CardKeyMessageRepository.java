package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.CardKeyMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardKeyMessageRepository extends JpaRepository<CardKeyMessage, Long> {
    @Query("SELECT m FROM CardKeyMessage m WHERE (:cardKey IS NULL OR m.cardKey = :cardKey) AND (:line IS NULL OR m.line = :line) AND (:platform IS NULL OR m.platform = :platform) AND (:phone IS NULL OR m.phone = :phone) ORDER BY m.createTime DESC")
    Page<CardKeyMessage> search(
            @Param("cardKey") String cardKey,
            @Param("line") String line,
            @Param("platform") String platform,
            @Param("phone") String phone,
            Pageable pageable);
}


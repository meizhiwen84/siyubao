package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.AdminOpLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminOpLogRepository extends JpaRepository<AdminOpLog, Long> {
    @Query("SELECT l FROM AdminOpLog l WHERE (:kw IS NULL OR LOWER(l.action) LIKE :kw OR LOWER(l.adminUsername) LIKE :kw OR LOWER(l.targetType) LIKE :kw OR LOWER(l.targetId) LIKE :kw) ORDER BY l.createTime DESC, l.id DESC")
    Page<AdminOpLog> search(@Param("kw") String keyword, Pageable pageable);
}

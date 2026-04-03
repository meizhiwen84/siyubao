package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.CardKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CardKeyRepository extends JpaRepository<CardKey, Long> {
    Optional<CardKey> findByCode(String code);

    boolean existsByCode(String code);

    @Modifying
    @Query("update CardKey k set k.remaining = k.remaining - 1, k.updatedAt = CURRENT_TIMESTAMP where k.code = :code and k.enabled = true and k.remaining > 0")
    int decrementIfAvailable(@Param("code") String code);
}


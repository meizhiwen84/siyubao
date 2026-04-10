package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    @Query("SELECT u FROM AppUser u WHERE (:kw IS NULL OR LOWER(u.username) LIKE :kw) ORDER BY u.id DESC")
    Page<AppUser> search(@Param("kw") String kw, Pageable pageable);

    boolean existsByRole(String role);
}

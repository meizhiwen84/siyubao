package cn.laobayou.siyubao.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalUserSessionRepository extends CrudRepository<cn.laobayou.siyubao.bean.LocalUserSession, Long> {
    @Query(value = "SELECT token FROM local_user_session WHERE id = 1", nativeQuery = true)
    String getToken();

    @Query(value = "SELECT user_id FROM local_user_session WHERE id = 1", nativeQuery = true)
    Long getUserId();

    @Query(value = "SELECT username FROM local_user_session WHERE id = 1", nativeQuery = true)
    String getUsername();

    @Modifying
    @Query(value = "DELETE FROM local_user_session", nativeQuery = true)
    void clearAll();

    @Modifying
    @Query(
            value = "INSERT INTO local_user_session(id, token, user_id, username, update_time) VALUES (1, :token, :userId, :username, :updateTime) " +
                    "ON CONFLICT(id) DO UPDATE SET token = :token, user_id = :userId, username = :username, update_time = :updateTime",
            nativeQuery = true
    )
    void upsert(String token, Long userId, String username, String updateTime);
}


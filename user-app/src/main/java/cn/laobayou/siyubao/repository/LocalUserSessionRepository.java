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

    @Query(value = "SELECT user_no FROM local_user_session WHERE id = 1", nativeQuery = true)
    String getUserNo();

    @Modifying
    @Query(value = "DELETE FROM local_user_session", nativeQuery = true)
    void clearAll();

    @Modifying
    @Query(value = "DELETE FROM local_user_session WHERE id = 1", nativeQuery = true)
    void deleteByIdOne();

    @Modifying
    @Query(
            value = "INSERT INTO local_user_session(id, token, user_id, username, user_no, update_time) VALUES (1, :token, :userId, :username, :userNo, :updateTime)",
            nativeQuery = true
    )
    void insert(String token, Long userId, String username, String userNo, String updateTime);
}


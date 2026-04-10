package cn.laobayou.siyubao.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalDeviceRepository extends CrudRepository<cn.laobayou.siyubao.bean.LocalDevice, Long> {
    @Query(value = "SELECT device_id FROM local_device WHERE id = 1", nativeQuery = true)
    String getDeviceId();

    @Modifying
    @Query(
            value = "INSERT INTO local_device(id, device_id, update_time) VALUES (1, :deviceId, :updateTime) " +
                    "ON CONFLICT(id) DO UPDATE SET device_id = :deviceId, update_time = :updateTime",
            nativeQuery = true
    )
    void upsert(String deviceId, String updateTime);
}


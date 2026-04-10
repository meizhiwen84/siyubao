package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}

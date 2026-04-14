package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.PaymentQrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentQrCodeRepository extends JpaRepository<PaymentQrCode, Long> {
    List<PaymentQrCode> findByPlatformAndEnabledTrueOrderBySortOrderAsc(String platform);
    
    List<PaymentQrCode> findByPlatformOrderBySortOrderAsc(String platform);
    
    List<PaymentQrCode> findAllByOrderBySortOrderAsc();
}

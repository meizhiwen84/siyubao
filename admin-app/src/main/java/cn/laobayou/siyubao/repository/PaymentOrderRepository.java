package cn.laobayou.siyubao.repository;

import cn.laobayou.siyubao.bean.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderNo(String orderNo);

    List<PaymentOrder> findByUserId(Long userId);

    @Query("SELECT o FROM PaymentOrder o WHERE o.userId = :userId ORDER BY o.createTime DESC")
    List<PaymentOrder> findByUserIdOrderByCreateTimeDesc(@Param("userId") Long userId);

    @Query("SELECT o FROM PaymentOrder o WHERE (:status IS NULL OR o.status = :status) ORDER BY o.createTime DESC")
    Page<PaymentOrder> findByStatusOrderByCreateTimeDesc(@Param("status") String status, Pageable pageable);

    @Query("SELECT o FROM PaymentOrder o WHERE (:status IS NULL OR o.status = :status) " +
           "AND (:keyword IS NULL OR o.orderNo LIKE %:keyword% OR o.transactionId LIKE %:keyword%) " +
           "ORDER BY o.createTime DESC")
    Page<PaymentOrder> search(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(o) FROM PaymentOrder o WHERE o.userId = :userId AND o.status = 'PENDING'")
    long countPendingByUserId(@Param("userId") Long userId);
}

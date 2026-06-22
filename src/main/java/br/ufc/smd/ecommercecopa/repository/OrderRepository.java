package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Order;
import br.ufc.smd.ecommercecopa.model.OrderStatus;
import br.ufc.smd.ecommercecopa.repository.projection.ClientPurchasesProjection;
import br.ufc.smd.ecommercecopa.repository.projection.DailyRevenueProjection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByClient_UserIdOrderByCreatedAtDesc(UUID clientId);

    Optional<Order> findByIdAndClient_UserId(UUID id, UUID clientId);

    List<Order> findAllByOrderByCreatedAtDesc();

    @Query("""
            select o.client.userId as clientId,
                   o.client.user.name as clientName,
                   count(o) as totalOrders,
                   coalesce(sum(o.totalValue), 0) as totalValue
            from Order o
            where o.createdAt >= :start
              and o.createdAt < :end
              and o.deletedAt is null
              and (o.status is null or o.status <> :canceled)
            group by o.client.userId, o.client.user.name
            order by o.client.user.name asc
            """)
    List<ClientPurchasesProjection> findClientPurchasesReport(@Param("start") LocalDateTime start,
                                                              @Param("end") LocalDateTime end,
                                                              @Param("canceled") OrderStatus canceled);

    @Query(value = """
            select cast(o.created_at as date) as day,
                   coalesce(sum(o.total_value), 0) as totalValue
            from orders o
            where o.created_at >= :start
              and o.created_at < :end
              and o.deleted_at is null
              and (o.status is null or o.status <> 'CANCELED')
            group by cast(o.created_at as date)
            order by day asc
            """, nativeQuery = true)
    List<DailyRevenueProjection> findDailyRevenueReport(@Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);
}

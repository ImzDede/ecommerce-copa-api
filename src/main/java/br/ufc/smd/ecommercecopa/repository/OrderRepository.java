package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Order;
import br.ufc.smd.ecommercecopa.model.OrderStatus;
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
            select o from Order o
            join fetch o.client c
            join fetch c.user u
            where o.createdAt >= :start
              and o.createdAt < :end
              and o.deletedAt is null
              and (o.status is null or o.status <> :canceled)
            order by o.createdAt asc
            """)
    List<Order> findReportOrders(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 @Param("canceled") OrderStatus canceled);
}

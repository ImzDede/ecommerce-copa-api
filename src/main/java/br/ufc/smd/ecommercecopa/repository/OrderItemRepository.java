package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.OrderItem;
import br.ufc.smd.ecommercecopa.model.OrderItemId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {
    List<OrderItem> findByOrder_IdOrderBySku_TitleAsc(UUID orderId);
}

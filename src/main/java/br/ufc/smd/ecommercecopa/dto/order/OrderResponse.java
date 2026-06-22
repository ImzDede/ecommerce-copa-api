package br.ufc.smd.ecommercecopa.dto.order;

import br.ufc.smd.ecommercecopa.dto.address.AddressResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID clientId,
        String clientName,
        AddressResponse address,
        BigDecimal totalValue,
        String status,
        String createdAt,
        String canceledAt,
        List<OrderItemResponse> items
) {
}

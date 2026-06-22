package br.ufc.smd.ecommercecopa.dto.order;

import java.util.List;

public record OrderListResponse(
        List<OrderResponse> items
) {
}

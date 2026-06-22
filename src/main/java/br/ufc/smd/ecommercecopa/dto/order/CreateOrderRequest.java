package br.ufc.smd.ecommercecopa.dto.order;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "Endereço é obrigatório")
        UUID addressId
) {
}

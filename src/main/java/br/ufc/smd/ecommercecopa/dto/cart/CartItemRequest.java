package br.ufc.smd.ecommercecopa.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CartItemRequest(
        @NotNull(message = "SKU é obrigatório")
        UUID skuId,

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        Integer amount
) {
}

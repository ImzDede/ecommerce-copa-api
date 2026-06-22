package br.ufc.smd.ecommercecopa.dto.order;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(
        @NotBlank(message = "Status é obrigatório")
        String status
) {
}

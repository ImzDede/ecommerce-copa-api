package br.ufc.smd.ecommercecopa.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateReviewRequest(
        @NotNull(message = "SKU é obrigatório")
        UUID skuId,

        @NotNull(message = "Estrelas são obrigatórias")
        @Min(value = 1, message = "Estrelas devem estar entre 1 e 5")
        @Max(value = 5, message = "Estrelas devem estar entre 1 e 5")
        Integer stars,

        @NotBlank(message = "Comentário é obrigatório")
        @Size(max = 2000, message = "Comentário deve ter no máximo 2000 caracteres")
        String comment
) {
}

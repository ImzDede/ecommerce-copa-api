package br.ufc.smd.ecommercecopa.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
        @Min(value = 1, message = "Estrelas devem estar entre 1 e 5")
        @Max(value = 5, message = "Estrelas devem estar entre 1 e 5")
        Integer stars,

        @Size(max = 2000, message = "Comentário deve ter no máximo 2000 caracteres")
        String comment
) {
}

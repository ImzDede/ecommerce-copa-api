package br.ufc.smd.ecommercecopa.dto.category;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(min = 2, max = 80, message = "Título deve ter entre 2 e 80 caracteres")
        String title,

        @Size(max = 255, message = "Imagem deve ter no máximo 255 caracteres")
        String image,

        Boolean featured
) {
}

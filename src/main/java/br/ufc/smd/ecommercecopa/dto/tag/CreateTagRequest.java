package br.ufc.smd.ecommercecopa.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank(message = "Texto é obrigatório")
        @Size(max = 80, message = "Texto deve ter no máximo 80 caracteres")
        String text,

        @NotBlank(message = "Cor é obrigatória")
        @Size(max = 30, message = "Cor deve ter no máximo 30 caracteres")
        String color
) {
}

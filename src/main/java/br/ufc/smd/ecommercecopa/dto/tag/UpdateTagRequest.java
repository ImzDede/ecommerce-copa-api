package br.ufc.smd.ecommercecopa.dto.tag;

import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
        @Size(max = 80, message = "Texto deve ter no máximo 80 caracteres")
        String text,

        @Size(max = 30, message = "Cor deve ter no máximo 30 caracteres")
        String color
) {
}

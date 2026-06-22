package br.ufc.smd.ecommercecopa.dto.tag;

import java.util.UUID;

public record TagResponse(
        UUID id,
        String text,
        String color
) {
}

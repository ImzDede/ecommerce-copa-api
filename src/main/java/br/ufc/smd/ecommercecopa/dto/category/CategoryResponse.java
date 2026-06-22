package br.ufc.smd.ecommercecopa.dto.category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String slug,
        String title,
        String image,
        boolean featured
) {
}

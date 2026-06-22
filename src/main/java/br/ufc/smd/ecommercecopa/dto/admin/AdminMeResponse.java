package br.ufc.smd.ecommercecopa.dto.admin;

import java.util.UUID;

public record AdminMeResponse(
        UUID userId,
        String name,
        String email,
        String profilePhoto
) {
}

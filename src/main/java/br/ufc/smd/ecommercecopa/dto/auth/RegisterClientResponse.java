package br.ufc.smd.ecommercecopa.dto.auth;

import java.util.UUID;

public record RegisterClientResponse(
        UUID id,
        String name,
        String email,
        String role
) {
}

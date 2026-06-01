package br.ufc.smd.ecommercecopa.dto.auth;

import java.util.UUID;

public record AuthUserResponse(
        UUID userId,
        String name,
        String email,
        String role,
        Boolean authenticated
) {
}

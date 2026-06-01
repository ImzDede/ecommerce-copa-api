package br.ufc.smd.ecommercecopa.dto.client;

import java.util.UUID;

public record ClientMeResponse(
        UUID userId,
        String name,
        String email,
        String cpf,
        String dateOfBirth
) {
}

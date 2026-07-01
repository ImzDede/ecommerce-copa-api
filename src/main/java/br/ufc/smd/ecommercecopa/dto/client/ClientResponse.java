package br.ufc.smd.ecommercecopa.dto.client;

import java.time.LocalDate;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String name,
        String email,
        String cpf,
        LocalDate dateOfBirth
) {}

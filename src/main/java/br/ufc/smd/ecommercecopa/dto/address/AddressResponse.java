package br.ufc.smd.ecommercecopa.dto.address;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String name,
        String street,
        String number,
        String state,
        String city,
        String neighborhood,
        String complement,
        String postalCode,
        boolean isDefault
) {
}

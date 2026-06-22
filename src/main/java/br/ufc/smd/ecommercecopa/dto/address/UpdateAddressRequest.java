package br.ufc.smd.ecommercecopa.dto.address;

import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @Size(max = 80, message = "Nome do endereço deve ter no máximo 80 caracteres")
        String name,

        @Size(max = 160, message = "Endereço deve ter no máximo 160 caracteres")
        String street,

        @Size(max = 30, message = "Número deve ter no máximo 30 caracteres")
        String number,

        @Size(max = 80, message = "Estado deve ter no máximo 80 caracteres")
        String state,

        @Size(max = 120, message = "Cidade deve ter no máximo 120 caracteres")
        String city,

        @Size(max = 120, message = "Bairro deve ter no máximo 120 caracteres")
        String neighborhood,

        @Size(max = 255, message = "Complemento deve ter no máximo 255 caracteres")
        String complement,

        @Size(max = 20, message = "CEP deve ter no máximo 20 caracteres")
        String postalCode,

        Boolean isDefault
) {
}

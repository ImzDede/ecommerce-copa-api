package br.ufc.smd.ecommercecopa.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank(message = "Nome do endereço é obrigatório")
        @Size(max = 80, message = "Nome do endereço deve ter no máximo 80 caracteres")
        String name,

        @NotBlank(message = "Endereço é obrigatório")
        @Size(max = 160, message = "Endereço deve ter no máximo 160 caracteres")
        String street,

        @NotBlank(message = "Número é obrigatório")
        @Size(max = 30, message = "Número deve ter no máximo 30 caracteres")
        String number,

        @NotBlank(message = "Estado é obrigatório")
        @Size(max = 80, message = "Estado deve ter no máximo 80 caracteres")
        String state,

        @NotBlank(message = "Cidade é obrigatória")
        @Size(max = 120, message = "Cidade deve ter no máximo 120 caracteres")
        String city,

        @NotBlank(message = "Bairro é obrigatório")
        @Size(max = 120, message = "Bairro deve ter no máximo 120 caracteres")
        String neighborhood,

        @Size(max = 255, message = "Complemento deve ter no máximo 255 caracteres")
        String complement,

        @NotBlank(message = "CEP é obrigatório")
        @Size(max = 20, message = "CEP deve ter no máximo 20 caracteres")
        String postalCode,

        Boolean isDefault
) {
}

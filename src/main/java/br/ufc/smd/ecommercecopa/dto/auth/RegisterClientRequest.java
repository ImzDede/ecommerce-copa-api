package br.ufc.smd.ecommercecopa.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterClientRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 60)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).+$", message = "Senha deve ter pelo menos uma letra maiúscula, uma letra minúscula e um caractere especial")
        String password,
        @NotBlank @Pattern(regexp = "^\\d{11}$", message = "CPF deve ter 11 dígitos") String cpf,
        @NotBlank String dateOfBirth
) {
}

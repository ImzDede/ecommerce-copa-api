package br.ufc.smd.ecommercecopa.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAdminMeRequest(
        @Size(min = 2, max = 120) String name,
        @Email String email,
        @Size(min = 6, max = 60) String password
) {
}

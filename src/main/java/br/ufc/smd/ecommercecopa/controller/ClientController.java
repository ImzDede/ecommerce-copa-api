package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.client.ClientMeResponse;
import br.ufc.smd.ecommercecopa.dto.client.UpdateClientMeRequest;
import br.ufc.smd.ecommercecopa.service.ClientService;
import br.ufc.smd.ecommercecopa.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Cliente", description = "Rotas privadas do cliente autenticado.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class ClientController {

    private final ClientService clientService;
    private final UserService userService;

    public ClientController(ClientService clientService, UserService userService) {
        this.clientService = clientService;
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil do cliente", description = "Retorna o perfil do cliente da sessão atual.")
    public ResponseEntity<ApiResponse<ClientMeResponse>> me(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(clientService.me(session)));
    }

    @PatchMapping("/me")
    @Operation(summary = "Atualizar perfil do cliente", description = "Altera nome, email e/ou senha do cliente autenticado.")
    public ResponseEntity<ApiResponse<ClientMeResponse>> updateMe(@Valid @RequestBody UpdateClientMeRequest request,
                                                                  HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(clientService.updateMe(request, session)));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Excluir conta do cliente", description = "Atalho legado para DELETE /api/users/me.")
    public ResponseEntity<Void> deleteMe(HttpSession session) {
        userService.deleteMe(session);
        return ResponseEntity.noContent().build();
    }
}

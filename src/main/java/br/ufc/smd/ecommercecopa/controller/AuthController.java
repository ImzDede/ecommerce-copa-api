package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.auth.AuthUserResponse;
import br.ufc.smd.ecommercecopa.dto.auth.LoginRequest;
import br.ufc.smd.ecommercecopa.dto.auth.RegisterClientRequest;
import br.ufc.smd.ecommercecopa.dto.auth.RegisterClientResponse;
import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, login, logout e usuário autenticado atual.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/client")
    @Operation(summary = "Cadastrar cliente", description = "Cria um usuário cliente e seu perfil de cliente.")
    public ResponseEntity<ApiResponse<RegisterClientResponse>> registerClient(@Valid @RequestBody RegisterClientRequest request) {
        RegisterClientResponse response = authService.registerClient(request);
        return ResponseEntity.status(201).body(new ApiResponse<>(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica por email e senha, criando uma sessão HTTP com cookie JSESSIONID.")
    public ResponseEntity<ApiResponse<AuthUserResponse>> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        AuthUserResponse response = authService.login(request, session);
        return ResponseEntity.ok(new ApiResponse<>(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalida a sessão HTTP atual.")
    @SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
    public ResponseEntity<Void> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário autenticado", description = "Retorna os dados básicos do usuário da sessão atual.")
    @SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
    public ResponseEntity<ApiResponse<AuthUserResponse>> me(HttpSession session) {
        AuthUserResponse response = authService.me(session);
        return ResponseEntity.ok(new ApiResponse<>(response));
    }
}

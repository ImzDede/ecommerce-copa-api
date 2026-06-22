package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.admin.AdminMeResponse;
import br.ufc.smd.ecommercecopa.dto.admin.UpdateAdminMeRequest;
import br.ufc.smd.ecommercecopa.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Rotas privadas do administrador autenticado.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil do admin", description = "Retorna o perfil do admin da sessão atual.")
    public ResponseEntity<ApiResponse<AdminMeResponse>> me(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(adminService.me(session)));
    }

    @PatchMapping("/me")
    @Operation(summary = "Atualizar perfil do admin", description = "Altera nome, email e/ou senha do admin autenticado.")
    public ResponseEntity<ApiResponse<AdminMeResponse>> updateMe(@Valid @RequestBody UpdateAdminMeRequest request,
                                                                 HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(adminService.updateMe(request, session)));
    }
}

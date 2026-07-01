package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.client.ClientResponse;
import br.ufc.smd.ecommercecopa.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/clients")
@Tag(name = "Admin - Clientes", description = "Gestão de clientes para o administrador.")
@SecurityRequirement(name = br.ufc.smd.ecommercecopa.config.OpenApiConfig.SESSION_AUTH)
public class AdminClientController {

    private final AdminService adminService;

    public AdminClientController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os clientes", description = "Retorna a lista de todos os clientes cadastrados.")
    public ResponseEntity<ApiResponse<List<ClientResponse>>> list(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(adminService.listClients(session)));
    }
}

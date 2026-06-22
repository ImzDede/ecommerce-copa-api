package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.order.OrderListResponse;
import br.ufc.smd.ecommercecopa.dto.order.OrderResponse;
import br.ufc.smd.ecommercecopa.dto.order.UpdateOrderStatusRequest;
import br.ufc.smd.ecommercecopa.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin - Pedidos", description = "Listagem e cancelamento administrativo de pedidos.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Listar pedidos")
    public ResponseEntity<ApiResponse<OrderListResponse>> list(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(orderService.listAdmin(session)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido")
    public ResponseEntity<ApiResponse<OrderResponse>> findById(@PathVariable UUID id, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(orderService.findAdmin(id, session)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status do pedido", description = "Aceita PROCESSING, SHIPPED, DELIVERED ou CANCELED.")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable UUID id,
                                                                   @Valid @RequestBody UpdateOrderStatusRequest request,
                                                                   HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(orderService.updateStatusAdmin(id, request, session)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar venda", description = "Faz soft delete do pedido e restaura o estoque dos SKUs.")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable UUID id, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(orderService.cancelAdmin(id, session)));
    }
}

package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.order.CreateOrderRequest;
import br.ufc.smd.ecommercecopa.dto.order.OrderListResponse;
import br.ufc.smd.ecommercecopa.dto.order.OrderResponse;
import br.ufc.smd.ecommercecopa.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Pedidos", description = "Checkout e pedidos do cliente autenticado.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Finalizar pedido", description = "Cria um pedido a partir do carrinho e decrementa o estoque dos SKUs.")
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request,
                                                             HttpSession session) {
        return ResponseEntity.status(201).body(new ApiResponse<>(orderService.create(request, session)));
    }

    @GetMapping
    @Operation(summary = "Listar meus pedidos", description = "Retorna pedidos confirmados e cancelados do cliente autenticado.")
    public ResponseEntity<ApiResponse<OrderListResponse>> listMine(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(orderService.listMine(session)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar meu pedido")
    public ResponseEntity<ApiResponse<OrderResponse>> findMine(@PathVariable UUID id, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(orderService.findMine(id, session)));
    }
}

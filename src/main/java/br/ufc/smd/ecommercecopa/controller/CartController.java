package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.cart.CartItemRequest;
import br.ufc.smd.ecommercecopa.dto.cart.CartResponse;
import br.ufc.smd.ecommercecopa.dto.cart.UpdateCartItemRequest;
import br.ufc.smd.ecommercecopa.service.CartService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Carrinho", description = "Rotas privadas do carrinho do cliente autenticado.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Visualizar carrinho")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.getMyCart(session)));
    }

    @PostMapping("/items")
    @Operation(summary = "Adicionar item ao carrinho", description = "Adiciona um SKU ou incrementa a quantidade se ele já estiver no carrinho.")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@Valid @RequestBody CartItemRequest request,
                                                             HttpSession session) {
        return ResponseEntity.status(201).body(new ApiResponse<>(cartService.addItem(request, session)));
    }

    @PatchMapping("/items/{skuId}")
    @Operation(summary = "Atualizar item do carrinho", description = "Define a quantidade exata do SKU no carrinho.")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(@PathVariable UUID skuId,
                                                                @Valid @RequestBody UpdateCartItemRequest request,
                                                                HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.updateItem(skuId, request, session)));
    }

    @DeleteMapping("/items/{skuId}")
    @Operation(summary = "Remover item do carrinho")
    public ResponseEntity<ApiResponse<CartResponse>> deleteItem(@PathVariable UUID skuId, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(cartService.deleteItem(skuId, session)));
    }

    @DeleteMapping
    @Operation(summary = "Limpar carrinho")
    public ResponseEntity<Void> clear(HttpSession session) {
        cartService.clear(session);
        return ResponseEntity.noContent().build();
    }
}

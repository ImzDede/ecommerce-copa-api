package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.review.CreateReviewRequest;
import br.ufc.smd.ecommercecopa.dto.review.ReviewListResponse;
import br.ufc.smd.ecommercecopa.dto.review.ReviewResponse;
import br.ufc.smd.ecommercecopa.dto.review.UpdateReviewRequest;
import br.ufc.smd.ecommercecopa.service.ReviewService;
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
@RequestMapping("/api/reviews")
@Tag(name = "Avaliações", description = "Rotas privadas de avaliações de SKUs feitas por clientes.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/me")
    @Operation(summary = "Listar minhas avaliações")
    public ResponseEntity<ApiResponse<ReviewListResponse>> listMine(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(reviewService.listMine(session)));
    }

    @PostMapping
    @Operation(summary = "Criar avaliação", description = "Cria uma avaliação do cliente autenticado para um SKU ativo.")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@Valid @RequestBody CreateReviewRequest request,
                                                              HttpSession session) {
        return ResponseEntity.status(201).body(new ApiResponse<>(reviewService.create(request, session)));
    }

    @PatchMapping("/{skuId}")
    @Operation(summary = "Atualizar avaliação", description = "Atualiza a avaliação do cliente autenticado para o SKU informado.")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(@PathVariable UUID skuId,
                                                              @Valid @RequestBody UpdateReviewRequest request,
                                                              HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(reviewService.update(skuId, request, session)));
    }

    @DeleteMapping("/{skuId}")
    @Operation(summary = "Excluir avaliação", description = "Remove a avaliação do cliente autenticado para o SKU informado.")
    public ResponseEntity<Void> delete(@PathVariable UUID skuId, HttpSession session) {
        reviewService.delete(skuId, session);
        return ResponseEntity.noContent().build();
    }
}

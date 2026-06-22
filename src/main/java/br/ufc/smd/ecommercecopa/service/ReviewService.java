package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.review.CreateReviewRequest;
import br.ufc.smd.ecommercecopa.dto.review.ReviewListResponse;
import br.ufc.smd.ecommercecopa.dto.review.ReviewResponse;
import br.ufc.smd.ecommercecopa.dto.review.UpdateReviewRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.model.Review;
import br.ufc.smd.ecommercecopa.model.ReviewId;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.ReviewRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final AuthService authService;
    private final ReviewRepository reviewRepository;
    private final SkuRepository skuRepository;

    public ReviewService(AuthService authService, ReviewRepository reviewRepository, SkuRepository skuRepository) {
        this.authService = authService;
        this.reviewRepository = reviewRepository;
        this.skuRepository = skuRepository;
    }

    @Transactional(readOnly = true)
    public ReviewListResponse listBySku(UUID skuId) {
        requireActiveSku(skuId);
        return new ReviewListResponse(reviewRepository.findBySku_IdOrderByCreatedAtDesc(skuId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public ReviewListResponse listMine(HttpSession session) {
        Client client = authService.requireClient(session);
        return new ReviewListResponse(reviewRepository.findByClient_UserIdOrderByCreatedAtDesc(client.getUserId()).stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional
    public ReviewResponse create(CreateReviewRequest request, HttpSession session) {
        Client client = authService.requireClient(session);
        Sku sku = requireActiveSku(request.skuId());
        ReviewId id = new ReviewId(client.getUserId(), sku.getId());

        if (reviewRepository.existsById(id)) {
            throw new AppException("DUPLICATE_RESOURCE", "Avaliação já cadastrada para este SKU", HttpStatus.CONFLICT);
        }

        Review review = new Review();
        review.setId(id);
        review.setClient(client);
        review.setSku(sku);
        review.setStars(request.stars());
        review.setComment(normalizeRequiredText(request.comment(), "Comentário é obrigatório"));

        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse update(UUID skuId, UpdateReviewRequest request, HttpSession session) {
        Client client = authService.requireClient(session);
        boolean hasStars = request.stars() != null;
        boolean hasComment = request.comment() != null;

        if (!hasStars && !hasComment) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Informe ao menos um campo para atualizar", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Review review = requireOwnReview(client.getUserId(), skuId);
        if (hasStars) {
            review.setStars(request.stars());
        }
        if (hasComment) {
            review.setComment(normalizeRequiredText(request.comment(), "Comentário é obrigatório"));
        }

        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public void delete(UUID skuId, HttpSession session) {
        Client client = authService.requireClient(session);
        Review review = requireOwnReview(client.getUserId(), skuId);
        reviewRepository.delete(review);
    }

    private Review requireOwnReview(UUID clientId, UUID skuId) {
        return reviewRepository.findById(new ReviewId(clientId, skuId))
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Avaliação não encontrada", HttpStatus.NOT_FOUND));
    }

    private Sku requireActiveSku(UUID skuId) {
        Sku sku = skuRepository.findByIdAndDeletedAtIsNull(skuId)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND));
        if (sku.getProduct().getDeletedAt() != null) {
            throw new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND);
        }
        return sku;
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value.trim();
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getClient().getUserId(),
                review.getClient().getUser().getName(),
                review.getSku().getId(),
                review.getSku().getTitle(),
                review.getStars(),
                review.getComment(),
                review.getCreatedAt().toString()
        );
    }
}

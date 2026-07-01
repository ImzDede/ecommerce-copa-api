package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.catalog.CatalogCategoryResponse;
import br.ufc.smd.ecommercecopa.dto.catalog.CatalogProductDetailResponse;
import br.ufc.smd.ecommercecopa.dto.catalog.CatalogSkuListResponse;
import br.ufc.smd.ecommercecopa.dto.catalog.CatalogSkuOptionResponse;
import br.ufc.smd.ecommercecopa.dto.catalog.CatalogSkuResponse;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import br.ufc.smd.ecommercecopa.repository.ReviewRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import br.ufc.smd.ecommercecopa.repository.projection.SkuReviewStatsProjection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final TagService tagService;

    public CatalogService(SkuRepository skuRepository,
                          ProductRepository productRepository,
                          ReviewRepository reviewRepository,
                          TagService tagService) {
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.tagService = tagService;
    }

    @Transactional(readOnly = true)
    public CatalogSkuListResponse listSkus(List<String> category, String q, int page, int size, String sort) {
        validatePagination(page, size);

        String normalizedSort = normalizeSort(sort);
        List<String> categories = normalizeCategories(category);
        List<String> repositoryCategories = categories.isEmpty() ? List.of("__none__") : categories;
        String query = normalizeFilter(q);

        if ("rating".equals(normalizedSort)) {
            List<Sku> skus = skuRepository.searchPublicCatalogForRating(repositoryCategories, categories.size(), query);
            Map<UUID, ReviewStats> statsBySkuId = loadReviewStats(skus);
            List<Sku> sortedSkus = skus.stream()
                    .sorted(ratingComparator(statsBySkuId))
                    .toList();
            List<Sku> pageItems = paginate(sortedSkus, page, size);

            return new CatalogSkuListResponse(
                    pageItems.stream().map(sku -> toCatalogSkuResponse(sku, statsBySkuId)).toList(),
                    page,
                    size,
                    (long) sortedSkus.size(),
                    totalPages(sortedSkus.size(), size)
            );
        }

        Pageable pageable = PageRequest.of(page - 1, size, resolveSort(sort));
        Page<Sku> result = skuRepository.searchPublicCatalog(repositoryCategories, categories.size(), query, pageable);
        Map<UUID, ReviewStats> statsBySkuId = loadReviewStats(result.getContent());

        return new CatalogSkuListResponse(
                result.getContent().stream().map(sku -> toCatalogSkuResponse(sku, statsBySkuId)).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public CatalogProductDetailResponse findProduct(UUID productId, UUID skuId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Produto não encontrado", HttpStatus.NOT_FOUND));

        List<Sku> skus = skuRepository.findByProduct_IdAndDeletedAtIsNull(product.getId(), Sort.by(Sort.Direction.ASC, "title"));
        if (skus.isEmpty()) {
            throw new AppException("RESOURCE_NOT_FOUND", "Produto não encontrado", HttpStatus.NOT_FOUND);
        }

        Sku selectedSku = selectSku(skus, skuId);
        Map<UUID, ReviewStats> statsBySkuId = loadReviewStats(skus);

        return new CatalogProductDetailResponse(
                product.getId(),
                toCategoryResponse(product.getCategory()),
                product.getSchema(),
                selectedSku.getId(),
                toSkuOptionResponse(selectedSku, statsBySkuId),
                skus.stream().map(sku -> toSkuOptionResponse(sku, statsBySkuId)).toList()
        );
    }

    private Sku selectSku(List<Sku> skus, UUID skuId) {
        if (skuId != null) {
            return skus.stream()
                    .filter(sku -> sku.getId().equals(skuId))
                    .findFirst()
                    .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado para este produto", HttpStatus.NOT_FOUND));
        }

        return skus.stream()
                .filter(sku -> sku.getStock() > 0)
                .min(Comparator.comparing(Sku::getTitle))
                .orElse(skus.getFirst());
    }

    private CatalogSkuResponse toCatalogSkuResponse(Sku sku, Map<UUID, ReviewStats> statsBySkuId) {
        ReviewStats stats = statsBySkuId.getOrDefault(sku.getId(), ReviewStats.empty());
        return new CatalogSkuResponse(
                sku.getId(),
                sku.getProduct().getId(),
                sku.getTitle(),
                sku.getDescription(),
                sku.getPrice(),
                sku.getOriginalPrice(),
                firstPhoto(sku),
                sku.getStock(),
                stats.rating(),
                stats.reviewCount(),
                sku.getAttributes(),
                toCategoryResponse(sku.getProduct().getCategory()),
                tagService.listSkuTags(sku.getId())
        );
    }

    private CatalogSkuOptionResponse toSkuOptionResponse(Sku sku, Map<UUID, ReviewStats> statsBySkuId) {
        ReviewStats stats = statsBySkuId.getOrDefault(sku.getId(), ReviewStats.empty());
        return new CatalogSkuOptionResponse(
                sku.getId(),
                sku.getTitle(),
                sku.getDescription(),
                sku.getPrice(),
                sku.getOriginalPrice(),
                firstPhoto(sku),
                sku.getStock(),
                stats.rating(),
                stats.reviewCount(),
                sku.getAttributes(),
                tagService.listSkuTags(sku.getId())
        );
    }

    private CatalogCategoryResponse toCategoryResponse(Category category) {
        return new CatalogCategoryResponse(category.getId(), category.getSlug(), category.getTitle(), category.getImage(), category.isFeatured());
    }

    private String firstPhoto(Sku sku) {
        List<String> photos = sku.getPhotos();
        return photos == null || photos.isEmpty() ? null : photos.getFirst();
    }

    private Sort resolveSort(String sort) {
        return switch (normalizeSort(sort)) {
            case "relevance" -> Sort.by(Sort.Direction.ASC, "title");
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new AppException("VALIDATION_ERROR", "Ordenação inválida", HttpStatus.BAD_REQUEST);
        };
    }

    private void validatePagination(int page, int size) {
        if (page < 1) {
            throw new AppException("VALIDATION_ERROR", "Página deve ser maior que zero", HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException("VALIDATION_ERROR", "Tamanho da página deve estar entre 1 e 100", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "relevance";
        }
        return sort.trim();
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeCategories(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private Map<UUID, ReviewStats> loadReviewStats(List<Sku> skus) {
        if (skus.isEmpty()) {
            return Map.of();
        }

        List<UUID> skuIds = skus.stream().map(Sku::getId).toList();
        Map<UUID, ReviewStats> statsBySkuId = new HashMap<>();
        for (SkuReviewStatsProjection projection : reviewRepository.findStatsBySkuIds(skuIds)) {
            statsBySkuId.put(projection.getSkuId(), new ReviewStats(
                    roundRating(projection.getRating()),
                    projection.getReviewCount()
            ));
        }
        return statsBySkuId;
    }

    private Double roundRating(Double rating) {
        if (rating == null) {
            return 0.0;
        }
        return Math.round(rating * 10.0) / 10.0;
    }

    private Comparator<Sku> ratingComparator(Map<UUID, ReviewStats> statsBySkuId) {
        return Comparator
                .comparingDouble((Sku sku) -> statsBySkuId.getOrDefault(sku.getId(), ReviewStats.empty()).rating())
                .reversed()
                .thenComparing(Comparator.comparingLong((Sku sku) -> statsBySkuId.getOrDefault(sku.getId(), ReviewStats.empty()).reviewCount()).reversed())
                .thenComparing(Sku::getTitle);
    }

    private List<Sku> paginate(List<Sku> skus, int page, int size) {
        int fromIndex = (page - 1) * size;
        if (fromIndex >= skus.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, skus.size());
        return skus.subList(fromIndex, toIndex);
    }

    private int totalPages(int totalItems, int size) {
        if (totalItems == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / size);
    }

    private record ReviewStats(Double rating, Long reviewCount) {
        private static ReviewStats empty() {
            return new ReviewStats(0.0, 0L);
        }
    }
}

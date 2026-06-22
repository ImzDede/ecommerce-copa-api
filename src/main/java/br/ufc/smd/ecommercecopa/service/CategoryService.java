package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.category.CategoryListResponse;
import br.ufc.smd.ecommercecopa.dto.category.CategoryResponse;
import br.ufc.smd.ecommercecopa.dto.category.CreateCategoryRequest;
import br.ufc.smd.ecommercecopa.dto.category.UpdateCategoryRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.repository.CategoryRepository;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final AuthService authService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(AuthService authService,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository) {
        this.authService = authService;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CategoryListResponse list(HttpSession session) {
        authService.requireAdmin(session);

        return new CategoryListResponse(categoryRepository.findByDeletedAtIsNull(categorySort())
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public CategoryListResponse listPublic() {
        return new CategoryListResponse(categoryRepository.findByDeletedAtIsNull(categorySort())
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        return toResponse(requireCategory(id));
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request, HttpSession session) {
        authService.requireAdmin(session);

        String title = normalizeTitle(request.title());
        Category category = new Category();
        category.setTitle(title);
        category.setSlug(generateUniqueSlug(title, null));
        category.setImage(normalizeOptionalText(request.image()));
        category.setFeatured(Boolean.TRUE.equals(request.featured()));

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request, HttpSession session) {
        authService.requireAdmin(session);

        boolean hasTitle = request.title() != null;
        boolean hasImage = request.image() != null;
        boolean hasFeatured = request.featured() != null;
        if (!hasTitle && !hasImage && !hasFeatured) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Informe ao menos um campo para atualizar", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Category category = requireCategory(id);
        if (hasTitle) {
            String title = normalizeTitle(request.title());
            category.setTitle(title);
            category.setSlug(generateUniqueSlug(title, category.getId()));
        }
        if (hasImage) {
            category.setImage(normalizeOptionalText(request.image()));
        }
        if (hasFeatured) {
            category.setFeatured(Boolean.TRUE.equals(request.featured()));
        }

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id, HttpSession session) {
        authService.requireAdmin(session);

        Category category = requireCategory(id);
        if (productRepository.existsByCategory_IdAndDeletedAtIsNull(category.getId())) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Categoria possui produtos vinculados", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        category.setDeletedAt(LocalDateTime.now());
    }

    private Category requireCategory(UUID id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Categoria não encontrada", HttpStatus.NOT_FOUND));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getSlug(), category.getTitle(), category.getImage(), category.isFeatured());
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Título é obrigatório", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return title.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Sort categorySort() {
        return Sort.by(Sort.Order.desc("featured"), Sort.Order.asc("title"));
    }

    private String generateUniqueSlug(String title, UUID ignoredId) {
        String baseSlug = slugify(title);
        String candidate = baseSlug;
        int suffix = 2;

        while (slugExists(candidate, ignoredId)) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private boolean slugExists(String slug, UUID ignoredId) {
        if (ignoredId == null) {
            return categoryRepository.existsBySlug(slug);
        }
        return categoryRepository.existsBySlugAndIdNot(slug, ignoredId);
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        return slug.isBlank() ? "categoria" : slug;
    }
}

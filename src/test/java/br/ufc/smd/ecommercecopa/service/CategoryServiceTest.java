package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.category.CreateCategoryRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.CategoryRepository;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private HttpSession session;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(authService, categoryRepository, productRepository);
        doReturn(new User()).when(authService).requireAdmin(session);
    }

    @Test
    void createGeneratesUniqueSlugWithSuffix() {
        when(categoryRepository.existsBySlug("albuns-da-copa")).thenReturn(true);
        when(categoryRepository.existsBySlug("albuns-da-copa-2")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
            return category;
        });

        var response = categoryService.create(new CreateCategoryRequest("Álbuns da Copa", " /uploads/categories/copa.png ", true), session);

        assertEquals("albuns-da-copa-2", response.slug());
        assertEquals("Álbuns da Copa", response.title());
        assertEquals("/uploads/categories/copa.png", response.image());
        assertEquals(true, response.featured());
    }

    @Test
    void deleteBlocksCategoryWithLinkedProducts() {
        UUID categoryId = UUID.randomUUID();
        Category category = category(categoryId);
        when(categoryRepository.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategory_IdAndDeletedAtIsNull(categoryId)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> categoryService.delete(categoryId, session));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteSoftDeletesCategoryWithoutActiveProducts() {
        UUID categoryId = UUID.randomUUID();
        Category category = category(categoryId);
        when(categoryRepository.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategory_IdAndDeletedAtIsNull(categoryId)).thenReturn(false);

        categoryService.delete(categoryId, session);

        assertNotNull(category.getDeletedAt());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    private Category category(UUID id) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", id);
        category.setSlug("chuteiras");
        category.setTitle("Chuteiras");
        return category;
    }
}

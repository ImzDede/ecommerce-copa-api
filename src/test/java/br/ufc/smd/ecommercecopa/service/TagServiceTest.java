package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.tag.CreateTagRequest;
import br.ufc.smd.ecommercecopa.dto.tag.UpdateSkuTagsRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.model.SkuTag;
import br.ufc.smd.ecommercecopa.model.Tag;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import br.ufc.smd.ecommercecopa.repository.SkuTagRepository;
import br.ufc.smd.ecommercecopa.repository.TagRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private SkuTagRepository skuTagRepository;

    @Mock
    private HttpSession session;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(authService, tagRepository, skuRepository, skuTagRepository);
    }

    @Test
    void createRejectsDuplicateText() {
        when(authService.requireAdmin(session)).thenReturn(new User());
        when(tagRepository.existsByTextIgnoreCase("Oferta")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> tagService.create(new CreateTagRequest(" Oferta ", "#f00"), session));

        assertEquals("DUPLICATE_RESOURCE", exception.getCode());
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void replaceSkuTagsDeletesOldLinksAndSavesNewOnes() {
        UUID skuId = UUID.randomUUID();
        Tag first = tag(UUID.randomUUID(), "Oferta", "#f00");
        Tag second = tag(UUID.randomUUID(), "Novo", "#0f0");
        when(authService.requireAdmin(session)).thenReturn(new User());
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku(skuId)));
        when(tagRepository.findAllById(any())).thenReturn(List.of(first, second));

        var response = tagService.replaceSkuTags(skuId, new UpdateSkuTagsRequest(List.of(first.getId(), second.getId())), session);

        assertEquals(2, response.items().size());
        verify(skuTagRepository).deleteBySku_Id(skuId);
        verify(skuTagRepository).saveAll(any());
    }

    @Test
    void deleteRejectsTagLinkedToSku() {
        UUID tagId = UUID.randomUUID();
        when(authService.requireAdmin(session)).thenReturn(new User());
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag(tagId, "Oferta", "#f00")));
        when(skuTagRepository.existsByTag_Id(tagId)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> tagService.delete(tagId, session));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        verify(tagRepository, never()).delete(any(Tag.class));
    }

    private Tag tag(UUID id, String text, String color) {
        Tag tag = new Tag();
        ReflectionTestUtils.setField(tag, "id", id);
        tag.setText(text);
        tag.setColor(color);
        return tag;
    }

    private Sku sku(UUID id) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        category.setSlug("chuteiras");
        category.setTitle("Chuteiras");

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        product.setCategory(category);
        product.setSchema(Map.of("selectors", List.of(Map.of("key", "size", "label", "Tamanho"))));

        Sku sku = new Sku();
        ReflectionTestUtils.setField(sku, "id", id);
        sku.setProduct(product);
        sku.setTitle("Chuteira Campo 40");
        sku.setDescription("Chuteira para campo");
        sku.setPrice(new BigDecimal("399.90"));
        sku.setStock(10);
        sku.setAttributes(Map.of("size", "40"));
        return sku;
    }
}

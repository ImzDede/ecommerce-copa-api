package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.tag.CreateTagRequest;
import br.ufc.smd.ecommercecopa.dto.tag.TagListResponse;
import br.ufc.smd.ecommercecopa.dto.tag.TagResponse;
import br.ufc.smd.ecommercecopa.dto.tag.UpdateSkuTagsRequest;
import br.ufc.smd.ecommercecopa.dto.tag.UpdateTagRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.model.SkuTag;
import br.ufc.smd.ecommercecopa.model.SkuTagId;
import br.ufc.smd.ecommercecopa.model.Tag;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import br.ufc.smd.ecommercecopa.repository.SkuTagRepository;
import br.ufc.smd.ecommercecopa.repository.TagRepository;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final AuthService authService;
    private final TagRepository tagRepository;
    private final SkuRepository skuRepository;
    private final SkuTagRepository skuTagRepository;

    public TagService(AuthService authService,
                      TagRepository tagRepository,
                      SkuRepository skuRepository,
                      SkuTagRepository skuTagRepository) {
        this.authService = authService;
        this.tagRepository = tagRepository;
        this.skuRepository = skuRepository;
        this.skuTagRepository = skuTagRepository;
    }

    @Transactional(readOnly = true)
    public TagListResponse list(HttpSession session) {
        authService.requireAdmin(session);
        return new TagListResponse(tagRepository.findAllByOrderByTextAsc().stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public TagResponse findById(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        return toResponse(requireTag(id));
    }

    @Transactional
    public TagResponse create(CreateTagRequest request, HttpSession session) {
        authService.requireAdmin(session);
        String text = normalizeRequiredText(request.text(), "Texto é obrigatório");
        if (tagRepository.existsByTextIgnoreCase(text)) {
            throw new AppException("DUPLICATE_RESOURCE", "Tag já cadastrada", HttpStatus.CONFLICT);
        }

        Tag tag = new Tag();
        tag.setText(text);
        tag.setColor(normalizeRequiredText(request.color(), "Cor é obrigatória"));
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse update(UUID id, UpdateTagRequest request, HttpSession session) {
        authService.requireAdmin(session);
        boolean hasText = request.text() != null;
        boolean hasColor = request.color() != null;

        if (!hasText && !hasColor) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Informe ao menos um campo para atualizar", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Tag tag = requireTag(id);
        if (hasText) {
            String text = normalizeRequiredText(request.text(), "Texto é obrigatório");
            if (tagRepository.existsByTextIgnoreCaseAndIdNot(text, tag.getId())) {
                throw new AppException("DUPLICATE_RESOURCE", "Tag já cadastrada", HttpStatus.CONFLICT);
            }
            tag.setText(text);
        }
        if (hasColor) {
            tag.setColor(normalizeRequiredText(request.color(), "Cor é obrigatória"));
        }

        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public void delete(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        Tag tag = requireTag(id);
        if (skuTagRepository.existsByTag_Id(tag.getId())) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Tag vinculada a SKU não pode ser excluída", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        tagRepository.delete(tag);
    }

    @Transactional
    public TagListResponse replaceSkuTags(UUID skuId, UpdateSkuTagsRequest request, HttpSession session) {
        authService.requireAdmin(session);
        Sku sku = skuRepository.findByIdAndDeletedAtIsNull(skuId)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND));
        if (sku.getProduct().getDeletedAt() != null) {
            throw new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND);
        }

        Set<UUID> tagIds = new LinkedHashSet<>(request.tagIds());
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new AppException("RESOURCE_NOT_FOUND", "Tag não encontrada", HttpStatus.NOT_FOUND);
        }

        skuTagRepository.deleteBySku_Id(sku.getId());
        List<SkuTag> skuTags = tags.stream()
                .map(tag -> newSkuTag(sku, tag))
                .toList();
        skuTagRepository.saveAll(skuTags);

        return new TagListResponse(tags.stream().map(this::toResponse).toList());
    }

    public List<TagResponse> listSkuTags(UUID skuId) {
        return skuTagRepository.findBySku_Id(skuId).stream()
                .map(SkuTag::getTag)
                .map(this::toResponse)
                .toList();
    }

    private SkuTag newSkuTag(Sku sku, Tag tag) {
        SkuTag skuTag = new SkuTag();
        skuTag.setId(new SkuTagId(sku.getId(), tag.getId()));
        skuTag.setSku(sku);
        skuTag.setTag(tag);
        return skuTag;
    }

    private Tag requireTag(UUID id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Tag não encontrada", HttpStatus.NOT_FOUND));
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value.trim();
    }

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getText(), tag.getColor());
    }
}

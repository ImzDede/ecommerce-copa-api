package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);

    Optional<Category> findByIdAndDeletedAtIsNull(UUID id);

    List<Category> findByDeletedAtIsNull(Sort sort);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}

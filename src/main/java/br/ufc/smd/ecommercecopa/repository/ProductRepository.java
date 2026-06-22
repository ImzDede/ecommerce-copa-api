package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByCategory_Id(UUID categoryId);

    boolean existsByCategory_IdAndDeletedAtIsNull(UUID categoryId);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    List<Product> findByDeletedAtIsNull(Sort sort);

    List<Product> findByCategory_SlugAndDeletedAtIsNull(String categorySlug, Sort sort);
}

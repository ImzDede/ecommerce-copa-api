package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Sku;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface SkuRepository extends JpaRepository<Sku, UUID> {
    @Query(value = """
            select s from Sku s
            join fetch s.product p
            join fetch p.category c
            where s.deletedAt is null
              and p.deletedAt is null
              and s.stock > 0
              and (:categoryCount = 0 or c.slug in :categories)
              and (coalesce(:q, '') = '' or lower(s.title) like lower(concat('%', coalesce(:q, ''), '%')) or lower(s.description) like lower(concat('%', coalesce(:q, ''), '%')))
            """,
            countQuery = """
            select count(s) from Sku s
            join s.product p
            join p.category c
            where s.deletedAt is null
              and p.deletedAt is null
              and s.stock > 0
              and (:categoryCount = 0 or c.slug in :categories)
              and (coalesce(:q, '') = '' or lower(s.title) like lower(concat('%', coalesce(:q, ''), '%')) or lower(s.description) like lower(concat('%', coalesce(:q, ''), '%')))
            """)
    Page<Sku> searchPublicCatalog(@Param("categories") List<String> categories,
                                   @Param("categoryCount") int categoryCount,
                                   @Param("q") String q,
                                   Pageable pageable);

    @Query("""
            select s from Sku s
            join fetch s.product p
            join fetch p.category c
            where s.deletedAt is null
              and p.deletedAt is null
              and s.stock > 0
              and (:categoryCount = 0 or c.slug in :categories)
              and (coalesce(:q, '') = '' or lower(s.title) like lower(concat('%', coalesce(:q, ''), '%')) or lower(s.description) like lower(concat('%', coalesce(:q, ''), '%')))
            """)
    List<Sku> searchPublicCatalogForRating(@Param("categories") List<String> categories,
                                            @Param("categoryCount") int categoryCount,
                                            @Param("q") String q);

    List<Sku> findByProduct_IdAndDeletedAtIsNull(UUID productId);

    List<Sku> findByProduct_IdAndDeletedAtIsNull(UUID productId, Sort sort);

    List<Sku> findByProduct_Category_SlugAndDeletedAtIsNull(String categorySlug, Sort sort);

    List<Sku> findByDeletedAtIsNull(Sort sort);

    @Query("""
            select s from Sku s
            join fetch s.product p
            join fetch p.category c
            where s.deletedAt is null
              and p.deletedAt is null
              and s.stock <= 0
            order by lower(coalesce(s.description, s.title)) asc, lower(s.title) asc
            """)
    List<Sku> findOutOfStockSkus();

    Optional<Sku> findByIdAndDeletedAtIsNull(UUID id);

    long countByProduct_IdAndDeletedAtIsNull(UUID productId);
}

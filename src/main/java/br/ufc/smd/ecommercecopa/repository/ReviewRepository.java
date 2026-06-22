package br.ufc.smd.ecommercecopa.repository;

import br.ufc.smd.ecommercecopa.model.Review;
import br.ufc.smd.ecommercecopa.model.ReviewId;
import br.ufc.smd.ecommercecopa.repository.projection.SkuReviewStatsProjection;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, ReviewId> {
    List<Review> findBySku_IdOrderByCreatedAtDesc(UUID skuId);

    List<Review> findByClient_UserIdOrderByCreatedAtDesc(UUID clientId);

    @Query("""
            select r.sku.id as skuId,
                   avg(r.stars) as rating,
                   count(r) as reviewCount
            from Review r
            where r.sku.id in :skuIds
            group by r.sku.id
            """)
    List<SkuReviewStatsProjection> findStatsBySkuIds(@Param("skuIds") Collection<UUID> skuIds);
}

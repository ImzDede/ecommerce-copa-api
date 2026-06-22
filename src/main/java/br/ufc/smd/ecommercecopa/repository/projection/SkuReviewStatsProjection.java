package br.ufc.smd.ecommercecopa.repository.projection;

import java.util.UUID;

public interface SkuReviewStatsProjection {
    UUID getSkuId();

    Double getRating();

    Long getReviewCount();
}

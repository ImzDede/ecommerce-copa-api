package br.ufc.smd.ecommercecopa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SkuTagId implements Serializable {

    @Column(name = "sku_id")
    private UUID skuId;

    @Column(name = "tag_id")
    private UUID tagId;

    public SkuTagId() {
    }

    public SkuTagId(UUID skuId, UUID tagId) {
        this.skuId = skuId;
        this.tagId = tagId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public UUID getTagId() {
        return tagId;
    }

    public void setTagId(UUID tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SkuTagId skuTagId)) {
            return false;
        }
        return Objects.equals(skuId, skuTagId.skuId) && Objects.equals(tagId, skuTagId.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skuId, tagId);
    }
}

package br.ufc.smd.ecommercecopa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ReviewId implements Serializable {

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "sku_id")
    private UUID skuId;

    public ReviewId() {
    }

    public ReviewId(UUID clientId, UUID skuId) {
        this.clientId = clientId;
        this.skuId = skuId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReviewId reviewId)) {
            return false;
        }
        return Objects.equals(clientId, reviewId.clientId) && Objects.equals(skuId, reviewId.skuId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, skuId);
    }
}

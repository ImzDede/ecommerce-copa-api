package br.ufc.smd.ecommercecopa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CartId implements Serializable {

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "sku_id")
    private UUID skuId;

    public CartId() {
    }

    public CartId(UUID clientId, UUID skuId) {
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
        if (!(o instanceof CartId cartId)) {
            return false;
        }
        return Objects.equals(clientId, cartId.clientId) && Objects.equals(skuId, cartId.skuId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, skuId);
    }
}

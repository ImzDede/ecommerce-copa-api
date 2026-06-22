package br.ufc.smd.ecommercecopa.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface ClientPurchasesProjection {
    UUID getClientId();

    String getClientName();

    Long getTotalOrders();

    BigDecimal getTotalValue();
}

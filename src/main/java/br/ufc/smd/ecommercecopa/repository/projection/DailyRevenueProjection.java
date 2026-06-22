package br.ufc.smd.ecommercecopa.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRevenueProjection {
    LocalDate getDay();

    BigDecimal getTotalValue();
}

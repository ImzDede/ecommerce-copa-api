package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.report.ClientPurchaseReportItem;
import br.ufc.smd.ecommercecopa.dto.report.ClientPurchaseReportResponse;
import br.ufc.smd.ecommercecopa.dto.report.DailyRevenueReportItem;
import br.ufc.smd.ecommercecopa.dto.report.DailyRevenueReportResponse;
import br.ufc.smd.ecommercecopa.dto.report.OutOfStockSkuReportItem;
import br.ufc.smd.ecommercecopa.dto.report.OutOfStockSkuReportResponse;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.OrderStatus;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.OrderRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import br.ufc.smd.ecommercecopa.repository.projection.ClientPurchasesProjection;
import br.ufc.smd.ecommercecopa.repository.projection.DailyRevenueProjection;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportService {

    private final AuthService authService;
    private final OrderRepository orderRepository;
    private final SkuRepository skuRepository;

    public AdminReportService(AuthService authService, OrderRepository orderRepository, SkuRepository skuRepository) {
        this.authService = authService;
        this.orderRepository = orderRepository;
        this.skuRepository = skuRepository;
    }

    @Transactional(readOnly = true)
    public ClientPurchaseReportResponse purchasesByClient(String startDate, String endDate, HttpSession session) {
        authService.requireAdmin(session);
        DateRange range = parseDateRange(startDate, endDate);

        return new ClientPurchaseReportResponse(orderRepository.findClientPurchasesReport(range.start(), range.endExclusive(), OrderStatus.CANCELED)
                .stream()
                .map(this::toClientPurchaseItem)
                .toList());
    }

    @Transactional(readOnly = true)
    public OutOfStockSkuReportResponse outOfStockSkus(HttpSession session) {
        authService.requireAdmin(session);
        return new OutOfStockSkuReportResponse(skuRepository.findOutOfStockSkus().stream()
                .map(this::toOutOfStockSkuItem)
                .toList());
    }

    @Transactional(readOnly = true)
    public DailyRevenueReportResponse dailyRevenue(String startDate, String endDate, HttpSession session) {
        authService.requireAdmin(session);
        DateRange range = parseDateRange(startDate, endDate);

        return new DailyRevenueReportResponse(orderRepository.findDailyRevenueReport(range.start(), range.endExclusive()).stream()
                .map(this::toDailyRevenueItem)
                .toList());
    }

    private ClientPurchaseReportItem toClientPurchaseItem(ClientPurchasesProjection projection) {
        return new ClientPurchaseReportItem(
                projection.getClientId(),
                projection.getClientName(),
                projection.getTotalOrders(),
                projection.getTotalValue()
        );
    }

    private OutOfStockSkuReportItem toOutOfStockSkuItem(Sku sku) {
        var category = sku.getProduct().getCategory();
        return new OutOfStockSkuReportItem(
                sku.getId(),
                sku.getProduct().getId(),
                sku.getTitle(),
                sku.getStock(),
                sku.getPrice(),
                category.getSlug(),
                category.getTitle()
        );
    }

    private DailyRevenueReportItem toDailyRevenueItem(DailyRevenueProjection projection) {
        return new DailyRevenueReportItem(projection.getDay().toString(), projection.getTotalValue());
    }

    private DateRange parseDateRange(String startDate, String endDate) {
        if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
            throw new AppException("VALIDATION_ERROR", "Período é obrigatório", HttpStatus.BAD_REQUEST);
        }

        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(startDate.trim());
            end = LocalDate.parse(endDate.trim());
        } catch (Exception exception) {
            throw new AppException("VALIDATION_ERROR", "Data deve estar no formato yyyy-MM-dd", HttpStatus.BAD_REQUEST);
        }

        if (start.isAfter(end)) {
            throw new AppException("VALIDATION_ERROR", "Data inicial deve ser menor ou igual à data final", HttpStatus.BAD_REQUEST);
        }

        return new DateRange(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private record DateRange(LocalDateTime start, LocalDateTime endExclusive) {
    }
}

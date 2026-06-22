package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.report.ClientPurchaseReportResponse;
import br.ufc.smd.ecommercecopa.dto.report.DailyRevenueReportResponse;
import br.ufc.smd.ecommercecopa.dto.report.OutOfStockSkuReportResponse;
import br.ufc.smd.ecommercecopa.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "Admin - Relatórios", description = "Relatórios administrativos de vendas e estoque.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping("/purchases-by-client")
    @Operation(summary = "Compras por cliente", description = "Agrupa pedidos não cancelados por cliente no período informado.")
    public ResponseEntity<ApiResponse<ClientPurchaseReportResponse>> purchasesByClient(@RequestParam String startDate,
                                                                                       @RequestParam String endDate,
                                                                                       HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(adminReportService.purchasesByClient(startDate, endDate, session)));
    }

    @GetMapping("/out-of-stock-skus")
    @Operation(summary = "SKUs sem estoque", description = "Lista SKUs ativos com estoque zerado ou negativo.")
    public ResponseEntity<ApiResponse<OutOfStockSkuReportResponse>> outOfStockSkus(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(adminReportService.outOfStockSkus(session)));
    }

    @GetMapping("/daily-revenue")
    @Operation(summary = "Receita diária", description = "Soma o valor recebido por dia considerando pedidos não cancelados no período.")
    public ResponseEntity<ApiResponse<DailyRevenueReportResponse>> dailyRevenue(@RequestParam String startDate,
                                                                                @RequestParam String endDate,
                                                                                HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(adminReportService.dailyRevenue(startDate, endDate, session)));
    }
}

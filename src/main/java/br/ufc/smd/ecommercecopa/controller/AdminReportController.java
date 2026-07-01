package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "Admin - Relatórios", description = "Geração de relatórios administrativos em PDF.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AdminReportController {

    private final AdminReportService reportService;

    public AdminReportController(AdminReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(value = "/purchases-by-client", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Relatório de compras por cliente (PDF)", description = "Gera PDF com total de compras por cliente em um período.")
    public ResponseEntity<byte[]> purchasesByClient(
            @Parameter(description = "Data inicial no formato yyyy-MM-dd", example = "2026-01-01")
            @RequestParam String startDate,
            @Parameter(description = "Data final no formato yyyy-MM-dd", example = "2026-01-31")
            @RequestParam String endDate,
            HttpSession session) {
        byte[] pdf = reportService.generatePurchasesByClientPdf(startDate, endDate, session);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=compras_clientes.pdf")
                .body(pdf);
    }

    @GetMapping(value = "/out-of-stock-skus", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Relatório de produtos sem estoque (PDF)", description = "Gera PDF com produtos que estão faltando em estoque.")
    public ResponseEntity<byte[]> outOfStock(HttpSession session) {
        byte[] pdf = reportService.generateOutOfStockPdf(session);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sem_estoque.pdf")
                .body(pdf);
    }

    @GetMapping(value = "/daily-revenue", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Relatório de receita diária (PDF)", description = "Gera PDF com valor total recebido por dia em um período.")
    public ResponseEntity<byte[]> dailyRevenue(
            @Parameter(description = "Data inicial no formato yyyy-MM-dd", example = "2026-01-01")
            @RequestParam String startDate,
            @Parameter(description = "Data final no formato yyyy-MM-dd", example = "2026-01-31")
            @RequestParam String endDate,
            HttpSession session) {
        byte[] pdf = reportService.generateDailyRevenuePdf(startDate, endDate, session);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receita_diaria.pdf")
                .body(pdf);
    }
}

package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.report.ClientPurchaseReportItem;
import br.ufc.smd.ecommercecopa.dto.report.ClientPurchaseReportResponse;
import br.ufc.smd.ecommercecopa.dto.report.DailyRevenueReportItem;
import br.ufc.smd.ecommercecopa.dto.report.DailyRevenueReportResponse;
import br.ufc.smd.ecommercecopa.dto.report.OutOfStockSkuReportItem;
import br.ufc.smd.ecommercecopa.dto.report.OutOfStockSkuReportResponse;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Order;
import br.ufc.smd.ecommercecopa.model.OrderStatus;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.OrderRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportService {

    private final AuthService authService;
    private final OrderRepository orderRepository;
    private final SkuRepository skuRepository;
    private final Path uploadRoot;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final java.awt.Color PRIMARY_BLUE = new java.awt.Color(37, 99, 235);
    private static final java.awt.Color BRAND_GREEN = new java.awt.Color(34, 197, 94);
    private static final java.awt.Color BRAND_RED = new java.awt.Color(185, 28, 28);
    private static final java.awt.Color SLATE_900 = new java.awt.Color(15, 23, 42);
    private static final java.awt.Color SLATE_800 = new java.awt.Color(30, 41, 59);
    private static final java.awt.Color SLATE_500 = new java.awt.Color(100, 116, 139);
    private static final java.awt.Color SLATE_200 = new java.awt.Color(226, 232, 240);
    private static final java.awt.Color SLATE_100 = new java.awt.Color(241, 245, 249);
    private static final java.awt.Color BLUE_50 = new java.awt.Color(239, 246, 255);
    private static final java.awt.Color BLUE_100 = new java.awt.Color(219, 234, 254);

    public AdminReportService(AuthService authService,
                              OrderRepository orderRepository,
                              SkuRepository skuRepository,
                              @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.authService = authService;
        this.orderRepository = orderRepository;
        this.skuRepository = skuRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public ClientPurchaseReportResponse purchasesByClient(String startDate, String endDate, HttpSession session) {
        authService.requireAdmin(session);
        DateRange range = parseDateRange(startDate, endDate);

        Map<UUID, ClientPurchaseAccumulator> purchasesByClient = new LinkedHashMap<>();
        for (Order order : orderRepository.findReportOrders(range.start(), range.endExclusive(), OrderStatus.CANCELED)) {
            UUID clientId = order.getClient().getUserId();
            String clientName = order.getClient().getUser().getName();
            purchasesByClient.computeIfAbsent(clientId, ignored -> new ClientPurchaseAccumulator(clientId, clientName))
                    .add(order.getTotalValue());
        }

        return new ClientPurchaseReportResponse(purchasesByClient.values().stream()
                .sorted(Comparator
                        .comparingLong(ClientPurchaseAccumulator::totalOrders).reversed()
                        .thenComparing(ClientPurchaseAccumulator::clientName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(ClientPurchaseAccumulator::toItem)
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

        TreeMap<LocalDate, BigDecimal> totalsByDay = new TreeMap<>();
        for (Order order : orderRepository.findReportOrders(range.start(), range.endExclusive(), OrderStatus.CANCELED)) {
            LocalDate day = order.getCreatedAt().toLocalDate();
            totalsByDay.merge(day, order.getTotalValue(), BigDecimal::add);
        }

        return new DailyRevenueReportResponse(totalsByDay.entrySet().stream()
                .map(entry -> new DailyRevenueReportItem(entry.getKey().toString(), entry.getValue()))
                .toList());
    }

    public byte[] generatePurchasesByClientPdf(String startDate, String endDate, HttpSession session) {
        ClientPurchaseReportResponse report = purchasesByClient(startDate, endDate, session);
        List<List<ReportCell>> rows = report.items().stream()
                .map(item -> List.of(
                        ReportCell.text(safeUuid(item.clientId())),
                        ReportCell.text(safeText(item.clientName(), "Cliente sem nome")),
                        ReportCell.text(String.valueOf(item.totalOrders() == null ? 0L : item.totalOrders())),
                        ReportCell.text(formatCurrency(item.totalValue()))
                ))
                .toList();

        return buildPdf(
                "Compras por Cliente",
                "Período: " + formatInputDate(startDate) + " a " + formatInputDate(endDate),
                new String[]{"ID Cliente", "Nome", "Qtd. Compras", "Valor Total"},
                new float[]{4.5f, 3.2f, 1.4f, 1.8f},
                rows
        );
    }

    public byte[] generateOutOfStockPdf(HttpSession session) {
        OutOfStockSkuReportResponse report = outOfStockSkus(session);
        List<List<ReportCell>> rows = report.items().stream()
                .map(item -> List.of(
                        ReportCell.image(item.photo()),
                        ReportCell.text(safeUuid(item.skuId())),
                        ReportCell.text(safeText(item.description(), "Sem descrição")),
                        ReportCell.text(safeText(item.categoryTitle(), "Sem categoria")),
                        ReportCell.text(String.valueOf(item.stock() == null ? 0 : item.stock())),
                        ReportCell.text(formatCurrency(item.price()))
                ))
                .toList();

        return buildPdf(
                "Produtos Sem Estoque",
                "SKUs ativos com estoque zerado ou negativo",
                new String[]{"Foto", "ID", "Descrição", "Categoria", "Estoque", "Preço"},
                new float[]{1.1f, 3.2f, 3.2f, 2f, 1f, 1.5f},
                rows
        );
    }

    public byte[] generateDailyRevenuePdf(String startDate, String endDate, HttpSession session) {
        DailyRevenueReportResponse report = dailyRevenue(startDate, endDate, session);
        List<List<ReportCell>> rows = report.items().stream()
                .map(item -> List.of(
                        ReportCell.text(formatInputDate(item.day())),
                        ReportCell.text(formatCurrency(item.totalValue()))
                ))
                .toList();

        return buildPdf(
                "Receita Diária",
                "Período: " + formatInputDate(startDate) + " a " + formatInputDate(endDate),
                new String[]{"Data", "Valor Recebido"},
                new float[]{2f, 3f},
                rows
        );
    }

    private byte[] buildPdf(String title, String subtitle, String[] headers, float[] widths, List<List<ReportCell>> rows) {
        Document document = new Document(PageSize.A4, 36, 36, 42, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addReportHeader(document, title, subtitle);

            if (rows.isEmpty()) {
                Font emptyFont = new Font(Font.HELVETICA, 12, Font.ITALIC, SLATE_500);
                Paragraph empty = new Paragraph("Nenhum registro encontrado para este relatório.", emptyFont);
                empty.setAlignment(Element.ALIGN_CENTER);
                empty.setSpacingBefore(36f);
                document.add(empty);
            } else {
                PdfPTable table = new PdfPTable(headers.length);
                table.setWidthPercentage(100);
                table.setWidths(widths);
                table.setSpacingBefore(6f);

                for (String header : headers) {
                    addHeaderCell(table, header);
                }
                for (List<ReportCell> row : rows) {
                    for (ReportCell cell : row) {
                        addBodyCell(table, cell);
                    }
                }
                document.add(table);
            }

            document.close();
        } catch (Exception e) {
            throw new AppException("INTERNAL_ERROR", "Erro ao gerar PDF", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return baos.toByteArray();
    }

    private void addReportHeader(Document document, String title, String subtitle) throws DocumentException {
        PdfPTable stripe = new PdfPTable(3);
        stripe.setWidthPercentage(100);
        stripe.setWidths(new float[]{1, 1, 1});
        stripe.addCell(colorCell(PRIMARY_BLUE));
        stripe.addCell(colorCell(BRAND_GREEN));
        stripe.addCell(colorCell(BRAND_RED));
        document.add(stripe);

        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(SLATE_900);
        headerCell.setBorderColor(SLATE_900);
        headerCell.setPaddingTop(18f);
        headerCell.setPaddingBottom(18f);
        headerCell.setPaddingLeft(20f);
        headerCell.setPaddingRight(20f);

        Font brandFont = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.WHITE);
        Paragraph brand = new Paragraph("ECOMMERCE COPA", brandFont);
        brand.setSpacingAfter(4f);
        headerCell.addElement(brand);

        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, java.awt.Color.WHITE);
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setSpacingAfter(6f);
        headerCell.addElement(titleParagraph);

        Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new java.awt.Color(203, 213, 225));
        headerCell.addElement(new Paragraph(subtitle, subtitleFont));
        headerCell.addElement(new Paragraph("Gerado em " + LocalDate.now().format(DATE_FORMAT), subtitleFont));
        header.addCell(headerCell);
        header.setSpacingAfter(18f);
        document.add(header);
    }

    private PdfPCell colorCell(java.awt.Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setFixedHeight(5f);
        cell.setBackgroundColor(color);
        cell.setBorderColor(color);
        return cell;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_BLUE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BLUE_50);
        cell.setPadding(8f);
        cell.setBorderColor(BLUE_100);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, ReportCell reportCell) {
        if (reportCell.imagePath() != null && !reportCell.imagePath().isBlank()) {
            addImageCell(table, reportCell.imagePath());
            return;
        }

        Font font = new Font(Font.HELVETICA, 10, Font.NORMAL, SLATE_900);
        PdfPCell cell = new PdfPCell(new Phrase(safeText(reportCell.text(), ""), font));
        cell.setPadding(7f);
        cell.setBorderColor(SLATE_200);
        table.addCell(cell);
    }

    private void addImageCell(PdfPTable table, String publicPath) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5f);
        cell.setBorderColor(SLATE_200);
        cell.setBackgroundColor(SLATE_100);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        try {
            Path imagePath = resolvePublicUpload(publicPath);
            if (imagePath != null && Files.exists(imagePath)) {
                Image image = Image.getInstance(imagePath.toString());
                image.scaleToFit(42f, 42f);
                cell.addElement(image);
            } else {
                cell.setPhrase(new Phrase("Sem foto", new Font(Font.HELVETICA, 8, Font.ITALIC, SLATE_500)));
            }
        } catch (Exception ignored) {
            cell.setPhrase(new Phrase("Sem foto", new Font(Font.HELVETICA, 8, Font.ITALIC, SLATE_500)));
        }
        table.addCell(cell);
    }

    private OutOfStockSkuReportItem toOutOfStockSkuItem(Sku sku) {
        var category = sku.getProduct().getCategory();
        return new OutOfStockSkuReportItem(
                sku.getId(),
                sku.getProduct().getId(),
                firstPhoto(sku),
                sku.getDescription() == null || sku.getDescription().isBlank() ? sku.getTitle() : sku.getDescription(),
                sku.getStock(),
                sku.getPrice(),
                category.getSlug(),
                category.getTitle()
        );
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value == null ? BigDecimal.ZERO : value);
    }

    private String firstPhoto(Sku sku) {
        List<String> photos = sku.getPhotos();
        return photos == null || photos.isEmpty() ? null : photos.getFirst();
    }

    private String safeUuid(UUID value) {
        return value == null ? "-" : value.toString();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Path resolvePublicUpload(String publicPath) {
        if (publicPath == null || publicPath.isBlank() || !publicPath.startsWith("/uploads/")) {
            return null;
        }
        Path target = uploadRoot.resolve(publicPath.substring("/uploads/".length())).normalize();
        return target.startsWith(uploadRoot) ? target : null;
    }

    private String formatInputDate(String value) {
        return LocalDate.parse(value).format(DATE_FORMAT);
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

    private record ReportCell(String text, String imagePath) {
        private static ReportCell text(String value) {
            return new ReportCell(value, null);
        }

        private static ReportCell image(String publicPath) {
            return new ReportCell(null, publicPath);
        }
    }

    private static class ClientPurchaseAccumulator {
        private final UUID clientId;
        private final String clientName;
        private long totalOrders;
        private BigDecimal totalValue = BigDecimal.ZERO;

        private ClientPurchaseAccumulator(UUID clientId, String clientName) {
            this.clientId = clientId;
            this.clientName = clientName;
        }

        private void add(BigDecimal value) {
            totalOrders++;
            totalValue = totalValue.add(value == null ? BigDecimal.ZERO : value);
        }

        private Long totalOrders() {
            return totalOrders;
        }

        private String clientName() {
            return clientName;
        }

        private ClientPurchaseReportItem toItem() {
            return new ClientPurchaseReportItem(clientId, clientName, totalOrders, totalValue);
        }
    }
}

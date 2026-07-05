package com.retrofit.backend.service;

import com.retrofit.backend.dto.ProjectItemResponseDto;
import com.retrofit.backend.model.Project;
import com.retrofit.backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayOutputStream;
import com.retrofit.backend.dto.GroupedProgressReportDto;
import com.retrofit.backend.dto.ProgressReportResponseDto;
import com.retrofit.backend.dto.StockSummaryDTO;
import com.retrofit.backend.model.InventoryTransaction;
import com.retrofit.backend.repository.InventoryTransactionRepository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final TemplateEngine templateEngine;
    private final ProjectItemService projectItemService;
    private final ProjectRepository projectRepository;
    private final ProgressReportService progressReportService; // Add dependency
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public byte[] generateApuReport(Long projectId) throws Exception {
        // 1. Obtener datos
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
                
        List<ProjectItemResponseDto> items = projectItemService.getItemsByProjectId(projectId);

        Context context = new Context();
        context.setVariable("logoBase64", getLogoBase64());
        
        context.setVariable("project", project);
        context.setVariable("items", items);
        context.setVariable("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // 3. Renderizar HTML
        String html = templateEngine.process("reporte-apu", context);

        // 4. Limpiar a XHTML con JSoup (Flying Saucer requiere XML válido)
        Document document = Jsoup.parse(html, "UTF-8");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        String xhtml = document.html();

        // 5. Generar PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(xhtml);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }

    public byte[] generateProgressReport(Long projectId, LocalDate startDate, LocalDate endDate, String itemCode) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        List<GroupedProgressReportDto> groupedReports = progressReportService.getFilteredAndGroupedReports(projectId, startDate, endDate, itemCode);
        
        List<ProgressReportResponseDto> flatReports = new ArrayList<>();
        boolean hasPhotos = false;
        for (GroupedProgressReportDto group : groupedReports) {
            flatReports.addAll(group.getReports());
            for(ProgressReportResponseDto report : group.getReports()) {
                if(report.getPhotoUrls() != null && !report.getPhotoUrls().isEmpty()) {
                    hasPhotos = true;
                }
            }
        }

        Context context = new Context();
        context.setVariable("logoBase64", getLogoBase64());
        context.setVariable("project", project);
        context.setVariable("flatReports", flatReports);
        context.setVariable("hasPhotos", hasPhotos);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        context.setVariable("startDateStr", startDate != null ? startDate.format(formatter) : "[DD/MM/AAAA]");
        context.setVariable("endDateStr", endDate != null ? endDate.format(formatter) : "[DD/MM/AAAA]");
        context.setVariable("currentDate", LocalDate.now().format(formatter));

        String html = templateEngine.process("reporte-avances", context);

        Document document = Jsoup.parse(html, "UTF-8");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        String xhtml = document.html();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(xhtml);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }

    public byte[] generateInventoryReport(Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        List<StockSummaryDTO> stockSummary = inventoryTransactionRepository.getProjectStockSummary(projectId, "", Pageable.unpaged()).getContent();
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProjectIdOrderByTransactionDateDesc(projectId);

        Context context = new Context();
        context.setVariable("logoBase64", getLogoBase64());
        context.setVariable("project", project);
        context.setVariable("stockSummary", stockSummary);
        context.setVariable("transactions", transactions);
        context.setVariable("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        String html = templateEngine.process("reporte-inventario", context);

        Document document = Jsoup.parse(html, "UTF-8");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        String xhtml = document.html();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(xhtml);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }

    private String getLogoBase64() {
        try {
            java.io.InputStream is = new org.springframework.core.io.ClassPathResource("templates/logo_base64.txt").getInputStream();
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ""; // Fallback si no lo encuentra
        }
    }
}

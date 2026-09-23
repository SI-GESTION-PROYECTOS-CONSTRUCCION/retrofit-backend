package com.retrofit.backend.service;

import com.retrofit.backend.dto.ProjectItemResponseDto;
import com.retrofit.backend.dto.GanttItemResponseDto;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProjectIdWithDetailsOrderByTransactionDateDesc(projectId);

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

    public byte[] generateGanttReport(Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        List<GanttItemResponseDto> ganttItems = projectItemService.getGanttItems(projectId);
        Map<Long, GanttItemResponseDto> itemById = new HashMap<>();
        Map<Long, List<GanttItemResponseDto>> childrenByParent = new HashMap<>();

        for (GanttItemResponseDto item : ganttItems) {
            itemById.put(item.getId(), item);
            if (item.getParentId() != null) {
                childrenByParent.computeIfAbsent(item.getParentId(), ignored -> new ArrayList<>()).add(item);
            }
        }

        Map<Long, DateRange> rangeCache = new HashMap<>();
        Map<Long, LocalDate> starts = new HashMap<>();
        Map<Long, LocalDate> ends = new HashMap<>();
        for (GanttItemResponseDto item : ganttItems) {
            DateRange range = resolveRange(item, itemById, childrenByParent, rangeCache, new java.util.HashSet<>());
            starts.put(item.getId(), range.start());
            ends.put(item.getId(), range.end());
        }
        return new GanttPdfRenderer().render(project, ganttItems, starts, ends);
    }

    private DateRange resolveRange(
            GanttItemResponseDto item,
            Map<Long, GanttItemResponseDto> itemById,
            Map<Long, List<GanttItemResponseDto>> childrenByParent,
            Map<Long, DateRange> cache,
            java.util.Set<Long> visiting) {
        DateRange cached = cache.get(item.getId());
        if (cached != null) return cached;
        if (!visiting.add(item.getId())) return fallbackRange(item);

        LocalDate start = item.getStartDate();
        LocalDate end = item.getEndDate() != null ? item.getEndDate().minusDays(1) : null;
        if (start == null && end != null) start = end;

        for (GanttItemResponseDto child : childrenByParent.getOrDefault(item.getId(), List.of())) {
            DateRange childRange = resolveRange(child, itemById, childrenByParent, cache, visiting);
            if (start == null || childRange.start().isBefore(start)) start = childRange.start();
            if (end == null || childRange.end().isAfter(end)) end = childRange.end();
        }

        if (start == null) start = LocalDate.now();
        if (end == null || end.isBefore(start)) {
            end = start.plusDays(Math.max(item.getBaseDurationDays() != null ? item.getBaseDurationDays() : 1, 1) - 1L);
        }

        DateRange range = new DateRange(start, end);
        cache.put(item.getId(), range);
        visiting.remove(item.getId());
        return range;
    }

    private DateRange fallbackRange(GanttItemResponseDto item) {
        LocalDate start = item.getStartDate() != null ? item.getStartDate() : LocalDate.now();
        LocalDate end = item.getEndDate() != null
                ? item.getEndDate().minusDays(1)
                : start.plusDays(Math.max(item.getBaseDurationDays() != null ? item.getBaseDurationDays() : 1, 1) - 1L);
        return new DateRange(start, end.isBefore(start) ? start : end);
    }

    private record DateRange(LocalDate start, LocalDate end) {}

    private String cachedLogoBase64 = null;

    private synchronized String getLogoBase64() {
        if (cachedLogoBase64 != null) {
            return cachedLogoBase64;
        }
        try {
            java.io.InputStream is = new org.springframework.core.io.ClassPathResource("templates/logo_base64.txt").getInputStream();
            cachedLogoBase64 = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            cachedLogoBase64 = ""; // Fallback si no lo encuentra
        }
        return cachedLogoBase64;
    }
}

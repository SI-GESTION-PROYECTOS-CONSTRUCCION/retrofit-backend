package com.retrofit.backend.service;

import com.retrofit.backend.dto.ProjectItemResponseDto;
import com.retrofit.backend.dto.GanttItemResponseDto;
import com.retrofit.backend.dto.GanttReportItemDto;
import com.retrofit.backend.dto.GanttTimelineTickDto;
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
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private static final double GANTT_TRACK_WIDTH_MM = 276.0;
    private static final double GANTT_TRACK_INSET_MM = 3.5;
    private static final double GANTT_TRACK_USABLE_WIDTH_MM = GANTT_TRACK_WIDTH_MM - (GANTT_TRACK_INSET_MM * 2);
    private static final int GANTT_ROWS_PER_PAGE = 28;

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
        List<GanttReportItemDto> reportItems = new ArrayList<>();
        for (GanttItemResponseDto item : ganttItems) {
            DateRange range = resolveRange(item, itemById, childrenByParent, rangeCache, new java.util.HashSet<>());

            GanttReportItemDto reportItem = new GanttReportItemDto();
            reportItem.setCode(item.getCode());
            reportItem.setName(item.getName());
            reportItem.setStartDate(range.start());
            reportItem.setEndDate(range.end());
            reportItem.setDurationDays((int) (ChronoUnit.DAYS.between(range.start(), range.end()) + 1));
            reportItem.setProgress(clampProgress(item.getCurrentProgressPercentage()));
            reportItem.setLevel(item.getLevel() != null ? item.getLevel() : 0);
            reportItem.setPaddingLeft("padding-left: " + (7 + (reportItem.getLevel() * 14)) + "px;");
            reportItem.setParent("project".equals(item.getType()) || childrenByParent.containsKey(item.getId()));
            reportItems.add(reportItem);
        }

        LocalDate timelineStart = reportItems.stream()
                .map(GanttReportItemDto::getStartDate)
                .min(Comparator.naturalOrder())
                .orElse(project.getStartDate() != null ? project.getStartDate() : LocalDate.now());
        LocalDate timelineEnd = reportItems.stream()
                .map(GanttReportItemDto::getEndDate)
                .max(Comparator.naturalOrder())
                .orElse(timelineStart);
        long timelineDays = Math.max(1, ChronoUnit.DAYS.between(timelineStart, timelineEnd) + 1);

        for (GanttReportItemDto item : reportItems) {
            long offsetDays = ChronoUnit.DAYS.between(timelineStart, item.getStartDate());
            long durationDays = ChronoUnit.DAYS.between(item.getStartDate(), item.getEndDate()) + 1;
            item.setOffsetPercent((offsetDays * 100.0) / timelineDays);
            item.setWidthPercent(Math.max(1.2, (durationDays * 100.0) / timelineDays));

            double offsetMm = GANTT_TRACK_INSET_MM + Math.max(0,
                    Math.min(GANTT_TRACK_USABLE_WIDTH_MM, item.getOffsetPercent() * GANTT_TRACK_USABLE_WIDTH_MM / 100.0));
            double widthMm = Math.max(2.5, Math.min(GANTT_TRACK_WIDTH_MM - GANTT_TRACK_INSET_MM - offsetMm,
                    item.getWidthPercent() * GANTT_TRACK_USABLE_WIDTH_MM / 100.0));
            item.setBarStyle(String.format(java.util.Locale.ROOT,
                    "left: %.3fmm; width: %.3fmm;", offsetMm, widthMm));
        }

        List<GanttTimelineTickDto> timelineTicks = buildTimelineTicks(timelineStart, timelineEnd, timelineDays);
        List<List<GanttReportItemDto>> reportPages = paginateGanttItems(reportItems);

        Context context = new Context();
        context.setVariable("logoBase64", getLogoBase64());
        context.setVariable("project", project);
        context.setVariable("pages", reportPages);
        context.setVariable("totalPages", reportPages.size());
        context.setVariable("timelineStart", timelineStart.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        context.setVariable("timelineEnd", timelineEnd.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        context.setVariable("timelineTicks", timelineTicks);
        context.setVariable("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        String html = templateEngine.process("reporte-cronograma", context);
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

    private List<List<GanttReportItemDto>> paginateGanttItems(List<GanttReportItemDto> items) {
        List<List<GanttReportItemDto>> pages = new ArrayList<>();
        List<GanttReportItemDto> currentPage = new ArrayList<>();
        int blockStart = 0;

        while (blockStart < items.size()) {
            int blockEnd = blockStart + 1;
            int parentLevel = items.get(blockStart).getLevel();
            while (blockEnd < items.size() && items.get(blockEnd).getLevel() > parentLevel) {
                blockEnd++;
            }

            List<GanttReportItemDto> block = items.subList(blockStart, blockEnd);
            if (!currentPage.isEmpty()
                    && block.size() <= GANTT_ROWS_PER_PAGE
                    && currentPage.size() + block.size() > GANTT_ROWS_PER_PAGE) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
            }

            int itemIndex = 0;
            while (itemIndex < block.size()) {
                int availableRows = GANTT_ROWS_PER_PAGE - currentPage.size();
                if (availableRows == 0) {
                    pages.add(currentPage);
                    currentPage = new ArrayList<>();
                    availableRows = GANTT_ROWS_PER_PAGE;
                }
                int batchEnd = Math.min(itemIndex + availableRows, block.size());
                currentPage.addAll(block.subList(itemIndex, batchEnd));
                itemIndex = batchEnd;
            }
            blockStart = blockEnd;
        }

        if (!currentPage.isEmpty()) {
            pages.add(currentPage);
        }
        if (pages.isEmpty()) {
            pages.add(new ArrayList<>());
        }
        return pages;
    }

    private List<GanttTimelineTickDto> buildTimelineTicks(LocalDate timelineStart, LocalDate timelineEnd, long timelineDays) {
        List<GanttTimelineTickDto> ticks = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-PE"));
        LocalDate tickDate = timelineStart;
        LocalDate lastTickDate = null;

        while (!tickDate.isAfter(timelineEnd)) {
            addTimelineTick(ticks, tickDate, timelineStart, timelineEnd, timelineDays, formatter);
            lastTickDate = tickDate;
            tickDate = tickDate.plusDays(7);
        }

        if (lastTickDate == null || !timelineEnd.equals(lastTickDate)) {
            if (ticks.size() > 1 && ChronoUnit.DAYS.between(lastTickDate, timelineEnd) < 4) {
                ticks.remove(ticks.size() - 1);
            }
            addTimelineTick(ticks, timelineEnd, timelineStart, timelineEnd, timelineDays, formatter);
        }

        return ticks;
    }

    private void addTimelineTick(
            List<GanttTimelineTickDto> ticks,
            LocalDate tickDate,
            LocalDate timelineStart,
            LocalDate timelineEnd,
            long timelineDays,
            DateTimeFormatter formatter) {
        long offsetDays = ChronoUnit.DAYS.between(timelineStart, tickDate);
        double positionMm = GANTT_TRACK_INSET_MM + ((offsetDays * GANTT_TRACK_USABLE_WIDTH_MM) / Math.max(1, timelineDays - 1));
        String positionStyle = String.format(Locale.ROOT, "left: %.3fmm;", positionMm);
        String labelStyle;

        if (tickDate.equals(timelineStart)) {
            labelStyle = positionStyle;
        } else if (tickDate.equals(timelineEnd)) {
            labelStyle = String.format(Locale.ROOT, "left: %.3fmm; margin-left: -26mm;", positionMm);
        } else {
            labelStyle = String.format(Locale.ROOT, "left: %.3fmm; margin-left: -13mm;", positionMm);
        }

        ticks.add(new GanttTimelineTickDto(tickDate.format(formatter), positionStyle, labelStyle));
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

    private double clampProgress(Double progress) {
        return Math.max(0, Math.min(100, progress != null ? progress : 0));
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

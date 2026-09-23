package com.retrofit.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.retrofit.backend.dto.GanttDependencyDto;
import com.retrofit.backend.dto.GanttItemResponseDto;
import com.retrofit.backend.model.DependencyType;
import com.retrofit.backend.model.Project;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Paginates both the activity list and time range without shrinking the chart to one page. */
final class GanttPdfRenderer {
    private static final int ROWS = 35;
    private static final int DAYS_PER_SEGMENT = 120;
    private static final float LEFT = 32, NAME_WIDTH = 305, CHART_WIDTH = 820, ROW_HEIGHT = 18;
    private static final Color INK = new Color(25, 59, 74);
    private static final Color TEAL = new Color(40, 169, 161);
    private static final Color GRID = new Color(218, 232, 233);
    private static final Rectangle LANDSCAPE_A3 = new Rectangle(PageSize.A3.getHeight(), PageSize.A3.getWidth());
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    byte[] render(Project project, List<GanttItemResponseDto> items,
                  Map<Long, LocalDate> starts, Map<Long, LocalDate> inclusiveEnds) throws Exception {
        List<PageSpec> pages = paginate(items, starts, inclusiveEnds);
        Map<Long, String> codes = new HashMap<>();
        for (GanttItemResponseDto item : items) codes.put(item.getId(), item.getCode());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(LANDSCAPE_A3, 0, 0, 0, 0);
        PdfWriter writer = PdfWriter.getInstance(document, output);
        BaseFont font = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        document.open();
        for (int index = 0; index < pages.size(); index++) {
            PageSpec page = pages.get(index);
            document.newPage();
            draw(writer.getDirectContent(), font, bold, project, page.rows(), codes, starts, inclusiveEnds,
                    page.from(), page.to(), index + 1, pages.size());
        }
        document.close();
        return output.toByteArray();
    }

    private List<PageSpec> paginate(List<GanttItemResponseDto> items,
                                    Map<Long, LocalDate> starts, Map<Long, LocalDate> ends) {
        List<PageSpec> pages = new ArrayList<>();
        if (items.isEmpty()) {
            pages.add(new PageSpec(List.of(), LocalDate.now(), LocalDate.now()));
            return pages;
        }
        int startIndex = 0;
        while (startIndex < items.size()) {
            int endIndex = Math.min(startIndex + ROWS, items.size());
            // Keep a heading with at least one of its activities on the next page.
            while (endIndex < items.size() && endIndex > startIndex + 1
                    && isParent(items.get(endIndex - 1), items)) {
                endIndex--;
            }
            List<GanttItemResponseDto> block = items.subList(startIndex, endIndex);
            LocalDate first = block.stream().map(item -> starts.get(item.getId()))
                    .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(LocalDate.now());
            LocalDate last = block.stream().map(item -> ends.get(item.getId()))
                    .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(first);
            for (LocalDate from = first; !from.isAfter(last); from = from.plusDays(DAYS_PER_SEGMENT)) {
                LocalDate to = from.plusDays(DAYS_PER_SEGMENT - 1);
                if (to.isAfter(last)) to = last;
                LocalDate pageFrom = from, pageTo = to;
                List<GanttItemResponseDto> visible = block.stream().filter(item -> {
                    LocalDate start = starts.get(item.getId()), end = ends.get(item.getId());
                    return start != null && end != null && !end.isBefore(pageFrom) && !start.isAfter(pageTo);
                }).toList();
                if (!visible.isEmpty()) pages.add(new PageSpec(visible, from, to));
            }
            startIndex = endIndex;
        }
        return pages;
    }

    private record PageSpec(List<GanttItemResponseDto> rows, LocalDate from, LocalDate to) {}

    private void draw(PdfContentByte canvas, BaseFont font, BaseFont bold, Project project,
                      List<GanttItemResponseDto> rows, Map<Long, String> codes, Map<Long, LocalDate> starts,
                      Map<Long, LocalDate> ends, LocalDate from, LocalDate to, int page, int totalPages) {
        float top = LANDSCAPE_A3.getHeight();
        float graphX = LEFT + NAME_WIDTH;
        float axisY = top - 108;
        int span = (int) ChronoUnit.DAYS.between(from, to) + 1;
        float dayWidth = CHART_WIDTH / span;
        text(canvas, bold, 18, INK, LEFT, top - 45, "CRONOGRAMA DE EJECUCION");
        text(canvas, font, 10, INK, LEFT, top - 65, safe(project.getCode()) + "  |  " + safe(project.getName()));
        text(canvas, bold, 9, INK, LEFT, top - 88, "ACTIVIDADES Y RELACIONES");
        text(canvas, bold, 9, INK, graphX + 6, top - 88,
                from.format(DATE) + "  -  " + to.format(DATE));
        canvas.setColorFill(new Color(234, 246, 244));
        canvas.rectangle(LEFT, axisY - 22, NAME_WIDTH + CHART_WIDTH, 24);
        canvas.fill();
        for (int day = 0; day < span; day++) {
            LocalDate date = from.plusDays(day);
            if (day > 0 && date.getDayOfWeek().getValue() != 1 && date.getDayOfMonth() != 1) continue;
            float x = graphX + day * dayWidth;
            line(canvas, GRID, x, axisY - 22, x, axisY - 22 - rows.size() * ROW_HEIGHT);
            if (day == 0 || date.getDayOfMonth() == 1 || dayWidth >= 7) {
                text(canvas, font, 7, INK, x + 3, axisY - 14,
                        date.getDayOfMonth() == 1 ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                : date.format(DateTimeFormatter.ofPattern("dd/MM")));
            }
        }
        Map<Long, Integer> localRows = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) localRows.put(rows.get(i).getId(), i);
        for (int i = 0; i < rows.size(); i++) {
            GanttItemResponseDto item = rows.get(i);
            float y = axisY - 22 - (i + 1) * ROW_HEIGHT;
            if (i % 2 == 1) {
                canvas.setColorFill(new Color(247, 250, 251));
                canvas.rectangle(LEFT, y, NAME_WIDTH + CHART_WIDTH, ROW_HEIGHT);
                canvas.fill();
            }
            line(canvas, GRID, LEFT, y, graphX + CHART_WIDTH, y);
            String name = safe(item.getCode()) + "  " + safe(item.getName());
            text(canvas, bold, 7.7f, INK, LEFT + 6 + Math.min(item.getLevel() == null ? 0 : item.getLevel(), 4) * 8,
                    y + 11, fit(name, 62));
            String deps = dependencyText(item, codes);
            if (!deps.isEmpty()) text(canvas, font, 6.3f, new Color(53, 88, 102), LEFT + 7, y + 3, fit(deps, 86));
            LocalDate start = starts.get(item.getId()), end = ends.get(item.getId());
            if (start == null || end == null || end.isBefore(from) || start.isAfter(to)) continue;
            float x1 = graphX + Math.max(0, ChronoUnit.DAYS.between(from, start)) * dayWidth;
            float x2 = graphX + (Math.min(span, ChronoUnit.DAYS.between(from, end) + 1)) * dayWidth;
            canvas.setColorFill(isParent(item, rows) ? INK : TEAL);
            canvas.roundRectangle(x1, y + 5, Math.max(2, x2 - x1), 11, 2);
            canvas.fill();
            float progress = item.getCurrentProgressPercentage() == null ? 0 :
                    Math.max(0, Math.min(100, item.getCurrentProgressPercentage().floatValue()));
            if (!isParent(item, rows) && progress > 0) {
                canvas.setColorFill(new Color(14, 111, 113));
                canvas.rectangle(x1, y + 5, Math.max(0, x2 - x1) * progress / 100f, 11);
                canvas.fill();
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            GanttItemResponseDto target = rows.get(i);
            for (GanttDependencyDto dep : dependencies(target)) {
                Integer sourceIndex = localRows.get(dep.getPredecessorId());
                if (sourceIndex == null) continue;
                LocalDate sourceStart = starts.get(dep.getPredecessorId());
                LocalDate sourceEnd = ends.get(dep.getPredecessorId());
                LocalDate targetStart = starts.get(target.getId());
                LocalDate targetEnd = ends.get(target.getId());
                if (sourceStart == null || sourceEnd == null || targetStart == null || targetEnd == null) continue;
                DependencyType type = dep.getType() == null ? DependencyType.FINISH_TO_START : dep.getType();
                LocalDate a = type == DependencyType.START_TO_START || type == DependencyType.START_TO_FINISH ? sourceStart : sourceEnd;
                LocalDate b = type == DependencyType.FINISH_TO_START || type == DependencyType.START_TO_START ? targetStart : targetEnd;
                if (a.isBefore(from) || a.isAfter(to) || b.isBefore(from) || b.isAfter(to)) continue;
                float x1 = graphX + (ChronoUnit.DAYS.between(from, a) + .5f) * dayWidth;
                float x2 = graphX + (ChronoUnit.DAYS.between(from, b) + .5f) * dayWidth;
                float y1 = axisY - 22 - (sourceIndex + .5f) * ROW_HEIGHT;
                float y2 = axisY - 22 - (i + .5f) * ROW_HEIGHT;
                float elbow = x2 > x1 + 22 ? (x1 + x2) / 2 : x1 - 12;
                Color color = switch (type) {
                    case START_TO_START -> new Color(25, 127, 190);
                    case FINISH_TO_FINISH -> new Color(139, 91, 183);
                    case START_TO_FINISH -> new Color(189, 122, 36);
                    default -> new Color(82, 122, 132);
                };
                line(canvas, color, x1, y1, elbow, y1);
                line(canvas, color, elbow, y1, elbow, y2);
                line(canvas, color, elbow, y2, x2, y2);
                text(canvas, bold, 6, color, elbow + 2, (y1 + y2) / 2 + 2, abbreviation(type));
            }
        }
        text(canvas, font, 8, INK, LEFT, 38,
                "Relaciones: FS fin-inicio | SS inicio-inicio | FF fin-fin | SF inicio-fin. Referencias por codigo en cada fila.");
        text(canvas, bold, 8, INK, graphX + CHART_WIDTH - 93, 38, "Pagina " + page + " de " + totalPages);
    }

    private List<GanttDependencyDto> dependencies(GanttItemResponseDto item) {
        if (item.getDependencies() != null && !item.getDependencies().isEmpty()) return item.getDependencies();
        if (item.getPredecessorId() == null) return List.of();
        GanttDependencyDto legacy = new GanttDependencyDto();
        legacy.setPredecessorId(item.getPredecessorId());
        legacy.setType(DependencyType.FINISH_TO_START);
        return List.of(legacy);
    }

    private String dependencyText(GanttItemResponseDto item, Map<Long, String> codes) {
        List<String> parts = new ArrayList<>();
        for (GanttDependencyDto dep : dependencies(item)) {
            String code = codes.getOrDefault(dep.getPredecessorId(), "#" + dep.getPredecessorId());
            parts.add(code + " " + abbreviation(dep.getType() == null ? DependencyType.FINISH_TO_START : dep.getType()));
        }
        return String.join("  |  ", parts);
    }

    private String abbreviation(DependencyType type) {
        return switch (type) {
            case FINISH_TO_START -> "FS";
            case START_TO_START -> "SS";
            case FINISH_TO_FINISH -> "FF";
            case START_TO_FINISH -> "SF";
        };
    }

    private boolean isParent(GanttItemResponseDto item, List<GanttItemResponseDto> rows) {
        return "project".equals(item.getType()) || rows.stream().anyMatch(row -> item.getId().equals(row.getParentId()));
    }

    private String safe(String value) { return value == null ? "" : value; }
    private String fit(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
    private void line(PdfContentByte canvas, Color color, float x1, float y1, float x2, float y2) {
        canvas.setColorStroke(color); canvas.setLineWidth(.7f); canvas.moveTo(x1, y1); canvas.lineTo(x2, y2); canvas.stroke();
    }
    private void text(PdfContentByte canvas, BaseFont font, float size, Color color, float x, float y, String value) {
        canvas.beginText(); canvas.setFontAndSize(font, size); canvas.setColorFill(color);
        canvas.setTextMatrix(x, y); canvas.showText(value); canvas.endText();
    }
}

package com.retrofit.backend.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.retrofit.backend.dto.GanttDependencyDto;
import com.retrofit.backend.dto.GanttItemResponseDto;
import com.retrofit.backend.model.DependencyType;
import com.retrofit.backend.model.Project;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GanttPdfRendererTest {
    @Test
    void paginatesLongScheduleWithManyActivitiesAndAllRelationshipTypes() throws Exception {
        Project project = new Project();
        project.setCode("TEST-001");
        project.setName("Proyecto de prueba extenso");
        List<GanttItemResponseDto> items = new ArrayList<>();
        Map<Long, LocalDate> starts = new HashMap<>();
        Map<Long, LocalDate> ends = new HashMap<>();
        DependencyType[] types = DependencyType.values();
        LocalDate origin = LocalDate.of(2025, 1, 1);
        for (long id = 1; id <= 68; id++) {
            GanttItemResponseDto item = new GanttItemResponseDto();
            item.setId(id);
            item.setCode(String.format("%02d", id));
            item.setName("Actividad " + id);
            item.setCurrentProgressPercentage(35.0);
            item.setLevel(1);
            if (id > 1) {
                GanttDependencyDto dependency = new GanttDependencyDto();
                dependency.setPredecessorId(id - 1);
                dependency.setType(types[(int) id % types.length]);
                item.setDependencies(List.of(dependency));
            }
            items.add(item);
            starts.put(id, origin.plusDays((id - 1) * 8));
            ends.put(id, origin.plusDays((id - 1) * 8 + 12));
        }
        byte[] pdf = new GanttPdfRenderer().render(project, items, starts, ends);
        PdfReader reader = new PdfReader(pdf);
        assertEquals(6, reader.getNumberOfPages());
        assertTrue(pdf.length > 10_000);
        reader.close();
    }

    @Test
    void doesNotRepeatActivityBlocksOnPagesWithoutBars() throws Exception {
        Project project = new Project();
        project.setCode("TEST-002");
        project.setName("Proyecto con dos etapas");
        List<GanttItemResponseDto> items = new ArrayList<>();
        Map<Long, LocalDate> starts = new HashMap<>();
        Map<Long, LocalDate> ends = new HashMap<>();
        LocalDate origin = LocalDate.of(2026, 6, 9);
        for (long id = 1; id <= 49; id++) {
            GanttItemResponseDto item = new GanttItemResponseDto();
            item.setId(id);
            item.setCode(String.format("%02d", id));
            item.setName(id == 1 ? "Actividad inicial" : id == 49 ? "Actividad final" : "Partida " + id);
            item.setLevel(1);
            items.add(item);
            long offset = id <= 35 ? (id - 1) * 3 : 109 + (id - 36) * 3;
            starts.put(id, origin.plusDays(offset));
            ends.put(id, origin.plusDays(offset + 2));
        }
        PdfReader reader = new PdfReader(new GanttPdfRenderer().render(project, items, starts, ends));
        assertEquals(2, reader.getNumberOfPages());
        String first = new PdfTextExtractor(reader).getTextFromPage(1);
        String second = new PdfTextExtractor(reader).getTextFromPage(2);
        assertTrue(first.contains("Actividad  inicial"));
        assertFalse(first.contains("Actividad  final"));
        assertTrue(second.contains("Actividad  final"));
        assertFalse(second.contains("Actividad  inicial"));
        reader.close();
    }
}

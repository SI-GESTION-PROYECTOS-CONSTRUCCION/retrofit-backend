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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final TemplateEngine templateEngine;
    private final ProjectItemService projectItemService;
    private final ProjectRepository projectRepository;

    public byte[] generateApuReport(Long projectId) throws Exception {
        // 1. Obtener datos
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
                
        List<ProjectItemResponseDto> items = projectItemService.getItemsByProjectId(projectId);

        Context context = new Context();
        
        try {
            String logoBase64 = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/logo_base64.txt"));
            context.setVariable("logoBase64", logoBase64);
        } catch (Exception e) {
            context.setVariable("logoBase64", ""); // Fallback si no lo encuentra
        }
        
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
}

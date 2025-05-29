package com.example.bloodsugartracker.controller;

import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.itextpdf.io.image.ImageDataFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RestController
public class PdfController {

    private static final Logger logger = LoggerFactory.getLogger(PdfController.class);

    @PostMapping(value = "/generate-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generatePdf(@RequestBody JsonNode request) throws Exception {
        logger.info("Received request to generate PDF");

        // Validate request
        if (request == null || !request.has("patientName") || !request.has("tables") || !request.has("charts") || !request.has("hba1c")) {
            logger.error("Invalid request payload: missing required fields (patientName, tables, charts, hba1c)");
            throw new IllegalArgumentException("Request must contain patientName, tables, charts, and hba1c");
        }

        String patientName = request.get("patientName").asText();
        JsonNode tablesNode = request.get("tables");
        JsonNode chartsNode = request.get("charts");
        JsonNode hba1cNode = request.get("hba1c");

        logger.info("Generating PDF for patient: {}", patientName);
        logger.debug("Request payload: {}", request.toString());

        // Validate nodes
        if (!tablesNode.isObject() || !chartsNode.isObject() || !hba1cNode.isObject()) {
            logger.error("Tables, charts, and hba1c must be objects");
            throw new IllegalArgumentException("Tables, charts, and hba1c must be objects");
        }

        // Extract HbA1c data
        String hba1cValueStr = hba1cNode.has("value") ? hba1cNode.get("value").asText("N/A") : "N/A";
        String hba1cStatus = hba1cNode.has("status") ? hba1cNode.get("status").asText("Unknown") : "Unknown";
        double hba1cValue = 0.0;
        String hba1cDisplayStr = "N/A";
        if (!hba1cValueStr.equals("N/A")) {
            try {
                String cleanedHba1c = hba1cValueStr.replace("%", "").trim();
                hba1cValue = Double.parseDouble(cleanedHba1c);
                hba1cDisplayStr = cleanedHba1c;
            } catch (NumberFormatException e) {
                logger.error("Failed to parse HbA1c value '{}': {}", hba1cValueStr, e.getMessage());
                hba1cValueStr = "N/A";
                hba1cStatus = "Invalid";
                hba1cDisplayStr = "N/A";
            }
        }

        // Create PDF in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.setDefaultPageSize(PageSize.A4);
        Document document = new Document(pdf);
        document.setMargins(36, 36, 36, 36); // Professional margins (1 inch)

        // Define fonts for professional typography
        PdfFont titleFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.TIMES_BOLD);
        PdfFont sectionFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.TIMES_BOLD);
        PdfFont bodyFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.TIMES_ROMAN);

        // Add header and footer on each page
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new IEventHandler() {
            @Override
            public void handleEvent(Event event) {
                PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
                PdfDocument pdfDoc = docEvent.getDocument();
                PdfCanvas canvas = new PdfCanvas(pdfDoc.getLastPage());
                float pageWidth = pdfDoc.getDefaultPageSize().getWidth();
                float pageHeight = pdfDoc.getDefaultPageSize().getHeight();

                // Header
                canvas.beginText()
                        .setFontAndSize(titleFont, 10)
                        .moveText(36, pageHeight - 30)
                        .showText("Blood Sugar Dashboard Report")
                        .endText();
                try {
                    Image logo = new Image(ImageDataFactory.create(new ClassPathResource("logo.png").getInputStream().readAllBytes()));
                    logo.setFixedPosition(pageWidth - 100, pageHeight - 50);
                    logo.setWidth(50);
                    canvas.addXObjectAt(logo.getXObject(), pageWidth - 100, pageHeight - 50);
                } catch (Exception e) {
                    logger.warn("Failed to load logo for header: {}", e.getMessage());
                }
                canvas.setLineWidth(0.5f);
                canvas.moveTo(36, pageHeight - 60);
                canvas.lineTo(pageWidth - 36, pageHeight - 60);
                canvas.stroke();

                // Footer
                canvas.beginText()
                        .setFontAndSize(bodyFont, 8)
                        .moveText(36, 30)
                        .showText("Page " + pdfDoc.getPageNumber(pdfDoc.getLastPage()) + " | Confidential: For medical use only")
                        .endText();
                canvas.moveTo(36, 40);
                canvas.lineTo(pageWidth - 36, 40);
                canvas.stroke();
            }
        });

        // Add cover page
        document.add(new Paragraph("Blood Sugar Dashboard Report")
                .setFont(titleFont)
                .setFontSize(24)
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(0, 51, 102)) // Dark blue
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(200));
        document.add(new Paragraph("Patient: Venkata Subbma")
                .setFont(sectionFont)
                .setFontSize(16)
                .setFontColor(ColorConstants.BLACK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20));
        document.add(new Paragraph("Age: 53 years | Gender: Female")
                .setFont(sectionFont)
                .setFontSize(14)
                .setFontColor(ColorConstants.BLACK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));
        document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy, hh:mm a 'IST'")))
                .setFont(bodyFont)
                .setFontSize(12)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));
        document.add(new Paragraph("Prepared by: Blood Sugar Tracker")
                .setFont(bodyFont)
                .setFontSize(12)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));
        document.add(new LineSeparator(new SolidLine(1f)).setMarginTop(20));
        pdf.addNewPage();

        // Add patient info and HbA1c
        document.add(new Paragraph("Patient Information")
                .setFont(sectionFont)
                .setFontSize(16)
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(0, 51, 102))
                .setMarginTop(20)
                .setMarginBottom(10));
        document.add(new Paragraph("Patient Name: Venkata Subbma")
                .setFont(bodyFont)
                .setFontSize(12));
        document.add(new Paragraph("Age: 53 years")
                .setFont(bodyFont)
                .setFontSize(12));
        document.add(new Paragraph("Gender: Female")
                .setFont(bodyFont)
                .setFontSize(12));
        document.add(new Paragraph("Report Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy, hh:mm a 'IST'")))
                .setFont(bodyFont)
                .setFontSize(12));
        document.add(new Paragraph("HbA1c: " + (hba1cDisplayStr.equals("N/A") ? "N/A" : hba1cDisplayStr + "%") + " (" + hba1cStatus + ")")
                .setFont(bodyFont)
                .setFontSize(12)
                .setFontColor(hba1cStatus.equals("Normal") ? ColorConstants.GREEN : hba1cStatus.equals("Pre-diabetes") ? new com.itextpdf.kernel.colors.DeviceRgb(255, 153, 0) : ColorConstants.RED)
                .setMarginBottom(20));

        // Define periods
        String[] periods = {"today", "yesterday", "week", "month", "year"};

        // Statistics for summary
        Map<String, Map<String, Double>> statistics = new HashMap<>();
        for (String period : periods) {
            statistics.put(period, new HashMap<>());
            statistics.get(period).put("avgFasting", 0.0);
            statistics.get(period).put("avgPostPrandial", 0.0);
            statistics.get(period).put("count", 0.0);
            statistics.get(period).put("maxFasting", 0.0);
            statistics.get(period).put("maxPostPrandial", 0.0);
            statistics.get(period).put("minFasting", Double.MAX_VALUE);
            statistics.get(period).put("minPostPrandial", Double.MAX_VALUE);
        }

        // Process tables and charts for each period
        for (String period : periods) {
            // Process table data
            if (!tablesNode.has(period)) {
                logger.warn("No table data for period: {}", period);
                document.add(new Paragraph("No records available for " + capitalize(period))
                        .setFont(bodyFont)
                        .setFontSize(12)
                        .setFontColor(ColorConstants.RED)
                        .setMarginTop(15));
                continue;
            }

            JsonNode periodData = tablesNode.get(period);
            logger.debug("Table data for period {}: {}", period, periodData.toString());
            if (!periodData.isArray() || periodData.size() == 0) {
                logger.debug("Skipping period {}: no table data or not an array", period);
                document.add(new Paragraph("No records available for " + capitalize(period))
                        .setFont(bodyFont)
                        .setFontSize(12)
                        .setFontColor(ColorConstants.RED)
                        .setMarginTop(15));
                continue;
            }

            // Calculate statistics
            double totalFasting = 0, totalPostPrandial = 0;
            double maxFasting = Double.MIN_VALUE, maxPostPrandial = Double.MIN_VALUE;
            double minFasting = Double.MAX_VALUE, minPostPrandial = Double.MAX_VALUE;
            int count = 0;
            Iterator<JsonNode> rows = periodData.elements();
            while (rows.hasNext()) {
                JsonNode row = rows.next();
                if (!row.isArray() || row.size() < 5) {
                    logger.warn("Invalid row in period {}: expected 5 elements, got {}. Row: {}", period, row.size(), row.toString());
                    continue;
                }
                try {
                    double fasting = row.get(1).isNull() || row.get(1).asText().equals("N/A") ? 0.0 : Double.parseDouble(row.get(1).asText());
                    double postPrandial = row.get(3).isNull() || row.get(3).asText().equals("N/A") ? 0.0 : Double.parseDouble(row.get(3).asText());
                    totalFasting += fasting;
                    totalPostPrandial += postPrandial;
                    maxFasting = Math.max(maxFasting, fasting);
                    maxPostPrandial = Math.max(maxPostPrandial, postPrandial);
                    minFasting = Math.min(minFasting, fasting > 0 ? fasting : minFasting);
                    minPostPrandial = Math.min(minPostPrandial, postPrandial > 0 ? postPrandial : minPostPrandial);
                    count++;
                } catch (Exception e) {
                    logger.error("Error parsing row in period {}: {}. Row: {}", period, e.getMessage(), row.toString());
                    continue;
                }
            }

            statistics.get(period).put("avgFasting", count > 0 ? totalFasting / count : 0.0);
            statistics.get(period).put("avgPostPrandial", count > 0 ? totalPostPrandial / count : 0.0);
            statistics.get(period).put("maxFasting", maxFasting == Double.MIN_VALUE ? 0.0 : maxFasting);
            statistics.get(period).put("maxPostPrandial", maxPostPrandial == Double.MIN_VALUE ? 0.0 : maxPostPrandial);
            statistics.get(period).put("minFasting", minFasting == Double.MAX_VALUE ? 0.0 : minFasting);
            statistics.get(period).put("minPostPrandial", minPostPrandial == Double.MAX_VALUE ? 0.0 : minPostPrandial);
            statistics.get(period).put("count", (double) count);

            // Add section header
            document.add(new Paragraph(capitalize(period) + " Blood Sugar Records")
                    .setFont(sectionFont)
                    .setFontSize(16)
                    .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(0, 51, 102))
                    .setMarginTop(20)
                    .setMarginBottom(10));

            // Add table
            Table table = new Table(new float[]{2, 2, 2, 2, 2, 2});
            table.setWidth(UnitValue.createPercentValue(100));
            table.setBorder(new SolidBorder(new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 0.5f));

            // Table headers
            String[] headers = {"Date", "Fasting Sugar (mg/dL)", "Fasting Time", "Postprandial Sugar (mg/dL)", "Postprandial Time", "Predicted HbA1c (%)"};
            for (String header : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(header).setFont(sectionFont).setFontSize(10))
                        .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(230, 240, 255))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorder(new SolidBorder(new com.itextpdf.kernel.colors.DeviceRgb(150, 150, 150), 0.5f))
                        .setPadding(8));
            }

            // Table rows
            rows = periodData.elements();
            int rowIndex = 0;
            while (rows.hasNext()) {
                JsonNode row = rows.next();
                if (!row.isArray() || row.size() < 5) {
                    logger.warn("Skipping invalid row in period {}: expected 5 elements, got {}. Row: {}", period, row.size(), row.toString());
                    continue;
                }
                try {
                    Cell[] cells = new Cell[6];
                    cells[0] = new Cell().add(new Paragraph(row.get(0).asText("N/A")).setFont(bodyFont).setFontSize(10));
                    cells[1] = new Cell().add(new Paragraph(row.get(1).asText("N/A")).setFont(bodyFont).setFontSize(10));
                    cells[2] = new Cell().add(new Paragraph(row.get(2).asText("N/A")).setFont(bodyFont).setFontSize(10));
                    cells[3] = new Cell().add(new Paragraph(row.get(3).asText("N/A")).setFont(bodyFont).setFontSize(10));
                    cells[4] = new Cell().add(new Paragraph(row.get(4).asText("N/A")).setFont(bodyFont).setFontSize(10));
                    cells[5] = new Cell().add(new Paragraph(hba1cDisplayStr.equals("N/A") ? "N/A" : hba1cDisplayStr + "%").setFont(bodyFont).setFontSize(10));
                    for (Cell cell : cells) {
                        cell.setPadding(8);
                        cell.setTextAlignment(TextAlignment.CENTER);
                        cell.setBorder(new SolidBorder(new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 0.5f));
                        if (rowIndex % 2 == 0) {
                            cell.setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245));
                        }
                        table.addCell(cell);
                    }
                    rowIndex++;
                } catch (Exception e) {
                    logger.error("Error adding row to table in period {}: {}. Row: {}", period, e.getMessage(), row.toString());
                    continue;
                }
            }
            document.add(table);
            document.add(new Paragraph("\n").setMarginBottom(10));

            // Add line chart
            logger.debug("Checking chart data for period: {}", period);
            JsonNode chartData = chartsNode.get(period);
            if (chartData != null && !chartData.isNull()) {
                logger.debug("Chart data for period {}: {}", period, chartData.toString());
                document.add(new Paragraph("Blood Sugar Trend for " + capitalize(period))
                        .setFont(sectionFont)
                        .setFontSize(14)
                        .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(0, 51, 102))
                        .setMarginTop(15)
                        .setMarginBottom(10));

                JsonNode labelsNode = chartData.get("labels");
                JsonNode datasetsNode = chartData.get("datasets");

                if (labelsNode == null || datasetsNode == null || !labelsNode.isArray() || !datasetsNode.isArray()) {
                    logger.warn("Invalid chart data for period {}: labels or datasets missing or not arrays", period);
                    document.add(new Paragraph("Invalid chart data for " + capitalize(period))
                            .setFont(bodyFont)
                            .setFontSize(12)
                            .setFontColor(ColorConstants.RED));
                    continue;
                }

                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                String[] labels = new String[labelsNode.size()];
                for (int i = 0; i < labelsNode.size(); i++) {
                    labels[i] = labelsNode.get(i).asText();
                }

                for (int i = 0; i < datasetsNode.size(); i++) {
                    JsonNode datasetNode = datasetsNode.get(i);
                    String label = datasetNode.get("label").asText();
                    JsonNode dataNode = datasetNode.get("data");

                    for (int j = 0; j < dataNode.size(); j++) {
                        double value = dataNode.get(j).isNull() ? 0.0 : dataNode.get(j).asDouble();
                        dataset.addValue(value, label, labels[j]);
                    }
                }

                JFreeChart lineChart = ChartFactory.createLineChart(
                        null, "Time", "Blood Sugar Level (mg/dL)", dataset,
                        PlotOrientation.VERTICAL, true, false, false);
                CategoryPlot plot = lineChart.getCategoryPlot();
                LineAndShapeRenderer renderer = new LineAndShapeRenderer(true, true);
                renderer.setSeriesPaint(0, new Color(0, 102, 204)); // Blue for fasting
                renderer.setSeriesPaint(1, new Color(255, 102, 0)); // Orange for postprandial
                renderer.setSeriesShapesVisible(0, true);
                renderer.setSeriesShapesVisible(1, true);
                plot.setRenderer(renderer);
                plot.setBackgroundPaint(Color.WHITE);
                plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
                plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
                plot.setRangeGridlinesVisible(true);
                plot.setDomainGridlinesVisible(true);

                ByteArrayOutputStream chartBaos = new ByteArrayOutputStream();
                ChartUtils.writeChartAsPNG(chartBaos, lineChart, 600, 350);
                byte[] chartBytes = chartBaos.toByteArray();

                Image chartImage = new Image(ImageDataFactory.create(chartBytes));
                chartImage.setAutoScale(true);
                chartImage.setMarginBottom(20);
                document.add(chartImage);
            } else {
                logger.warn("No chart data available for period: {}", period);
                document.add(new Paragraph("No chart data available for " + capitalize(period))
                        .setFont(bodyFont)
                        .setFontSize(12)
                        .setFontColor(ColorConstants.RED));
            }
        }

        // Add summary statistics
        document.add(new Paragraph("Summary Statistics")
                .setFont(sectionFont)
                .setFontSize(16)
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(0, 51, 102))
                .setMarginTop(20)
                .setMarginBottom(10));
        Table statsTable = new Table(new float[]{2, 2, 2, 2, 2, 2, 2});
        statsTable.setWidth(UnitValue.createPercentValue(100));
        statsTable.setBorder(new SolidBorder(new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 0.5f));

        // Stats table headers
        String[] statsHeaders = {"Period", "Records", "Avg Fasting (mg/dL)", "Avg Postprandial (mg/dL)", "Min Fasting (mg/dL)", "Max Fasting (mg/dL)", "Max Postprandial (mg/dL)"};
        for (String header : statsHeaders) {
            statsTable.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setFont(sectionFont).setFontSize(10))
                    .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(230, 240, 255))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(new com.itextpdf.kernel.colors.DeviceRgb(150, 150, 150), 0.5f))
                    .setPadding(8));
        }

        // Stats table rows
        for (String period : periods) {
            double count = statistics.get(period).get("count");
            if (count == 0) continue;

            Cell[] cells = new Cell[7];
            cells[0] = new Cell().add(new Paragraph(capitalize(period)).setFont(bodyFont).setFontSize(10));
            cells[1] = new Cell().add(new Paragraph(String.format("%.0f", count)).setFont(bodyFont).setFontSize(10));
            cells[2] = new Cell().add(new Paragraph(String.format("%.2f", statistics.get(period).get("avgFasting"))).setFont(bodyFont).setFontSize(10));
            cells[3] = new Cell().add(new Paragraph(String.format("%.2f", statistics.get(period).get("avgPostPrandial"))).setFont(bodyFont).setFontSize(10));
            cells[4] = new Cell().add(new Paragraph(String.format("%.2f", statistics.get(period).get("minFasting"))).setFont(bodyFont).setFontSize(10));
            cells[5] = new Cell().add(new Paragraph(String.format("%.2f", statistics.get(period).get("maxFasting"))).setFont(bodyFont).setFontSize(10));
            cells[6] = new Cell().add(new Paragraph(String.format("%.2f", statistics.get(period).get("maxPostPrandial"))).setFont(bodyFont).setFontSize(10));
            for (Cell cell : cells) {
                cell.setPadding(8);
                cell.setTextAlignment(TextAlignment.CENTER);
                cell.setBorder(new SolidBorder(new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 0.5f));
                statsTable.addCell(cell);
            }
        }
        document.add(statsTable);

        // Add HbA1c color significance
        document.add(new Paragraph("HbA1c Interpretation")
                .setFont(sectionFont)
                .setFontSize(16)
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(0, 51, 102))
                .setMarginTop(20)
                .setMarginBottom(10));
        document.add(new Paragraph("The Predicted HbA1c values are color-coded to indicate diabetes risk levels:")
                .setFont(bodyFont)
                .setFontSize(12));
        document.add(new Paragraph("• Green: HbA1c < 5.7% (Non-diabetic range, indicating good control)")
                .setFont(bodyFont)
                .setFontSize(12)
                .setFontColor(ColorConstants.GREEN));
        document.add(new Paragraph("• Yellow: HbA1c 5.7%–6.4% (Pre-diabetes range, indicating risk)")
                .setFont(bodyFont)
                .setFontSize(12)
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(255, 153, 0)));
        document.add(new Paragraph("• Red: HbA1c ≥ 6.5% (Diabetes range, requires medical management)")
                .setFont(bodyFont)
                .setFontSize(12)
                .setFontColor(ColorConstants.RED));

        // Close the document
        document.close();

        // Get PDF bytes
        byte[] pdfBytes = baos.toByteArray();

        // Set response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", sanitizeFileName("Venkata Subbma") + "_Blood_Sugar_Report.pdf");

        logger.info("PDF generated successfully for patient: Venkata Subbma");
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
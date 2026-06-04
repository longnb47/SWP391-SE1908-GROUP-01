package com.se1908.group01.service;

import com.se1908.group01.config.OcrProperties;
import com.se1908.group01.dto.TextSegment;
import com.se1908.group01.util.FileExtensionUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentParsingService {

	private final OcrService ocrService;
	private final OcrProperties ocrProperties;

	public DocumentParsingService(OcrService ocrService, OcrProperties ocrProperties) {
		this.ocrService = ocrService;
		this.ocrProperties = ocrProperties;
	}

	public List<TextSegment> extractSegments(MultipartFile file) throws IOException {
		var filename = file.getOriginalFilename();
		var ext = FileExtensionUtil.getExtensionLower(filename);
		var contentType = file.getContentType();
		if (StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/")) {
			return extractImage(file);
		}
		if (!StringUtils.hasText(ext)) {
			throw new IllegalArgumentException("Cannot detect file extension");
		}

		return switch (ext) {
			case "pdf" -> extractPdf(file);
			case "docx" -> extractDocx(file);
			case "doc" -> extractDoc(file);
			case "pptx" -> extractPptx(file);
			case "xlsx", "xls" -> extractExcel(file);
			case "png", "jpg", "jpeg", "webp", "gif", "bmp", "tif", "tiff" -> extractImage(file);
			default -> throw new IllegalArgumentException("Unsupported file extension for parsing: " + ext);
		};
	}

	private List<TextSegment> extractPdf(MultipartFile file) throws IOException {
		try (var in = file.getInputStream(); var doc = PDDocument.load(in)) {
			var stripper = new PDFTextStripper();
			stripper.setSortByPosition(true);
			var renderer = new PDFRenderer(doc);

			List<TextSegment> segments = new ArrayList<>();
			int pages = doc.getNumberOfPages();
			for (int i = 1; i <= pages; i++) {
				stripper.setStartPage(i);
				stripper.setEndPage(i);
				var pageText = stripper.getText(doc);

				int imageCount = countImages(doc.getPage(i - 1));
				var ocrText = "";
				if (shouldOcrPdfPage(pageText)) {
					var pageImage = renderer.renderImageWithDPI(i - 1, ocrProperties.getPdfDpi(), ImageType.RGB);
					ocrText = ocrService.extractText(pageImage);
				}

				var combined = new StringBuilder();
				combined.append("[PAGE ").append(i).append("]\n");
				if (StringUtils.hasText(pageText)) {
					combined.append(pageText.trim()).append("\n");
				}
				if (StringUtils.hasText(ocrText)) {
					combined.append("[OCR]\n").append(ocrText.trim()).append("\n");
				}
				if (imageCount > 0) {
					combined.append("[IMAGE_COUNT=").append(imageCount).append("]\n");
				}

				var text = combined.toString().trim();
				if (StringUtils.hasText(pageText) || StringUtils.hasText(ocrText) || imageCount > 0) {
					segments.add(new TextSegment(text, i));
				}
			}
			return segments;
		}
	}

	private boolean shouldOcrPdfPage(String pageText) {
		if (!ocrService.isEnabled()) {
			return false;
		}
		if (!StringUtils.hasText(pageText)) {
			return true;
		}
		var compactTextLength = pageText.replaceAll("\\s+", "").length();
		return compactTextLength < Math.max(1, ocrProperties.getPdfTextMinCharacters());
	}

	private List<TextSegment> extractImage(MultipartFile file) throws IOException {
		var ocrText = ocrService.extractText(file);
		if (!StringUtils.hasText(ocrText)) {
			return List.of();
		}
		return List.of(new TextSegment("[IMAGE OCR]\n" + ocrText.trim(), null));
	}

	private static int countImages(PDPage page) throws IOException {
		int count = 0;
		if (page.getResources() == null) {
			return 0;
		}
		for (var name : page.getResources().getXObjectNames()) {
			PDXObject xObject = page.getResources().getXObject(name);
			if (xObject instanceof PDImageXObject) {
				count++;
			}
		}
		return count;
	}

	private static List<TextSegment> extractDocx(MultipartFile file) throws IOException {
		try (var in = file.getInputStream(); var doc = new XWPFDocument(in)) {
			List<TextSegment> segments = new ArrayList<>();
			StringBuilder sb = new StringBuilder();

			for (XWPFParagraph p : doc.getParagraphs()) {
				var t = p.getText();
				if (StringUtils.hasText(t)) {
					sb.append(t.trim()).append("\n");
				}
				for (XWPFRun run : p.getRuns()) {
					if (run.getEmbeddedPictures() != null && !run.getEmbeddedPictures().isEmpty()) {
						sb.append("[IMAGE]\n");
					}
				}
			}

			for (XWPFTable table : doc.getTables()) {
				sb.append("[TABLE]\n");
				table.getRows().forEach(row -> {
					var rowText = row.getTableCells().stream()
							.map(c -> c.getText() == null ? "" : c.getText().trim())
							.reduce((a, b) -> a + "\t" + b)
							.orElse("");
					if (StringUtils.hasText(rowText)) {
						sb.append(rowText).append("\n");
					}
				});
			}

			if (!doc.getAllPictures().isEmpty()) {
				sb.append("[IMAGE_COUNT=").append(doc.getAllPictures().size()).append("]\n");
			}

			var text = sb.toString().trim();
			if (StringUtils.hasText(text)) {
				segments.add(new TextSegment(text, null));
			}
			return segments;
		}
	}

	private static List<TextSegment> extractDoc(MultipartFile file) throws IOException {
		try (var in = file.getInputStream(); var doc = new HWPFDocument(in); var extractor = new WordExtractor(doc)) {
			var text = extractor.getText();
			if (!StringUtils.hasText(text)) {
				return List.of();
			}
			return List.of(new TextSegment(text.trim(), null));
		}
	}

	private static List<TextSegment> extractPptx(MultipartFile file) throws IOException {
		try (var in = file.getInputStream(); SlideShow<?, ?> slideShow = new XMLSlideShow(in)) {
			List<TextSegment> segments = new ArrayList<>();
			int slideNumber = 0;
			for (Slide<?, ?> slide : slideShow.getSlides()) {
				slideNumber++;
				if (!(slide instanceof XSLFSlide xslfSlide)) {
					continue;
				}
				var sb = new StringBuilder();
				sb.append("[SLIDE ").append(slideNumber).append("]\n");

				for (XSLFShape shape : xslfSlide.getShapes()) {
					if (shape instanceof XSLFTextShape textShape) {
						var t = textShape.getText();
						if (StringUtils.hasText(t)) {
							sb.append(t.trim()).append("\n");
						}
					} else if (shape instanceof XSLFPictureShape) {
						sb.append("[IMAGE]\n");
					} else if (shape instanceof XSLFGraphicFrame) {
						// Charts/tables can be frames; we keep a placeholder for basic coverage.
						sb.append("[GRAPHIC_FRAME]\n");
					}
				}

				var text = sb.toString().trim();
				if (StringUtils.hasText(text)) {
					segments.add(new TextSegment(text, slideNumber));
				}
			}
			return segments;
		}
	}

	private static List<TextSegment> extractExcel(MultipartFile file) throws IOException {
		try (var in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
			List<TextSegment> segments = new ArrayList<>();
			for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
				var sheet = workbook.getSheetAt(si);
				var sb = new StringBuilder();
				sb.append("[SHEET ").append(si + 1).append("] ").append(sheet.getSheetName()).append("\n");

				for (Row row : sheet) {
					StringBuilder rowSb = new StringBuilder();
					for (Cell cell : row) {
						var cellText = cell.toString();
						if (cellText == null) {
							cellText = "";
						}
						cellText = cellText.trim();
						if (!cellText.isEmpty()) {
							if (!rowSb.isEmpty()) {
								rowSb.append("\t");
							}
							rowSb.append(cellText);
						}
					}
					if (!rowSb.isEmpty()) {
						sb.append(rowSb).append("\n");
					}
				}

				var text = sb.toString().trim();
				if (StringUtils.hasText(text)) {
					segments.add(new TextSegment(text, si + 1));
				}
			}
			return segments;
		} catch (Exception ex) {
			// WorkbookFactory can throw runtime exceptions for corrupted/unsupported files.
			throw new IOException("Failed to parse Excel file", ex);
		}
	}
}

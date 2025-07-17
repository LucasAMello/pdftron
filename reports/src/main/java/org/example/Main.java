package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StopWatch;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.HTML2PDF;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFNet;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageSet;
import com.pdftron.pdf.Rect;
import com.pdftron.sdf.SDFDoc;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class Main {

	private static final String LICENSE_KEY = "demo:1747155613907:61e8c82003000000006be23640732f3fd2c8dc397bb049792f7376dbe1";
	private static final String HTML2PDF_MODULE_PATH = System.getenv().getOrDefault("HTML2PDF_PATH", "/opt/HTML2PDF");

	private static final Configuration freemarkerConfig;

	public record MyBookmark(Bookmark bookmark, int level) {}

	static {
		freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
		freemarkerConfig.setClassLoaderForTemplateLoading(Main.class.getClassLoader(), "templates");
	}

	public static void main(String[] args) {
		try {
			PDFNet.initialize(LICENSE_KEY);
			HTML2PDF.setModulePath(HTML2PDF_MODULE_PATH);
			if (!HTML2PDF.isModuleAvailable()) {
				throw new IllegalStateException("HTML2PDF module not found at " + HTML2PDF_MODULE_PATH);
			}
		} catch (PDFNetException e) {
			System.err.println("PDFNet initialization failed: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("PDFNet initialization failed", e);
		}

		try {
			StopWatch sw = new StopWatch();
			sw.start();
			createPDFTronReport();
			sw.stop();
			System.out.println("PDFTron report generated in " + sw.getTotalTimeSeconds() + " seconds");
		} catch (PDFNetException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void createPDFTronReport() throws PDFNetException, IOException {
		var pdfCover = getCoverDoc();
		var pdfReport = getReportDoc();

		Page tocPage = createTableOfContentsPage(pdfReport);
		pdfReport.pagePushFront(tocPage);

		try (PDFDoc merged = new PDFDoc()) {
			merged.initSecurityHandler();

			for (PDFDoc doc : List.of(pdfCover, pdfReport)) {
				merged.insertPages(merged.getPageCount() + 1, doc, new PageSet(1, doc.getPageCount()),
						PDFDoc.InsertBookmarkMode.NONE, null);
			}

			merged.save("pdfs/report.pdf", SDFDoc.SaveMode.LINEARIZED, null);
		} catch (PDFNetException e) {
			System.err.println("PDFNetException: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
			throw new RuntimeException("Apryse operation failed", e);
		} catch (Exception e) {
			System.err.println("Exception during PDF processing: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
			throw new RuntimeException("PDF processing failed", e);
		}
	}

	static PDFDoc getCoverDoc() throws IOException, PDFNetException {
		// Process Free Marker template
		Template template = freemarkerConfig.getTemplate("cover.ftl");
		String html;
		try (StringWriter sw = new StringWriter()) {
			template.process(new HashMap<>(), sw);
			html = sw.toString();
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		}

		// Configure Apryse HTML2PDF conversion
		HTML2PDF.WebPageSettings settings = new HTML2PDF.WebPageSettings();
		settings.setBlockLocalFileAccess(false);

		PDFDoc pdfDoc = new PDFDoc();
		try (HTML2PDF converter = new HTML2PDF()) {
			converter.setMargins("0cm", "0cm", "0cm", "0cm");
			converter.insertFromHtmlString(html, settings);
			converter.convert(pdfDoc);
		} catch (PDFNetException e) {
			System.err.println("Apryse HTML2PDF conversion error: " + e.getMessage());
			throw new RuntimeException("Apryse HTML2PDF conversion failed", e);
		}

		return pdfDoc;
	}

	static PDFDoc getReportDoc() throws IOException, PDFNetException {
		// Process Free Marker template
		Template template = freemarkerConfig.getTemplate("body.ftl");
		String html;
		try (StringWriter sw = new StringWriter()) {
			template.process(new HashMap<>(), sw);
			html = sw.toString();
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		}

		HTML2PDF.WebPageSettings settings = new HTML2PDF.WebPageSettings();
		settings.setBlockLocalFileAccess(false);

		PDFDoc pdfDoc = new PDFDoc();
		try (HTML2PDF converter = new HTML2PDF()) {
			converter.setHeader(getHeader());
			converter.setFooter(getFooter());
			converter.setMargins("2.2cm", "2.2cm", "2.2cm", "2.2cm");
			converter.insertFromHtmlString(html, settings);
			converter.convert(pdfDoc);
		} catch (PDFNetException e) {
			System.err.println("Apryse HTML2PDF conversion error: " + e.getMessage());
			throw new RuntimeException("Apryse HTML2PDF conversion failed", e);
		}

		return pdfDoc;
	}

	static Page createTableOfContentsPage(PDFDoc pdfReport) throws PDFNetException, IOException {
		pdfReport.initSecurityHandler();

		try (ElementBuilder builder = new ElementBuilder(); ElementWriter writer = new ElementWriter()) {
			InputStream fontStream = new ClassPathResource("static/Inter.ttf").getInputStream();

			List<MyBookmark> entries = new ArrayList<>();
			for (Bookmark bm = pdfReport.getFirstBookmark(); bm.isValid(); bm = bm.getNext()) {
				addBookmarks(bm, 0, entries);
			}

			Page tocPage = pdfReport.pageCreate(new Rect(0, 0, 612, 792));
			writer.begin(tocPage);

			double indent = 64;
			Font interFont = Font.createTrueTypeFont(pdfReport, fontStream, true);
			writer.writeElement(builder.createTextBegin(interFont, 24));
			Element headingText = builder.createTextRun("Table of Contents");
			headingText.setTextMatrix(new Matrix2D(1, 0, 0, 1, indent, 720));
			writer.writeElement(headingText);
			writer.writeElement(builder.createTextEnd());

			writer.writeElement(builder.createTextBegin(interFont, 11));

			double y = 660;

			for (MyBookmark bm : entries) {
				String titleStr = bm.bookmark().getTitle();
				if (titleStr == null) {
					titleStr = "";
				}

				double titleIndent = indent + bm.level() * 20;
				Matrix2D titleMat = new Matrix2D(1, 0, 0, 1, titleIndent, y);
				Element titleText = builder.createTextRun(titleStr);
				titleText.setTextMatrix(titleMat);
				writer.writeElement(titleText);

				double pageWidth = 612;
				double pageNumX = pageWidth - indent;

				String pageNumStr = String.valueOf(bm.bookmark().getAction().getDest().getPage().getIndex());

				Matrix2D pageNumMat = new Matrix2D(1, 0, 0, 1, pageNumX, y);
				Element pageNumText = builder.createTextRun(pageNumStr);
				pageNumText.setTextMatrix(pageNumMat);
				writer.writeElement(pageNumText);

				y -= 24;
			}

			writer.writeElement(builder.createTextEnd());
			writer.end();

			return tocPage;
		}
	}

	static String getHeader() {
		return "<div style='width: 100%; font-size: 11px; margin-top: 32px; margin-left: 96px;'>" +
				"Executive Report" +
				"</div>";
	}

	static String getFooter() {
		return "<div style='width: 100%; font-size: 12px; margin: 0 96px 32px; display: flex; flex-direction: row; justify-content: space-between;'>" +
				"<div>Footer</div>" +
				"<div><span class='pageNumber'></span></div>" +
				"</div>";
	}

	static void addBookmarks(Bookmark bm, int level, List<MyBookmark> entries) throws PDFNetException {
		entries.add(new MyBookmark(bm, level));

		if (bm.hasChildren()) {
			for (Bookmark child = bm.getFirstChild(); child.isValid(); child = child.getNext()) {
				addBookmarks(child, level + 1, entries);
			}
		}
	}
}

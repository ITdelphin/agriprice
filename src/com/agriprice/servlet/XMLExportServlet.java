package com.agriprice.servlet;

import com.agriprice.dao.PriceDAO;
import com.agriprice.model.PriceReport;
import com.agriprice.util.XPathDemoTest;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

public class XMLExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final PriceDAO priceDAO = new PriceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String format = request.getParameter("format"); // "xml", "html", "grouped", "sms", "validate"
        format = (format == null || format.trim().isEmpty()) ? "html" : format.trim().toLowerCase(Locale.ROOT);
        if (!"xml".equals(format) && !"html".equals(format) && !"grouped".equals(format)
                && !"sms".equals(format) && !"validate".equals(format) && !"xpath".equals(format)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported bulletin format.");
            return;
        }

        Date bulletinDate;
        String dateParam = request.getParameter("date");
        try {
            if (dateParam != null && !dateParam.trim().isEmpty()) {
                if (!dateParam.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException("Date must use YYYY-MM-DD.");
                bulletinDate = Date.valueOf(LocalDate.parse(dateParam, DateTimeFormatter.ISO_LOCAL_DATE));
            } else {
                bulletinDate = priceDAO.getLatestReportDate();
                if (bulletinDate == null) bulletinDate = new Date(System.currentTimeMillis());
            }
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid bulletin date. Use YYYY-MM-DD.");
            return;
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not determine the latest bulletin date.");
            return;
        }

        try {
            List<PriceReport> reports = priceDAO.getFilteredPrices(null, null, bulletinDate, null);
            String xmlContent = generateXmlBulletin(bulletinDate, reports);

            // XSD Validation test request
            if ("validate".equalsIgnoreCase(format)) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();
                String xsdPath = getServletContext().getRealPath("/xml/prices.xsd");
                boolean isValid = validateXmlAgainstXsd(xmlContent, xsdPath);
                out.print("{\"success\":true,\"valid\": " + isValid + ", \"message\":\"XML conforms to prices.xsd Schema.\"}");
                return;
            }

            String responseContent = xmlContent;
            String responseType = "application/xml";
            long lastModifiedMillis = Math.max(bulletinDate.getTime(), priceDAO.getLatestReportUpdatedAt(bulletinDate));

            // Render the browser-facing bulletin before calculating validators so an HTML
            // response never reuses the ETag of the raw XML representation.
            if ("html".equals(format) || "grouped".equals(format) || "sms".equals(format)) {
                String xslPath = getServletContext().getRealPath("/xml/prices.xsl");
                File xslFile = xslPath == null ? null : new File(xslPath);
                if (xslFile == null || !xslFile.isFile()) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Bulletin layout is unavailable.");
                    return;
                }
                responseContent = applyXslt(xmlContent, xslFile, format);
                responseType = "sms".equals(format) ? "text/plain" : "text/html";
                lastModifiedMillis = Math.max(lastModifiedMillis, xslFile.lastModified());
            } else if ("xpath".equals(format)) {
                responseContent = renderXPathResults(xmlContent);
                responseType = "text/html";
            }

            // HTTP Conditional Caching uses the rendered representation, including its format.
            String etag = computeETag(responseContent);

            response.setHeader("ETag", etag);
            response.setDateHeader("Last-Modified", lastModifiedMillis);
            response.setHeader("Cache-Control", "no-cache, must-revalidate");

            String ifNoneMatch = request.getHeader("If-None-Match");
            long ifModifiedSince = request.getDateHeader("If-Modified-Since");

            if ((ifNoneMatch != null && ifNoneMatch.equals(etag)) || 
                (ifModifiedSince >= lastModifiedMillis && ifNoneMatch == null)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED); // 304 Not Modified
                return;
            }

            // Raw XML is available explicitly through ?format=xml.
            response.setContentType(responseType);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            out.write(responseContent);
            out.flush();

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setCharacterEncoding("UTF-8");
            if ("html".equals(format) || "grouped".equals(format)) {
                response.setContentType("text/html");
                response.getWriter().write("<!doctype html><html lang=\"en\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Bulletin unavailable - AgriPrice Rwanda</title></head><body style=\"font-family:Arial,sans-serif;background:#f5f7f5;color:#202b24;margin:0;padding:48px 20px\"><main style=\"max-width:640px;margin:auto;background:#fff;padding:32px;border-radius:12px\"><h1>Daily bulletin unavailable</h1><p>We couldn't load the market prices right now. Please try again shortly.</p><p><a href=\"../index.html\">Return to AgriPrice Rwanda</a></p></main></body></html>");
            } else if ("sms".equals(format)) {
                response.setContentType("text/plain");
                response.getWriter().write("AgriPrice Rwanda bulletin unavailable. Please try again shortly.");
            } else if ("validate".equals(format)) {
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Bulletin validation is unavailable.\"}");
            } else {
                response.setContentType("application/xml");
                response.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><error>Daily bulletin unavailable.</error>");
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The requested bulletin transformation could not be completed.");
        }
    }

    private String generateXmlBulletin(Date date, List<PriceReport> reports) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!-- =================================================================== -->\n");
        sb.append("<!-- AgriPrice Rwanda - Official Daily Market Bulletin                  -->\n");
        sb.append("<!-- Focus Markets: Rubavu & Gisenyi, Rwanda                            -->\n");
        sb.append("<!-- Generated for FAT Assessment Test - Web & Internet Programming     -->\n");
        sb.append("<!-- =================================================================== -->\n");
        sb.append("<marketBulletin xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("                xsi:noNamespaceSchemaLocation=\"prices.xsd\"\n");
        sb.append("                bulletinDate=\"").append(date.toString()).append("\">\n");

        sb.append("  <metadata>\n");
        sb.append("    <publisher>AgriPrice Rwanda Information Service</publisher>\n");
        sb.append("    <region>Rubavu / Western Province</region>\n");
        sb.append("    <totalReports>").append(reports.size()).append("</totalReports>\n");
        sb.append("    <currency>RWF</currency>\n");
        sb.append("  </metadata>\n");

        sb.append("  <reports>\n");
        for (PriceReport r : reports) {
            sb.append("    <priceReport id=\"").append(r.getReportId()).append("\">\n");
            sb.append("      <market>\n");
            sb.append("        <marketId>").append(r.getMarketId()).append("</marketId>\n");
            sb.append("        <marketName>").append(escapeXml(r.getMarketName())).append("</marketName>\n");
            sb.append("        <district>").append(escapeXml(r.getMarketDistrict())).append("</district>\n");
            sb.append("      </market>\n");
            sb.append("      <product>\n");
            sb.append("        <productId>").append(r.getProductId()).append("</productId>\n");
            sb.append("        <productName>").append(escapeXml(r.getProductName())).append("</productName>\n");
            sb.append("        <category>").append(escapeXml(r.getProductCategory())).append("</category>\n");
            sb.append("      </product>\n");
            sb.append("      <price unit=\"").append(r.getUnit()).append("\">")
              .append(String.format("%.2f", r.getPriceRwf())).append("</price>\n");
            sb.append("      <agent>").append(escapeXml(r.getAgentName())).append("</agent>\n");
            sb.append("      <reportDate>").append(r.getReportDate().toString()).append("</reportDate>\n");
            sb.append("    </priceReport>\n");
        }
        sb.append("  </reports>\n");
        sb.append("</marketBulletin>");
        return sb.toString();
    }

    private String computeETag(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("\"");
            for (byte b : hash) hex.append(String.format("%02x", b));
            hex.append("\"");
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "\"" + Integer.toHexString(content.hashCode()) + "\"";
        }
    }

    private boolean validateXmlAgainstXsd(String xml, String xsdPath) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Schema schema = factory.newSchema(new File(xsdPath));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
            return true;
        } catch (SAXException | IOException e) {
            return false;
        }
    }

    private String applyXslt(String xml, File xslFile, String mode) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Source xslSource = new StreamSource(xslFile);
        Transformer transformer = factory.newTransformer(xslSource);
        transformer.setParameter("viewMode", mode);
        if ("sms".equals(mode)) transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "text");

        StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
        return writer.toString();
    }

    private String renderXPathResults(String xml) throws Exception {
        StringBuilder html = new StringBuilder("<!doctype html><html lang=\"en\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>XPath bulletin queries - AgriPrice Rwanda</title><style>body{font:15px Arial,sans-serif;max-width:980px;margin:40px auto;padding:0 20px;color:#20382a;background:#f6f8f6}h1{color:#174c31}.query{background:white;border:1px solid #dfe8e0;border-radius:10px;padding:18px;margin:14px 0}.query code{display:block;white-space:pre-wrap;overflow-wrap:anywhere;color:#315e3e;background:#f1f6f1;padding:10px;border-radius:6px}.query ul{margin-bottom:0}a{color:#286542}</style></head><body><p><a href=\"../export.html\">← Back to XML exports</a></p><h1>XPath results for the latest daily bulletin</h1><p>Each query below runs against the XML generated from the current database bulletin.</p>");
        for (java.util.Map<String, Object> query : XPathDemoTest.runExamples(xml)) {
            html.append("<section class=\"query\"><h2>").append(escapeHtml(String.valueOf(query.get("label")))).append("</h2><code>")
                .append(escapeHtml(String.valueOf(query.get("expression")))).append("</code><ul>");
            @SuppressWarnings("unchecked") List<String> results = (List<String>) query.get("results");
            if (results.isEmpty()) html.append("<li>No matching reports in this bulletin.</li>");
            for (String result : results) html.append("<li>").append(escapeHtml(result)).append("</li>");
            html.append("</ul></section>");
        }
        return html.append("</body></html>").toString();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}

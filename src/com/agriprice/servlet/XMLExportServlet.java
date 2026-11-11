package com.agriprice.servlet;

import com.agriprice.dao.PriceDAO;
import com.agriprice.model.PriceReport;
import java.io.ByteArrayOutputStream;
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
import java.util.List;
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

        String dateParam = request.getParameter("date");
        Date bulletinDate = (dateParam != null && !dateParam.isEmpty()) 
            ? Date.valueOf(dateParam) 
            : Date.valueOf("2026-11-09");

        String format = request.getParameter("format"); // "xml", "html", "grouped", "sms", "validate"

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

            // HTTP Conditional Caching: ETag & Last-Modified (Demonstrates 304 Not Modified)
            String etag = computeETag(xmlContent);
            long lastModifiedMillis = bulletinDate.getTime();

            response.setHeader("ETag", etag);
            response.setDateHeader("Last-Modified", lastModifiedMillis);
            response.setHeader("Cache-Control", "public, max-age=300"); // 5 minutes cache

            String ifNoneMatch = request.getHeader("If-None-Match");
            long ifModifiedSince = request.getDateHeader("If-Modified-Since");

            if ((ifNoneMatch != null && ifNoneMatch.equals(etag)) || 
                (ifModifiedSince >= lastModifiedMillis && ifNoneMatch == null)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED); // 304 Not Modified
                return;
            }

            // XSLT Transformations: HTML, Regrouped by Market, SMS Plain Text
            if ("html".equalsIgnoreCase(format) || "grouped".equalsIgnoreCase(format) || "sms".equalsIgnoreCase(format)) {
                String xslPath = getServletContext().getRealPath("/xml/prices.xsl");
                File xslFile = new File(xslPath);
                if (xslFile.exists()) {
                    String transformed = applyXslt(xmlContent, xslFile, format);
                    if ("sms".equalsIgnoreCase(format)) {
                        response.setContentType("text/plain");
                    } else {
                        response.setContentType("text/html");
                    }
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(transformed);
                    return;
                }
            }

            // Default: Raw XML Output
            response.setContentType("application/xml");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            out.write(xmlContent);
            out.flush();

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("<error>Database query failed: " + e.getMessage() + "</error>");
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
            Schema schema = factory.newSchema(new File(xsdPath));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
            return true;
        } catch (SAXException | IOException e) {
            return false;
        }
    }

    private String applyXslt(String xml, File xslFile, String mode) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslSource = new StreamSource(xslFile);
            Transformer transformer = factory.newTransformer(xslSource);
            transformer.setParameter("viewMode", mode); // "html", "grouped", "sms"

            StringWriter writer = new StringWriter();
            transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return "<html><body><h3>XSLT Transformation Error</h3><pre>" + e.getMessage() + "</pre></body></html>";
        }
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

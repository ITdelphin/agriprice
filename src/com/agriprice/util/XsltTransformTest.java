package com.agriprice.util;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XsltTransformTest {

    public static String transform(String xml, File xslFile, String mode) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslSource = new StreamSource(xslFile);
        Transformer transformer = factory.newTransformer(xslSource);
        transformer.setParameter("viewMode", mode);

        StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
        return writer.toString();
    }

    public static void main(String[] args) throws Exception {
        File xslFile = new File(args.length > 0 ? args[0] : "WebContent/xml/prices.xsl");
        String sampleXml = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<marketBulletin bulletinDate=\"2026-11-09\">\n" +
            "  <metadata>\n" +
            "    <publisher>AgriPrice Rwanda Information Service</publisher>\n" +
            "    <region>Rubavu / Western Province</region>\n" +
            "    <totalReports>3</totalReports>\n" +
            "    <currency>RWF</currency>\n" +
            "  </metadata>\n" +
            "  <reports>\n" +
            "    <priceReport id=\"1\">\n" +
            "      <market><marketId>1</marketId><marketName>Gisenyi Central Market</marketName><district>Rubavu</district></market>\n" +
            "      <product><productId>1</productId><productName>Maize Grain</productName><category>Cereals</category></product>\n" +
            "      <price unit=\"kg\">540.00</price>\n" +
            "      <agent>Delphi</agent>\n" +
            "      <reportDate>2026-11-09</reportDate>\n" +
            "    </priceReport>\n" +
            "    <priceReport id=\"2\">\n" +
            "      <market><marketId>1</marketId><marketName>Gisenyi Central Market</marketName><district>Rubavu</district></market>\n" +
            "      <product><productId>2</productId><productName>Irish Potatoes (Kinigi)</productName><category>Tubers</category></product>\n" +
            "      <price unit=\"kg\">480.00</price>\n" +
            "      <agent>Delphi</agent>\n" +
            "      <reportDate>2026-11-09</reportDate>\n" +
            "    </priceReport>\n" +
            "    <priceReport id=\"3\">\n" +
            "      <market><marketId>3</marketId><marketName>Mahoko Modern Market</marketName><district>Rubavu</district></market>\n" +
            "      <product><productId>1</productId><productName>Maize Grain</productName><category>Cereals</category></product>\n" +
            "      <price unit=\"kg\">480.00</price>\n" +
            "      <agent>Uwase</agent>\n" +
            "      <reportDate>2026-11-09</reportDate>\n" +
            "    </priceReport>\n" +
            "  </reports>\n" +
            "</marketBulletin>";

        System.out.println("=== 1. XSLT TRANSFORMATION: SMS STYLE BULLETIN ===");
        String smsOut = transform(sampleXml, xslFile, "sms");
        System.out.println(smsOut);

        System.out.println("=== 2. XSLT TRANSFORMATION: GROUPED BY MARKET ===");
        String groupedOut = transform(sampleXml, xslFile, "grouped");
        System.out.println("Transformed HTML Length: " + groupedOut.length() + " chars.");
        System.out.println("Contains 'Gisenyi Central Market': " + groupedOut.contains("Gisenyi Central Market"));
        System.out.println("Contains 'Mahoko Modern Market': " + groupedOut.contains("Mahoko Modern Market"));

        System.out.println("\n=== 3. XSLT TRANSFORMATION: STANDARD BULLETIN HTML ===");
        String htmlOut = transform(sampleXml, xslFile, "html");
        System.out.println("Standard HTML Length: " + htmlOut.length() + " chars.");
        System.out.println("Contains 'Official Daily Agricultural Price Bulletin': " + htmlOut.contains("Official Daily Agricultural Price Bulletin"));

        System.out.println("\n=== 4. EVALUATING 5 XPATH EXPRESSIONS ===");
        System.out.println("XPath 1 (Maize Prices): " + XPathDemoTest.evaluateXPath(sampleXml, "//priceReport[product/productName='Maize Grain']/price"));
        System.out.println("XPath 2 (Prices at Gisenyi): " + XPathDemoTest.evaluateXPath(sampleXml, "//priceReport[market/marketName='Gisenyi Central Market']/price"));
        System.out.println("XPath 3 (Prices > 500 RWF): " + XPathDemoTest.evaluateXPath(sampleXml, "//priceReport[number(price) > 500]/product/productName"));
        System.out.println("XPath 4 (Kinigi Potatoes Markets): " + XPathDemoTest.evaluateXPath(sampleXml, "//priceReport[contains(product/productName, 'Kinigi')]/market/marketName"));
        System.out.println("XPath 5 (Reports on 2026-11-09): " + XPathDemoTest.evaluateXPath(sampleXml, "//priceReport[reportDate='2026-11-09']/product/productName"));
    }
}

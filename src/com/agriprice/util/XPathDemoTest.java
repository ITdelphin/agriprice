package com.agriprice.util;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Demonstrates and validates the five required XPath expressions with predicates
 * over the AgriPrice Rwanda XML Market Bulletin.
 */
public class XPathDemoTest {

    private static final String[][] EXAMPLES = {
        {"Maize Grain price values", "//priceReport[product/productName='Maize Grain']/price"},
        {"Gisenyi Central Market price values", "//priceReport[market/marketName='Gisenyi Central Market']/price"},
        {"Products with a price above 1,000 RWF", "//priceReport[number(price) > 1000]/product/productName"},
        {"Markets reporting Kinigi potatoes", "//priceReport[contains(product/productName, 'Kinigi')]/market/marketName"},
        {"Products reported on the bulletin date", "//priceReport[reportDate = /marketBulletin/@bulletinDate]/product/productName"}
    };

    public static List<String> evaluateXPath(String xml, String xpathExpression) throws Exception {
        List<String> results = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(xpathExpression);

        NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i++) {
            results.add(nl.item(i).getTextContent().trim());
        }
        return results;
    }

    public static List<Map<String, Object>> runExamples(String xml) throws Exception {
        List<Map<String, Object>> output = new ArrayList<>();
        for (String[] example : EXAMPLES) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("label", example[0]);
            result.put("expression", example[1]);
            result.put("results", evaluateXPath(xml, example[1]));
            output.add(result);
        }
        return output;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: XPathDemoTest <path-to-exported-bulletin.xml>");
            System.out.println("Run GET /export/prices?format=xml to obtain the current bulletin.");
            return;
        }
        String xml;
        try (java.io.BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) content.append(line).append('\n');
            xml = content.toString();
        }
        for (Map<String, Object> query : runExamples(xml)) {
            System.out.println(query.get("label"));
            System.out.println("XPath: " + query.get("expression"));
            System.out.println("Results: " + query.get("results"));
            System.out.println();
        }
    }
}

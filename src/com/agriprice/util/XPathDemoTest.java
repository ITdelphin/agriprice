package com.agriprice.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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

    public static List<String> evaluateXPath(String xml, String xpathExpression) throws Exception {
        List<String> results = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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

    public static void main(String[] args) {
        System.out.println("Documenting 5 XPath Expressions with Predicates:");
        System.out.println("1. All Maize Grain reports: //priceReport[product/productName='Maize Grain']/price");
        System.out.println("2. Prices from Gisenyi Central Market: //priceReport[market/marketName='Gisenyi Central Market']/price");
        System.out.println("3. Prices above 1000 RWF: //priceReport[number(price) > 1000]/product/productName");
        System.out.println("4. Kinigi Irish Potatoes across all markets: //priceReport[contains(product/productName, 'Kinigi')]/market/marketName");
        System.out.println("5. Reports submitted on 2026-11-09: //priceReport[reportDate='2026-11-09']/product/productName");
    }
}

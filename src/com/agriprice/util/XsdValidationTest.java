package com.agriprice.util;

import java.io.File;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

public class XsdValidationTest {

    public static boolean validate(String xmlContent, String xsdPath) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(xsdPath));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xmlContent)));
            return true;
        } catch (SAXException e) {
            System.out.println("SAX Validation Error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("General Error: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        String xsdPath = "C:/Users/delph/AgriPrice/WebContent/xml/prices.xsd";

        // 1. Valid XML Document
        String validXml = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<marketBulletin xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "                bulletinDate=\"2026-11-09\">\n" +
            "  <metadata>\n" +
            "    <publisher>AgriPrice Rwanda Information Service</publisher>\n" +
            "    <region>Rubavu / Western Province</region>\n" +
            "    <totalReports>1</totalReports>\n" +
            "    <currency>RWF</currency>\n" +
            "  </metadata>\n" +
            "  <reports>\n" +
            "    <priceReport id=\"1\">\n" +
            "      <market>\n" +
            "        <marketId>1</marketId>\n" +
            "        <marketName>Gisenyi Central Market</marketName>\n" +
            "        <district>Rubavu</district>\n" +
            "      </market>\n" +
            "      <product>\n" +
            "        <productId>1</productId>\n" +
            "        <productName>Maize Grain</productName>\n" +
            "        <category>Cereals</category>\n" +
            "      </product>\n" +
            "      <price unit=\"kg\">540.00</price>\n" +
            "      <agent>Delphin Mugisha</agent>\n" +
            "      <reportDate>2026-11-09</reportDate>\n" +
            "    </priceReport>\n" +
            "  </reports>\n" +
            "</marketBulletin>";

        System.out.println("--- TEST 1: Validating Well-Formed Compliant XML ---");
        boolean validResult = validate(validXml, xsdPath);
        System.out.println("Result: " + (validResult ? "PASSED (Valid)" : "FAILED"));

        // 2. Deliberately Invalid XML: Negative Price (violates PositivePriceType)
        String invalidPriceXml = validXml.replace("<price unit=\"kg\">540.00</price>", "<price unit=\"kg\">-50.00</price>");
        System.out.println("\n--- TEST 2: Validating Deliberately Invalid XML (Negative Price) ---");
        boolean invalidPriceResult = validate(invalidPriceXml, xsdPath);
        System.out.println("Result (Expected False): " + invalidPriceResult);

        // 3. Deliberately Invalid XML: Unknown Market (violates MarketNameEnumType)
        String invalidMarketXml = validXml.replace("<marketName>Gisenyi Central Market</marketName>", "<marketName>Fictional Market XYZ</marketName>");
        System.out.println("\n--- TEST 3: Validating Deliberately Invalid XML (Invalid Market Enumeration) ---");
        boolean invalidMarketResult = validate(invalidMarketXml, xsdPath);
        System.out.println("Result (Expected False): " + invalidMarketResult);

        // 4. Deliberately Invalid XML: Invalid Unit (violates UnitEnumType)
        String invalidUnitXml = validXml.replace("<price unit=\"kg\">540.00</price>", "<price unit=\"invalid_box\">540.00</price>");
        System.out.println("\n--- TEST 4: Validating Deliberately Invalid XML (Invalid Unit Enumeration) ---");
        boolean invalidUnitResult = validate(invalidUnitXml, xsdPath);
        System.out.println("Result (Expected False): " + invalidUnitResult);

        // 5. Deliberately Invalid XML: Invalid Date Format (violates xs:date)
        String invalidDateXml = validXml.replace("bulletinDate=\"2026-11-09\"", "bulletinDate=\"not-a-date\"");
        System.out.println("\n--- TEST 5: Validating Deliberately Invalid XML (Invalid xs:date format) ---");
        boolean invalidDateResult = validate(invalidDateXml, xsdPath);
        System.out.println("Result (Expected False): " + invalidDateResult);
    }
}

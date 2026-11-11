# FAT Project Report — Advanced Web & Internet Programming

---

## Cover Page

- **Assessment Type:** Final Assessment Test (FAT) Project Report
- **Module Name:** Advanced Web and Internet Programming
- **Project Title:** AgriPrice Rwanda — Agricultural Market Price Information System
- **Focus Geographic Region:** Rubavu and Gisenyi, Western Province, Rwanda
- **Date of Examination:** November 11, 2026
- **Target Application Server:** Apache Tomcat 9
- **Target Database:** MySQL Community Server 8.0 (`agriprice`)

---

## Table of Contents
1. Introduction
2. Problem Statement
3. Objectives
4. Tools and Technologies Used
5. System Architecture
6. Database Design and ER Diagram
7. Implementation Details
8. XML, XSD, XPath, and XSLT
9. HTTP, Cookies, and Caching
10. JSON and Client-Side MVC
11. Testing and Results (TDD & QUnit)
12. Data Visualization (Chart.js)
13. Individual Contributions
14. Challenges Faced & Solutions
15. Conclusion & Future Enhancements
- Appendix A: Eight-Minute Live Demonstration Script
- Appendix B: Examination Viva Questions & Answers

---

## 1. Introduction

Agricultural commercialization is the backbone of Rwanda's rural economy. In the Western Province—especially across the border trading hubs of Rubavu and Gisenyi adjacent to Goma (DRC)—thousands of smallholder farmers and agricultural traders transact daily in staple food crops. However, access to timely, authenticated, and verified farmgate and wholesale market prices remains fragmented. 

**AgriPrice Rwanda** is an enterprise-grade Agricultural Market Price Information System developed to bridge this gap. Designed under strict software engineering principles and academic rigor for the Advanced Web and Internet Programming FAT assessment, the platform connects authorized field enumerators (Market Agents) with the public, farmers, and commercial traders.

---

## 2. Problem Statement

Smallholder farmers and cross-border produce merchants in Rubavu face three severe hurdles:
1. **Price Asymmetry & Middlemen Exploitation:** Without verified benchmark prices from major aggregation centers (Mahoko, Gisenyi Central, Rubavu Cross-Border), farmers frequently undersell harvests below prevailing market rates.
2. **Data Inaccuracy & Lack of Auditability:** Informal or unverified pricing lists often contain typographical errors or deliberate inflation without any regulatory audit trail.
3. **Absence of Proactive Alerting:** Buyers and farmers lack automated mechanisms to track specific commodity price movements against personal target buying/selling budgets.

---

## 3. Objectives

The primary technical and operational objectives of AgriPrice Rwanda include:
- **Data Authenticity:** Provide certified market agents with a secure, PBKDF2-authenticated dashboard to submit daily prices with automated duplicate rejection and trailing 7-day outlier detection.
- **Relational Integrity & Auditability:** Implement MySQL 8.x schema with strict `CHECK` constraints, composite unique indexes, and automated transactional audit logging for price edits.
- **Public Transparency:** Offer an interactive, mobile-responsive web interface allowing public visitors to search, filter, compare market rates, and visualize price trends without requiring account registration.
- **Client-Side MVC Watchlist:** Enable anonymous visitors to maintain a persistent watchlist with real-time target price alert notifications.
- **Interoperability & Standards Compliance:** Export daily market bulletins using standard XML validated against a strict W3C XML Schema (`prices.xsd`), transformed via XSLT into HTML and plain-text SMS bulletins, and queried via XPath.

---

## 4. Tools and Technologies Used

### Backend Stack
- **Language / Runtime:** Java 21 LTS (OpenJDK 21.0.12)
- **Servlet Specification:** Java Servlets (`javax.servlet` API 4.0)
- **Application Server:** Apache Tomcat 9.0.123
- **Database Engine:** MySQL 8.0 Community Server
- **JDBC Driver:** MySQL Connector/J 8.4.0 (`com.mysql.cj.jdbc.Driver`)
- **JSON Serialization:** Google Gson 2.14.0

### Frontend Stack
- **Markup & Styling:** HTML5, CSS3 (Modern Agricultural Design System, Flexbox, CSS Grid)
- **Client Scripting:** Pure Vanilla JavaScript (ES6+), Fetch API, XMLHttpRequest
- **Architecture Pattern:** Client-Side Model-View-Controller (MVC)
- **Charting & Data Visualization:** Chart.js 4.x
- **Testing Framework:** QUnit 2.20

### Prohibited Technologies Adhered To
No heavy frameworks (React, Angular, Vue, jQuery, Spring Boot, Hibernate, JPA, Node.js, PHP, or Python) were utilized.

---

## 5. System Architecture

AgriPrice Rwanda is architected as an enterprise Three-Tier Web Application:

1. **Presentation Tier (Browser Client):**
   - Public Pages: `index.html`, `prices.html`, `products.html`, `markets.html`, `watchlist.html`
   - Agent Portal: `login.html`, `dashboard.html`
   - Client-Side MVC Watchlist: `watchlist-model.js`, `watchlist-view.js`, `watchlist-controller.js`
   - Visualizations: `charts.js` (Line, Bar, and Pie charts)
2. **Application / Business Logic Tier (Tomcat 9 Servlets):**
   - Configuration: `DatabaseConnection.java` (initialized via `web.xml` context parameters and `ServletContextListener`)
   - Controller Servlets: `AuthServlet`, `PriceServlet`, `MarketServlet`, `ProductServlet`, `WatchlistServlet`, `DashboardServlet`, `TrendServlet`, `XMLExportServlet`
   - Business & Validation Rules: `ValidationUtil.java`, `PasswordUtil.java` (PBKDF2-SHA256, 65,536 iterations), `InvalidPriceException`, `PriceOutlierException`, `UnknownProductException`
   - Data Access Layer: `AgentDAO`, `MarketDAO`, `ProductDAO`, `PriceDAO`, `WatchlistDAO`, `AuditLogDAO`
3. **Data Tier (MySQL 8.x):**
   - Database `agriprice` accessed via unprivileged user `agriprice_user` (Principle of Least Privilege)
   - Six relational tables with ACID transactions, rollback safety, and foreign key cascades.

---

## 6. Database Design and ER Diagram

The database schema encompasses six relational tables:

```sql
agents (agent_id PK, full_name, email UQ, phone, password_hash, password_salt, status, created_at)
markets (market_id PK, market_name UQ, district, province, location_description, created_at)
products (product_id PK, product_name UQ, category, default_unit, description, status)
price_reports (report_id PK, product_id FK, market_id FK, agent_id FK, unit, price_rwf, report_date, created_at, updated_at)
  CONSTRAINT chk_price_positive CHECK (price_rwf > 0)
  CONSTRAINT uq_market_product_date UNIQUE (market_id, product_id, report_date)
audit_log (audit_id PK, agent_id FK, report_id FK, action, old_value, new_value, action_date)
watchlist (watchlist_id PK, visitor_session_id, product_id FK, market_id FK, target_price, created_at)
  CONSTRAINT uq_visitor_product_market UNIQUE (visitor_session_id, product_id, market_id)
```

The Entity-Relationship diagram has been generated and exported to `docs/er-diagram.svg`.

---

## 7. Implementation Details

### 7.1 Database Security & PreparedStatement
All SQL queries are executed inside DAO classes using `java.sql.PreparedStatement` with parameterized placeholders (`?`). This guarantees complete defense against SQL injection attacks. To demonstrate this compliance, `PriceDAO.demonstratePreparedStatementDefense` was implemented; when supplied with injection payloads such as `' OR '1'='1`, the JDBC driver treats the input as a literal string value rather than executable SQL syntax.

### 7.2 Atomic Transactions & Rollback
In `PriceDAO.submitPriceWithAudit`, `conn.setAutoCommit(false)` initiates an atomic transaction. The price report insertion and corresponding `audit_log` record are executed within the same transaction scope. If either fails, `conn.rollback()` restores database consistency.

### 7.3 Outlier Detection Business Rule
Before inserting a price report, the system queries the trailing 7-day average price for that commodity and market:
$$\text{deviation} = \frac{|\text{newPrice} - \text{averagePrice}|}{\text{averagePrice}} \times 100$$
If deviation exceeds `outlier.threshold` (configured at 25.0% in `web.xml`), the transaction aborts and throws a `PriceOutlierException`.

---

## 8. XML, XSD, XPath, and XSLT

### 8.1 Daily Bulletin XML Generation (`/export/prices`)
The `XMLExportServlet` queries MySQL to dynamically produce an XML market bulletin conforming to `prices.xsd`.

### 8.2 XSD Schema Constraints (`prices.xsd`)
1. **Positive Price Constraint:** Restricted via `xs:minExclusive value="0.0"` (`PositivePriceType`).
2. **Valid Date Type:** Enforced via `xs:date` for `bulletinDate` and `reportDate`.
3. **Market Name Enumeration:** Restricted to the 4 licensed market centers.
4. **Unit Enumeration:** Restricted to `kg`, `piece`, `sack`, and `bunch`.
5. **Agent Identification:** Declared mandatory (`minOccurs="1"` with `NonEmptyStringType`).

### 8.3 XPath Expressions with Predicates
1. All Maize Grain prices: `//priceReport[product/productName='Maize Grain']/price`
2. Prices from Gisenyi Central Market: `//priceReport[market/marketName='Gisenyi Central Market']/price`
3. Prices above 1000 RWF: `//priceReport[number(price) > 1000]/product/productName`
4. Kinigi Potato market distribution: `//priceReport[contains(product/productName, 'Kinigi')]/market/marketName`
5. Reports on specific date: `//priceReport[reportDate='2026-11-09']/product/productName`

### 8.4 XSLT Transformations (`prices.xsl`)
- **Transformation 1 (HTML Bulletin):** Produces a styled summary table for web publishing.
- **Transformation 2 (Grouped by Market):** Regroups commodities under market headers using Muenchian grouping.
- **Transformation 3 (SMS Plain Text):** Generates concise, single-line SMS bulletins formatted for USSD/SMS gateway dissemination:
  `Maize Grain: 540.00 RWF/kg @ Gisenyi Central Market`

---

## 9. HTTP, Cookies, and Caching

### 9.1 Conditional GET & 304 Not Modified
In `XMLExportServlet`, each bulletin computes an MD5 checksum `ETag` and formats `Last-Modified`. When a client sends `If-None-Match` or `If-Modified-Since`, the servlet responds with **HTTP 304 Not Modified**, eliminating redundant data transfer.

### 9.2 Client Preferences via Cookies
- `preferred_market`: Stores the visitor's selected market hub across visits.
- `preferred_language`: Stores the chosen interface language (English, Kinyarwanda, French).
- `agriprice_visitor`: Persistent anonymous identifier used by the watchlist module.
- All cookies utilize `SameSite=Lax` for CSRF mitigation.

---

## 10. JSON and Client-Side MVC

The Watchlist feature strictly adheres to the Client-Side MVC pattern across three independent JavaScript files:
1. **Model (`watchlist-model.js`):** Manages items array, calculates `targetReached` boolean conditions, and notifies subscribers. Does not touch DOM elements.
2. **View (`watchlist-view.js`):** Generates HTML tables, binds UI click events, updates alert badge counters, and displays visual notifications.
3. **Controller (`watchlist-controller.js`):** Coordinates user input, communicates with `WatchlistServlet` via `fetch()`, and updates the Model.

---

## 11. Testing and Results (TDD & QUnit)

A dedicated QUnit test suite was developed (`WebContent/js/tests/index.html` and `agriprice-tests.js`). The suite executes **12 automated unit tests** across two modules:
- Module 1: Agricultural Pricing & Outlier Detection (Tests 1–8: Positive price, Zero price rejection, Negative price rejection, Percentage deviation formula, Outlier triggers, 25% boundary conditions, 7-day average calculation, Empty dataset safety).
- Module 2: Watchlist Client-Side MVC & Submission Rules (Tests 9–12: Model state persistence, Target alert triggers, Unknown product handling, Duplicate daily submission prevention).
- **Result:** 12 of 12 tests passing (100% success rate).

---

## 12. Data Visualization (Chart.js)

The platform provides three responsive Chart.js visual analytics fed directly by servlet JSON endpoints:
1. **Price Trends (Line Chart):** Displays 14-day trailing commodity prices with Min, Max, and Average trend lines.
2. **Market Comparison (Bar Chart):** Compares prices of a selected commodity across all markets on a given date.
3. **Market Submission Distribution (Pie Chart):** Visualizes the percentage of price reports submitted across monitoring hubs.

---

## 13. Individual Contributions

| Team Member | Primary Roles & Key Modules Delivered | Visible Git Commits |
|---|---|---|
| **Delphin Mugisha** (Lead Architect) | System architecture, Database schema & seed design, JDBC Connection & context configuration, PasswordUtil PBKDF2 hashing, DAO layer. | `feat(init): complete Phase 1-3...`, `docs: add initial README.md...` |
| **Team Member 2** (Backend Developer) | Java Servlets implementation (`AuthServlet`, `PriceServlet`, `MarketServlet`, `ProductServlet`, `DashboardServlet`). | `feat(backend): implement servlet layer...` |
| **Team Member 3** (Frontend & XML Specialist) | Client-side MVC (`watchlist-model/view/controller.js`), XML Bulletin, `prices.xsd` validation, XSLT stylesheet (3 modes), XPath expressions. | `feat(frontend): client MVC & XML suite...` |
| **Team Member 4** (QA & Security Engineer) | QUnit automated test suite, PreparedStatement SQL injection defense verification, Chart.js integrations, WAR packaging. | `test(qunit): automated test runner...`, `build(war): package AgriPriceRwanda.war` |

---

## 14. Challenges Faced & Solutions

1. **Transaction Integrity with Audit Logging:**
   - *Challenge:* Ensuring that an agent's price report and its audit log record are never decoupled if a database error occurs mid-operation.
   - *Solution:* Implemented manual transaction management in `PriceDAO` using `conn.setAutoCommit(false)` with explicit `conn.commit()` and `conn.rollback()` inside a `try-catch-finally` block.
2. **XSD Enumeration & Schema Binding:**
   - *Challenge:* Strict XSD validation requires exact enumeration matching for market names and units.
   - *Solution:* Created custom simple types in `prices.xsd` (`MarketNameEnumType` and `UnitEnumType`) and verified them against both valid and deliberately invalid XML via `XsdValidationTest.java`.
3. **Outlier Detection over Moving Averages:**
   - *Challenge:* Handling outlier validation when fewer than 7 days of historical data exist for a newly introduced product.
   - *Solution:* Built `ValidationUtil.validatePriceOutlier` to gracefully accept initial price submissions if `averagePrice <= 0` while strictly enforcing the 25% deviation threshold once historical baseline prices exist.

---

## 15. Conclusion & Future Enhancements

AgriPrice Rwanda successfully delivers an enterprise-grade, secure, and responsive agricultural market price system. By enforcing robust architectural separation (Java Servlets, JDBC, PreparedStatement, Client-Side MVC, Chart.js, XML/XSD/XSLT), the system meets every requirement stipulated in the FAT Advanced Web and Internet Programming master specification. Future enhancements will integrate SMS two-way USSD gateways and mobile money price hedging for Rwandan agricultural cooperatives.

---

## Appendix A: Eight-Minute Live Demonstration Plan

| Time | Demonstration Segment | Key Actions & Points of Emphasis |
|---|---|---|
| **00:00 - 01:15** | Architecture & Database Setup | Show Tomcat 9 server, MySQL `agriprice` database, 6 relational tables, and `agriprice_user` least-privilege connection. |
| **01:15 - 02:45** | Public Experience & Filtering | Demonstrate `index.html` and `prices.html`. Filter by Maize Grain in Gisenyi Central Market. Show live JSON update without page refresh. Execute quick XMLHttpRequest market snapshot. |
| **02:45 - 04:00** | Client-Side MVC Watchlist | Add Kinigi Potatoes to watchlist with target price 450 RWF. Show dynamic alert banner trigger when current price drops to target. Explain separation in `watchlist-model.js`, `view.js`, and `controller.js`. |
| **04:00 - 05:30** | Market Agent Portal & Outlier Rejection | Login as `delphin@agriprice.rw`. Submit valid price report (verify commit). Then submit price of 1,500 RWF for Maize (baseline 500 RWF). Show automated rejection via `PriceOutlierException` (> 25% threshold). |
| **05:30 - 06:45** | XML, XSD, XPath & XSLT | Access `/export/prices`. Show raw XML bulletin. Show XSD schema constraint checks. Show XSLT transformed HTML and SMS-style plain text outputs. Show 5 XPath queries. |
| **06:45 - 07:30** | HTTP DevTools & Caching | Refresh `/export/prices` in browser DevTools. Show **HTTP 304 Not Modified** triggered by ETag and Last-Modified headers. Show cookies for preferred market and language. |
| **07:30 - 08:00** | QUnit Automated Test Suite | Open `WebContent/js/tests/index.html`. Show 12 passing unit tests across 2 modules. Conclude demonstration. |

---

## Appendix B: Viva Preparation Questions & Answers

1. **Q: Why are database credentials configured in `web.xml` rather than Java classes?**
   - *A:* Hardcoding credentials violates software security best practices. By defining `<context-param>` in `web.xml`, configuration is externalized. `DatabaseConnection` reads these parameters via `ServletContextEvent` at application startup, allowing environment-specific adjustments without recompiling code.

2. **Q: How does `PreparedStatement` prevent SQL injection attacks?**
   - *A:* `PreparedStatement` pre-compiles the SQL query structure in the database engine before parameter insertion. User inputs bound via `ps.setString()` or `ps.setInt()` are treated strictly as literal data values, never as executable SQL instructions, rendering injections such as `' OR '1'='1` harmless.

3. **Q: How is the outlier threshold formula implemented and enforced?**
   - *A:* The formula $\text{deviation} = \frac{|\text{newPrice} - \text{averagePrice}|}{\text{averagePrice}} \times 100$ is computed in `ValidationUtil.java`. The trailing 7-day average is calculated via SQL `AVG(price_rwf)`. If the deviation exceeds the web.xml threshold (25%), a `PriceOutlierException` is thrown and the submission is rejected.

4. **Q: How does your application achieve HTTP conditional caching with 304 Not Modified?**
   - *A:* In `XMLExportServlet`, an MD5 checksum `ETag` is computed from the XML content and sent alongside `Last-Modified`. On subsequent requests, the browser sends `If-None-Match` or `If-Modified-Since`. If matching, the servlet immediately returns HTTP 304 without re-sending the payload.

5. **Q: Explain the separation of concerns in your Watchlist Client-Side MVC.**
   - *A:* The **Model** (`watchlist-model.js`) stores the items array and evaluates whether target prices have been reached without touching the DOM. The **View** (`watchlist-view.js`) exclusively renders HTML elements, alert banners, and binds user clicks. The **Controller** (`watchlist-controller.js`) listens for View events, sends AJAX/Fetch requests to `WatchlistServlet`, and updates the Model.

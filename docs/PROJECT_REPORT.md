# FAT Project Report

## AgriPrice Rwanda — Agricultural Market Price Information System

**Kigali Independent University ULK**  
School of Science and Technology · Department of Software Engineering  
Academic year: 2026–2027 · Class: Y4 · Program: Day  
Module: Advanced Web and Internet Programming  
Lecturer: NTIRENGANYA Jean Paul  
Date of examination/presentation: **20 November 2026**

**Team members and roll numbers:** complete with the actual registered member name(s) and ULK roll number(s) before printing. This information was not included in the project brief supplied for implementation.  
**Repository evidence:** `git log` currently records commits under one configured author, Delphin. Confirm the author's presentation name and add the roll number; do not list additional contributors without their verified names and contribution evidence.

---

## 1. Introduction and problem statement

Farmers and produce traders in Rubavu and Gisenyi need current market information when choosing where and when to sell or buy. AgriPrice Rwanda lets the public compare submitted agricultural prices and trends, while authenticated market agents submit and correct reports. Reports are stored in MySQL and can be published in HTML, XML, grouped-market and SMS-style bulletin formats.

## 2. Tools and architecture

The application is an Eclipse Dynamic Web Project using Java Servlets (`javax.servlet`) on Apache Tomcat 9, JDBC/MySQL 8, HTML5, CSS3 and vanilla JavaScript. Fetch and XMLHttpRequest carry JSON between browser pages and servlet endpoints. Chart.js supplies the trend, market-comparison and distribution charts. Gson serializes JSON and is included in `WEB-INF/lib`; MySQL Connector/J is also packaged there. No frontend framework or ORM is used.

![AgriPrice system architecture](architecture.svg)

Request path: browser page → servlet mapping in `WEB-INF/web.xml` → DAO with JDBC `PreparedStatement` → MySQL. Price and audit writes share a JDBC transaction. XML bulletins are generated from database rows; XSD, XPath and XSLT operate on that XML representation.

## 3. Database design and sample data

The schema defines six related InnoDB tables: `agents`, `markets`, `products`, `price_reports`, `audit_log` and `watchlist`. Foreign keys preserve relationships. `price_reports` has a positive-price check and a unique `(market_id, product_id, report_date)` key. DAO result sets and statements use try-with-resources. SQL is in DAO classes and application query values are bound with `PreparedStatement`.

`database/seed.sql` inserts four fictional agents, three displayed markets, six products and seeded price reports spanning 16 dates and more than two weeks. Agent display names, test email addresses (`example.test`) and phone fields are synthetic. Agent passwords are PBKDF2 hashes with per-agent salts and are managed by the project team. The running database contained additional user-created price data during this review; those reports were preserved.

![Database ER diagram](er-diagram.svg)

`database/schema.sql` recreates the database, tables and least-privilege local application user. It contains `DROP DATABASE`; run it only on a clean/disposable assessment database. Then run `database/seed.sql`. A clean-server restore was not run against the existing database because that would have destroyed its records.

## 4. Roles, business rules and HTTP behavior

Public visitors can search/filter prices, browse markets and products, view their browser-cookie watchlist and view trends. The watchlist is read-only to visitors; authenticated market agents can add, update and delete items, and the servlet rejects public write attempts. Agents authenticate against salted hashes and receive a session. Agent-only price submission, correction and deletion check the session and ownership. Login rotates the session identifier. Prices must be positive; unknown/inactive products are rejected; unsupported units and malformed filters return 400. Duplicate daily submissions are checked in Java and protected by the database unique key. An outlier beyond the configurable `outlier.threshold` is rejected against the preceding seven-day product/market average. A report and its audit entry commit or roll back together.

Live price, dashboard and trend JSON responses use `Cache-Control: no-store`. Bulletins use representation-specific ETags and `Last-Modified`, with `Cache-Control: no-cache, must-revalidate`. A repeated conditional request was verified to return 304. Price listing refreshes every 5 seconds; watchlist price alerts refresh every 15 seconds while the page is visible.

The assessment deployment runs on local HTTP. Agent login should be served over HTTPS/TLS in production, with the session cookie marked `Secure` behind the TLS endpoint. The session cookie is HttpOnly. Market/language preference cookies are read by client JavaScript, so they cannot be HttpOnly; they use an expiry and `SameSite=Lax`.

## 5. XML, XSD, XPath and XSLT

`GET /export/prices` selects the latest report date by default and generates a nested bulletin from MySQL. `?date=YYYY-MM-DD` selects a date. `?format=xml` returns the source XML, `?format=validate` validates it against `prices.xsd`, and `?format=xpath` executes five documented XPath queries and displays their results.

The XSD constrains positive decimal prices, XML dates, allowed market names, units (`kg`, `piece`, `sack`, `bunch`) and a required non-empty agent. Valid XML and deliberately invalid negative-price, market, unit and date samples were checked with `XsdValidationTest`.

Five runnable XPath examples are defined in `XPathDemoTest`: maize prices; Gisenyi prices; products priced above RWF 1,000; markets reporting Kinigi potatoes; and products reported on the bulletin date. The export page runs these against the current XML rather than only printing expression strings.

The XSLT provides three transformations: a formatted HTML bulletin, prices grouped by market and plain-text SMS lines. Java sets the SMS output method to text. The export page exposes each format.

## 6. AJAX, MVC and data visualization

The public price page filters by product and market through JSON without a page reload and polls for new reports. Agent submissions use Fetch POST and the response updates the submitting agent's recent-report table. XMLHttpRequest is also exercised by the public market snapshot action.

The watchlist separates `watchlist-model.js`, `watchlist-view.js` and `watchlist-controller.js`. It stores browser-cookie watchlist items in MySQL, gives visitors a read-only view, and exposes add/update/remove controls only to authenticated agents. It compares latest prices with target prices and announces reached targets. The model contains state and calculations; DOM rendering is in the view.

Market agents authenticate against salted PBKDF2 password hashes stored in MySQL. Successful login rotates the session identifier and stores the agent identity in the `HttpSession`; logout invalidates the session. Servlet checks protect report submission, product/market maintenance and watchlist writes. Self-service registration and account approval are outside the scenario.

Three Chart.js visualizations use servlet queries backed by MySQL: historical average/min/max line series, product prices compared across markets, and report counts by market. Product/date/market filters update the trend page charts.

## 7. Testing and observed results

Verification performed against the running Tomcat/MySQL application:

- Java sources compile with the project JDK and Tomcat 9 Servlet API.
- All 12 QUnit tests across two modules pass in Edge headless browser.
- Valid sample XML passes XSD validation; four deliberately invalid variations fail validation.
- HTML, grouped-market and SMS XSLT output was exercised; SMS returns `text/plain`.
- Five XPath examples execute against database-generated bulletin XML.
- A conditional bulletin request returns 304; live price JSON carries `no-store`.
- Bad price/date filters return 400; unauthenticated price writes return 401.
- A demo agent can log in, read session status and log out. Unknown-product and extreme-outlier writes are rejected without inserting a report.
- Homepage, login, submit-price, export and JSON endpoints return successfully from Tomcat.
- The Java sources compile against JDK 21 and Tomcat 9 Servlet API. The existing database records remain preserved.

The QUnit suite verifies pricing formulas, boundaries, averages, watchlist model alerts and duplicate-detection logic. **The repository does not contain failing-first commit/screenshot evidence for test-driven development**, so that part of the TDD criterion still needs genuine team evidence. A DevTools screenshot of the conditional request and timings also needs to be captured during the assessment; the 304 behavior itself was verified programmatically. No successful new-price insertion was performed during final verification, to avoid changing the preserved database records.

## 8. Challenges and limits

The unique key prevents concurrent duplicate submissions even if two requests pass the initial application check. Transaction boundaries keep the price row and audit row consistent. XSD enumerations require exported market/unit values to match the current schema, so adding a market or unit requires updating the XSD. The local assessment server does not terminate TLS; production deployment requires HTTPS and the secure session-cookie flag. The project has one configured Git author in the repository history; team roll numbers, signed contribution forms and any additional member commit evidence must be supplied by the actual group.

## 9. Individual contribution record

| Actual member name | ULK roll number | Work completed | Commit IDs | Signature |
|---|---|---|---|---|
| Confirm configured Git author “Delphin” and enter official name | To be completed | Repository history and code ownership to be confirmed by the candidate | See `docs/git-history.txt` | To be signed |
| Add a row only if a real team member contributed | To be completed | Describe the member's actual work | Add verified commit IDs | To be signed |

Print a completed, signed contribution table with the report. Do not submit the placeholders as final member information.

## 10. Conclusion

The agent dashboard has its own sidebar-based layout. Other management tools use horizontal navigation and page-specific layouts for the product catalog, market directory, price explorer, watchlist, report form, trend charts, XML console and preferences. Product management accepts optional JPEG, PNG, or GIF photos up to 2 MB with the image path saved in MySQL, and the catalog uses the same image-led card grid as the market directory. Duplicate product and market names return conflict messages. Products linked to price history are archived on removal, while markets referenced by historical reports cannot be deleted; those report relationships remain intact. The markets directory and homepage market cards use locally bundled photos for each displayed seeded market (Gisenyi, Rubavu and Mahoko), with a Kigali market hero photo; authors, sources and licenses are listed in `docs/PRODUCT_IMAGE_CREDITS.md`. AgriPrice Rwanda implements the core public price-information and authenticated agent-reporting workflow with MySQL/JDBC, Servlet JSON endpoints, XML/XSD/XPath/XSLT, client-side MVC, QUnit and Chart.js. Existing database-backed features were verified against the preserved database. A clean disposable-database import, manual DevTools screenshots, failing-first TDD evidence, official roll numbers and signed contribution form remain presentation preparation items rather than verified results.

## Appendix: eight-minute demonstration outline

1. **0:00–1:00:** Open the public homepage and show current reports and product/market filters.
2. **1:00–2:00:** Show a price filter updating JSON results and the 5-second refresh; point out Fetch and XHR.
3. **2:00–3:00:** Sign in with a fictional demo agent. Submit an invalid/outlier value and show the returned error. Use an already duplicated sample key for duplicate rejection; avoid creating a new report during rehearsal unless using a disposable database.
4. **3:00–4:00:** Show the visitor read-only watchlist, sign in as an agent, then demonstrate add, target update, removal and the periodic target alert.
5. **4:00–5:15:** Show trend, comparison and distribution charts with product/date/market controls.
6. **5:15–6:30:** Open XML, run XSD validation and XPath examples, then show grouped and SMS outputs.
7. **6:30–7:15:** In DevTools Network, show request filters and a repeated bulletin request returning 304; capture status, headers and timing.
8. **7:15–8:00:** Run the QUnit page and explain the verified tests and remaining test-first evidence honestly.

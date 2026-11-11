# AgriPrice Rwanda — Agricultural Market Price Information System

**Academic Purpose:** Final Assessment Test (FAT) — Advanced Web & Internet Programming  
**Geographic Focus:** Rubavu and Gisenyi, Western Province, Rwanda  
**Primary Users:** Farmers, agricultural cooperatives, market traders, field enumerators, and the public  

---

## 1. Project Overview & Deliverables

AgriPrice Rwanda is a complete, secure, database-driven agricultural market price information system connecting market centers in Rubavu District and national aggregation terminals in Rwanda.

### Final Deliverables Summary
1. **Complete Dynamic Web Project:** Ready in Eclipse workspace at `C:\Users\delph\AgriPrice` (`C:\Users\delph\eclipse-workspace\AgriPriceRwanda`).
2. **Deployable WAR File:** [`AgriPriceRwanda.war`](file:///C:/Users/delph/AgriPrice/AgriPriceRwanda.war) (2.8 MB, pre-packaged with compiled classes, WebContent, and libraries).
3. **Zipped Eclipse Project:** [`AgriPriceRwanda.zip`](file:///C:/Users/delph/AgriPrice/AgriPriceRwanda.zip).
4. **Database Scripts:** [`database/schema.sql`](file:///C:/Users/delph/AgriPrice/database/schema.sql) and [`database/seed.sql`](file:///C:/Users/delph/AgriPrice/database/seed.sql).
5. **Architectural & ER Diagrams:** [`docs/architecture.svg`](file:///C:/Users/delph/AgriPrice/docs/architecture.svg) and [`docs/er-diagram.svg`](file:///C:/Users/delph/AgriPrice/docs/er-diagram.svg).
6. **Project Report:** [`docs/PROJECT_REPORT.md`](file:///C:/Users/delph/AgriPrice/docs/PROJECT_REPORT.md) & [`docs/PROJECT_REPORT.html`](file:///C:/Users/delph/AgriPrice/docs/PROJECT_REPORT.html) (formatted for direct PDF export).
7. **Automated Test Suite:** [`WebContent/js/tests/index.html`](file:///C:/Users/delph/AgriPrice/WebContent/js/tests/index.html) (12 QUnit tests across 2 modules).
8. **XML/XSD/XSLT Subsystem:** [`WebContent/xml/prices.xsd`](file:///C:/Users/delph/AgriPrice/WebContent/xml/prices.xsd) & [`WebContent/xml/prices.xsl`](file:///C:/Users/delph/AgriPrice/WebContent/xml/prices.xsl).
9. **Git History:** [`docs/git-history.txt`](file:///C:/Users/delph/AgriPrice/docs/git-history.txt).

---

## 2. Technology Stack & Software Versions

- **JDK:** OpenJDK 21 LTS (Temurin-21.0.12)
- **Servlet Specification:** Java Servlet 4.0 (`javax.servlet`)
- **Application Server:** Apache Tomcat 9.0.123
- **Database Engine:** MySQL Community Server 8.0
- **JDBC Driver:** MySQL Connector/J 8.4.0 (`mysql-connector-j.jar`)
- **JSON Serialization:** Google Gson 2.14.0 (`gson.jar`)
- **Frontend:** HTML5, CSS3, Vanilla JavaScript (ES6+), Fetch API, XMLHttpRequest
- **Client-Side Architecture:** Client-Side MVC (Model, View, Controller)
- **Data Visualizations:** Chart.js 4.x
- **Testing:** QUnit 2.20

---

## 3. Database Setup & Dedicated User Setup

### 3.1 Import Commands
Execute the following commands in PowerShell or Command Prompt to initialize the database:

```powershell
# 1. Recreate database, tables, constraints, and dedicated user
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "source C:/Users/delph/AgriPrice/database/schema.sql"

# 2. Populate demonstration seed data (4 agents, 4 markets, 6 commodities, 80 price reports across 16 days)
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "source C:/Users/delph/AgriPrice/database/seed.sql"
```

### 3.2 Dedicated Application User Credentials
Following the Principle of Least Privilege:
- **Username:** `agriprice_user`
- **Password:** `AgriPrice@2026`
- **Host:** `localhost`
- **Database:** `agriprice`
- **Permissions:** `SELECT, INSERT, UPDATE, DELETE` on `agriprice.*`

---

## 4. Demonstration Agent Accounts

All passwords are encrypted with **PBKDF2WithHmacSHA256** (65,536 iterations with distinct random 16-byte cryptographic salts).

| Agent ID | Full Name | Email Address | Phone Number | Status | Default Password |
|---|---|---|---|---|---|
| 1 | Delphin Mugisha | `delphin@agriprice.rw` | `+250788123456` | ACTIVE | `Agent@123456` |
| 2 | Jean Claude Bizimana | `bizimana@agriprice.rw` | `+250788234567` | ACTIVE | `Agent@123456` |
| 3 | Aline Uwase | `uwase@agriprice.rw` | `+250788345678` | ACTIVE | `Agent@123456` |
| 4 | Eric Habimana | `habimana@agriprice.rw` | `+250788456789` | ACTIVE | `Agent@123456` |

---

## 5. Deployment on Apache Tomcat 9

### Option A: Eclipse IDE (Recommended for FAT Demonstration)
1. Open Eclipse IDE for Enterprise Java Developers.
2. Select File $\rightarrow$ Open Projects from File System $\rightarrow$ browse to `C:\Users\delph\AgriPrice`.
3. In the "Servers" view, right-click **Tomcat v9.0 Server at localhost** $\rightarrow$ Add and Remove... $\rightarrow$ add **AgriPriceRwanda**.
4. Click **Start Server** (or `Ctrl+Alt+R`).
5. Open browser at: `http://localhost:8080/AgriPriceRwanda/`

### Option B: Deploy Pre-built WAR File
1. Copy `C:\Users\delph\AgriPrice\AgriPriceRwanda.war` into your Tomcat `webapps/` folder:
   `C:\Users\delph\OneDrive\Desktop\web\apache-tomcat-9.0.123 - Copy\apache-tomcat-9.0.123 - Copy\apache-tomcat-9.0.123\webapps\`
2. Start Tomcat using `bin\startup.bat`.
3. Access the application at: `http://localhost:8080/AgriPriceRwanda/`

---

## 6. Complete API Endpoint Directory

| Method | Endpoint Pattern | Description | Access Role |
|---|---|---|---|
| `GET` | `/api/prices` | Filter prices by `productId`, `marketId`, `date`, `search` | Public |
| `POST` | `/api/prices` | Submit daily price (enforces outlier check &amp; duplicate check) | Authenticated Agent |
| `POST` | `/api/prices` (`action=update`) | Update permitted price report with audit log | Authenticated Agent |
| `POST` | `/api/prices` (`action=delete`) | Delete permitted price report with audit log | Authenticated Agent |
| `GET` | `/api/prices/agent` | List reports submitted by authenticated agent | Authenticated Agent |
| `GET` | `/api/prices/sql-injection-demo` | Verifies PreparedStatement defense against injection payloads | Public / Demonstration |
| `GET` | `/api/markets` | Retrieve all physical markets | Public |
| `POST` | `/api/markets` | Create, update, or delete market centers | Authenticated Agent |
| `GET` | `/api/products` | Retrieve all agricultural commodities | Public |
| `POST` | `/api/products` | Create, update, or delete agricultural products | Authenticated Agent |
| `GET` | `/api/watchlist` | Get watched commodities with targetReached alerts | Public (Session-based) |
| `POST` | `/api/watchlist` | Add item, update target buy price, or remove item | Public (Session-based) |
| `GET` | `/api/dashboard` | Return aggregate metrics, latest reports &amp; distribution | Public / Agent |
| `GET` | `/api/trends/history` | Return 14-day trailing prices for Line Chart | Public |
| `GET` | `/api/trends/comparison` | Return cross-market prices on a date for Bar Chart | Public |
| `GET` | `/api/trends/distribution` | Return market reporting distribution for Pie Chart | Public |
| `GET` | `/export/prices` | Export daily XML bulletin (ETag / Last-Modified 304 Caching) | Public |
| `GET` | `/export/prices?format=html` | XSLT transformed daily price bulletin in HTML | Public |
| `GET` | `/export/prices?format=grouped`| XSLT transformed prices regrouped by market | Public |
| `GET` | `/export/prices?format=sms` | XSLT transformed SMS plain-text bulletin | Public |
| `POST` | `/api/auth/login` | Authenticate agent via PBKDF2 hash comparison | Public |
| `POST` | `/api/auth/logout` | Invalidate HttpSession | Authenticated Agent |
| `GET` | `/api/auth/status` | Verify current session authentication state | Public |

---

## 7. Business Rules & Outlier Detection Logic

1. **Price Validation:** Price must strictly exceed 0 RWF (`chk_price_positive` in MySQL and `InvalidPriceException` in Java).
2. **Duplicate Prevention:** Only one submission per market, product, and day (`uq_market_product_date` in MySQL and duplicate verification in Java).
3. **7-Day Trailing Outlier Calculation:**
   $$\text{deviation} = \frac{|\text{newPrice} - \text{averagePrice}|}{\text{averagePrice}} \times 100$$
   If deviation exceeds `outlier.threshold` (25.0% in `web.xml`), `PriceDAO` throws a `PriceOutlierException` and rejects the submission.
4. **Audit Logging:** Every price update and deletion is committed within the same database transaction (`conn.setAutoCommit(false)` and `conn.rollback()`).

---

## 8. Eight-Minute Live Demonstration Guide

| Time | Target Feature | Demonstration Action |
|---|---|---|
| **00:00 - 01:15** | Architecture & DB | Point out Tomcat 9, MySQL Community 8.0, least-privilege `agriprice_user`, and the 6 relational tables. |
| **01:15 - 02:45** | Public Price Search | Demonstrate live AJAX filtering by commodity and market on `prices.html`. Click the "Quick XHR Market Snapshot" button to verify the examination's XMLHttpRequest requirement. |
| **02:45 - 04:00** | Client-Side MVC Watchlist | Add Kinigi Potatoes on `watchlist.html` at target price 450 RWF. Show dynamic alert banner trigger when market price meets target. Explain Model/View/Controller separation. |
| **04:00 - 05:30** | Agent Dashboard & Outliers | Log in as `delphin@agriprice.rw`. Submit an outlier price (e.g. Maize at 1,500 RWF vs 500 RWF average). Show automated rejection via `PriceOutlierException`. Submit valid price and show audit log record. |
| **05:30 - 06:45** | XML, XSD, XPath & XSLT | Open `/export/prices`. Show raw XML bulletin and schema validation. Open `/export/prices?format=html` and `?format=sms`. Demonstrate the 5 XPath queries. |
| **06:45 - 07:30** | HTTP DevTools & Caching | Open Network tab in DevTools. Refresh `/export/prices`. Demonstrate **HTTP 304 Not Modified** triggered by ETag and Last-Modified headers. Show cookies for preferred market and language. |
| **07:30 - 08:00** | Automated QUnit Suite | Open `WebContent/js/tests/index.html`. Show all 12 unit tests passing across Module 1 and Module 2. Conclude. |

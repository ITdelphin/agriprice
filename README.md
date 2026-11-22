# AgriPrice Rwanda â€” Agricultural Market Price Information System

**Academic Purpose:** Final Assessment Test (FAT) â€” Advanced Web & Internet Programming  
**Geographic Focus:** Rubavu and Gisenyi, Western Province, Rwanda  
**Primary Users:** Farmers, agricultural cooperatives, market traders, field enumerators, and the public  

---

## 1. Project Overview & Deliverables

AgriPrice Rwanda is a complete, secure, database-driven agricultural market price information system connecting market centers in Rubavu District and national aggregation terminals in Rwanda.

### Deliverables

1. Eclipse Dynamic Web Project and deployable [`AgriPriceRwanda.war`](AgriPriceRwanda.war).
2. Zipped project source [`AgriPriceRwanda.zip`](AgriPriceRwanda.zip), including `WEB-INF/lib`.
3. Database rebuild scripts: [schema.sql](database/schema.sql) and [seed.sql](database/seed.sql).
4. System and ER diagrams: [architecture.svg](docs/architecture.svg) and [er-diagram.svg](docs/er-diagram.svg).
5. Report files: [Markdown source](docs/PROJECT_REPORT.md), [HTML print layout](docs/PROJECT_REPORT.html), and [PDF](docs/PROJECT_REPORT.pdf).
6. Browser test runner: [QUnit suite](WebContent/js/tests/index.html); screenshots and HTTP verification notes are in `docs/`.
7. XML artifacts: [prices.xsd](WebContent/xml/prices.xsd) and [prices.xsl](WebContent/xml/prices.xsl).
8. Repository history: [git-history.txt](docs/git-history.txt).

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

### Application Screens

- Public homepage: `index.html` with live market summaries, product search, recent prices, featured markets, and trend previews.
- Agent workspace: `dashboard.html`, `submit-price.html`, `products.html`, `markets.html`, `prices.html`, `watchlist.html`, `trends.html`, `export.html`, and `settings.html`.
- The agent dashboard has its own dark-green workspace sidebar, responsive KPI layout, activity feed, and Chart.js panels. Other pages retain the public navigation.
- Management tools no longer reuse the dashboard shell: product catalog, market directory, price explorer, watchlist, submission form, trend charts, XML console and preferences each have their own page layout with a compact horizontal navigation.
- Agents can attach an optional JPEG, PNG, or GIF product photo (maximum 2 MB); the relative image path is saved with the product in MySQL. Products without a matching photograph use a neutral placeholder.
- The product catalog has a dedicated catalog layout, responsive image-led product card grid, client-side category/status filters, paging, and signed-in CRUD controls. It uses optimized local crop photographs; image sources and license details are in [PRODUCT_IMAGE_CREDITS.md](docs/PRODUCT_IMAGE_CREDITS.md).
- The watchlist is read-only for public visitors. Signed-in market agents can add, update, and delete targets for the watchlist associated with their browser; the servlet rejects all public write requests.
- Duplicate product and market names return clear conflict messages. A product with historical price reports is archived as inactive when removed, and markets linked to reports cannot be deleted; existing report history is preserved.
- The markets directory and homepage market cards use locally bundled town and market photographs for each displayed seeded market (Gisenyi, Rubavu and Mahoko); the directory hero uses a Kigali produce-market photo. Authors, sources and licenses are listed in [PRODUCT_IMAGE_CREDITS.md](docs/PRODUCT_IMAGE_CREDITS.md).
- The daily bulletin defaults to the latest report date and opens as formatted HTML at `/export/prices`; raw XML is available at `/export/prices?format=xml`. XPath examples run at `/export/prices?format=xpath`.
- Market agents sign in with the seeded demonstration accounts. Successful login rotates the session and stores the agent identity; self-service registration and account approval are outside the scenario.

---

## 3. Database Setup & Dedicated User Setup

### 3.1 Rebuild and seed a clean database

**Warning:** `schema.sql` intentionally drops and recreates `agriprice`. Run it only against a clean/disposable assessment database. From the project root, open a MySQL client as an administrator and execute:

```powershell
SOURCE database/schema.sql;
SOURCE database/seed.sql;
```

For an existing installation, apply `database/upgrade_product_images.sql` before deploying the image-enabled product editor. It adds only a nullable product image path and preserves existing rows.

The scripts create seven related tables and seed four agent accounts, three displayed markets, six products and seeded price reports over 16 dates. Agent passwords are stored as PBKDF2 hashes with per-agent salts. All `.test` emails and phone placeholders are fictional.

### 3.2 Dedicated Application User Credentials
Following the Principle of Least Privilege, database settings are supplied at runtime through environment variables:

- `AGRIPRICE_DB_URL` — e.g. `jdbc:mysql://localhost:3306/agriprice?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8`
- `AGRIPRICE_DB_USERNAME` — e.g. `agriprice_user`
- `AGRIPRICE_DB_PASSWORD` — set locally; never committed

Database user permissions: `SELECT, INSERT, UPDATE, DELETE` on `agriprice.*`.

### 3.3 Reset Email Settings
Password reset links are delivered over SMTP SSL. Configure these runtime variables:

- `AGRIPRICE_SMTP_HOST` — defaults to `smtp.gmail.com`
- `AGRIPRICE_SMTP_PORT` — defaults to `465`
- `AGRIPRICE_SMTP_USER` — mailbox used to authenticate
- `AGRIPRICE_SMTP_PASSWORD` — mailbox app password; never committed
- `AGRIPRICE_SMTP_FROM` — optional sender address; defaults to `AGRIPRICE_SMTP_USER`
- `AGRIPRICE_PUBLIC_BASE_URL` — e.g. `https://agripricerwanda.vercel.app`
- `AGRIPRICE_RESET_PREVIEW` — development-only `true` value that returns the link in the API response; never enable in production

---

## 4. Agent Accounts

Agent credentials are seeded in `database/seed.sql` and managed by the project team. Passwords are stored with **PBKDF2WithHmacSHA256** (65,536 iterations with distinct random 16-byte cryptographic salts).

Sign in with an agent account to submit and manage price reports.

### Password recovery
1. On the sign-in page, select **Forgot password?** and enter the registered agent email.
2. The server emails a single-use verification link valid for 15 minutes; the token is never returned in the API response.
3. Open the emailed link, choose a new password of at least 8 characters and confirm it.
4. Sign in again with the same email and the new password. Expired or used links are rejected.

For existing installations, apply `database/upgrade_password_reset.sql` before using this flow.

---

## 5. Deployment on Apache Tomcat 9

### Option A: Eclipse IDE (Recommended for FAT Demonstration)
1. Open Eclipse IDE for Enterprise Java Developers.
2. Select **File â†’ Import â†’ General â†’ Existing Projects into Workspace**, then choose the extracted project folder.
3. In the "Servers" view, right-click **Tomcat v9.0 Server at localhost** $\rightarrow$ Add and Remove... $\rightarrow$ add **AgriPriceRwanda**.
4. Click **Start Server** (or `Ctrl+Alt+R`).
5. Open browser at: `http://localhost:8080/AgriPriceRwanda/`

### Option B: Deploy Pre-built WAR File
1. Copy `AgriPriceRwanda.war` into your Tomcat 9 `webapps/` folder.
2. Start Tomcat using `bin\startup.bat`.
3. Access the application at: `http://localhost:8080/AgriPriceRwanda/`

### Option C: Static Hosted Demo on Vercel
The `public/` directory contains the read-only static build served at `https://agripricerwanda.vercel.app`. Data is a snapshot in `public/data/`; sign-in and write operations are disabled in this hosted demo.

1. Connect the GitHub repository to Vercel.
2. Keep `vercel.json` at the repository root; it sets `public/` as the output directory.
3. Deploy with `vercel --prod` or through the Vercel dashboard.

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
| `GET` | `/api/watchlist` | View the browser-cookie watchlist and targetReached alerts | Public (Read-only) |
| `POST` | `/api/watchlist` | Add item, update target price, or delete item | Authenticated Market Agent |
| `GET` | `/api/dashboard` | Return aggregate metrics, latest reports &amp; distribution | Public / Agent |
| `GET` | `/api/trends/history` | Return 14-day trailing prices for Line Chart | Public |
| `GET` | `/api/trends/comparison` | Return cross-market prices on a date for Bar Chart | Public |
| `GET` | `/api/trends/distribution` | Return market reporting distribution for Pie Chart | Public |
| `GET` | `/export/prices` | View formatted daily price bulletin (HTML; ETag / Last-Modified caching) | Public |
| `GET` | `/export/prices?format=xml` | Export raw daily bulletin XML | Public |
| `GET` | `/export/prices?format=html` | XSLT transformed daily price bulletin in HTML | Public |
| `GET` | `/export/prices?format=grouped`| XSLT transformed prices regrouped by market | Public |
| `GET` | `/export/prices?format=sms` | XSLT transformed SMS plain-text bulletin | Public |
| `GET` | `/export/prices?format=xpath` | Run and display five XPath expressions on the generated bulletin | Public |
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
| **02:45 - 04:00** | Client-Side MVC Watchlist | Show the visitor read-only view, then sign in as an agent to add, update and delete a target on `watchlist.html`. Show the target alert and explain Model/View/Controller separation. |
| **04:00 - 05:30** | Agent Dashboard & Outliers | Log in with a seeded agent account. Submit an outlier price (e.g. Maize at 1,500 RWF vs 500 RWF average). Show automated rejection via `PriceOutlierException`. Submit valid price and show audit log record. |
| **05:30 - 06:45** | XML, XSD, XPath & XSLT | Open `/export/prices` for the formatted bulletin and `/export/prices?format=xml` for raw XML. Show schema validation and the SMS format. Demonstrate the 5 XPath queries. |
| **06:45 - 07:30** | HTTP DevTools & Caching | Open Network tab in DevTools. Refresh `/export/prices`. Demonstrate **HTTP 304 Not Modified** triggered by ETag and Last-Modified headers. Show cookies for preferred market and language. |
| **07:30 - 08:00** | Automated QUnit Suite | Open `WebContent/js/tests/index.html`. Show all 12 unit tests passing across Module 1 and Module 2. Conclude. |

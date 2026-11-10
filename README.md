# AgriPrice Rwanda - Agricultural Market Price Information System

**Academic Assessment:** Final Assessment Test (FAT) — Advanced Web & Internet Programming  
**Focus Region:** Rubavu & Gisenyi, Western Province, Rwanda  
**Primary Target Audience:** Farmers, agricultural traders, enumerators, and the public  

---

## 1. System Architecture & Environment Overview

- **Operating System:** Windows 11
- **JDK:** OpenJDK 21 LTS (Adoptium / Eclipse JustJ Temurin-21.0.12)
- **Application Server:** Apache Tomcat 9.0.123
- **Database:** MySQL Community Server 8.0 (`agriprice`)
- **IDE:** Eclipse IDE for Enterprise Java and Web Developers
- **Backend Architecture:** Java Servlets (`javax.servlet`), Pure JDBC, PreparedStatements, DAO Pattern
- **Frontend:** HTML5, CSS3, Vanilla JavaScript (ES6+), Fetch API, XMLHttpRequest, Client-Side MVC, Chart.js, QUnit
- **Data Interchange & Transformation:** JSON, XML, XSD, XPath, XSLT

---

## 2. Database Setup & Dedicated Application User

### 2.1 MySQL Administrator Import Commands
To recreate the schema and populate the demonstration data, execute the following commands in PowerShell or Command Prompt:

```powershell
# Import Schema (Recreates database, constraints, tables, and dedicated user)
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "source C:/Users/delph/AgriPrice/database/schema.sql"

# Import Demonstration Seed Data (Over 80 price reports across 16 days, 4 agents, 4 markets, 6 commodities)
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "source C:/Users/delph/AgriPrice/database/seed.sql"
```

### 2.2 Dedicated Application User Credentials
Following the Principle of Least Privilege, the web application connects using an unprivileged MySQL account:
- **Username:** `agriprice_user`
- **Password:** `AgriPrice@2026`
- **Host:** `localhost`
- **Database:** `agriprice`
- **Granted Permissions:** `SELECT, INSERT, UPDATE, DELETE` on `agriprice.*`

---

## 3. Demonstration Agent Accounts

All agent passwords are encrypted using **PBKDF2WithHmacSHA256** (65,536 iterations with distinct 16-byte random cryptographic salts).

| Agent ID | Full Name | Email Address | Phone Number | Status | Default Password |
|---|---|---|---|---|---|
| 1 | Delphin Mugisha | `delphin@agriprice.rw` | `+250788123456` | ACTIVE | `Agent@123456` |
| 2 | Jean Claude Bizimana | `bizimana@agriprice.rw` | `+250788234567` | ACTIVE | `Agent@123456` |
| 3 | Aline Uwase | `uwase@agriprice.rw` | `+250788345678` | ACTIVE | `Agent@123456` |
| 4 | Eric Habimana | `habimana@agriprice.rw` | `+250788456789` | ACTIVE | `Agent@123456` |

---

## 4. Project Directory Layout

```
AgriPriceRwanda/ (C:\Users\delph\AgriPrice)
│
├── src/
│   └── com/agriprice/
│       ├── config/
│       │   └── DatabaseConnection.java
│       ├── model/
│       │   ├── Agent.java
│       │   ├── Market.java
│       │   ├── Product.java
│       │   ├── PriceReport.java
│       │   ├── Watchlist.java
│       │   └── AuditLog.java
│       ├── dao/
│       │   ├── AgentDAO.java
│       │   ├── MarketDAO.java
│       │   ├── ProductDAO.java
│       │   ├── PriceDAO.java
│       │   ├── WatchlistDAO.java
│       │   └── AuditLogDAO.java
│       ├── servlet/
│       │   ├── AuthServlet.java
│       │   ├── PriceServlet.java
│       │   ├── MarketServlet.java
│       │   ├── ProductServlet.java
│       │   ├── WatchlistServlet.java
│       │   ├── DashboardServlet.java
│       │   ├── TrendServlet.java
│       │   └── XMLExportServlet.java
│       └── util/
│           ├── PasswordUtil.java
│           ├── ValidationUtil.java
│           ├── InvalidPriceException.java
│           ├── PriceOutlierException.java
│           └── UnknownProductException.java
│
├── WebContent/
│   ├── index.html
│   ├── login.html
│   ├── dashboard.html
│   ├── prices.html
│   ├── watchlist.html
│   ├── products.html
│   ├── markets.html
│   ├── css/style.css
│   ├── js/
│   │   ├── app.js
│   │   ├── api.js
│   │   ├── charts.js
│   │   ├── watchlist-model.js
│   │   ├── watchlist-view.js
│   │   ├── watchlist-controller.js
│   │   └── tests/
│   │       ├── index.html
│   │       └── agriprice-tests.js
│   ├── xml/
│   │   ├── prices.xsd
│   │   └── prices.xsl
│   ├── assets/images/
│   └── WEB-INF/
│       ├── web.xml
│       └── lib/
│           ├── mysql-connector-j.jar
│           └── gson.jar
│
├── database/
│   ├── schema.sql
│   └── seed.sql
│
├── docs/
│   ├── architecture.png
│   └── er-diagram.png
│
├── README.md
└── .gitignore
```

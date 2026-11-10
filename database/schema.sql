-- =============================================================================
-- AgriPrice Rwanda - Database Schema Definition
-- Academic Purpose: Final Assessment Test (FAT) - Advanced Web & Internet Programming
-- Database Management System: MySQL 8.x
-- Character Set: UTF-8 (utf8mb4)
-- Location Focus: Rubavu & Gisenyi, Rwanda
-- =============================================================================

-- 1. Recreate the Database Safely
DROP DATABASE IF EXISTS agriprice;
CREATE DATABASE agriprice 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE agriprice;

-- 2. Dedicated Application User Setup
-- Principle of Least Privilege: grants only CRUD operations to the app user
CREATE USER IF NOT EXISTS 'agriprice_user'@'localhost' IDENTIFIED BY 'AgriPrice@2026';
GRANT SELECT, INSERT, UPDATE, DELETE ON agriprice.* TO 'agriprice_user'@'localhost';
FLUSH PRIVILEGES;

-- =============================================================================
-- 3. Table Definitions
-- =============================================================================

-- Table 3.1: agents (Market Enumerators & Data Collectors)
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS watchlist;
DROP TABLE IF EXISTS price_reports;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS markets;
DROP TABLE IF EXISTS agents;

CREATE TABLE agents (
    agent_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_agent_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table 3.2: markets (Physical Agricultural Markets in Rwanda)
CREATE TABLE markets (
    market_id INT AUTO_INCREMENT PRIMARY KEY,
    market_name VARCHAR(100) NOT NULL UNIQUE,
    district VARCHAR(50) NOT NULL,
    province VARCHAR(50) NOT NULL,
    location_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table 3.3: products (Agricultural Commodities)
CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,
    default_unit VARCHAR(20) NOT NULL DEFAULT 'kg',
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_product_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table 3.4: price_reports (Daily Agricultural Price Submissions)
CREATE TABLE price_reports (
    report_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    market_id INT NOT NULL,
    agent_id INT NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'kg',
    price_rwf DECIMAL(10,2) NOT NULL,
    report_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_price_positive CHECK (price_rwf > 0),
    CONSTRAINT uq_market_product_date UNIQUE (market_id, product_id, report_date),
    
    -- Foreign Keys
    CONSTRAINT fk_price_product FOREIGN KEY (product_id) 
        REFERENCES products(product_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_price_market FOREIGN KEY (market_id) 
        REFERENCES markets(market_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_price_agent FOREIGN KEY (agent_id) 
        REFERENCES agents(agent_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Performance Indexes for fast searching and chart aggregation
CREATE INDEX idx_price_report_date ON price_reports(report_date);
CREATE INDEX idx_price_product_id ON price_reports(product_id);
CREATE INDEX idx_price_market_id ON price_reports(market_id);
CREATE INDEX idx_price_composite ON price_reports(product_id, market_id, report_date);

-- Table 3.5: audit_log (Audit Trail for Regulatory & Assessment Compliance)
CREATE TABLE audit_log (
    audit_id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id INT NULL,
    report_id INT NULL,
    action VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_audit_agent FOREIGN KEY (agent_id) 
        REFERENCES agents(agent_id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_audit_report FOREIGN KEY (report_id) 
        REFERENCES price_reports(report_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_action_date ON audit_log(action_date);
CREATE INDEX idx_audit_agent_id ON audit_log(agent_id);

-- Table 3.6: watchlist (Visitor Watchlist with Target Price Alerts)
CREATE TABLE watchlist (
    watchlist_id INT AUTO_INCREMENT PRIMARY KEY,
    visitor_session_id VARCHAR(100) NOT NULL,
    product_id INT NOT NULL,
    market_id INT NULL,
    target_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_target_price_positive CHECK (target_price > 0),
    CONSTRAINT uq_visitor_product_market UNIQUE (visitor_session_id, product_id, market_id),
    
    -- Foreign Keys
    CONSTRAINT fk_watchlist_product FOREIGN KEY (product_id) 
        REFERENCES products(product_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_watchlist_market FOREIGN KEY (market_id) 
        REFERENCES markets(market_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_watchlist_session ON watchlist(visitor_session_id);

<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- Parameter controlling which of the 3 transformations to render -->
  <xsl:param name="viewMode" select="'html'"/> <!-- 'html', 'grouped', 'sms' -->

  <xsl:output method="html" encoding="UTF-8" indent="yes"/>

  <!-- Root Template -->
  <xsl:template match="/marketBulletin">
    <xsl:choose>
      <!-- Transformation 3: SMS-Style Plain Text Bulletin (One line per commodity) -->
      <xsl:when test="$viewMode = 'sms'">
        <xsl:text>AGRIPRICE RWANDA DAILY BULLETIN (</xsl:text>
        <xsl:value-of select="@bulletinDate"/>
        <xsl:text>)&#10;</xsl:text>
        <xsl:for-each select="reports/priceReport">
          <xsl:value-of select="product/productName"/>
          <xsl:text>: </xsl:text>
          <xsl:value-of select="price"/>
          <xsl:text> RWF/</xsl:text>
          <xsl:value-of select="price/@unit"/>
          <xsl:text> @ </xsl:text>
          <xsl:value-of select="market/marketName"/>
          <xsl:text>&#10;</xsl:text>
        </xsl:for-each>
        <xsl:text>SMS info: dial *812# AgriPrice Rubavu</xsl:text>
      </xsl:when>

      <!-- Transformation 2: Prices Regrouped by Market -->
      <xsl:when test="$viewMode = 'grouped'">
        <html>
          <head>
            <title>AgriPrice Rwanda - Prices Regrouped by Market</title>
            <style>
              body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f4fbf7; margin: 30px; color: #1e3a29; }
              .header { background: #1b5e20; color: white; padding: 20px; border-radius: 8px; margin-bottom: 25px; }
              .market-card { background: white; border: 1px solid #c8e6c9; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
              .market-title { background: #2e7d32; color: white; margin: 0; padding: 12px 20px; border-radius: 8px 8px 0 0; }
              table { width: 100%; border-collapse: collapse; }
              th, td { padding: 10px 20px; text-align: left; border-bottom: 1px solid #e8f5e9; }
              th { background: #e8f5e9; color: #1b5e20; font-weight: 600; }
              .price-cell { font-weight: bold; color: #2e7d32; }
            </style>
          </head>
          <body>
            <div class="header">
              <h1>AgriPrice Rwanda — Market Grouped Price Bulletin</h1>
              <p>Bulletin Date: <xsl:value-of select="@bulletinDate"/> | Focus Area: Rubavu &amp; Gisenyi, Rwanda</p>
            </div>

            <!-- Grouping by Market Name using Muenchian style / distinct market enumeration -->
            <xsl:for-each select="reports/priceReport[generate-id() = generate-id(key('markets-by-name', market/marketName)[1])]">
              <xsl:variable name="currentMarket" select="market/marketName"/>
              <div class="market-card">
                <h3 class="market-title">Market: <xsl:value-of select="$currentMarket"/> (<xsl:value-of select="market/district"/>)</h3>
                <table>
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th>Category</th>
                      <th>Price (RWF)</th>
                      <th>Unit</th>
                      <th>Agent</th>
                    </tr>
                  </thead>
                  <tbody>
                    <xsl:for-each select="/marketBulletin/reports/priceReport[market/marketName = $currentMarket]">
                      <tr>
                        <td><strong><xsl:value-of select="product/productName"/></strong></td>
                        <td><xsl:value-of select="product/category"/></td>
                        <td class="price-cell"><xsl:value-of select="price"/> RWF</td>
                        <td><xsl:value-of select="price/@unit"/></td>
                        <td><xsl:value-of select="agent"/></td>
                      </tr>
                    </xsl:for-each>
                  </tbody>
                </table>
              </div>
            </xsl:for-each>
          </body>
        </html>
      </xsl:when>

      <!-- Transformation 1 (Default): Daily Price Bulletin HTML Document -->
      <xsl:otherwise>
        <html>
          <head>
            <title>AgriPrice Rwanda - Official Daily Market Bulletin</title>
            <style>
              body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #fafdfb; color: #212529; margin: 40px; }
              .bulletin-box { max-width: 960px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border-top: 6px solid #2e7d32; }
              .meta-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin: 20px 0; background: #e8f5e9; padding: 15px; border-radius: 6px; }
              .meta-item { font-size: 0.9em; }
              .meta-label { font-weight: bold; color: #1b5e20; display: block; font-size: 0.8em; text-transform: uppercase; }
              table { width: 100%; border-collapse: collapse; margin-top: 15px; }
              th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #e0e0e0; }
              th { background: #2e7d32; color: white; font-weight: 600; }
              tr:hover { background-color: #f1f8e9; }
              .price-val { font-weight: bold; color: #1b5e20; }
              .badge { background: #e8f5e9; color: #2e7d32; padding: 4px 8px; border-radius: 4px; font-size: 0.85em; }
              .footer { margin-top: 25px; font-size: 0.85em; color: #666; text-align: center; border-top: 1px solid #eee; padding-top: 15px; }
            </style>
          </head>
          <body>
            <div class="bulletin-box">
              <h2>AgriPrice Rwanda — Official Daily Agricultural Price Bulletin</h2>
              <div class="meta-grid">
                <div class="meta-item">
                  <span class="meta-label">Bulletin Date</span>
                  <xsl:value-of select="@bulletinDate"/>
                </div>
                <div class="meta-item">
                  <span class="meta-label">Publisher</span>
                  <xsl:value-of select="metadata/publisher"/>
                </div>
                <div class="meta-item">
                  <span class="meta-label">Region</span>
                  <xsl:value-of select="metadata/region"/>
                </div>
                <div class="meta-item">
                  <span class="meta-label">Total Reports</span>
                  <xsl:value-of select="metadata/totalReports"/>
                </div>
              </div>

              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Product</th>
                    <th>Category</th>
                    <th>Market</th>
                    <th>District</th>
                    <th>Price</th>
                    <th>Agent</th>
                  </tr>
                </thead>
                <tbody>
                  <xsl:for-each select="reports/priceReport">
                    <tr>
                      <td><xsl:value-of select="@id"/></td>
                      <td><strong><xsl:value-of select="product/productName"/></strong></td>
                      <td><span class="badge"><xsl:value-of select="product/category"/></span></td>
                      <td><xsl:value-of select="market/marketName"/></td>
                      <td><xsl:value-of select="market/district"/></td>
                      <td class="price-val"><xsl:value-of select="price"/> RWF / <xsl:value-of select="price/@unit"/></td>
                      <td><xsl:value-of select="agent"/></td>
                    </tr>
                  </xsl:for-each>
                </tbody>
              </table>

              <div class="footer">
                AgriPrice Rwanda System | Rubavu &amp; Gisenyi Commercial Market Information
              </div>
            </div>
          </body>
        </html>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Key definition for Muenchian grouping by market name -->
  <xsl:key name="markets-by-name" match="/marketBulletin/reports/priceReport" use="market/marketName"/>

</xsl:stylesheet>

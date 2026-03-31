# SEC EDGAR Company Facts API -- Integration Guide

## 1. API Overview

SEC EDGAR provides free, unauthenticated RESTful APIs at `data.sec.gov` for accessing XBRL-structured financial data. No API keys required. All data is JSON-formatted.

---

## 2. Company Tickers Mapping (Ticker to CIK)

**Endpoint**: `https://www.sec.gov/files/company_tickers.json`

**Response Structure**:
```json
{
  "0": {"cik_str": 320193, "ticker": "AAPL", "title": "Apple Inc."},
  "1": {"cik_str": 789019, "ticker": "MSFT", "title": "MICROSOFT CORP"},
  "2": {"cik_str": 1018724, "ticker": "AMZN", "title": "AMAZON COM INC"},
  "3": {"cik_str": 1652044, "ticker": "GOOGL", "title": "Alphabet Inc."}
}
```

**Key Details**:
- Keys are numeric strings ("0", "1", "2", ...), NOT an array
- `cik_str`: integer, NO leading zeros
- `ticker`: uppercase stock symbol
- `title`: company name (mixed case, inconsistent formatting)

**CIK Padding Rule**: All subsequent API calls require CIK padded to 10 digits with leading zeros.
```java
String paddedCik = String.format("CIK%010d", cikNumber);
// cikNumber=320193 -> "CIK0000320193"
```

---

## 3. Company Facts API

**Endpoint**: `https://data.sec.gov/api/xbrl/companyfacts/CIK{paddedCik}.json`

**Example**: `https://data.sec.gov/api/xbrl/companyfacts/CIK0000320193.json` (Apple)

Returns ALL XBRL concepts ever filed by a company in a single response. Response can be very large (several MB for large filers).

### Complete JSON Response Structure

```json
{
  "cik": 320193,
  "entityName": "Apple Inc.",
  "facts": {
    "dei": {
      "EntityCommonStockSharesOutstanding": {
        "label": "Entity Common Stock, Shares Outstanding",
        "description": "Indicate number of shares...",
        "units": {
          "shares": [
            {
              "end": "2023-09-30",
              "val": 15550061000,
              "accn": "0000320193-23-000106",
              "fy": 2023,
              "fp": "FY",
              "form": "10-K",
              "filed": "2023-11-03",
              "frame": "CY2023Q3I"
            }
          ]
        }
      }
    },
    "us-gaap": {
      "Revenues": {
        "label": "Revenues",
        "description": "Amount of revenue recognized from goods sold...",
        "units": {
          "USD": [
            {
              "start": "2022-10-01",
              "end": "2023-09-30",
              "val": 383285000000,
              "accn": "0000320193-23-000106",
              "fy": 2023,
              "fp": "FY",
              "form": "10-K",
              "filed": "2023-11-03",
              "frame": "CY2023"
            },
            {
              "start": "2023-07-02",
              "end": "2023-09-30",
              "val": 89498000000,
              "accn": "0000320193-23-000106",
              "fy": 2023,
              "fp": "Q4",
              "form": "10-K",
              "filed": "2023-11-03",
              "frame": "CY2023Q3"
            }
          ]
        }
      },
      "Assets": {
        "label": "Assets",
        "description": "Sum of the carrying amounts...",
        "units": {
          "USD": [
            {
              "end": "2023-09-30",
              "val": 352583000000,
              "accn": "0000320193-23-000106",
              "fy": 2023,
              "fp": "FY",
              "form": "10-K",
              "filed": "2023-11-03",
              "frame": "CY2023Q3I"
            }
          ]
        }
      },
      "EarningsPerShareBasic": {
        "label": "Earnings Per Share, Basic",
        "description": "The amount of net income...",
        "units": {
          "USD/shares": [
            {
              "start": "2022-10-01",
              "end": "2023-09-30",
              "val": 6.16,
              "accn": "0000320193-23-000106",
              "fy": 2023,
              "fp": "FY",
              "form": "10-K",
              "filed": "2023-11-03"
            }
          ]
        }
      }
    }
  }
}
```

### Unit Entry Fields

| Field | Type | Description |
|-------|------|-------------|
| `start` | String | Period start date (YYYY-MM-DD). Present for duration concepts (revenue, income). Absent for instant concepts (assets, liabilities). |
| `end` | String | Period end date (YYYY-MM-DD). Always present. |
| `val` | Number | The reported value. Monetary values in raw units (NOT thousands/millions). |
| `accn` | String | Accession number of the filing (format: `0000000000-##-######`) |
| `fy` | Integer | Fiscal year (e.g., 2023) |
| `fp` | String | Fiscal period: `"FY"`, `"Q1"`, `"Q2"`, `"Q3"`, `"Q4"` |
| `form` | String | SEC form type: `"10-K"`, `"10-Q"`, `"8-K"`, `"20-F"`, `"40-F"`, `"6-K"` |
| `filed` | String | Date the filing was submitted (YYYY-MM-DD) |
| `frame` | String | Optional. Calendar frame alignment: `"CY2023"` (annual), `"CY2023Q3"` (quarterly duration), `"CY2023Q3I"` (quarterly instant) |

### Duration vs Instant Concepts

- **Duration concepts** (income statement, cash flow): have both `start` and `end`. Represent a period of activity.
- **Instant concepts** (balance sheet): have only `end`. Represent a point-in-time snapshot.

### Unit Types

- `"USD"` -- monetary values in US dollars
- `"shares"` -- share counts
- `"USD/shares"` -- per-share values (EPS)
- `"pure"` -- dimensionless ratios

### Taxonomy Namespaces in `facts`

- `"dei"` -- Document and Entity Information (shares outstanding, fiscal year end, etc.)
- `"us-gaap"` -- US Generally Accepted Accounting Principles (all financial statement data)
- `"ifrs-full"` -- International Financial Reporting Standards (for foreign filers using IFRS)

---

## 4. Company Concept API

**Endpoint**: `https://data.sec.gov/api/xbrl/companyconcept/CIK{paddedCik}/{taxonomy}/{tag}.json`

**Example**: `https://data.sec.gov/api/xbrl/companyconcept/CIK0000320193/us-gaap/Revenues.json`

### Response Structure

```json
{
  "cik": 320193,
  "taxonomy": "us-gaap",
  "tag": "Revenues",
  "label": "Revenues",
  "description": "Amount of revenue recognized...",
  "entityName": "Apple Inc.",
  "units": {
    "USD": [
      {
        "start": "2016-09-25",
        "end": "2017-09-30",
        "val": 229234000000,
        "accn": "0000320193-17-000070",
        "fy": 2017,
        "fp": "FY",
        "form": "10-K",
        "filed": "2017-11-03",
        "frame": "CY2017"
      }
    ]
  }
}
```

### When to Use Company Concept vs Company Facts

| Scenario | Use |
|----------|-----|
| Need 1-3 specific metrics for a company | **Company Concept** -- smaller payload, faster |
| Need comprehensive financial data (10+ metrics) | **Company Facts** -- one call gets everything |
| Building a dashboard with many financial line items | **Company Facts** -- cache the full response |
| Checking a single metric across time | **Company Concept** -- focused data |
| Bulk data for many companies | **Bulk ZIP files** (companyfacts.zip, recompiled nightly) |

---

## 5. XBRL Taxonomy Tag Names (us-gaap)

### Income Statement

| Concept | Primary Tag | Common Alternatives |
|---------|------------|-------------------|
| Revenue | `Revenues` | `RevenueFromContractWithCustomersExcludingAssessedTax`, `SalesRevenueNet`, `RevenueFromContractWithCustomersIncludingAssessedTax` |
| Cost of Revenue | `CostOfGoodsAndServicesSold` | `CostOfGoodsSold`, `CostOfRevenue`, `CostOfSales` |
| Gross Profit | `GrossProfit` | -- |
| Operating Income | `OperatingIncomeLoss` | -- |
| Net Income | `NetIncomeLoss` | `ProfitLoss`, `NetIncomeLossAvailableToCommonStockholdersBasic` |

### Balance Sheet (Instant Concepts -- no `start` field)

| Concept | Primary Tag | Common Alternatives |
|---------|------------|-------------------|
| Total Assets | `Assets` | -- |
| Current Assets | `AssetsCurrent` | -- |
| Total Liabilities | `Liabilities` | -- |
| Current Liabilities | `LiabilitiesCurrent` | -- |
| Stockholders' Equity | `StockholdersEquity` | `StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest` |
| Cash and Equivalents | `CashAndCashEquivalentsAtCarryingValue` | `CashCashEquivalentsAndShortTermInvestments` |

### Cash Flow Statement

| Concept | Primary Tag | Common Alternatives |
|---------|------------|-------------------|
| Operating Cash Flow | `NetCashProvidedByUsedInOperatingActivities` | `NetCashProvidedByUsedInOperatingActivitiesContinuingOperations` |
| Investing Cash Flow | `NetCashProvidedByUsedInInvestingActivities` | `NetCashProvidedByUsedInInvestingActivitiesContinuingOperations` |
| Financing Cash Flow | `NetCashProvidedByUsedInFinancingActivities` | `NetCashProvidedByUsedInFinancingActivitiesContinuingOperations` |
| Capital Expenditures | `PaymentsToAcquirePropertyPlantAndEquipment` | `PaymentsToAcquireProductiveAssets` |

### Per-Share Metrics

| Concept | Primary Tag | Unit Type |
|---------|------------|-----------|
| EPS (Basic) | `EarningsPerShareBasic` | `USD/shares` |
| EPS (Diluted) | `EarningsPerShareDiluted` | `USD/shares` |

### Important: Tag Name Variations

Companies do NOT always use the same tag for the same concept. For example:
- Apple uses `RevenueFromContractWithCustomersExcludingAssessedTax` for revenue
- Microsoft might use `Revenues`
- Some companies use `SalesRevenueNet`

**Recommended approach**: Try multiple tag names in priority order (fallback chain).

```java
// Revenue fallback chain
private static final List<String> REVENUE_TAGS = List.of(
    "Revenues",
    "RevenueFromContractWithCustomersExcludingAssessedTax",
    "RevenueFromContractWithCustomersIncludingAssessedTax",
    "SalesRevenueNet",
    "SalesRevenueGoodsNet"
);
```

---

## 6. Filtering Annual (10-K) vs Quarterly (10-Q)

### Method 1: Filter by `form` Field

```java
// Filter for annual data only
entries.stream()
    .filter(e -> "10-K".equals(e.getForm()))
    .collect(Collectors.toList());

// Filter for quarterly data only
entries.stream()
    .filter(e -> "10-Q".equals(e.getForm()))
    .collect(Collectors.toList());
```

**Form values**: `"10-K"`, `"10-Q"`, `"10-K/A"` (amended), `"10-Q/A"` (amended), `"8-K"`, `"20-F"` (foreign annual), `"40-F"` (Canadian annual), `"6-K"` (foreign interim)

### Method 2: Filter by `fp` (Fiscal Period) Field

```java
// Annual only
entries.stream()
    .filter(e -> "FY".equals(e.getFp()))
    .collect(Collectors.toList());
```

### Method 3: Filter by `frame` Field

```
CY2023      -- Annual duration (calendar year 2023, ~365 days)
CY2023Q1    -- Q1 duration (calendar Q1 2023, ~91 days)
CY2023Q3I   -- Q3 instant (point-in-time at end of Q3 2023)
```

**Calendar Year alignment**: The `frame` field normalizes data to calendar periods regardless of a company's fiscal year. A company with fiscal year ending September 30 will still have its annual data aligned to the nearest CY frame.

### Recommended Filtering Strategy for Annual Data

```java
// Most reliable: combine form + fp
boolean isAnnual = "10-K".equals(entry.getForm()) && "FY".equals(entry.getFp());

// For foreign filers, also check:
boolean isForeignAnnual = "20-F".equals(entry.getForm()) && "FY".equals(entry.getFp());
```

### Handling Duplicates

A single concept may have multiple entries for the same fiscal year (e.g., original filing + amended filing). Always prefer the most recently filed entry:

```java
// Group by fiscal year, keep latest filed date
entries.stream()
    .filter(e -> "10-K".equals(e.getForm()))
    .collect(Collectors.groupingBy(
        Entry::getFy,
        Collectors.collectingAndThen(
            Collectors.maxBy(Comparator.comparing(Entry::getFiled)),
            Optional::orElseThrow
        )
    ));
```

---

## 7. Rate Limiting and Required Headers

### Rate Limit

- **Maximum 10 requests per second** across all IP addresses for the same user/application
- Exceeding the limit results in temporary IP-level blocking
- Block is lifted after request rate drops below threshold for 10 minutes
- No API key system -- enforcement is purely IP-based + User-Agent based

### Required Headers

```
User-Agent: CompanyName admin@company.com
Accept: application/json
```

**User-Agent Format**: `{Application/Company Name} {contact email}`

```java
// Spring RestTemplate example
HttpHeaders headers = new HttpHeaders();
headers.set("User-Agent", "MyStockApp admin@mycompany.com");
headers.setAccept(List.of(MediaType.APPLICATION_JSON));
```

**Requests without a proper User-Agent header will receive 403 Forbidden responses.**

### CORS

SEC EDGAR APIs do NOT support CORS. All requests must be made server-side.

---

## 8. Best Practices for Java/Spring Boot Integration

### Caching

```
1. Cache company_tickers.json -- changes infrequently (refresh daily)
2. Cache Company Facts responses -- filings update at most quarterly
   - Recommended TTL: 24 hours for active monitoring, 7 days for historical analysis
3. Use Caffeine cache (already in project tech stack)
```

### Rate Limiting Implementation

```java
// Use a rate limiter to stay under 10 req/sec
// Recommend: 5-8 req/sec to leave margin
private final RateLimiter rateLimiter = RateLimiter.create(8.0); // Guava

public <T> T fetchWithRateLimit(String url, Class<T> type) {
    rateLimiter.acquire();
    return restTemplate.getForObject(url, type);
}
```

### Error Handling

| HTTP Status | Meaning | Recommended Action |
|-------------|---------|-------------------|
| 200 | Success | Process response |
| 403 | Missing/invalid User-Agent OR rate limited | Check User-Agent header; back off and retry |
| 404 | CIK not found or tag not used by company | Return empty/null; try alternative tags |
| 429 | Rate limited (rare, usually 403) | Exponential backoff, wait 10+ minutes |
| 500/503 | Server error | Retry with backoff (max 3 retries) |

### Handling Missing Data

Not all companies report all tags. A robust integration must handle:

```java
// Tag might not exist in company's facts
JsonNode revenueNode = factsNode
    .path("us-gaap")
    .path("Revenues");

if (revenueNode.isMissingNode()) {
    // Try alternative tag names
    revenueNode = factsNode
        .path("us-gaap")
        .path("RevenueFromContractWithCustomersExcludingAssessedTax");
}
```

### CIK Formatting

```java
public String formatCik(int cik) {
    return String.format("CIK%010d", cik);
}
// 320193 -> "CIK0000320193"
```

### Accession Number Formatting

When constructing URLs to actual filing documents, remove dashes from accession numbers:
```java
String accnForUrl = accessionNumber.replace("-", "");
// "0000320193-23-000106" -> "000032019323000106"
```

---

## 9. Bulk Data Downloads

For large-scale data collection, use bulk ZIP files instead of per-company API calls:

- **companyfacts.zip**: `https://www.sec.gov/Archives/edgar/daily-index/xbrl/companyfacts.zip`
  - Contains ALL Company Facts JSON files. Recompiled nightly.
  - Best for: initial data load, rebuilding cache

- **submissions.zip**: `https://efts.sec.gov/LATEST/search-index/submissions.zip`
  - Contains all company submission metadata

---

## 10. Frame API (Cross-Company Comparison)

**Endpoint**: `https://data.sec.gov/api/xbrl/frames/{taxonomy}/{tag}/{unit}/{period}.json`

**Example**: `https://data.sec.gov/api/xbrl/frames/us-gaap/Revenues/USD/CY2023.json`

Returns the specified concept for ALL companies in a single frame period. Useful for screening and comparison across companies.

**Period formats**:
- `CY2023` -- annual
- `CY2023Q1` -- quarterly duration
- `CY2023Q1I` -- quarterly instant

---

## Sources

- [SEC EDGAR Application Programming Interfaces](https://www.sec.gov/search-filings/edgar-application-programming-interfaces)
- [SEC Accessing EDGAR Data (Fair Access Policy)](https://www.sec.gov/search-filings/edgar-search-assistance/accessing-edgar-data)
- [SEC Rate Control Limits Announcement](https://www.sec.gov/filergroup/announcements-old/new-rate-control-limits)
- [Introduction to Working with SEC EDGAR API (The Full Stack Accountant)](https://www.thefullstackaccountant.com/blog/intro-to-edgar)
- [So You Want to Integrate with the SEC API (GreenFlux)](https://blog.greenflux.us/so-you-want-to-integrate-with-the-sec-api/)
- [XBRL Tag Mappings from 32,000 Filings (EdgarTools)](https://www.edgartools.io/i-learnt-xbrl-mappings-from-32-000-sec-filings/)
- [US GAAP Taxonomy FAQ (FASB)](https://xbrl.fasb.org/resources/taxonomyfaq.pdf)
- [SEC Company Tickers File](https://www.sec.gov/file/company-tickers)
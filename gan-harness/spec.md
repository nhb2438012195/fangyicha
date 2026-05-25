# Product Specification: 房易查 (RealEstateQuery)

> Generated from brief: "房地产客户购房查询软件 - 前后端分离架构，Spring Boot + MyBatis-Plus + MySQL + Vue 3 + Element Plus + JWT认证"

## Vision

房易查 (FangYiCha) is a full-featured real estate inquiry platform bridging property developers and home buyers. Developers manage listings and gain market insight through vacancy correlation analytics; customers discover suitable properties via smart guided search and multi-condition queries, submit preferences, and generate printable reports. The system delivers a clean, professional, responsive web experience accessible from any device on the local network or public internet.

## Design Direction

- **Color palette**: Primary #1a73e8 (trust blue), Secondary #34a853 (growth green for positive indicators), Danger #ea4335 (alert red), Warning #fbbc04 (amber). Background #f5f7fa (light grey-blue), Card surface #ffffff, Text primary #1f2937, Text secondary #6b7280. Sidebar #1e293b (dark slate).
- **Typography**: System font stack: `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif`. Headings weight 600, body weight 400. Monospace for code: `"SF Mono", "Fira Code", "Cascadia Code", monospace`.
- **Layout philosophy**: Three-tier dashboard: fixed sidebar (240px) for role-based navigation, collapsible; main content area with max-width 1400px, centered; action toolbar at top of each data page. Card-based data displays with consistent 16px/24px spacing grid.
- **Visual identity**: Flat, functional UI with subtle shadows (box-shadow: 0 1px 3px rgba(0,0,0,0.08)) on cards. No gradients, no stock illustrations, no decorative blobs. Clean table rows with alternating zebra striping. Consistent 8px border-radius on cards and buttons. Icons use Element Plus built-in icon set only -- no external icon packs.
- **Anti-AI-slop directives**: No gradient backgrounds on cards. No generic "team photo" or building stock imagery. No unnecessary border-radius on non-interactive elements. No shadow overlays on modals beyond standard Element Plus defaults. Every visual element must serve a functional purpose.
- **Inspiration**: Element Plus documentation site (layout clarity), Airbnb dashboard (data density), Bloomberg terminal-style data tables (compact, sortable, filterable), Zillow property cards (image + key stats at a glance).

## Features (prioritized)

### Must-Have (Sprint 1-2)

1. **Role-based Authentication & Authorization**: JWT login for both developer and customer roles. Registration for customers only; developer accounts are seeded. Token stored in localStorage, sent as Bearer token in Axios interceptor. Token refresh on 401 response.
   - Acceptance: Login with role redirects to role-specific dashboard. Invalid credentials show inline error. Token expiry redirects to login with message. Logout clears token and redirects.

2. **Property CRUD (Developer)**: Developer creates, reads, updates, deletes their own properties. Form includes: property name, location (text + map coordinate fields), floor number range (min/max), floor plan type (dropdown: 一室一厅/两室一厅/三室两厅/四室两厅/复式/别墅), total units, vacant units (auto-calculates vacancy rate), price per sqm, total price, area sqm, description, image URL(s), status (在售/已售/待开盘).
   - Acceptance: Create with validation all required fields. Edit pre-populates form. Delete with confirmation dialog. List paginated with search/filter. Only the owning developer sees their properties.

3. **Guided Step Wizard (Customer)**: 4-step wizard with stepper component: Step 1 - Select Location (dropdown or map region selector), Step 2 - Select Property Type (floor plan type checkboxes), Step 3 - Price Range (dual slider for min-max), Step 4 - Results Preview (matching properties in card grid). Back/Next navigation with state persistence. Skip optional steps.
   - Acceptance: Each step validates before advancing. Results update in real-time when step changes. Empty state if no matches. Wizard state preserved if user navigates away and returns in same session.

4. **Multi-condition Custom Query (Customer)**: Advanced search panel with fields: location (text input, fuzzy match), floor range (min floor - max floor number inputs), floor plan type (multi-select dropdown), price range (min and max inputs), vacancy rate range (min% - max% inputs). "Search" and "Reset" buttons. Results in sortable, paginated table.
   - Acceptance: Any combination of fields works. Empty query returns all properties. Pagination: 10/20/50 per page. Sort by price, area, vacancy rate. Results count displayed.

5. **Developer Company Info Management**: Developer can view and edit their own company profile: company name, contact person, phone, email, address, business license number, description. Read-only fields indicated.
   - Acceptance: Edit form saves correctly. Validation on required fields. Page title shows "公司信息管理".

6. **Customer Registration**: Registration form with: username, password (with confirmation), real name, phone, email, ID number (optional). Username uniqueness check on blur. Password strength indicator. Agreement checkbox.
   - Acceptance: Successful registration redirects to login. Duplicate username shows error. Weak password rejected.

7. **Customer Profile Management**: Customer edits: phone, email, home buying intention (multi-select: 自住/投资/改善/学区/养老), preferred locations (text), budget range (min-max), urgency (dropdown: 一个月内/三个月内/半年内/一年内/不限).
   - Acceptance: All fields update correctly. Intention tags removable/addable. Budget validation (max >= min).

### Should-Have (Sprint 3-4)

8. **Vacancy Correlation Analytics (Developer)**: Three chart views using ECharts: (a) Bar chart: vacancy rate by location/region, (b) Grouped bar chart: vacancy rate by floor plan type per location, (c) Scatter plot: floor level vs vacancy rate with trend line. Date range filter. Export chart as PNG.
   - Acceptance: Charts render with real data. Empty data shows "暂无数据" placeholder. Hover tooltips show exact values. Date filter correct. Multiple properties aggregated correctly per developer.

9. **Suggestion Submission (Customer)**: Customer submits suggestion to a developer. Form: select developer (from list), preferred floor plan type (dropdown), acceptable price range (min/max), additional notes (textarea). Suggestion is linked to both customer and developer.
   - Acceptance: Submission saved and visible in customer's "My Suggestions" list. Developer can view suggestions directed to them (in their dashboard). Empty state when no suggestions.

10. **Print Report / PDF Export**: "Print" button on all property result lists. Uses `window.print()` for browser print dialog. Also generates PDF via html2canvas + jsPDF or similar. Print preview includes: company header, query criteria summary, result table, timestamp, page numbers.
    - Acceptance: Print dialog opens with correct layout. PDF downloads with filename format: `report_YYYYMMDD_HHmmss.pdf`. Developer info header included. All columns visible.

11. **Developer List (Customer)**: Browse all developers in card grid view. Each card shows: company name, contact person, phone, brief description. Click to view detail page with full company info. Search by company name.
    - Acceptance: Cards render correctly. Empty search shows "没有找到匹配的开发商". Pagination for >10 developers.

### Nice-to-Have (Sprint 5+)

12. **Dashboard Home Pages**: Role-specific dashboards: Developer dashboard shows property count, total units, average vacancy rate, recent suggestions, quick action buttons. Customer dashboard shows recent searches, saved preferences, pending suggestions.
    - Acceptance: Metrics cards animate on load. Data refreshes on page focus.

13. **Property Image Management**: Upload property images (multi-file, drag-and-drop). Image preview in property cards. Max 5 images per property. Image optimization (compress to 800px width). Accept jpg/png/webp only.
    - Acceptance: Upload progress bar. Delete individual images. Gallery lightbox view.

14. **Dark Mode**: Toggle between light and dark themes using CSS variables. Persist preference in localStorage. Automatic system theme detection on first visit.
    - Acceptance: Toggle works instantly. All pages render correctly in dark mode. No flash of light mode on reload.

15. **Activity Logging & Audit Trail**: Backend logs all CRUD operations with actor, action, timestamp, entity type, entity ID. Developer can view recent activity. Admin-only (or developer-only) access to full audit log.
    - Acceptance: Logs created for all write operations. Developer sees logs only for their own actions. Logs paginated.

## Technical Stack

- **Frontend**: Vue 3 (Composition API + `<script setup>`), Vue Router 4, Pinia (state management), Axios (HTTP client), Element Plus (UI framework), ECharts 5 (charts), html2canvas + jsPDF (PDF export), dayjs (date formatting)
- **Backend**: Spring Boot 3.x, Spring Security (JWT filter), MyBatis-Plus 3.5.x, MySQL 8.x, Maven 3.9+, Lombok, Hutool (utility library), Knife4j (API docs / Swagger UI)
- **Key libraries**:
  - `io.jsonwebtoken:jjwt-api:0.12.x` for JWT
  - `com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter` for API docs
  - `cn.hutool:hutool-all` for string, date, collection utilities
  - `org.apache.poi:poi-ooxml` for Excel export (optional)
  - `com.baomidou:mybatis-plus-spring-boot3-starter`
  - `echarts` + `vue-echarts` for chart integration
  - `html2canvas` + `jspdf` for PDF generation
  - `@element-plus/icons-vue` for icons

## Data Model Design

### Table: `developer`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT PK AUTO | Primary key |
| company_name | VARCHAR(200) NOT NULL | Company name |
| contact_person | VARCHAR(100) | Contact person |
| phone | VARCHAR(20) | Phone number |
| email | VARCHAR(100) | Email address |
| address | VARCHAR(500) | Company address |
| business_license | VARCHAR(100) | Business license number |
| description | TEXT | Company description |
| username | VARCHAR(50) UNIQUE NOT NULL | Login username |
| password | VARCHAR(255) NOT NULL | BCrypt hashed password |
| status | TINYINT DEFAULT 1 | 1=active, 0=disabled |
| created_time | DATETIME | Creation time |
| updated_time | DATETIME | Last update time |

### Table: `customer`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT PK AUTO | Primary key |
| username | VARCHAR(50) UNIQUE NOT NULL | Login username |
| password | VARCHAR(255) NOT NULL | BCrypt hashed password |
| real_name | VARCHAR(100) | Real name |
| phone | VARCHAR(20) | Phone number |
| email | VARCHAR(100) | Email |
| id_card | VARCHAR(18) | ID card number |
| intention | VARCHAR(200) | Buying intention (comma-separated tags) |
| preferred_locations | VARCHAR(500) | Preferred areas |
| budget_min | DECIMAL(12,2) | Minimum budget |
| budget_max | DECIMAL(12,2) | Maximum budget |
| urgency | VARCHAR(20) | Timeframe: 一个月内/三个月内/半年内/一年内/不限 |
| status | TINYINT DEFAULT 1 | 1=active, 0=disabled |
| created_time | DATETIME | Creation time |
| updated_time | DATETIME | Last update time |

### Table: `property`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT PK AUTO | Primary key |
| developer_id | BIGINT FK NOT NULL | Reference to developer |
| property_name | VARCHAR(200) NOT NULL | Property/estate name |
| location | VARCHAR(500) NOT NULL | Geographic location address |
| longitude | DECIMAL(10,7) | Longitude for map |
| latitude | DECIMAL(10,7) | Latitude for map |
| floor_min | INT | Minimum floor number |
| floor_max | INT | Maximum floor number |
| floor_plan_type | VARCHAR(50) NOT NULL | Floor plan type: 一室一厅/两室一厅/三室两厅/四室两厅/复式/别墅 |
| total_units | INT NOT NULL | Total number of units |
| vacant_units | INT NOT NULL | Number of vacant units |
| vacancy_rate | DECIMAL(5,2) | Auto-calculated: vacant/total * 100 |
| price_per_sqm | DECIMAL(12,2) | Price per square meter |
| total_price | DECIMAL(14,2) | Total price for the unit/listing |
| area_sqm | DECIMAL(10,2) | Area in square meters |
| decoration | VARCHAR(50) | Decoration status: 毛坯/简装/精装/豪装 |
| status | VARCHAR(20) DEFAULT '在售' | Status: 在售/已售/待开盘 |
| description | TEXT | Property description |
| image_urls | VARCHAR(2000) | Comma-separated image URLs |
| created_time | DATETIME | Creation time |
| updated_time | DATETIME | Last update time |

### Table: `suggestion`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT PK AUTO | Primary key |
| customer_id | BIGINT FK NOT NULL | Reference to customer |
| developer_id | BIGINT FK NOT NULL | Reference to developer |
| preferred_type | VARCHAR(50) | Preferred floor plan type |
| price_min | DECIMAL(12,2) | Acceptable min price |
| price_max | DECIMAL(12,2) | Acceptable max price |
| notes | TEXT | Additional notes |
| status | VARCHAR(20) DEFAULT '待回复' | Status: 待回复/已回复/已关闭 |
| reply_content | TEXT | Developer reply |
| created_time | DATETIME | Creation time |
| updated_time | DATETIME | Last update time |

### Table: `report` (optional)
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT PK AUTO | Primary key |
| user_id | BIGINT NOT NULL | Actor user ID (customer or developer) |
| user_role | VARCHAR(20) NOT NULL | Role: ROLE_CUSTOMER or ROLE_DEVELOPER |
| report_type | VARCHAR(50) | Type: 查询结果/统计报表 |
| query_params | TEXT | JSON string of query parameters used |
| result_summary | TEXT | JSON summary of results |
| file_path | VARCHAR(500) | Path to generated PDF file |
| created_time | DATETIME | Creation time |

## Backend API Design

All APIs return unified response: `{ "code": 200, "message": "success", "data": {...} }`

### Auth APIs
| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/auth/login | All | Login, returns JWT |
| POST | /api/auth/register | Customer | Customer registration |
| GET | /api/auth/me | All | Get current user info |

### Developer APIs
| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /api/developers | Customer, Developer | List all developers |
| GET | /api/developers/{id} | All | Get developer detail |
| PUT | /api/developers/profile | Developer | Update own company info |

### Property APIs
| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /api/properties | All | List properties with multi-condition query, pagination |
| GET | /api/properties/{id} | All | Get property detail |
| POST | /api/properties | Developer | Create property |
| PUT | /api/properties/{id} | Developer | Update property (own only) |
| DELETE | /api/properties/{id} | Developer | Delete property (own only) |
| GET | /api/properties/my | Developer | List own properties |
| GET | /api/properties/statistics/vacancy-by-location | Developer | Vacancy rate by location |
| GET | /api/properties/statistics/vacancy-by-type | Developer | Vacancy rate by floor plan type |
| GET | /api/properties/statistics/vacancy-by-floor | Developer | Floor level vs vacancy scatter |

### Customer APIs
| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /api/customers/profile | Customer | Get own profile |
| PUT | /api/customers/profile | Customer | Update own profile |

### Suggestion APIs
| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/suggestions | Customer | Submit suggestion |
| GET | /api/suggestions/my | Customer | List own suggestions |
| GET | /api/suggestions/received | Developer | List suggestions received |
| PUT | /api/suggestions/{id}/reply | Developer | Reply to suggestion |

### Report APIs
| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/reports | All | Save a report record |
| GET | /api/reports | All | List my saved reports |
| GET | /api/reports/{id}/export | All | Export report as file |

## Frontend Route Design

```
/login                        - Login page
/register                     - Customer registration

/developer/dashboard          - Developer home dashboard
/developer/properties         - Property management (CRUD table)
/developer/properties/create  - Create property form
/developer/properties/:id/edit - Edit property form
/developer/analytics          - Vacancy correlation charts
/developer/profile            - Company info edit
/developer/suggestions        - Received suggestions list

/customer/dashboard           - Customer home dashboard
/customer/properties          - Property search & results
/customer/wizard              - Guided step wizard
/customer/developers          - Developer list
/customer/developers/:id      - Developer detail
/customer/suggestions         - My suggestions
/customer/suggestions/new     - Submit new suggestion
/customer/profile             - Personal info & intentions

/reports                      - Saved reports (shared)
```

## Evaluation Criteria

### Design Quality (weight: 0.3)
- Clean, professional UI consistent with the specified color palette and layout philosophy
- No visual elements that resemble "AI-slop" (no gratuitous gradients, no stock illustrations, no decorative blobs)
- Proper responsive behavior on mobile (sidebar collapses, tables scroll horizontally, cards stack vertically)
- Consistent spacing, typography, and alignment across all pages
- Form layouts are aligned left, labels right-aligned within their column
- Tables have consistent column widths, sort indicators, and zebra striping

### Originality (weight: 0.2)
- The step wizard for guided property search is a standout UX element -- it should feel intuitive, not cluttered
- The vacancy analytics dashboard should provide genuine insight, not just generic charts
- Smart defaults: query form fields remember last input, wizard preserves state on navigation
- The suggestion system creates a genuine feedback loop between customers and developers

### Craft (weight: 0.3)
- All CRUD operations have loading states, success/error toast notifications
- Empty states are informative: "暂无房产信息，点击上方按钮添加" with a CTA button
- Error states show user-friendly messages, not raw error codes
- Form validation is comprehensive: required fields, format checks (phone, email), range checks (budget, floor)
- Charts have proper tooltips, legends, and responsive sizing
- Print/PDF output is properly formatted with headers and page breaks
- Table sorting and pagination persist across page re-renders
- Navigation guards prevent unauthorized access (redirect to login with message)

### Functionality (weight: 0.2)
- Complete CRUD lifecycle for properties with proper authorization
- Multi-condition query returns correct filtered results for any combination
- Guided wizard produces same results as equivalent custom query
- Charts reflect actual data correctly (verify with known dataset)
- Registration flow: create account, login, access protected routes
- Print output matches screen display
- JWT expiry handling: 401 triggers redirect without losing user state

## Sprint Plan

### Sprint 1: Foundation & Auth (Days 1-3)
- Goals: Project scaffolding, database schema, authentication, base frontend layout
- Features: #1 (Auth), #6 (Registration), project setup
- Definition of done: User can register, login, see role-based landing page. JWT flow complete. Database tables created. CRUD base classes (Entity, Mapper, Service, Controller) for all 5 tables generated.

### Sprint 2: Core Feature Sprint (Days 4-8)
- Goals: Property CRUD, developer profile, developer list, custom query
- Features: #2 (Property CRUD), #3 (Developer Profile), #4 (Custom Query), #11 (Developer List)
- Definition of done: Developer can fully manage properties. Customer can browse developers and query properties with all filter combinations.

### Sprint 3: Intelligent Features (Days 9-12)
- Goals: Guided wizard, suggestion system, analytics charts
- Features: #5 (Guided Wizard), #8 (Analytics Charts), #9 (Suggestions)
- Definition of done: Customer can complete the 4-step wizard to find properties. Charts render with real data. Suggestions flow works end-to-end.

### Sprint 4: Polish & Reports (Days 13-15)
- Goals: Print/report, profile management, dashboard pages
- Features: #7 (Customer Profile), #10 (Print/PDF), #12 (Dashboards)
- Definition of done: All CRUD complete. Print generates formatted output. Dashboards show relevant metrics.

### Sprint 5: Enhancement (Days 16-18)
- Goals: Image upload, dark mode, audit logging
- Features: #13 (Image Upload), #14 (Dark Mode), #15 (Audit Log)
- Definition of done: All Nice-to-Have features working. Polish pass on all pages. Responsive verified on mobile viewport.

## Anti-Patterns to Avoid (Explicit Directives for Generator)

1. Do NOT use `any` type in TypeScript -- use proper interfaces for all data structures
2. Do NOT put business logic in controllers -- service layer must handle all logic
3. Do NOT use `System.out.println` for logging -- use SLF4J/Logger
4. Do NOT create single-file monolithic Vue components -- split into components (max 300 lines per `.vue` file)
5. Do NOT hardcode API URLs -- use `.env` for environment-specific config
6. Do NOT skip input validation on either frontend or backend -- validate everywhere
7. Do NOT use `@Autowired` field injection -- use constructor injection with `final` fields
8. Do NOT ignore SQL injection -- MyBatis-Plus `$` interpolation only for known-safe values
9. Do NOT leave unused imports, commented code, or debug console.log statements
10. Do NOT use Element Plus el-table without specifying column widths or min-widths for responsiveness

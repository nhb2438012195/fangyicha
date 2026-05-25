# Generator State — Iteration 002

## What Was Built (v1.4)

### Sprint 1: Backend Infrastructure
- Created `price_history` table schema in schema.sql
- Created `PriceHistory.java` entity
- Created `PriceHistoryMapper.java` with `selectByPropertyIdAndMonths` query
- Created `PriceHistoryService.java` and `PriceHistoryServiceImpl.java`
- Created `PropertyDetailDTO.java` (extends Property with developerName)
- Created `RecommendationDTO.java` (with reason field)
- Created `RecommendationServiceImpl.java` with location LIKE matching + budget sorting algorithm (max 6 results)
- Added `GET /api/properties/{id}/price-history` endpoint to PropertyController
- Modified `GET /api/properties/{id}` to return PropertyDetailDTO with developerName
- Added `GET /api/customers/recommendations` endpoint to CustomerController
- Updated DataInitializer to generate 24-month price history for all 120+ properties programmatically

### Sprint 2: Frontend Detail Page Enhancement
- Created `PropertyCarousel.vue` — Image carousel with thumbnail strip, 16:9 ratio, picsum.photos fallback
- Created `PriceTrendChart.vue` — ECharts dual-axis line chart (单价/总价) with tooltip, dataZoom slider, area gradient
- Created `PropertyLocation.vue` — Map placeholder with pin marker animation, address info, nearby POI landmarks grid
- Rewrote `PropertyDetailView.vue` — Tab layout (楼盘详情 | 价格走势 | 位置周边), skeleton loading, responsive
- Updated property.ts API with `getPriceHistory()` method
- Updated customer.ts API with `getRecommendations()` and `updatePreferredLocations()` methods
- Updated types/index.ts with `PriceHistoryItem`, `RecommendationItem`, `PropertyDetail` interfaces

### Sprint 3: Homepage Recommendations + Preference Editor
- Created `PreferredLocationEditor.vue` — Tag editor with add/remove, common locations popover, auto-save to server
- Created `PropertyRecommendCard.vue` — Recommendation card with hover scale/shadow effect, image overlay, reason tag
- Modified `DashboardView.vue` — Added recommendation grid section, preference editor, 5-min localStorage cache with manual refresh button

### Sprint 4: Polish
- Empty states for charts (no price data), recommendations (no preference set), carousel (no images), property not found
- Error states for recommendation loading
- Responsive layouts with media queries in all new/modified components
- Price trend chart enhanced with dataZoom slider, dual Y-axis, smooth gradient area fills
- Nearby POI data simulated with location-based regional lookup
- All interactive elements have hover micro-animations (translateY, shadow)

## New API Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /api/properties/{id}/price-history | PUBLIC | 获取房产近24个月价格历史 |
| GET | /api/customers/recommendations | CUSTOMER | 获取个性化房产推荐 |

## Modified API Endpoints

| Method | Path | Change |
|--------|------|--------|
| GET | /api/properties/{id} | Now returns PropertyDetailDTO with developerName |

## New Backend Files
- `entity/PriceHistory.java`
- `mapper/PriceHistoryMapper.java`
- `service/PriceHistoryService.java`
- `service/impl/PriceHistoryServiceImpl.java`
- `service/RecommendationService.java`
- `service/impl/RecommendationServiceImpl.java`
- `dto/PropertyDetailDTO.java`
- `dto/RecommendationDTO.java`

## New/Major Modified Frontend Files
- `views/customer/components/PropertyCarousel.vue` (new)
- `views/customer/components/PriceTrendChart.vue` (new)
- `views/customer/components/PropertyLocation.vue` (new)
- `views/customer/components/PreferredLocationEditor.vue` (new)
- `views/customer/components/PropertyRecommendCard.vue` (new)
- `views/customer/PropertyDetailView.vue` (rewritten)
- `views/customer/DashboardView.vue` (modified)
- `types/index.ts` (modified - new interfaces)
- `api/property.ts` (modified - new method)
- `api/customer.ts` (modified - new methods)

## Known Issues
- PriceHistoryMapper returns data sorted DESC by date; frontend reverses for display
- Nearby POI data is simulated (static lookup by city/district) rather than real GIS
- Map display uses picsum.photos fallback instead of real map API (AMap/Baidu)
- Backend needs MySQL `price_history` table (created via schema.sql)
- RecommendationService uses LIKE matching which may give false positives for partial location matches

## Dev Server
- Frontend: http://localhost:5173 (Vite default)
- Backend: http://localhost:8088 (Spring Boot)
- Status: not started

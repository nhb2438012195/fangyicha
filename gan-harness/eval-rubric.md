# Evaluation Rubric: v1.4 Property Detail Enhancement + Dashboard Recommendations

> Evaluator consumes this file directly. Score each criterion on a scale of 0.0 to 1.0. Final score = sum(weight * score). Minimum pass: 0.75.

## Global Anti-Pattern Checks (Auto-Fail if Violated)

If ANY of the following are found, score is 0.0 and generation fails immediately:
- Any `console.log` statements left in production code (excluding error logging)
- Any "TODO", "FIXME", or "HACK" comments indicating incomplete implementation
- Any gradient backgrounds used as image placeholders (e.g., CSS gradients where real images should be)
- Any Lorem Ipsum or placeholder text shown to users
- Any generic stock illustrations or vector icons used as content decoration
- Any functional regressions: existing features (property search, orders, suggestions, wizard) broken or modified unintentionally
- Price trend chart renders as empty container with no data lines
- Recommendation section shows "暂无数据" empty state when user has preferences set and matching properties exist
- Carousel shows one static image without arrow/pagination controls

## Evaluation Criteria

### 1. Design Quality (weight: 0.3)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Image carousel polish | 0.20 | Carousel uses `el-carousel` or equivalent with smooth transition. Image aspect ratio maintained (16:9 container). Navigation arrows visible on hover. Bottom thumbnail indicators show current slide. Autoplay with pause-on-hover. Placeholder image when no images exist (not a gradient). |
| Price chart legibility | 0.20 | Chart uses ECharts 6.1 with proper axis labels. X-axis shows month labels ("YYYY-MM" format). Y-axis shows price with ¥ prefix. Tooltip on hover shows exact values. DataZoom slider at bottom for range selection. Smooth curve lines (not jagged). Area fill with low opacity below lines. |
| Tab layout integration | 0.20 | Three tabs (楼盘详情, 价格走势, 位置周边) feel native, not bolted on. Tab bar styled consistently with rest of the app. Tab switch animation (fade, ~300ms). Content inside each tab properly padded and aligned. |
| Recommendation card consistency | 0.20 | Recommendation cards visually match existing dashboard cards (same border-radius, shadow, hover effect). Image thumbnail on card has proper aspect ratio. Match reason badge positioned correctly. Card hover lift effect (translateY + shadow). |
| Location editor UX | 0.10 | Tags displayed with proper spacing and remove (x) button. Add button visually distinct. Dropdown/input for adding new locations appears in correct position. Save feedback (toast) visible on auto-save. |
| Color/style system adherence | 0.10 | All new components use existing warm palette: background `#f5f0ea`, card `#fdf8f3`, text `#4a3728`/`#8a7a6a`, primary `#f5a623`, borders `#e8ddd0`. No introduction of new color systems. |

### 2. Originality (weight: 0.2)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Match reason labels | 0.30 | Recommendation cards show contextual match reason (e.g., "在您的偏好区域", "符合您的预算范围"). Labels are visually distinct from card content (small pill badge). Colors differentiate reason types. |
| Recommendation integration | 0.30 | The recommendation section feels like a natural part of the dashboard, not an afterthought. The section ties into the location editor above it. The "为你推荐" heading feels personal, not generic. |
| Location editor fluidity | 0.20 | Editing preferred locations on the dashboard feels faster and more natural than going to profile page. Auto-save is instant (debounced 500ms). No page reload. Location tags animate in/out. |
| Detail page information density | 0.20 | The tab layout reveals progressively more detail (overview -> prices -> location). Information is not overwhelming. Key decision-making info (price, status, images) is immediately visible without scrolling. |

### 3. Craft (weight: 0.3)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Loading states | 0.20 | ALL async sections have skeleton loading: carousel skeleton, chart skeleton, map skeleton, recommendation cards skeleton. No flash of empty content before data loads. Skeletons match final layout dimensions. |
| Empty states | 0.20 | ALL "no data" scenarios handled: no images (placeholder), no price history (text + icon), no location data (address text only), no recommendations (guidance + action button), no preferences set (prompt to set). Empty states are informative and actionable, not dismissive. |
| Error handling | 0.15 | API errors show toast messages. Failed image loads show fallback per slide. Failed chart renders show error state. Failed recommendation loads show retry option. Network timeout handled gracefully. |
| Animations and transitions | 0.15 | Tab switch: fade animation (~300ms). Carousel: smooth slide transition. Recommendation card: hover lift. Location tag: add/remove fade. Chart: ECharts built-in animation on load. Preference save: success toast appears and auto-dismisses. |
| Responsive behavior | 0.10 | Carousel height reduces on small screens (<768px: 250px vs 400px). Recommendation grid goes from 3 columns to 2 to 1. Tab labels stay readable. Info grid goes from 2 cols to 1. Chart container adapts height. |
| Touch/mobile considerations | 0.05 | Carousel supports touch swipe on mobile. Recommendation cards have adequate tap targets (min 44px). Tab bar scrollable if overflow. Location editor dropdown usable on touch. |
| Keyboard accessibility | 0.05 | Carousel navigable with left/right arrow keys. Tabs navigable with keyboard. Buttons focusable and activatable with Enter/Space. Escape closes any open dropdowns/dialogs. |

### 4. Functionality (weight: 0.2)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Detail page critical flow | 0.20 | User opens property detail -> sees image carousel with multiple images -> can navigate through images -> scrolls to tabbed content -> sees property info in first tab -> switches to price tab -> sees chart -> switches to location tab -> sees map/address -> can click buy button -> order created successfully. |
| Dashboard recommendation flow | 0.20 | User opens dashboard -> sees stats row (unchanged) -> sees preferred location tags -> sees "为你推荐" section with property cards -> each card has match reason badge -> clicking card navigates to property detail -> switching back to dashboard preserves state. |
| Location editor flow | 0.20 | User clicks "x" on a location tag -> tag disappears -> toast "偏好区域已更新" appears -> recommendation list automatically refreshes with new results -> user clicks "+" -> dropdown appears -> selects "广州市" -> tag appears -> recommendations refresh. |
| No location set flow | 0.15 | User opens dashboard with no preferredLocations -> sees guidance message "设置偏好区域，获取个性化推荐" -> clicks "去设置" -> location editor activates -> adds first location -> recommendations appear. |
| Empty recommendation flow | 0.15 | User sets preferred locations that don't match any property -> sees "暂无匹配的房源" message with suggestion to adjust -> clicks "修改偏好区域" -> location editor focused -> changes location -> recommendations update. |
| Price history data accuracy | 0.10 | PriceHistoryItem records match database seed data. Chart displays correct number of data points (24 months). X-axis labels show correct months. Values are formatted with 2 decimal places. |

## Scoring Formula

```
final_score = (0.3 * design_avg) + (0.2 * originality_avg) + (0.3 * craft_avg) + (0.2 * functionality_avg)

where each *_avg = sum(sub_criterion_weight * sub_criterion_score) / sum(sub_criterion_weight)
```

## Pass/Fail Threshold

- **Pass**: final_score >= 0.75
- **Conditional Pass**: 0.60 <= final_score < 0.75 (generator must address all items scoring < 0.5)
- **Fail**: final_score < 0.60 (full regeneration required)
- **Auto-Fail**: Any Global Anti-Pattern check violated

## Test Scenarios (Manual Verification)

### Scenario A: Property Detail Immersion
1. Login as customer (zhangsan / 123456)
2. Navigate to any property detail from search results
3. Verify: carousel shows 3+ images with arrow navigation and thumbnail indicators
4. Verify: images load with proper aspect ratio, no broken images
5. Verify: header shows property name, price, status tag correctly
6. Click "价格走势" tab: verify chart renders with line series and DataZoom
7. Verify: tooltip works on chart hover
8. Click "位置周边" tab: verify map/address displayed
9. Verify: "立即购买" button works if property is "在售"

### Scenario B: Dashboard Personalization
1. Customer zhangsan has preferredLocations "广州市,深圳市"
2. Visit `/customer/dashboard`
3. Verify: "为你推荐" section shows properties from 广州市 or 深圳市
4. Verify: each card shows a match reason (e.g., "在您的偏好区域")
5. Verify: cards are clickable and navigate to detail page
6. Remove "深圳市" from location tags
7. Verify: recommendations refresh, now show only 广州市 properties (or fewer)
8. Add "长沙市" back: recommendations refresh with new results

### Scenario C: No Preferences Flow
1. Login as a new customer with no preferredLocations set
2. Visit `/customer/dashboard`
3. Verify: guidance message appears "设置偏好区域，获取个性化推荐"
4. Click "去设置"
5. Add a location (e.g., "北京市")
6. Verify: recommendations load with matching properties
7. Verify: toast "偏好区域已更新" appears

### Scenario D: Price Trend Data Verification
1. Open property detail for "美的·蓝溪谷" (株洲天元区, pricePerSqm=7200)
2. Switch to "价格走势" tab
3. Verify: chart shows line from ~2024-06 to ~2026-05 (24 data points)
4. Verify: current month pricePerSqm is approximately 7200
5. Verify: DataZoom slider at bottom allows time range selection
6. Verify: chart is responsive (resize browser window, chart adapts)

### Scenario E: Edge Cases
1. Find a property with null imageUrls: verify placeholder image, not broken link
2. Find a property with null latitude/longitude: verify address text fallback, not map error
3. Set preferredLocations to a remote area with no properties: verify "暂无匹配" guidance
4. Trigger API error (e.g., disable network): verify error toast + retry option
5. Resize to 768px viewport: verify layout adapts correctly
6. Test keyboard navigation on carousel (left/right arrows): verify image changes

### Scenario F: No Regression Check
1. Property search (`/customer/properties`): search, sort, paginate all work
2. Wizard (`/customer/wizard`): step-by-step flow works
3. Orders (`/customer/orders`): list and detail work
4. Suggestions (`/customer/suggestions`): CRUD works
5. Profile (`/customer/profile`): editing and saving works
6. Developer dashboards and views are unaffected

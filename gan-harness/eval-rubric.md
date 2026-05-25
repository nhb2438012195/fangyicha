# Evaluation Rubric: 房易查 (RealEstateQuery)

> Evaluator consumes this file directly. Score each criterion on a scale of 0.0 to 1.0. Final score = sum(weight * score). Minimum pass: 0.75.

## Evaluation Criteria

### 1. Design Quality (weight: 0.3)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Color & typography consistency | 0.25 | Verify palette matches spec: #1a73e8 primary, #f5f7fa background, #1e293b sidebar, #1f2937 text. System font stack used. No gradient abuse, no stock images. |
| Layout & spacing | 0.25 | Fixed sidebar 240px. Card-based displays. Consistent 16px/24px spacing. Max-width 1400px centered content. 8px border-radius on interactive elements. |
| Responsive behavior | 0.25 | Sidebar collapses on <768px. Tables horizontally scrollable. Cards stack vertically on mobile. Forms remain usable on 375px viewport. |
| Anti-AI-slop compliance | 0.25 | No gradient backgrounds on cards. No stock illustrations. No decorative blobs. No unnecessary border-radius. Every visual element serves a function. |

### 2. Originality (weight: 0.2)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Guided wizard UX | 0.4 | 4-step wizard feels intuitive. Steps: Location, Type, Price, Results. Step state preserved on navigation. Back/Next works. Skip optional steps allowed. |
| Analytics insight | 0.3 | Charts are not generic -- they show specific vacancy correlations (by location, by type, by floor). Tooltips show exact values. Empty state handled. |
| Smart defaults | 0.3 | Query fields remember last input per session. Wizard state saved. Search params reflected in URL query string. |

### 3. Craft (weight: 0.3)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Loading & transition states | 0.2 | v-loading on tables/cards. Skeleton placeholders for initial load. Button loading state during form submit. Smooth page transitions. |
| Empty states | 0.2 | Every list has informative empty state: "暂无房产信息，点击上方按钮添加" with CTA. Charts show "暂无数据" placeholder. |
| Error handling | 0.2 | Network errors show friendly toast (not raw error). Form validation errors inline. 401 redirects to login with "登录已过期，请重新登录" message. |
| Form validation | 0.15 | Required field indicators. Phone/email format validation. Password strength indicator. Budget range validation (max >= min). Floor range validation. |
| Table quality | 0.15 | Sort indicators. Column widths set. Zebra striping. Pagination with page size selector. Column headers in Chinese. |
| Print/PDF output | 0.1 | Print output includes company header, query criteria, result table, timestamp. PDF downloads with correct filename. |

### 4. Functionality (weight: 0.2)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Auth & role control | 0.2 | Login with role redirects correctly. JWT protected routes redirect unauthorized. Customer cannot access developer routes and vice versa. Token expiry handled. |
| Property CRUD | 0.15 | Create with validation. Edit pre-populates. Delete with confirmation. Paginated list. Only owning developer sees their properties. |
| Multi-condition query | 0.2 | All 6 filter fields work: location (fuzzy), floor range, plan type (multi), price range, vacancy range. Any combination returns correct results. Pagination works. |
| Guided wizard | 0.15 | All 4 steps functional. Results match equivalent manual query. Empty results handled gracefully. |
| Suggestion flow | 0.1 | Customer submits suggestion to a developer. Developer views received suggestions. Reply functionality works. |
| Print/export | 0.1 | Print button exists on result lists. Browser print dialog opens. PDF download works. |
| Customer profile | 0.1 | Profile update saves. Intention tags add/remove. Budget validation. |

## Scoring Formula

```
final_score = (0.3 * design_avg) + (0.2 * originality_avg) + (0.3 * craft_avg) + (0.2 * functionality_avg)

where each *_avg = sum(sub_criterion_weight * sub_criterion_score) / sum(sub_criterion_weight)
```

## Pass/Fail Threshold

- **Pass**: final_score >= 0.75
- **Conditional Pass**: 0.60 <= final_score < 0.75 (generator must address all items scoring < 0.5)
- **Fail**: final_score < 0.60 (full regeneration required)

## Test Scenarios (Manual)

### Scenario A: Happy path -- Customer finds a home
1. Register as `test_customer` / `password123`
2. Login, see customer dashboard
3. Use Guided Wizard: select location "朝阳区", type "两室一厅", price 200-500万
4. See matching properties
5. Refine with Custom Query: add vacancy rate < 20%
6. Click "Print" on results
7. Browser print dialog opens
8. Click a developer name, see developer detail
9. Submit suggestion to that developer with preferred type "两室一厅" and price 300万

### Scenario B: Happy path -- Developer manages listings
1. Login as `developer01` / `password123`
2. Add a property: "朝阳花园3号楼", "朝阳区建国路88号", floor 5-18, "三室两厅", 200 total, 15 vacant, 80000/sqm, 120sqm
3. Verify property appears in list
4. Edit the property: change price to 85000/sqm
5. Verify update reflected
6. View Analytics page: see 3 charts
7. Check received suggestions
8. Reply to a suggestion
9. Edit company profile, save

### Scenario C: Edge cases
1. Login with wrong password -- error message shown
2. Access /developer/properties as customer -- redirected to customer dashboard or login
3. Submit empty query -- all properties returned
4. Query with no matches -- empty state shown with "没有找到匹配的房源"
5. Delete property -- confirmation dialog, property removed after confirm
6. Register with existing username -- error "用户名已存在"
7. Navigate directly to /customer/wizard without login -- redirected to /login

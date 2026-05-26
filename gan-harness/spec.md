# Full-Site UI Style Unification Plan

## Vision

Transform the entire 房易查 real estate inquiry platform from a generic blue/gray admin dashboard into a warm, residential-branded application that evokes trust and comfort. The login page's warm orange-and-brown palette (already implemented) becomes the foundation for all authenticated pages, creating a cohesive brand experience from login through every workflow.

Current state: Login page has been redesigned with warm residential styling. All other pages still use the old blue (`#1a73e8`) primary color on a cool gray (`#f5f7fa`) background with `#1f2937` text. The sidebar is dark navy (`#1e293b`) with blue accent. The result is a jarring visual break between the login page and the main app.

Target state: Every page in the app uses warm tones -- orange primary, warm beige backgrounds, brown text, warm dark sidebar -- forming a unified residential brand identity.

## Design Direction

- **Color palette**: Orange primary `#f5a623`, hover `#e0961a`, active `#d4870e`; warm beige bg `#f5f0ea`; warm white cards `#fdf8f3`; deep brown text `#4a3728`; secondary brown `#8a7a6a`; muted brown `#c4b5a5`; warm borders `#e8ddd0`; warm dark sidebar `#3d2c1e`; warm brown shadows `rgba(180,130,80,0.12)`

- **Typography**: System font stack (`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif`); 22px page titles in `#4a3728`; 16px section headings; 14px body in `#4a3728`; 13px secondary in `#8a7a6a`; 12px muted in `#c4b5a5`

- **Layout philosophy**: Airy single-column content area with max-width 1200-1400px, consistent with existing layout. Cards use warm white `#fdf8f3` instead of pure white. All shadows use warm brown tint. Page-level background is warm beige `#f5f0ea`.

- **Visual identity**: Warm residential aesthetic. Sidebar uses warm dark brown `#3d2c1e` with orange accent. All primary interactive elements use orange `#f5a623`. Cards have subtle warm shadows. Borders are warm beige `#e8ddd0` instead of cool gray.

- **Anti-AI-slop directives**: No gradient abuse (only login brand area uses gradients). No generic blue buttons anywhere. No stock illustrations. No conflicting color systems -- every file must use the warm palette consistently. Card shadows must use warm brown tint, never generic black `rgba(0,0,0,...)`.

## Features (prioritized)

### Must-Have (Phase 1: Foundation)
1. **Global CSS Variables + Element Plus Theme Override**: Define `:root` CSS custom properties for the warm palette. Override `el-button--primary`, `el-table` header styles, `el-tag--primary`, `el-card` border, scrollbar colors. Change `body` background from `#f5f7fa` to `#f5f0ea` and text color from `#1f2937` to `#4a3728`.
   - Acceptance: All `<el-button type="primary">` render as orange `#f5a623`. `<el-table>` headers use warm beige `#fef7ed` background. `<el-tag type="primary">` uses orange styling. Page background is warm beige.

2. **Sidebar Full Color Overhaul**: Change sidebar from dark navy `#1e293b` to warm dark brown `#3d2c1e`. Logo icon from blue `#1a73e8` to orange `#f5a623`. Menu text from `#94a3b8` to `#c4b5a5`. Active item from blue `#1a73e8` to orange `#f5a623`. Hover state uses `rgba(255,255,255,0.08)`. Border colors use warm transparent white. Avatar backgrounds use warm brand colors.
   - Acceptance: Sidebar renders as warm dark brown. All active/hover states use orange. No blue remains in the sidebar.

3. **MainLayout Color Adaptation**: Change layout background from `#f5f7fa` to `#f5f0ea`. Navbar background from `#ffffff` to `#fdf8f3`. Navbar border from `#e5e7eb` to `#e8ddd0`. Navbar shadow from `rgba(0,0,0,0.04)` to `rgba(180,130,80,0.10)`. Update breadcrumb, user dropdown, and collapse button colors.
   - Acceptance: Navbar and content area use warm background. No cool gray `#f5f7fa` in layout.

### Must-Have (Phase 2: Dashboards + Key Views)
4. **Customer Dashboard (DashboardView.vue)**: Update quick action icon colors from blue `#1a73e8`/green `#34a853`/amber `#f59e0b`/purple `#9334e6` to warm palette: `#f5a623`, `#d4a373`, `#8a7a6a`, `#4a3728`. Update stat card value colors. Change all text `#1f2937` to `#4a3728`, `#6b7280` to `#8a7a6a`. Card shadows use warm tint.
   - Acceptance: Quick actions show warm-colored icons. Stat cards use warm text. No blue or green icon colors.

5. **Developer Dashboard (DashboardView.vue)**: Update metric card left border colors, icon background colors, action card icon colors from blue/green/purple to warm palette. All text colors updated.
   - Acceptance: Metric cards use warm left borders. Action icons use warm colors. All text is warm.

6. **Property Search + Table (PropertySearchView.vue)**: Global button override handles primary button. Update table header colors (handled globally). Verify card shadows, border colors, text colors.
   - Acceptance: Search card and result card use warm styling. Table headers use warm beige. Text is warm brown.

7. **Wizard View (WizardView.vue)**: Update step colors, tag selection border colors, type-card border on hover from `#1a73e8` to `#f5a623`, price highlight from `#1a73e8` to `#f5a623`, button colors.
   - Acceptance: All interactive elements use orange. Price highlights use orange. No blue remains.

8. **Developer List + Detail (DeveloperListView.vue, DeveloperDetailView.vue)**: Update avatar backgrounds, button colors, card colors, text colors, descriptions border colors.
   - Acceptance: Developer cards and detail page use warm palette.

### Must-Have (Phase 3: Forms + Tables)
9. **All Form Views**: ProfileView (customer), NewSuggestionView, ProfileView (developer), PropertyFormView -- update button colors, divider colors from `#e5e7eb` to `#e8ddd0`, text colors, card colors.
   - Acceptance: All forms use warm-border inputs, warm buttons, warm text.

10. **All Table Views**: SuggestionsView (customer), SuggestionsView (developer), PropertyListView -- update button colors, table header styles, pagination styles, tag colors, text colors.
    - Acceptance: All table-based views use warm styling consistently.

### Should-Have (Phase 4)
11. **Analytics View (AnalyticsView.vue)**: Update chart header border colors, text colors, export button colors, card colors.
    - Acceptance: Analytics page uses warm neutral tones. Chart areas have warm headers.

12. **Register Page (RegisterView.vue)**: Full warm redesign matching login page: page bg to `#f5f0ea`, card bg to `#fdf8f3`, card shadow to warm tint, card radius to 16px, title colors to `#4a3728`/`#8a7a6a`, input styles matching login (border `#e8ddd0`, focus `#f5a623`), link colors to `#f5a623`, checkbox checked color to `#f5a623`.
    - Acceptance: Register page visually matches login page. No blue `#1a73e8` anywhere.

### Nice-to-Have (Phase 5)
13. **Animation Polish**: Add subtle transition/animation when sidebar active item changes. Add hover card lift animation consistency across all pages (already partially implemented, verify uniform application).

14. **Print Style Updates**: Update print media queries in App.vue and PropertySearchView to use warm colors where applicable for printed reports.

## Color Mapping Reference

For every file, replace these exact colors:

| Old Color | New Color | Scope |
|-----------|-----------|-------|
| `#1a73e8` | `#f5a623` | Primary buttons, links, active states, avatar bg |
| `#1557b0` | `#e0961a` | Button hover |
| `#34a853` | `#d4a373` | Secondary/green accents, some avatar bg |
| `#9334e6` | `#8a7a6a` | Purple accents -> warm secondary |
| `#1f2937` | `#4a3728` | All body text, headings, titles |
| `#6b7280` | `#8a7a6a` | Secondary text, descriptions, labels |
| `#9ca3af` | `#c4b5a5` | Muted text, placeholders |
| `#f5f7fa` | `#f5f0ea` | Page background |
| `#ffffff` | `#fdf8f3` | Card backgrounds (keep `#ffffff` for high-contrast elements) |
| `#e5e7eb` | `#e8ddd0` | Borders, dividers |
| `#f3f4f6` | `#f3ece4` | Subtle borders, light dividers |
| `#f8fafc` | `#fef7ed` | Table header bg, striped row bg |
| `#1e293b` | `#3d2c1e` | Sidebar background |
| `#94a3b8` | `#c4b5a5` | Sidebar text |
| `rgba(0,0,0,0.08)` | `rgba(180,130,80,0.12)` | Card shadows |
| `rgba(0,0,0,0.1)` | `rgba(180,130,80,0.15)` | Card hover shadows |

## Files to Modify (Complete List)

```
P0: frontend/src/App.vue
P0: frontend/src/layouts/components/Sidebar.vue
P0: frontend/src/layouts/MainLayout.vue
P1: frontend/src/views/customer/DashboardView.vue
P1: frontend/src/views/developer/DashboardView.vue
P1: frontend/src/views/customer/PropertySearchView.vue
P1: frontend/src/views/customer/WizardView.vue
P1: frontend/src/views/customer/DeveloperListView.vue
P1: frontend/src/views/customer/DeveloperDetailView.vue
P1: frontend/src/views/customer/SuggestionsView.vue
P1: frontend/src/views/customer/NewSuggestionView.vue
P1: frontend/src/views/customer/ProfileView.vue
P1: frontend/src/views/developer/PropertyListView.vue
P1: frontend/src/views/developer/PropertyFormView.vue
P1: frontend/src/views/developer/AnalyticsView.vue
P1: frontend/src/views/developer/ProfileView.vue
P1: frontend/src/views/developer/SuggestionsView.vue
P2: frontend/src/views/register/RegisterView.vue
P2: frontend/src/views/customer/OrderListView.vue (if exists)
P2: frontend/src/views/customer/OrderDetailView.vue (if exists)
P2: frontend/src/views/developer/DeveloperOrderListView.vue (if exists)
P2: frontend/src/views/developer/DeveloperOrderDetailView.vue (if exists)
```

Total: ~23 files

## Detailed Modification Per File

### App.vue -- Global Overrides

Replace entire `<style>` block:

```css
/* Global CSS Variables */
:root {
  --color-primary: #f5a623;
  --color-primary-hover: #e0961a;
  --color-primary-active: #d4870e;
  --color-primary-bg: #fef7ed;
  --color-bg-page: #f5f0ea;
  --color-bg-card: #fdf8f3;
  --color-text-primary: #4a3728;
  --color-text-secondary: #8a7a6a;
  --color-text-muted: #c4b5a5;
  --color-border: #e8ddd0;
  --color-border-light: #f3ece4;
  --color-shadow: rgba(180, 130, 80, 0.12);
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
}

/* Element Plus Overrides */
.el-card {
  border: 1px solid var(--color-border) !important;
  border-radius: 10px;
}

.el-card__body {
  padding: 20px;
}

.el-table th.el-table__cell {
  background-color: var(--color-primary-bg) !important;
  color: var(--color-text-primary);
  font-weight: 600;
}

.el-table--striped .el-table__body tr.el-table__row--striped td {
  background-color: var(--color-bg-card);
}

.el-table__body tr:hover > td {
  background-color: var(--color-primary-bg) !important;
}

.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-hover);
  --el-button-hover-border-color: var(--color-primary-hover);
  --el-button-active-bg-color: var(--color-primary-active);
  --el-button-active-border-color: var(--color-primary-active);
}

.el-button--primary.is-link,
.el-button--primary.is-text {
  --el-button-text-color: var(--color-primary);
  --el-button-hover-text-color: var(--color-primary-hover);
}

.el-tag--primary {
  --el-tag-bg-color: var(--color-primary-bg);
  --el-tag-border-color: var(--color-primary);
  --el-tag-text-color: var(--color-primary);
}

.el-radio-button__orig-radio:checked + .el-radio-button__inner {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: -1px 0 0 0 var(--color-primary);
}

.el-link.el-link--primary {
  --el-link-text-color: var(--color-primary);
  --el-link-hover-text-color: var(--color-primary-hover);
}

.el-pagination.is-background .el-pager li.is-active {
  background-color: var(--color-primary);
}

.el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.el-checkbox__input.is-checked + .el-checkbox__label {
  color: var(--color-primary);
}

.el-divider__text.is-left {
  color: var(--color-text-primary);
  font-weight: 600;
}

.el-divider--horizontal {
  border-top-color: var(--color-border);
}

.el-progress-bar__outer {
  background-color: var(--color-border-light);
}

.el-descriptions__title {
  color: var(--color-text-primary);
}

.el-descriptions__label {
  color: var(--color-text-secondary);
}

.el-table__empty-text {
  color: var(--color-text-muted);
}

/* Print styles unchanged except body background */
@media print {
  body { background: white !important; }
  .sidebar-container, .top-navbar, .el-button, .el-pagination { display: none !important; }
  .content-area { padding: 0 !important; margin: 0 !important; }
}

/* Scrollbar */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #d4c5b5; }
```

### Sidebar.vue -- Full Color Overhaul

```diff
- background: #1e293b;
+ background: #3d2c1e;

- .logo-icon { background: #1a73e8; }
+ .logo-icon { background: var(--color-primary); }

- .sidebar-menu (background-color="#1e293b", text-color="#94a3b8", active-text-color="#ffffff")
+ .sidebar-menu (background-color="#3d2c1e", text-color="#c4b5a5", active-text-color="#ffffff")

- .sidebar-menu .el-menu-item.is-active { background-color: #1a73e8 !important; }
+ .sidebar-menu .el-menu-item.is-active { background-color: var(--color-primary) !important; }

- .sidebar-menu .el-menu-item:hover { background-color: rgba(255,255,255,0.08) !important; }
+ (same hover, keep it)

- .user-role { color: #94a3b8; }
+ .user-role { color: #c4b5a5; }

- Avatar bg: { backgroundColor: '#1a73e8' } or '#34a853' 
+ Avatar bg: use computed warm colors based on role or use '#d4a373' for customer, '#f5a623' for developer

- .logout-btn { color: #94a3b8; }
+ .logout-btn { color: #c4b5a5; }
```

### MainLayout.vue -- Warm Adaptation

```diff
- .layout-container { background: #f5f7fa; }
+ .layout-container { background: var(--color-bg-page); }

- .top-navbar { background: #ffffff; border-bottom: 1px solid #e5e7eb; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
+ .top-navbar { background: var(--color-bg-card); border-bottom: 1px solid var(--color-border); box-shadow: 0 1px 3px rgba(180,130,80,0.10); }

- .collapse-btn { color: #6b7280; }
+ .collapse-btn { color: var(--color-text-secondary); }

- .user-dropdown-name { color: #1f2937; }
+ .user-dropdown-name { color: var(--color-text-primary); }

- .user-dropdown:hover { background-color: #f3f4f6; }
+ .user-dropdown:hover { background-color: var(--color-border-light); }

- router-link breadcrumb style="color: #1f2937"
+ router-link breadcrumb style="color: var(--color-text-primary)"
```

### Customer Dashboard (customer/DashboardView.vue)

```diff
Quick actions color mapping:
- Search:  { color: '#1a73e8' }  -> { color: '#f5a623' } with bg '#fef7ed'
- Wizard:  { color: '#34a853' }  -> { color: '#d4a373' } with bg '#fdf8f3'
- Developers: { color: '#f59e0b' } -> { color: '#8a7a6a' } with bg '#f5f0ea'
- Suggestions: { color: '#9334e6' } -> { color: '#4a3728' } with bg '#f3ece4'

- .page-title { color: #1f2937; } -> { color: var(--color-text-primary); }
- .page-desc { color: #6b7280; } -> { color: var(--color-text-secondary); }
- .stat-card: box-shadow: 0 1px 3px rgba(0,0,0,0.08) -> box-shadow: 0 1px 3px rgba(180,130,80,0.12)
- .stat-value { color: #1f2937; } -> { color: var(--color-text-primary); }
- .stat-label { color: #6b7280; } -> { color: var(--color-text-secondary); }
- .section-title { color: #1f2937; } -> { color: var(--color-text-primary); }
- .action-label { color: #1f2937; } -> { color: var(--color-text-primary); }
- .action-desc { color: #6b7280; } -> { color: var(--color-text-secondary); }
- .action-arrow { color: #9ca3af; } -> { color: var(--color-text-muted); }
- .guide-section: box-shadow update
- Stat card value '#f59e0b' stays (warm), '#1a73e8' -> '#f5a623'
```

### Developer Dashboard (developer/DashboardView.vue)

Replace the `metricCards` array and inline colors:

```diff
// From:
{ label: '房产总数', color: '#1a73e8', bgColor: '#e8f0fe' },
{ label: '总户数', color: '#34a853', bgColor: '#e6f4ea' },
{ label: '平均空置率', color: '#f59e0b', bgColor: '#fef3c7' },
{ label: '待回复建议', color: '#ea4335', bgColor: '#fce8e6' },
{ label: '在售楼盘', color: '#34a853', bgColor: '#e6f4ea' },
{ label: '待开盘楼盘', color: '#9334e6', bgColor: '#f3e8ff' },

// To:
{ label: '房产总数', color: '#f5a623', bgColor: '#fef7ed' },
{ label: '总户数', color: '#d4a373', bgColor: '#fdf8f3' },
{ label: '平均空置率', color: '#e09e2f', bgColor: '#fef3c7' },
{ label: '待回复建议', color: '#e85c41', bgColor: '#fce8e6' },
{ label: '在售楼盘', color: '#5ea84f', bgColor: '#e6f4ea' },
{ label: '待开盘楼盘', color: '#8a7a6a', bgColor: '#f5f0ea' },

Action card icon colors:
- Add property: #1a73e8 -> #f5a623
- View analytics: #34a853 -> #d4a373
- Customer suggestions: #f59e0b -> #e09e2f
- Company info: #9334e6 -> #8a7a6a
```

### All Remaining View Files

Each file needs these systematic changes. Search for each old color and replace:

1. `.page-title { color: #1f2937 }` -> `{ color: var(--color-text-primary) }` (or `#4a3728`)
2. `.page-desc { color: #6b7280 }` -> `{ color: var(--color-text-secondary) }` (or `#8a7a6a`)
3. Any `color: #1f2937` -> `color: var(--color-text-primary)`
4. Any `color: #6b7280` -> `color: var(--color-text-secondary)`
5. Any `color: #9ca3af` -> `color: var(--color-text-muted)`
6. Any `border: 1px solid #e5e7eb` -> `border: 1px solid var(--color-border)`
7. Any `border-bottom: 1px solid #f3f4f6` -> `border-bottom: 1px solid var(--color-border-light)`
8. Any `background: #f3f4f6` -> `background: var(--color-border-light)`
9. Any `background: #f9fafb` -> `background: var(--color-bg-card)`
10. Any `border-color: #1a73e8` on hover -> `border-color: var(--color-primary)`
11. Any `box-shadow: rgba(0,0,0,...)` -> `rgba(180,130,80,...)`

## Specific Inline Color Changes (Non-Variable)

For script section inline colors that can't use CSS variables:

**Customer Dashboard (script):**
```diff
- { label: '房产查询', color: '#1a73e8', ... },
- { label: '引导查询', color: '#34a853', ... },
- { label: '开发商列表', color: '#f59e0b', ... },
- { label: '提交建议', color: '#9334e6', ... }
+ { label: '房产查询', color: '#f5a623', ... },
+ { label: '引导查询', color: '#d4a373', ... },
+ { label: '开发商列表', color: '#8a7a6a', ... },
+ { label: '提交建议', color: '#4a3728', ... }
```

**Developer Dashboard (script):**
```diff
- { label: '房产总数', color: '#1a73e8', bgColor: '#e8f0fe', ... },
- { label: '总户数', color: '#34a853', bgColor: '#e6f4ea', ... },
+ { label: '房产总数', color: '#f5a623', bgColor: '#fef7ed', ... },
+ { label: '总户数', color: '#d4a373', bgColor: '#fdf8f3', ... },
```

**MainLayout template:**
```diff
- el-avatar :style="{ backgroundColor: authStore.isDeveloper ? '#1a73e8' : '#34a853' }"
+ el-avatar :style="{ backgroundColor: authStore.isDeveloper ? '#f5a623' : '#d4a373' }"
```

**Sidebar template:**
```diff
- el-avatar :style="{ backgroundColor: authStore.isDeveloper ? '#1a73e8' : '#34a853' }"
+ el-avatar :style="{ backgroundColor: authStore.isDeveloper ? '#f5a623' : '#d4a373' }"
```

**DeveloperDetailView template:**
```diff
- el-avatar :style="{ backgroundColor: '#1a73e8' }"
+ el-avatar :style="{ backgroundColor: '#f5a623' }"
```

**DeveloperListView template:**
```diff
- el-avatar :style="{ backgroundColor: '#1a73e8' }"
+ el-avatar :style="{ backgroundColor: '#f5a623' }"
```

**WizardView:**
```diff
- .type-card:hover { border-color: #1a73e8; background: #f0f6ff; }
+ .type-card:hover { border-color: var(--color-primary); background: var(--color-primary-bg); }

- .detail-value.highlight { color: #1a73e8; }
+ .detail-value.highlight { color: var(--color-primary); }
```

**AnalyticsView (echarts chart colors):**
The chart gradient colors `#1a73e8`, `#34a853`, `#64b5f6`, `#81c784` should use warm-toned alternatives:
- Location chart: `#f5a623` to `#f8c16a` (orange gradient)
- Type chart: `#d4a373` to `#e8ddd0` (beige gradient)
- Floor scatter: `#f5a623` to `#d4a373`

## Register Page -- Full Redesign

```diff
- .register-page { background: #f5f7fa; }
+ .register-page { background: var(--color-bg-page); }

- .register-card { background: #ffffff; box-shadow: 0 1px 3px rgba(0,0,0,0.08); border-radius: 12px; }
+ .register-card { background: var(--color-bg-card); box-shadow: 0 8px 32px rgba(180,130,80,0.12); border-radius: 16px; }

- .register-title { color: #1f2937; }
+ .register-title { color: var(--color-text-primary); }

- .register-subtitle { color: #6b7280; }
+ .register-subtitle { color: var(--color-text-secondary); }

- .login-link { color: #1a73e8; }
+ .login-link { color: var(--color-primary); }

+ Add input wrapper styling matching login page:
+  .register-form :deep(.el-input__wrapper) {
+    border-radius: 8px;
+    border: 1px solid var(--color-border);
+    box-shadow: none;
+  }
+  .register-form :deep(.el-input__wrapper.is-focus) {
+    border-color: var(--color-primary);
+    box-shadow: 0 0 0 1px var(--color-primary);
+  }
```

## Verification Script

After all changes, run in the project to confirm no old colors remain:

```bash
# Should return ZERO results in .vue files:
grep -rn "#1a73e8" frontend/src/views/ frontend/src/layouts/
grep -rn "#1f2937" frontend/src/views/ frontend/src/layouts/ frontend/src/App.vue
grep -rn "#f5f7fa" frontend/src/views/ frontend/src/layouts/ frontend/src/App.vue

# Should still exist (kept for dark sidebar):
grep -rn "rgba(255, 255, 255, 0." frontend/src/  # OK in Sidebar.vue

# Should exist in App.vue (global overrides) only:
grep -rn "el-button--primary" frontend/src/
```

## Sprint Plan

### Sprint 1: Foundation -- App.vue + Layout
- Goals: Establish the global CSS foundation and update the sidebar and main layout
- Files: App.vue, Sidebar.vue, MainLayout.vue
- Definition of done: Page background is warm beige. Sidebar is warm dark brown with orange active state. Navbar uses warm colors. All global Element Plus overrides use orange primary.

### Sprint 2: Dashboards + Key Views
- Goals: Update the two dashboard pages and the most-visited customer/developer views
- Files: Customer DashboardView, Developer DashboardView, PropertySearchView, WizardView, DeveloperListView, DeveloperDetailView
- Definition of done: Dashboard metric cards and quick actions use warm palette. Search and wizard views have no blue. Developer list and detail use warm colors.

### Sprint 3: All Form + Table Views
- Goals: Update every remaining authenticated view
- Files: All Profile, Suggestion, Property, Order views + Developer PropertyForm, Analytics
- Definition of done: Every authenticated page uses the warm palette consistently. No `#1a73e8` remains in any .vue file.

### Sprint 4: Register Page + Polish
- Goals: Redesign register page to match login, verify all files for consistency
- Files: RegisterView.vue
- Verification: Run grep search for old colors. Visual test of every page. Fix any missed spots.
- Definition of done: Register page matches login page aesthetic. `grep -rn "#1a73e8" frontend/src/` returns zero results in .vue files.

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Missed inline color in script section | Medium | Medium | Use grep to search each old color systematically |
| Element Plus deep selector breakage | High | Low | Test primary button, table, tag, checkbox, radio after change |
| Sidebar contrast too low | Medium | Low | Verify WCAG AA contrast: `#c4b5a5` on `#3d2c1e` passes at 14px |
| Chart colors look worse | Medium | Medium | Review AnalyticsView charts visually after update |
| Mixed old/new colors in same file | Medium | Low | Only mark a file done when grep confirms zero old colors |

## Acceptance Criteria

1. `grep -rn "#1a73e8" frontend/src/views/ frontend/src/layouts/ frontend/src/App.vue` returns zero results
2. `grep -rn "#1f2937" frontend/src/views/ frontend/src/layouts/ frontend/src/App.vue` returns zero results
3. `grep -rn "#f5f7fa" frontend/src/views/ frontend/src/layouts/ frontend/src/App.vue` returns zero results
4. Sidebar renders as warm dark brown `#3d2c1e` with orange `#f5a623` active state
5. All `<el-button type="primary">` render as orange `#f5a623`
6. Page-level background is warm beige `#f5f0ea`
7. Register page visually matches login page design
8. All card shadows use warm brown tint (no generic black shadows)
9. All `#6b7280` secondary text colors replaced with `#8a7a6a`
10. All borders use `#e8ddd0` instead of `#e5e7eb`

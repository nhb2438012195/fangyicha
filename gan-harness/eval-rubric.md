# Evaluation Rubric: 房易AI助手 (FangYi Assistant)

> Evaluator consumes this file directly. Score each criterion on a scale of 0.0 to 1.0. Final score = sum(weight * score). Minimum pass: 0.75.

## Global Anti-Pattern Checks (Auto-Fail if Violated)

If ANY of the following are found, score is 0.0 and generation fails immediately:
- Any `console.log` statements left in production code (excluding error logging)
- Any "TODO", "FIXME", or "HACK" comments indicating incomplete implementation
- Any gradient backgrounds used as chat UI decor, generic sparkle/star/wand icons for the AI assistant
- Any generic "How can I help you today?" or other English-placeholder default messages shown in Chinese app
- Any markdown-rendered text used where structured card components should be (recommendations, favorites, order summaries)
- Any Lorem Ipsum or placeholder text shown to users
- Any functional regressions: existing features (property search, orders, suggestions, wizard, favorites, dashboards) broken or modified unintentionally
- Chat bubble or window fails to appear on authenticated customer pages
- DeepSeek API calls fail due to incorrect base URL or model name configuration

## Evaluation Criteria

### 1. Design Quality (weight: 0.3)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Floating bubble visual polish | 0.15 | Bubble is 56x56px circle with house icon in `#f5a623` on `#f5f0ea`. Hover scale + shadow animation. No generic chat bubble or sparkle icon. Hidden on login/register. `cursor: pointer` hand cursor present. |
| Chat window visual integration | 0.20 | Header uses warm dark brown `#3d2c1e` with orange accent `#f5a623`. Body uses `#fdf8f3` background. User bubbles are orange `#f5a623` with white text. AI bubbles are warm beige `#f5f0ea` with brown text `#4a3728`. Timestamps in `#c4b5a5`. Warm shadow `rgba(180,130,80,0.15)` on window. No blue colors anywhere. |
| Session panel UX | 0.15 | Panel slides from left, 280px wide. "新建对话" button prominent. Session list shows title + relative timestamp. Active session highlighted. Delete on hover. Search filters in real-time. Smooth slide animation. |
| Recommendation card design | 0.15 | Compact 340px cards inside chat bubble. Thumbnail (60x60) or placeholder. Name, location, type, price, match reason pill badge. Click navigates to property detail. Max 3 per response with overflow link. Cards visually match main app card design (same borders, shadows, typography). |
| Knowledge base page design | 0.15 | Upload drop zone with dashed border and hover state. File list with status badges (gray/green/red for uploaded/indexed/error). Preview modal shows parsed text. Empty state with illustration. All colors match warm palette. Layout consistent with other developer management pages. |
| Message states polish | 0.20 | Typing indicator (three orange dots, staggered bounce). Welcome message + quick-action chips in empty session. Error state with retry link. Long message expand/collapse. Scroll-to-bottom button. Orange dot badge for pending order state. |

### 2. Originality (weight: 0.2)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| AI personality implementation | 0.30 | The assistant feels like a warm friend, not a corporate chatbot. System prompt creates this personality. Responses are in conversational Chinese, not formal/robotic. The greeting message uses casual tone. No "powered by" or "AI" labels visible anywhere. |
| Chat-as-overlay integration | 0.25 | The assistant is embedded as a floating overlay, not a separate page. It exists alongside all existing functionality. It does not disrupt the main UI flow. The transition between pages preserves chat state. No route changes to access the assistant. |
| Cards-in-chat paradigm | 0.25 | Structured data (recommendations, favorites, orders) renders as actual card components, not text or markdown. Cards are compact enough to fit in the 380px chat window. Cards are interactive (clickable). This feels natural, not bolted-on. |
| Knowledge base as developer tool | 0.20 | The knowledge base management page feels like a natural part of the developer dashboard, not an afterthought. Upload flow is intuitive. Preview is useful. The connection between uploaded docs and AI responses is clear. Empty state guides the user. |

### 3. Craft (weight: 0.3)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Chat loading and empty states | 0.20 | Typing indicator during AI response. Welcome message in new sessions. Error message with retry. Session list empty state ("还没有对话记录"). Knowledge base empty state. No flash of empty content before data loads. |
| Session persistence | 0.15 | Session survives page navigation. Session survives browser refresh (stored in backend). Session list updates after creating new sessions. Switching sessions loads correct message history. Deleting session removes it from list and clears UI. |
| RAG context accuracy | 0.15 | AI responses reference knowledge base content when relevant. Source attribution visible. Responses that should use RAG context do use it. Responses for out-of-scope questions politely decline. |
| Error handling | 0.15 | DeepSeek API down: friendly error message, no crash. File upload fails: error toast, no lost data. Document parse fails: specific error message per failure type (password, corrupt, size). Network timeout: graceful fallback. |
| Animations and transitions | 0.15 | Bubble open/close: scale + fade (250ms). Session panel slide: 200ms. Message appear: subtle fade-in (150ms). Typing indicator: stagger bounce (1.2s loop). Card hover: lift effect. Knowledge base upload zone hover: border color + bg change. |
| Responsive behavior | 0.10 | Chat window full-width on mobile (<640px). Bubble position adjusts on mobile. Recommendation cards stack well at small widths. Session panel adapts to mobile width. Knowledge base page has responsive table/card layout. |
| Keyboard accessibility | 0.10 | Enter sends message (Shift+Enter for newline). Escape closes chat window. Session list keyboard navigable. Session search input auto-focused when panel opens. Delete button accessible via keyboard. Close (X) button in header keyboard-accessible. |

### 4. Functionality (weight: 0.2)

| Criterion | Weight | What to check |
|-----------|--------|---------------|
| Core chat flow | 0.20 | User opens bubble -> sees welcome message -> types "推荐几个株洲的楼盘" -> AI responds with recommendation cards -> cards are clickable -> navigates to property detail -> bubble still present on detail page. |
| Session management flow | 0.20 | User opens chat -> starts conversation -> new session created -> types more messages -> opens session panel -> sees session with auto-title -> creates new session -> starts fresh conversation -> switches back to previous session -> sees history. |
| Add favorite via AI flow | 0.15 | User types "帮我收藏美的蓝溪谷" -> AI calls add_favorite function -> success message "已收藏美的蓝溪谷" -> user checks favorites page -> property appears in favorites list. |
| View favorites via AI flow | 0.15 | User types "看看我的收藏" -> AI calls view_favorites function -> displays up to 5 favorite cards -> cards are clickable -> navigates to property detail. |
| Create order via AI flow | 0.15 | User types "我想买美的蓝溪谷" -> AI asks for confirmation details -> user provides info -> AI shows order preview card -> user says "确认" -> order created -> AI shows order number. |
| Developer knowledge base flow | 0.15 | Developer logs in -> navigates to knowledge base page -> sees empty state -> uploads a PDF -> file appears with "uploaded" status -> clicks "纳入知识库" -> status changes to "indexed" -> preview shows parsed text -> customer asks question -> AI response references uploaded document. |

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

### Scenario A: Core Chat + Recommendation
1. Login as customer (zhangsan / 123456)
2. Navigate to customer dashboard
3. Verify: floating bubble appears at bottom-right with house icon
4. Click bubble: chat window opens with warm header and welcome message
5. Verify: three quick-action chips visible in welcome message
6. Type "推荐几个株洲天元区的楼盘"
7. Verify: typing indicator shows during API response
8. Verify: AI responds with recommendation cards (max 3)
9. Verify: each card shows thumbnail, name, location, price, match reason badge
10. Click a card: navigates to property detail page
11. Verify: bubble is still present on detail page

### Scenario B: Session Management
1. From existing chat, open session panel (hamburger icon)
2. Verify: panel slides in from left
3. Verify: current session has auto-generated title (truncated to 25 chars)
4. Click "新建对话": panel closes, new empty session opens
5. Type a message, verify it belongs to the new session
6. Open session panel again, switch back to previous session
7. Verify: previous conversation history loads correctly
8. Delete a session: confirmation dialog appears, confirming removes it

### Scenario C: Favorites via AI
1. Type "帮我收藏美的蓝溪谷" in chat
2. Verify: AI confirms "已收藏美的蓝溪谷"
3. Navigate to "我的收藏" page (`/customer/favorites`)
4. Verify: 美的蓝溪谷 appears in favorites list
5. Return to chat, type "看看我的收藏"
6. Verify: AI shows up to 5 favorite cards
7. Verify: cards are clickable and navigate to property detail

### Scenario D: Create Order via AI
1. Type "我想买美的蓝溪谷" in chat
2. Verify: AI asks for confirmation or shows preview
3. If asked for details, provide them (or verify AI uses profile info)
4. Verify: order summary card appears with property details, price, customer info
5. Type "确认"
6. Verify: AI confirms order created, shows order number
7. Check orders page (`/customer/orders`): new order appears with correct data

### Scenario E: Developer Knowledge Base
1. Login as developer (bgy_admin / 123456)
2. Navigate to `/developer/knowledge-base`
3. Verify: empty state with upload guidance
4. Upload a PDF document
5. Verify: file appears in list with "uploaded" status
6. Click "纳入知识库"
7. Verify: status changes to "indexed"
8. Click preview: modal shows extracted text (first 500 chars)
9. Login as customer (zhangsan / 123456)
10. Ask a question related to the uploaded document content
11. Verify: AI response references the uploaded document

### Scenario F: Edge Cases
1. Send empty message: verify send button is disabled when input empty
2. Open chat on mobile viewport (<640px): verify full-width layout
3. Disable network, send a message: verify error state with retry option
4. Navigate to login page: verify bubble is hidden
5. Verify: chat state persists when navigating between customer pages
6. Test keyboard: Enter sends, Escape closes window
7. Verify: existing features (property search, wizard, suggestions, orders) are not broken

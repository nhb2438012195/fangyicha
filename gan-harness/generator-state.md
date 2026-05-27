# Generator State — Iteration 001

## What Was Built

### Sprint 1: Core Infrastructure + Chat Foundation
- Backend: Spring Boot 3.2 + Spring AI 1.0.0-M6 with DeepSeek API integration
- Backend: AI session management (CRUD with cascade delete)
- Backend: AI message persistence (up to 500 msgs/session)
- Backend: Apache Lucene 9.12 RAG engine with BM25 + vector hybrid search
- Backend: Apache Tika 2.9.2 document parsing (PDF, DOC, DOCX)
- Backend: DeepSeek embedding via direct REST API (1024-dim vectors)
- Backend: Knowledge base seeder (exports properties on startup, rebuilds Lucene index)
- Frontend: FloatingBubble component (56px, house icon, z-index 9999)
- Frontend: ChatWindow component (380x560px, warm palette, all states)
- Frontend: SessionPanel component (280px slide-in, search, delete)
- Frontend: Pinia chat store with sessions, messages, loading states

### Sprint 2: Session UI + Knowledge Base + Embeddings
- Frontend: SessionPanel with search, relative timestamps, new/delete/switch
- Backend: RAG context integration (5 chunks, hybrid BM25+vector scoring)
- Backend: DeepSeek embeddings cached via SHA-256 hash in data/emb-cache/
- Backend: DocumentParserService with Tika (10MB limit, error messages for protected/broken files)
- Backend: KnowledgeBaseService with upload/index/delete/preview pipeline

### Sprint 3: AI Operations + Rich Cards
- Frontend: RecommendCard component (thumbnail, name, location, price, reason badge)
- Frontend: OrderSummaryCard component (key-value rows, warm beige + orange border)
- Frontend: Card rendering for recommendations, favorites, and order summaries in chat
- Backend: FunctionCallService with 4 tools (add_favorite, view_favorites, create_order_preview, confirm_order)
- Backend: Text-pattern based intent detection in AiChatService
- Backend: PendingOrder table with two-phase order flow (preview + confirm)

### Sprint 4: Polish + Edge Cases
- ChatWindow: Welcome message with 3 quick-action chips
- ChatWindow: Typing indicator (3 bouncing dots)
- ChatWindow: Error state with retry button
- ChatWindow: Long message expand/collapse (>500 chars)
- ChatWindow: Scroll-to-bottom button
- ChatWindow: Escape to close, Enter to send
- Mobile responsive: full-width on small screens
- App.vue: Conditional rendering of FloatingBubble + ChatWindow for authenticated customers

### Developer Knowledge Base Management
- Frontend: KnowledgeBaseView.vue with drag-drop upload zone (dashed border, hover highlight)
- Frontend: File list table with status badges (uploaded/indexed/error)
- Frontend: Preview dialog with loading state
- Backend: KnowledgeBaseController with upload/index/delete/preview endpoints
- Router: /developer/knowledge-base route added
- Sidebar: "知识库管理" menu item with Notebook icon

## What Changed This Iteration
- Initial implementation of all 4 sprints from the spec
- Added types/ai.ts for AI-specific TypeScript interfaces
- Re-exported AI types from types/index.ts for backward compatibility
- Fixed all TypeScript build errors including pre-existing ones
- Vite proxy configured for /api -> localhost:8088

## Known Issues
- DeepSeek API key must be set via DEEPSEEK_API_KEY environment variable (default: sk-placeholder)
- Spring Security returns 403 on API endpoints without JWT token (expected - frontend handles auth)
- RAG vector search falls back to BM25-only when embedding extraction fails (Lucene 9.12 FloatVectorValues API)
- Function calling uses text-pattern matching rather than DeepSeek native function calling
- Port 3000/3001 were in use, dev server running on port 3002

## Dev Server
- Frontend URL: http://localhost:3002
- Backend URL: http://localhost:8088
- Status: both running
- Frontend command: npm run dev
- Backend command: mvn spring-boot:run

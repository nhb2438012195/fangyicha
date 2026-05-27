# Product Specification: 房易AI助手 (FangYi Assistant)

> Generated from brief: "AI 购房助手 — 基于 DeepSeek + RAG 的智能对话式购房辅助系统"
> Requirements source: `docs/superpowers/specs/2026-05-26-ai-assistant-requirements.md`

## Vision

房易AI助手 (FangYi Assistant) transforms the 房易查 platform from a tool-you-search into a platform-that-advises. It is a conversational AI assistant embedded directly into every customer page, accessible via a floating bubble. It answers questions about properties, remembers user preferences, recommends matching listings, manages favorites, and assists with order creation -- all through natural dialogue. The assistant feels like a knowledgeable friend helping you find your dream home, not a corporate chatbot.

The system uses a RAG (Retrieval-Augmented Generation) architecture powered by DeepSeek API for both chat and embeddings, Apache Lucene for hybrid BM25 + vector search over a knowledge base of platform data, real estate domain knowledge, and developer-uploaded documents. A dedicated developer knowledge base management page allows developers to upload, delete, and preview documents that enrich the assistant's responses.

## Design Direction

- **Color palette**: Matches the existing warm residential palette. Floating bubble uses orange primary `#f5a623` with warm white `#fdf8f3` interior. Chat window header uses warm dark brown `#3d2c1e` with orange accent `#f5a623`. User message bubbles use orange `#f5a623` with white text `#ffffff`. AI message bubbles use warm beige `#f5f0ea` background with brown text `#4a3728`. Timestamps use muted brown `#c4b5a5`. Session list items hover at `rgba(180,130,80,0.08)`. Knowledge base page uses the same palette as the existing developer dashboard (card bg `#fdf8f3`, borders `#e8ddd0`).

- **Typography**: Chat messages use system font stack at 14px body. AI assistant name "房易小助手" in semi-bold 14px `#f5a623`. User name label in semi-bold 14px `#4a3728`. Message text at 14px `#4a3728` with 1.6 line-height. Property cards embedded in chat use the same card-level typography as the main app (13px secondary `#8a7a6a`, 16px title `#4a3728`). Session title in 14px `#4a3728`, session timestamp in 12px `#c4b5a5`. Knowledge base file names in 14px `#4a3728`, file metadata in 12px `#8a7a6a`.

- **Layout philosophy**: The chat interface is a floating overlay, not a page. Fixed positioning at bottom-right, highest z-index. The bubble is a 56x56px circle. The expanded window is 380px wide by 560px tall on desktop, full-width on mobile (< 640px). The chat window contains three zones: header (session info + close), body (scrollable message list), footer (input + send). Sessions panel slides in from the left within the window. Property recommendation cards embedded in chat are compact (320px wide card within the 380px window). Knowledge base management is a full-page view in the developer dashboard, using the same layout as existing management pages.

- **Visual identity**: The assistant distinguishes itself from "AI-slop chatbots" through warm, residential-branded materials. The floating bubble has a small house icon inside the circle (not a generic chat bubble or sparkle). The AI avatar in messages is a warm orange-toned circle with a house silhouette. Empty states show a friendly illustration of a house with a smile (hand-drawn style, not stock). The typing indicator is three animated dots in orange `#f5a623`. The chat window has subtle warm shadow `rgba(180,130,80,0.15)`.

- **Anti-AI-slop directives**: No sparkle/star/wand icons for the AI (replace with house). No gradient backgrounds on the chat window. No "AI-powered" badges or labels. No robotic "How can I help you today?" defaults -- use warm, conversational openers like "想了解哪个楼盘？随便问我吧！" No loading spinners -- use skeleton-style message bubbles or the custom typing indicator. No generic bot avatar -- always the house-in-circle. No separate "AI" page -- it is always a floating overlay, never a navigated-to route. No markdown-rendered responses -- all structured data (recommendations, favorites, orders) renders as actual card components, not formatted text. No streaming text cursor animation. No "clear conversation" confirmation dialogs with destructive red buttons.

- **Inspiration**: Line customer service chat widget (for the floating bubble pattern), ChatGPT sidebar session list (for multi-session UX), Apple Messages (for bubble alignment and clean input area), Notion AI (for how it embeds structured data inline in chat).

## Features (prioritized)

### Must-Have (Sprint 1-2: Core Chat Infrastructure + RAG)

1. **Floating Bubble Entry Point**: A 56x56px circular button fixed at bottom-right corner of the viewport (z-index: 9999). Shows only when user is logged in (hidden on login/register pages). Displays a house icon in orange `#f5a623` on warm beige `#f5f0ea` background. Click toggles the chat window. Smooth scale+fade transition (200ms cubic-bezier(0.4, 0, 0.2, 1)). On mobile viewports (< 640px), the bubble positions 16px from bottom and right edges. Hover state shows a subtle scale(1.05) with shadow `rgba(180,130,80,0.2)`.
   - Acceptance: Bubble renders on all authenticated customer pages. Bubble is hidden on login/register pages. Clicking opens the chat window. House icon is visible. The bubble has `cursor: pointer` hand cursor.

2. **Chat Window Overlay**: A fixed-position panel (380px x 560px) anchored to the bottom-right corner with 16px spacing from the floating bubble. Three zones: (a) **Header** -- 48px tall, warm dark brown `#3d2c1e` background, left side shows session menu icon (hamburger) + "房易小助手" title in white 14px semi-bold, right side shows close (X) icon in `#c4b5a5` hover `#ffffff`. (b) **Body** -- flex-grow, scrollable, `#fdf8f3` background, 12px horizontal padding, 8px gap between messages. Auto-scrolls to bottom on new messages. (c) **Footer** -- 56px tall, 12px horizontal padding, contains a text input (no border, 14px, placeholder "输入问题...") and a send button (orange `#f5a623` circle, 36x36px, white arrow icon, disabled when input empty). Window opens with a scale(0.9) to scale(1) + fade transition (250ms). On mobile, window is full-width, full-height minus 80px, positioned at bottom.
   - Acceptance: Window opens/closes with animation. Three zones visible and functional. Text input captures keyboard input. Send button activates only with non-empty text. Scroll works. Auto-scroll on new messages.

3. **Session Management Backend (MySQL)**: Four new database tables — `ai_session`, `ai_message`, `pending_order`, and `knowledge_documents`. `ai_session` schema: `id BIGINT PK AUTO_INCREMENT`, `customer_id BIGINT NOT NULL`, `title VARCHAR(100)` (auto-generated from first user message, max 30 chars), `created_time DATETIME`, `updated_time DATETIME`. `ai_message` schema: `id BIGINT PK AUTO_INCREMENT`, `session_id BIGINT NOT NULL`, `role VARCHAR(10) NOT NULL` ('user' or 'assistant'), `content TEXT NOT NULL`, `message_type VARCHAR(20) DEFAULT 'text'` ('text', 'recommendation', 'favorites', 'order_summary'), `metadata JSON` (optional structured data for card rendering), `created_time DATETIME`. Index on `(session_id, created_time)`. Maximum 500 messages per session (stop accepting at limit). `pending_order` schema: `id BIGINT PK AUTO_INCREMENT`, `session_id BIGINT NOT NULL`, `customer_id BIGINT`, `property_id BIGINT`, `customer_name VARCHAR(50)`, `customer_phone VARCHAR(20)`, `status VARCHAR(20) DEFAULT 'pending'` ('pending'|'confirmed'|'cancelled'), `created_time DATETIME`, `confirmed_time DATETIME`. Unique constraint on `(session_id, status)` where status='pending'. `knowledge_documents` schema: `id BIGINT PK AUTO_INCREMENT`, `developer_id BIGINT NOT NULL`, `filename VARCHAR(255)` (original name), `stored_filename VARCHAR(255)` (name in `data/rag-uploads/`), `file_size BIGINT` (bytes), `status VARCHAR(20) DEFAULT 'uploaded'` ('uploaded'|'indexed'|'error'), `error_message VARCHAR(500)`, `uploaded_time DATETIME`, `indexed_time DATETIME`. REST endpoints: `POST /api/ai/sessions` (create), `GET /api/ai/sessions` (list, sorted by updated_time DESC, max 50), `DELETE /api/ai/sessions/{id}` (cascade delete messages), `GET /api/ai/sessions/{id}/messages` (paginated, 50 per page), `POST /api/ai/sessions/{id}/messages` (send message + get AI reply).
   - Acceptance: Tables are created in schema.sql. All endpoints return proper JSON responses. Session deletion cascade-deletes messages. Pagination works. Title auto-generated from first user message content (truncated to 30 chars with "...").

4. **Spring AI Integration with DeepSeek**: Add Spring AI dependency (`spring-ai-openai-spring-boot-starter`). Configure in `application.yml`: base-url set to `https://api.deepseek.com/v1`, api key from `DEEPSEEK_API_KEY` environment variable, model `deepseek-chat`. Create `AiChatService` bean that wraps `ChatClient` with system prompt: "你是一个亲切友好的购房助手，名字叫房易小助手。你是房易查平台的AI助手，帮助用户查询房产信息、推荐楼盘、管理收藏和创建订单。用口语化的中文回答，温暖亲切，像朋友帮你参谋购房。回答基于提供的知识库内容，如果知识库中没有相关信息，请告知用户无法回答。" Temperature set to 0.7. Max tokens 1024. HTTP POST, non-streaming. All responses returned as complete JSON. Conversation history injected as messages (last 3 rounds = 6 messages max, ordered oldest to newest).
   - Acceptance: Spring AI configured with DeepSeek endpoint. System prompt sent with every request. Conversation history includes exactly the last 3 user + 3 assistant messages. API errors caught and returned as user-friendly messages. No streaming implementation.

5. **Lucene RAG Engine + Platform Data Export**: Add Apache Lucene dependencies (core, queryparser, analysis/common). Create `RagService` with a unified Lucene index in `data/lucene-index/`.

     **Platform data export (startup, before index build):**
     A `KnowledgeBaseSeeder` bean runs on `ApplicationReadyEvent`. It queries the `property` and `developer` tables and generates plain-text documents into `data/rag-platform/`:
     - For each property: `"楼盘名称：{name}，位置：{location}，户型：{floorPlan}，面积：{area}平米，价格：{totalPrice}万元，开发商：{developerName}，描述：{description}"`
     - For each developer: `"开发商：{name}，所在地：{location}，简介：{description}"`
     - Each file named `property_{id}.txt` or `developer_{id}.txt`. This ensures platform data in MySQL is always reflected in the knowledge base.

     **Index build (startup):**
     After seeding, build the platform data index. Use `OpenMode.CREATE` for platform data — rebuild from scratch on every startup to avoid stale duplicates (filenames are deterministic, so a rebuild is equivalent to an upsert). Use Lucene's StandardAnalyzer. Index fields: `id` (filename), `content` (text), `source` (string: 'platform' or 'upload').

     **Uploaded documents (runtime, two-phase):**
     Phase 1 — Upload: Developer uploads a document via the knowledge base page. The file is stored as-is in `data/rag-uploads/` and a metadata record is written to `knowledge_documents` table (status='uploaded'). No parsing or indexing occurs yet.

     Phase 2 — Include in knowledge base: Developer clicks "纳入知识库" on an uploaded file. This triggers the full pipeline: Tika parse → 300-char chunking with 50-char overlap → DeepSeek embedding for each chunk → embed cache `.emb` files → Lucene incremental index (source='upload'). On success, status updates to 'indexed'. On failure, status updates to 'error' with error message. Deleting a file removes it from both `data/rag-uploads/` storage and the Lucene index.

     **Document chunking:**
     Split text into chunks of 300 characters with **50 characters overlap** between adjacent chunks to prevent semantic fragmentation at segment boundaries. Each chunk indexed separately with a chunk index field.

     **Hybrid retrieval (equal weight):**
     (1) Perform BM25 keyword search, retrieving top 20 candidates by keyword relevance.
     (2) Generate embedding vector for the user query via DeepSeek Embedding API (model `deepseek-embedding`, dimension 1024).
     (3) For each BM25 candidate, compute vector cosine similarity against the query embedding.
     (4) Compute final score: BM25 score (normalized 0-1) * 0.5 + vector cosine similarity * 0.5.
     (5) Sort by final score descending, return top 5 chunks.
     (6) Minimum final score threshold of 0.15. If no chunks exceed threshold, return empty results (don't force results).
   - Acceptance: `data/rag-platform/` is populated with .txt files from MySQL on startup. Platform index is rebuilt fresh each startup (no stale duplicates). `RagService.search(query)` returns top 5 results with content and source. Chunking uses 300 chars + 50 char overlap. Empty query returns empty results. DeepSeek Embedding API called with correct model name and dimension 1024.

6. **Apache Tika Document Parsing**: Add Apache Tika dependency (`tika-core`, `tika-parsers-standard-package`). Create `DocumentParserService` with a single method: `parseDocument(InputStream input, String filename) -> String`. Detect file type by extension (.pdf, .docx, .doc). For PDFs, use `AutoDetectParser`. For Word docs, use `AutoDetectParser` (Tika auto-detects). Extract text content. Handle password-protected documents by returning an error message "该文档受密码保护，无法解析". Handle corrupt documents by returning "文档格式错误，请重新上传". Handle documents over 10MB by returning "文档过大，请上传10MB以内的文件". Return extracted text as a plain string (no formatting/markup preservation needed).
   - Acceptance: PDF files parse to plain text. Word files (.docx, .doc) parse to plain text. Password-protected files return error message. Corrupt files return error message. Files over 10MB return size error. Chunking at 300 chars happens after parsing, before indexing.

7. **Chat API Endpoint + Function Calling Loop**: `POST /api/ai/chat` endpoint in new `AiController`. Request body: `{ sessionId (optional, null = new session), message (string) }`. Response: `{ sessionId, reply (object with type and content fields for cards or text) }`.

     **Request flow:**
     (1) Find or create session. If sessionId is null, create new session with auto-title from first 30 chars of user message. (2) Save user message to `ai_message` table. (3) Load last 3 rounds (6 messages max) from this session. (4) Call RAG search with user message as query. (5) Build Spring AI prompt with system prompt + conversation history + RAG context + function definitions. (6) Call DeepSeek API (HTTP POST, non-streaming). (7) Parse response — if `finish_reason` is `tool_calls`, enter **Function Calling loop**. If `finish_reason` is `stop`, skip to step 8.

     **Function Calling loop** (max 5 iterations to prevent infinite loops):
     (a) Extract function name and arguments from DeepSeek response. (b) Execute the function locally via `FunctionCallService`:
       - `add_favorite(propertyId)` → calls `FavoriteService.toggle()`, returns confirmation text
       - `view_favorites(filter?)` → calls `FavoriteService.list(customerId)`, filters, returns top 5 as cards
       - `create_order_preview(propertyId, customerName, customerPhone)` → returns order summary object (no DB write)
       - `confirm_order(sessionId)` → reads pending preview from database, creates real order, returns order number
     (c) Append the function result as a `tool` role message to the conversation. (d) Call DeepSeek API again with updated messages. (e) If response `finish_reason` is `stop`, exit loop. If another `tool_calls`, repeat from (a). (f) If loop exceeds 5 iterations, break and return error.

     (8) Save assistant's final text response + any structured card data to `ai_message` table (role='assistant', message_type based on content, metadata JSON stores card data). (9) Return response.

     **Configuration:** Timeout: 30 seconds. Max function call iterations: 5. On timeout, return "AI 助手暂时忙不过来，请稍后再试". On DeepSeek API error, return "AI 助手暂时不可用，请稍后重试".
   - Acceptance: Endpoint accepts POST requests. New session created when sessionId is null. Messages saved to database. Function calling loop executes at most 5 iterations. Response contains reply text and optional card data. Timeout and error cases handled. Empty input returns "请输入问题".

### Should-Have (Sprint 3-4: AI Capabilities + Session UI)

8. **Multi-Session Management UI**: In the chat window header, tapping the hamburger icon slides in a session panel from the left (overlay, 280px wide, same height as chat window, warm beige `#f5f0ea` background, z-index above chat body). Panel contains: (a) A "新建对话" button at top (orange `#f5a623` button, full-width, rounded). (b) A search input (14px, placeholder "搜索历史对话...", with clear icon). (c) A scrollable session list showing max 50 sessions. Each session item shows: auto-generated title (truncated to 25 chars), timestamp (relative: "刚刚"/"5分钟前"/"1小时前"/"昨天"/"周一"/"2026-05-20"), a delete button (trash icon, appears on hover, `#e85c41` on hover). Clicking a session item closes the panel and loads that session's messages. The active session is highlighted with warm beige background `rgba(245,166,35,0.1)`. Session search filters by title client-side (instant filter, no API call). Panel slides in with 200ms ease-out, slides out with 150ms ease-in.
   - Acceptance: Session panel opens/closes with animation. New session button creates and switches to a fresh session. Session list shows all sessions for current user. Search filters sessions in real-time. Delete removes session with confirmation ("确认删除该对话？"). Active session is visually distinct. Panel closes on session selection or clicking outside.

9. **Property Recommendation Cards in Chat**: When the assistant identifies a recommendation intent (e.g., user says "推荐几个楼盘" or "有什么三室的房子"), the backend enriches the response with structured card data.

     **Data source — RAG-based property lookup:**
     RAG search returns chunks whose Lucene `id` field encodes the source. Platform data chunks have IDs like `property_123.txt` or `developer_5.txt`. Uploaded document chunks have their stored filename as the ID. After DeepSeek generates its text response, the backend runs a post-processing step:
     (1) Extract all `property_*` IDs from the RAG top-5 results that contributed to this response.
     (2) For each extracted property ID, query the `property` table for full structured data (name, location, floorPlan, area, totalPrice, imageUrls).
     (3) For upload-document chunks (non-property IDs), parse the AI response text for known property names via `LIKE` match against the `property` table — if matched, also include them.
     (4) Build a `recommendation` card array from the collected properties (deduplicated, max 3). Store the card array in `ai_message.metadata` as JSON.

     **Card rendering:** Each card renders as a compact 340px-wide card within the chat message bubble: left side shows a 60x60px property thumbnail (or placeholder house icon if no image), right side shows property name (14px semi-bold `#4a3728`), location (12px `#8a7a6a`), floor plan type + area (12px `#8a7a6a`), price (14px bold `#f5a623` with "¥" prefix), and a match reason label (small pill badge, 11px, `#f5a623` text on `#fef7ed` bg, e.g., "符合预算" "偏好区域"). Each card is `cursor: pointer` and clicking navigates to `/customer/properties/{id}`. Cards are arranged vertically in the message bubble (max 3 per response). If more than 3 matches, show "还有 N 个匹配楼盘" link at bottom.
   - Acceptance: Recommendation cards render inside AI message bubbles. Cards show thumbnail, name, location, type, price, and match reason. Click navigates to property detail. Max 3 cards per message. Overflow shows count link.

10. **Function Calling: Add Favorite + View Favorites**: DeepSeek function definitions registered with the chat completion. Two tools defined using DeepSeek's function calling format (OpenAI-compatible): (1) `add_favorite` with parameter `{ "propertyId": "number" }`. When the function is called, the backend resolves the property name to an ID using a three-step lookup: (i) First, SQL LIKE match on property name in the `property` table. (ii) If no result, fall back to RAG semantic search to find the closest matching property. (iii) If still ambiguous or no result, return a clarification prompt to the user ("你是指哪个楼盘呢？"). On successful match, call `FavoriteService.toggle(customerId, propertyId)`, and the Function Calling loop feeds the result back to DeepSeek to generate a confirmation message like "已收藏 [property_name]！可以在「我的收藏」中查看". (2) `view_favorites` with optional parameter `{ "filter": "string" }` (natural language condition). When called (e.g., "看看我的收藏" or "帮我看看朝阳区的收藏"), backend calls `FavoriteService.getFavorites()`, optionally filters client-side for simple conditions (price range, location keyword), returns up to 5 most recent favorites as a card list in the message. Favorite cards match the same visual design as recommendation cards. If no favorites exist, return "你还没有收藏任何楼盘哦，试试搜索你感兴趣的楼盘吧！"
    - Acceptance: Function definitions are registered with DeepSeek chat. `add_favorite` correctly identifies property by name or ID. `view_favorites` returns up to 5 favorites as cards. Filter parameters parsed from natural language where possible. Error messages shown for ambiguous property names. Cards match recommendation card design.

11. **Function Calling: Create Order (Two-Phase with State Persistence)**:

     **Two functions registered with DeepSeek:**
     - `create_order_preview(propertyId, customerName, customerPhone)` — no side effects
     - `confirm_order(sessionId)` — creates real order

     **Pending state persistence:** A `pending_order` table stores the preview state between the two phases:
     - Columns: `id BIGINT PK`, `session_id BIGINT`, `customer_id BIGINT`, `property_id BIGINT`, `customer_name VARCHAR(50)`, `customer_phone VARCHAR(20)`, `status VARCHAR(20) DEFAULT 'pending'` ('pending' | 'confirmed' | 'cancelled'), `created_time DATETIME`, `confirmed_time DATETIME`
     - Only one pending order per session (unique constraint on session_id + status='pending')

     **Flow:**
     (1) AI detects user intent to order (e.g., "我想买这个房子" or "帮我下单").
     (2) AI asks clarifying questions if needed (property name → resolved via same 3-step lookup as add_favorite; customer name, phone from user profile if available, otherwise ask).
     (3) AI calls `create_order_preview` function. `FunctionCallService` resolves propertyId, writes a row to `pending_order` table (status='pending'), returns an order summary object: `{ propertyId, propertyName, price, area, floorPlan, customerName, customerPhone, previewId }`.
     (4) The function result goes back to DeepSeek via the Function Calling loop, which generates an order summary card in chat.
     (5) AI asks user to confirm ("请确认订单信息，确认后我将为你提交").
     (6) On user confirmation ("确认" "是的" "提交"), AI calls `confirm_order(sessionId)`. `FunctionCallService` reads the pending order from `pending_order` table (WHERE session_id = ? AND status = 'pending'), creates the actual order in the `order` table, updates pending status to 'confirmed', and returns the order number.
     (7) AI displays success message with order number.
     (8) If user cancels ("不买了" "算了"), AI updates pending status to 'cancelled' and acknowledges.
     (9) If chat is closed/reopened during pending state, Feature #16 handles badge + reminder.

     Order summary card visual style: warm beige `#fef7ed` background, 2px border `#f5a623`, key-value pairs in two columns.
    - Acceptance: Order preview writes to `pending_order` table without creating an actual order. `confirm_order` reads from `pending_order` and creates the real order. Pending state persists across page navigation and chat close/reopen. Same session cannot have two pending orders. Cancellation clears pending state. Missing info prompted before preview.

12. **Developer Knowledge Base Management Page + Backend Pipeline**: New route under developer layout: `/developer/knowledge-base` (title "知识库管理", icon "Notebook").

     **Frontend:**
     (a) Upload zone at top — a dashed-border drop area (border `#f5a623`, bg `#fef7ed` on hover, 2px border-radius) with text "拖拽文档到此处或点击上传" and supported format badges (PDF, DOC, DOCX). Click opens a file picker limited to .pdf, .doc, .docx. Max file size 10MB. Upload stores the file in staging only (does NOT index yet). (b) File list below — a table showing: file name, file size (KB/MB), upload time, status, actions. Three actions per row:
       - "纳入知识库" button (for 'uploaded' files) — triggers indexing pipeline
       - "预览" — opens modal showing parsed text content (if already indexed) or raw filename (if not yet indexed)
       - "删除" — removes file from storage and, if indexed, from Lucene index
     (c) Status badges: uploaded = gray `#c4b5a5`, indexed = green `#5ea84f`, error = red `#e85c41`. (d) Empty state: "还没有上传文档，上传后点击「纳入知识库」让AI助手学习文档内容"。

     **Backend pipeline (triggered by "纳入知识库"):**
     The `POST /api/ai/knowledge-base/{fileId}/index` endpoint executes the full processing chain:
     (1) Read file metadata from `knowledge_documents` table by fileId. (2) Read file bytes from `data/rag-uploads/{stored_filename}`. (3) Apache Tika parses → plain text. (4) Chunk: 300 chars + 50 char overlap per chunk. (5) For each chunk, generate embedding via DeepSeek Embedding API and cache to `data/emb-cache/{sha256}.emb`. (6) Index all chunks into Lucene (source='upload'). (7) Update `knowledge_documents.status='indexed'` and `indexed_time=NOW()`. On any failure, status='error' with error_message.

     **API endpoints:**
     | Endpoint | Method | Description |
     |----------|--------|-------------|
     | /api/ai/knowledge-base/upload | POST | Upload file (multipart/form-data). Saves to `data/rag-uploads/`, creates `knowledge_documents` record with status='uploaded' |
     | /api/ai/knowledge-base/files | GET | List all documents for current developer (from `knowledge_documents` table) |
     | /api/ai/knowledge-base/{fileId}/index | POST | Trigger indexing pipeline for a file (parse → chunk → embed → index) |
     | /api/ai/knowledge-base/{fileId} | DELETE | Delete file from storage + remove from Lucene index + delete `knowledge_documents` record |
    - Acceptance: Drag-and-drop upload stores file with status 'uploaded'. File list shows all uploaded files. "纳入知识库" triggers full pipeline and status changes to 'indexed'. Delete removes file from storage and index. Preview shows parsed text for indexed files. Empty state shown when no files. Error state shows error_message in UI.

13. **AI Chat with RAG Context**: Integration between chat flow and RAG engine. Before sending the prompt to DeepSeek, the backend calls `RagService.search(userMessage)` and injects the top 5 results as context. The system prompt is extended with: "以下是与用户问题相关的知识库内容，请基于这些内容回答。如果知识库内容不足以回答，请告知用户。" followed by the concatenated search results. Each result prefixed with `[来源: {filename}]`. Total RAG context limited to 2000 characters to keep prompt within token limits. If RAG returns zero results, the system prompt omits the RAG context and the assistant responds based on general knowledge only (but constrained to real estate domain).
    - Acceptance: RAG search is called before every chat completion. Top 5 results are injected into the prompt. Source attribution is included. 2000-char limit on RAG context is enforced. Zero-result case handled gracefully.

14. **Embedding Generation + Lucene Vector Indexing**: DeepSeek Embedding API client integrated into `RagService`. On document index, generate embedding vectors via `POST https://api.deepseek.com/v1/embeddings` with model `deepseek-embedding`. Store embeddings in Lucene's `KnnFloatVectorField` (dimension 1024). On search, generate embedding for the query and perform `KnnFloatVectorQuery` with 10 candidates. Combine with BM25 results using weighted scoring (BM25 0.5, vector 0.5).

     **Embedding cache:** To avoid re-embedding on every startup, cache each chunk's embedding vector in `data/emb-cache/`. File naming: `{sha256_of_chunk_content}.emb` — the SHA-256 hash of the chunk text serves as both the cache key and filename. Each `.emb` file contains the raw 1024-dim float32 vector (4KB). On startup, for each chunk, compute its SHA-256 hash; if the corresponding `.emb` file exists in `data/emb-cache/`, load it instead of calling the API. If the chunk content changed (new hash), the old `.emb` is orphaned and a new one is created. For upload documents, new chunks also get their `.emb` files written during indexing.
    - Acceptance: Embeddings are generated and stored during indexing. `.emb` files are created per chunk in `data/emb-cache/`, named by SHA-256 hash of chunk content. On restart, cached embeddings are loaded from file when hash matches. Hybrid search returns combined BM25 + vector results. Weighted scoring formula is correct. Embedding API calls use the correct model `deepseek-embedding`.

### Nice-to-Have (Sprint 5+: Polish + Advanced)

15. **Chat UI Polish + States**: Loading state: when AI is responding, the last message shows a typing indicator (three orange dots with staggered bounce animation, 1.2s loop). Empty state: first message in a new session shows a welcome message from "房易小助手": "你好！我是房易小助手，可以帮你查找楼盘、推荐合适的房子、管理收藏，甚至帮你下单。想先了解什么？" with three quick-action suggestion chips below the welcome message: "推荐几个楼盘" "看看我的收藏" "三室两厅有什么选择" (chips are 14px, `#f5a623` text on `#fef7ed` bg, rounded-pill, clickable). Error state: if the API call fails, show a red-tinted message bubble with text "消息发送失败" and a "重试" link (orange `#f5a623`). Long message handling: messages longer than 500 chars get a "展开全文" / "收起" toggle. Message timestamp: show on hover or for the last message (12px `#c4b5a5`, right-aligned). Scroll-to-bottom button: appears as a floating 32px circle with down arrow when scrolled up more than 100px.
    - Acceptance: Typing indicator shows during API call. Welcome message + quick action chips appear in empty sessions. Error state shows retry option. Long messages are collapsible. Timestamps visible. Scroll-to-bottom button appears and works.

16. **Order Confirmation Flow Polish**: If user navigates away from the chat during an active order creation flow (i.e., after preview but before confirm), the session preserves the state. Reopening the chat shows the pending order preview card. A subtle badge on the floating bubble (a small orange dot, 8px, top-right of the bubble) indicates there is an unread or pending state. The badge clears when the user completes or cancels the order flow. On session switch, pending order state is preserved per-session. If the user has been inactive for more than 5 minutes during an order flow, show a soft reminder "您的订单尚未确认，还需要我帮忙吗？"
    - Acceptance: Order preview persists across chat close/reopen. Orange dot badge appears during pending order flow. Session switch preserves per-session order state. Inactivity reminder shown after 5 min. Badge clears on completion or cancellation.

## Technical Stack

- **Frontend**: Vue 3.5 with TypeScript 6, Element Plus 2.14, Pinia 3, Axios, Vite 8. Custom chat UI components (not Element Plus chat components -- they don't exist). Floating overlay managed via a new `useChatStore` Pinia store. Session management via `useSessionStore`. Chat components in `frontend/src/views/customer/components/chat/` directory.
- **Backend**: Spring Boot 3.2 with Java 17. Spring AI for LLM integration (`spring-ai-openai-spring-boot-starter`). Apache Lucene 9.x for vector + BM25 hybrid search. Apache Tika 2.x for document parsing. MyBatis-Plus for session/message persistence. JWT auth (existing). MySQL 8 for storage.
- **Key libraries**: `spring-ai-openai-spring-boot-starter` (LLM), `lucene-core`, `lucene-queryparser`, `lucene-analysis-common`, `lucene-queries` (search), `tika-core`, `tika-parsers-standard-package` (document parsing). File I/O uses `java.nio.file`.
- **Data storage**: MySQL (session + message tables), local filesystem (`data/rag-platform/` + `data/rag-uploads/`), Lucene index files (in `data/lucene-index/`), embedding cache files (`data/emb-cache/`).
- **API Key management**: `DEEPSEEK_API_KEY` environment variable, read at application startup, verified with a simple health check (`GET https://api.deepseek.com/v1/models` on startup), logs warning if not set or invalid.

## Evaluation Criteria

> Evaluator consumes this section directly for scoring. Score each criterion on a scale of 0.0 to 1.0. Final score = sum(weight * score). Minimum pass: 0.75.

### Global Anti-Pattern Checks (Auto-Fail if Violated)

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
| Animations and transitions | 0.15 | Bubble open/close: scale + fade (250ms). Session panel slide: 200ms. Message appear: subtle fade-in (150ms). Typing indicator: stagger bounce (1.2s loop). Card hover: lift effect. Knowlege base upload zone hover: border color + bg change. |
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

## Sprint Plan

### Sprint 1: Core Infrastructure + Chat Foundation
- **Goals**: Establish backend AI infrastructure, RAG engine, and basic chat UI
- **Features**: #1 (Floating Bubble), #2 (Chat Window), #3 (Session Management Backend), #4 (Spring AI Integration), #5 (Lucene RAG Engine), #6 (Apache Tika Parsing), #7 (Chat API Endpoint)
- **Definition of done**: Floating bubble appears on all customer pages. Chat window opens and accepts text input. Session and message tables exist in MySQL. Spring AI successfully connects to DeepSeek API and returns responses. RAG engine indexes `data/rag-platform/` files and returns search results. Tika parses PDF and Word documents. Chat endpoint receives messages, queries RAG, calls DeepSeek, and returns replies. Session persistence works across refreshes.

### Sprint 2: Session UI + Knowledge Base + Embeddings
- **Goals**: Build the session management UI, developer knowledge base page, and implement embedding-based RAG
- **Features**: #8 (Multi-Session UI), #12 (Developer Knowledge Base), #14 (DeepSeek Embeddings + Vector Indexing), #13 (RAG Context Integration)
- **Definition of done**: Session panel opens/closes with animation. New session creation, switching, deletion, and search work. Knowledge base page allows upload, preview, and delete. Uploaded documents are indexed in Lucene. Embeddings generated via DeepSeek API and cached in `data/emb-cache/` as `.emb` files keyed by SHA-256 hash. Hybrid BM25 + vector search returns combined results. RAG context injected into every chat request.

### Sprint 3: AI Operations + Rich Cards
- **Goals**: Implement all five AI capabilities and the card rendering system
- **Features**: #9 (Recommendation Cards), #10 (Add/View Favorites via Function Calling), #11 (Create Order via Function Calling)
- **Definition of done**: Recommendation cards render inside AI messages with thumbnail, price, match reason. Function calling for add_favorite correctly identifies properties and toggles favorites. view_favorites returns cards for up to 5 favorites. create_order_preview shows summary card without side effects. confirm_order creates actual order. All three flows work end-to-end with user confirmation for orders.

### Sprint 4: Polish + Edge Cases
- **Goals**: Polish all states, handle edge cases, ensure quality
- **Features**: #15 (Chat UI Polish + States), #16 (Order Confirmation Flow Polish)
- **Definition of done**: Typing indicator animates during AI response. Welcome message with quick-action chips shows in empty sessions. Error states show retry option. Long messages toggle expand/collapse. Scroll-to-bottom button appears when scrolled up. Pending order state is preserved across chat close/reopen. Orange dot badge appears for pending orders. Inactivity reminder works after 5 minutes. All existing functionality is not regressed. Grep for old colors returns no false positives.

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| DeepSeek API rate limiting | High | Medium | Add user-level rate limiting (max 20 requests/min). Cache embeddings. Show friendly "稍后再试" message. |
| Lucene index corruption on crash | Medium | Low | Index is rebuildable from source files. Startup checks index integrity. If corrupt, rebuild from `data/rag-platform/`. |
| Large uploaded documents causing slow indexing | Low | Medium | 10MB file size limit. Sync indexing is acceptable for anticipated document sizes. Monitor and consider async queue if needed. |
| Spring AI compatibility with DeepSeek | Medium | Low | DeepSeek is OpenAI-compatible. Verify base URL and model name. Add startup health check that tests connection. |
| Chat UI conflicting with page-level z-index | Medium | Medium | Use explicit z-index: 9999 on bubble, 9998 on window. Test on every page type (dashboard, search, detail, forms, tables). |

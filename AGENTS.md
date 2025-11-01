# Agent Instructions

## How to Use This Document

This is the **primary entry point** for agents working on Pudi. It provides sufficient context for most decisions.

**When you need more detail:**
- Module structure, navigation, or DI patterns → @ARCHITECTURE.md
- Implementing/debugging USF ViewModels → @ARCHITECTURE-USF.md (comprehensive 3600+ line guide)
  - New screen? → "USF Quickstart Guide" section
  - Complex features? → "USF Plugin Architecture" section
  - Debugging? → "USF Troubleshooting Guide" section
- Testing patterns and utilities → @TESTING.md

## Project Overview
Pudi is an RSS reader for Android that aims to provide the world's best reading experience on mobile. Unlike feature-heavy RSS readers with endless customization, Pudi is intentionally opinionated and subtle—designed for users who want the most efficient way to consume content without configuration overhead.

**Technology stack**: Kotlin-first with Jetpack Compose, Clean Architecture, Unidirectional State Flow (USF) ViewModels, and compile-time DI via kotlin-inject + Anvil. Modular by design: features ship as isolated modules, domain logic stays UI-agnostic, and shared utilities live in `:common`.

## Important Commands
- `make` — Default build target (runs full build including tests)
- `make build-debug` — Assemble debug build without lint
- `make clean` — Remove build outputs and caches
- `make tests` — Execute all unit tests
- `make lint` — Run Android/Kotlin lint checks
- `make ktfmt` — Format staged Kotlin files
- `make ktfmt-all` — Format every Kotlin file

## Core Principles

### 1. Subtle and Opinionated
- No endless settings or customization options; deliver the best reading experience out of the box
- Progressive disclosure: long-press for advanced options, infer user preferences from behavior
- Persist style choices throughout the experience without explicit configuration

### 2. Uncompromising Reading Experience
- Typography, spacing, and padding rigorously aligned for optimal readability (inspiration: Superhuman, NetNewsWire)
- Clean visual hierarchy with subtle text colors and dividers
- Font choices optimized for extended reading sessions

### 3. Speed is a Feature
- Never block the app; instant opening even in low network conditions
- No splash screens or loading gates (contrast with Feedly's blocking splash)
- Prioritize perceived performance and responsiveness

### 4. Offline Companion
- Automatically save "read later" articles for offline reading
- Pre-cache content for flights/offline scenarios without manual intervention

## Key Features & Priorities

### Implemented / In Progress
- Feed management: add, organize, and read RSS/Atom feeds
- Clean reading interface with favicon-based feed representation
- Offline article storage for "read later"

### High Priority
- **Read Later**: first-class feature for saving articles
- **Reddit feed support**: integrate Reddit as a feed source
- **Newsletter-to-reading**: transform Substack/email newsletters to in-app reading (share-to-Pudi)
- **Discover**: seamless search and feed discovery (Google search webview + quick-add for HN, popular blogs)
- **Feed sniffing**: detect RSS feeds from arbitrary websites (check `<link>` metadata before guessing URLs)

### Future Exploration
- Social sharing: export feed lists as "starter packs"
- Stories for today: surface popular articles from your feeds
- Stream view: show top 10-20 articles only (like HN front page)
- Mark above/below as read
- Multi-source feeds: support beyond RSS (Bluesky, etc.)
- LLM summaries for articles
- Podcast generation from feeds (NotebookLM-style)

## Design & UX Principles

### Information Architecture
- Collapsible folder groups for feed organization
- Consistent favicon usage across list items
- Minimal chrome; let content breathe
- Top bar fades subtly; no heavy UI chrome

### Reading Experience
- Single-page reading view with optimized spacing
- Subtle dividers for content separation
- Fixed typography hierarchy
- WebView rendering for full article content (see ReadYou implementation)

### Discover/Search
- Fixed search bar at top
- On focus: show starter text + Google search webview
- Quick-add shortcuts for HN, popular blog categories
- Seamless navigation from search to add

## Architecture

### Module Layout
- `:app` — Entry point; wires dependency graph, root composables, navigation display
- `:features:*` — Independent feature modules (e.g., `:features:feeds`, `:features:reader`, `:features:discover`)
- `:domain:*` — Shared business logic (feed parsing, article storage, sync)
- `:common:*` — Cross-cutting utilities (logging, networking, USF core, UI components)
- `:build-logic` — Gradle convention plugins

### Unidirectional State Flow (USF)
- Every screen uses `UsfViewModel<Event, UiState, Effect>` with immutable state and one-off effects
- Events flow through `process` inside `ResultScope`; state updates via `updateState { … }`, side effects via `emitEffect`
- Model callbacks as part of state to keep composables dumb:
  - Use `inputEventCallback` for simple events: `onAction = inputEventCallback(Event.Action)`
  - Use `inputEventCallback` for parameterized callbacks: `onTextChanged = inputEventCallback(Event::TextChanged)`
  - These helpers ensure **reference stability** for Compose, preventing unnecessary recompositions
  - Callbacks are created once and preserved via `.copy()`, ensuring stable references across state updates

**USF Documentation:**
- **@USF.md** — Core patterns for 90% of cases (basic ViewModels, UI integration, testing)
- **@USF-QUICKSTART.md** — Complete tutorial if new to USF
- **@USF-PLUGINS.md** — Plugin composition for complex features (read only when needed)
- **@USF-TESTING.md** — Comprehensive testing patterns (read only when writing tests)
- **@USF-TROUBLESHOOTING.md** — Debugging guide (read only when stuck)

**Agent instructions:** Read @USF.md for basic patterns. Read specialized guides only when the task requires them. Don't read all files upfront—pull specific guides as needed.

### Navigation 3 Back Stack
- Developer-owned: singleton `Navigator` keeps `SnapshotStateList<NavRoute>`
- Routes are serializable, compile-time safe objects
- Features contribute `EntryProviderInstaller` to map routes to composables
- `navigator.goTo(route)` / `navigator.goBack()` / `navigator.clearAndGoTo(route)`

### Dependency Injection (kotlin-inject + Anvil)
- `@Inject` constructors, `@SingleIn(scope)` for lifetime
- `@ContributesBinding(scope)` for interface implementations
- Feature-scoped subcomponents: `@ContributesSubcomponent(FeatureScope::class)`
- Prefer constructor injection; use `Lazy<T>` for deferred work

## Testing
- JUnit 5, `kotlinx.coroutines.test`, MockK, AssertJ, Turbine
- Test USF pipelines by driving events, capturing state/effects, controlling virtual time
- All public logic must have unit test coverage
- Run `make tests` or `make build` for full verification
- See `TESTING.md` for patterns and utilities


## Coding Conventions

### General
- Optimize for legibility: clear `if/else`, single-responsibility functions (<20 lines), immutable data
- PascalCase types, camelCase members, no star imports, trailing commas in multiline literals
- Avoid Kotlin `object` except for true constants; rely on scoped DI classes
- UI is Compose-only; keep composables thin, delegate logic to ViewModels and domain layers

### Pudi-Specific
- **RSS/Feed handling**: always check `<link rel="alternate" type="application/rss+xml">` in HTML `<head>` before guessing feed URLs
- **Reading experience**: maintain consistent spacing/padding; consult Superhuman/NetNewsWire patterns
- **Performance**: never block the UI; prefer incremental loading and optimistic updates
- **Offline**: default to offline-first architecture; cache articles automatically for "read later"

### Logging, Concurrency, Naming
- Logging: never include sensitive data; prefix with data marks (`"[FFF][CC] message"`)
- Concurrency: no `GlobalScope` or raw threads; inject scopes, use suspend functions, clean up via USF lifecycle
- Naming: skip generic suffixes (Manager/Helper/Util); choose domain terms (e.g., `FeedSniffer`, `ArticleRepository`)

### Comments & Instruction Priority
- Comments document *why* decisions were made (space-shuttle style); no TODOs without owner/context
- Instruction priority: **user requests → project rules → professional standards → this document**
- Default to precision over politeness; surface flawed assumptions immediately

## Additional Resources

- @ARCHITECTURE.md — Detailed module responsibilities, DI scopes, navigation wiring, UI composition patterns
- @USF.md — Core USF patterns for daily development (basic ViewModels, UI integration, testing essentials)
- @USF-QUICKSTART.md — Complete USF tutorial from scratch
- @USF-PLUGINS.md — Advanced plugin composition for complex features
- @USF-TESTING.md — Comprehensive testing patterns and utilities
- @USF-TROUBLESHOOTING.md — Debugging guide for common USF issues
- @TESTING.md — Test patterns, utilities, and conventions for JUnit 5 + Coroutines

# ExecPlans
When writing complex features or significant refactors, use an ExecPlan (as described in @.ai/plans/PLANS.md or @~/.ai/plans/PLANS.md) from design to implementation. Write new plans to the @.ai/plans directory. Place any temporary research, clones etc., in the .gitignored subdirectory @.ai/plans/tmp

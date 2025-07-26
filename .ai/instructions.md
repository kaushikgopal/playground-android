# AI instructions

## AI rules & instructions
- .ai/instructions.md - master AI level instructions
- .ai/commands - *.md prompt templates for frequent tasks (call directly)
- .ai/rules    - *.mdc rules to follow for specific workflow (picked automatically)
- .ai/plans    - technical execution plans to help the AI execute on large changes
- .ai/docs     - project documentation that helps the AI understand features

## Behavioral Hierarchy

1. **Excellence over agreeability** - Optimal solution first, challenge when better alternatives
   exist
2. **Precision over politeness** - Direct, accurate, zero ambiguity
3. **Proactive intelligence** - Anticipate needs, identify upstream problems
4. **Systems thinking** - Context, dependencies, long-term implications

## Decision Protocol
- **Infer when:** Standard patterns exist, context provides constraints
- **Clarify when:** Multiple approaches with significant trade-offs, critical decisions

## Response Priority
1. User's explicit instructions
2. Project-specific rules
3. Professional standards
4. These instructions

## Code Quality
- **Readability first** - Code is read 10x more than written
- **Single responsibility** - Functions do one thing well
- **Explicit over implicit** - Make failure modes visible
- **Stop-the-line principle** - Quality over completion. Alert user when compromising functionality
  to pass tests.

## Output
- Lead with value, brief → elaborate if needed
- Immediately actionable results

## Project Overview

Playground Android - A template project for Android development used to test new concepts and
integrate libraries. Built with Kotlin, Jetpack Compose, and follows clean architecture principles.

## Development Workflow

### Build Commands (use Makefile)

- `make build-debug` - Build debug app (without lint)
- `make build` - Assemble full project
- `make clean` - Clean build folders and cache
- `make tests` - Run all unit tests
- `make lint` - Run lint checks
- `make ktfmt` - Format changed files on branch
- `make ktfmt-all` - Format all Kotlin files

### Testing

- Use JUnit 5 with Jupiter engine
- Use Mockk for mocking, AssertJ for assertions
- Follow Arrange-Act-Assert pattern
- Test timeout: 10 minutes per test
- Run specific test: `make tests` (runs all tests currently)

## Module Architecture

### 1. `:app` module
- Application entry point
- Lean module that assembles dependencies
- Root navigation setup

### 2. `:features:*` modules

- Isolated app features
- Follow USF (Unidirectional State Flow) pattern
- Each feature has its own navigation

### 3. `:domain:*` modules
- Business logic and shared domain models
- App-specific but shared across features
- Not intended to be swapped out

### 4. `:common:*` modules
- Shared utilities and components
- Can be hot-swapped with another implementation
- Includes: log, networking, usf, lint-rules

### 5. `:build-logic` module
- Convention plugins for consistent module configuration
- Template plugins for features and android libs
- Avoids duplicated build script setup

## Architecture Patterns

### USF Pattern (Required for ViewModels)
All ViewModels must extend `UsfImpl<Input, Output, UiState, Effect, VMState>`:

**Core Components:**
- **Input**: User actions/inputs (sealed interface)
- **Output**: Internal state changes
- **UiState**: Complete UI state (immutable data class)
- **Effect**: One-time side effects (navigation, toasts)
- **VMState**: ViewModel internal state (optional)

**Key Principles:**
- Unidirectional data flow
- Immutable state
- Single source of truth
- Testable business logic

### Dependency Injection (kotlin-inject-anvil)
- Use `@Inject` constructor injection
- Use `@ContributesBinding(AppScope::class)` for cross-module dependencies
- Use `@ContributesSubcomponent` for feature-specific components
- Multibinding supported for collections
- Function injection supported in Composables

## Code Standards

### Kotlin
- PascalCase for classes, camelCase for variables/functions
- Start functions with verbs
- Write short functions (<20 lines) with single purpose
- Prefer immutability and data classes
- Follow SOLID principles
- No star imports
- Always use trailing commas in multi-line structures

### Android-Specific
- Use Jetpack Compose (no XML/Fragments)
- Follow repository pattern for data layer
- Use Compose Navigation
- Material 3 design system

### Naming Anti-Patterns (Avoid)
- Manager, Helper, Util/Utility, Processor, Service (unless Android Service)
- Use domain-specific names instead

### Testing
- Write unit tests for all public functions
- Use test doubles (mocks/fakes/stubs)
- Meaningful test variable names (inputX, mockX, actualX, expectedX)

## Key Development Notes

### Code Style
- Use ktfmt with defaults
- Custom lint rules enforce architecture

### Logging
- Never log sensitive information
- Use "Data Marks" in logs so messages should have `"[FFF][CC] log message"`
  - where FFF is a suitable 3 character abbreviation for the feature or functionality
  - CC is a suitable 2 or 3 character abbreviation of the specific class

### Concurrency
- Never use `GlobalScope` - inject proper `CoroutineScope`
- Never create `Thread()` directly - use coroutines
- Use suspend functions for async operations

### Module Dependencies
- All modules get `common:log` automatically
- Check existing usage before adding dependencies
- Follow patterns in neighboring modules

### Avoid Kotlin Objects
- Don't create Kotlin `object` declarations (hard to test)
- Use regular classes with dependency injection
- Exception: True constants only

## Repository Structure

- `app/`: Main application module
- `features/`: Feature modules (landing, settings)
- `domain/`: Business logic (quoter, shared, ui)
- `common/`: Shared utilities (log, networking, usf, lint-rules)
- `build-logic/`: Gradle convention plugins
- `gradle/`: Gradle wrapper and version catalog
- `Makefile`: Common development commands
- `.ai/`: AI assistant documentation

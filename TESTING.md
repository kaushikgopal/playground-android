# Testing

## Philosophy
- Treat `process` as a pure function pipeline: feed events, assert the resulting state/effects, and avoid inspecting internals.
- Follow Arrange–Act–Assert with explicit setup so asynchronous work and debouncing remain predictable.
- Prefer deterministic coroutine scheduling with `kotlinx.coroutines.test` utilities (`runTest`, `advanceTimeBy`, `runCurrent`).

## Tooling & Dependencies
- Unit tests run on JUnit 5 (Jupiter). Common dependencies live in Gradle version catalogs:
  - `libs.junit.jupiter`
  - `libs.kotlinx.coroutines.test`
  - `libs.mockk`
  - `libs.assertj.core`
  - `libs.turbine` for Flow assertions
- Compose UI logic should be exercised via state-driven ViewModel tests first; add Compose UI tests only when layout-specific behavior requires it.

## Running the Suite
- `make test name="<ClassName>"` — executes single unit test by finding the module from the file path.
- `make tests` — executes all unit tests; default timeout per test is 10 minutes.
- `make build` runs tests as part of the full build; use `make build-debug` for a faster smoke build without lint.

## Patterns for USF ViewModels
- **State capture**: collect `state` into a mutable list (or use Turbine) to assert the initial and final snapshots.
- **Effects**: collect the `effects` Flow separately; treat each emission as one-time and assert order where relevant.
- **Async operations**: advance virtual time with `advanceTimeBy` after dispatching events to simulate delays (network, debounced input, etc.).
- **Error handling**: mock collaborators to throw; verify the state records the failure and an effect (toast/navigation) is emitted.
- **Plugins**: register plugins in tests exactly as production code does; mock plugin collaborators independently so individual behaviors can be validated.

## Helpful Utilities
- Use a JUnit extension (e.g., `CoroutineTestRule`) to expose a `TestScope` and dispatcher to the ViewModel under test.
- Keep factories like `createViewModel()` inside the test class to share setup while keeping dependencies explicit.
- For Flow debugging, capture sequences with Turbine's `test { … }` or collect into lists and print indexes when diagnosing order issues.

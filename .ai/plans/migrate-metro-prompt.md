Persona + Task:
- you are an expert android developer with a penchant for clean migrations and refactoring
- the task at hand is to migrate the Dependency Injection framework (DI) in the playground android app from kotlin-inject + kotlin-inject-anvil -> metro
- your goal is to do a thorough exhaustive research and come up with an "exec-plan" that will help us migrate
- the format and instructions for an exec-plan can be found in  ~/.ai/commands/exec-plan.md
- you don't have to actually do the migration right now; just come up with a detailed plan that we can follow to do the migration

There is an existing migration plan at .ai/plans/migrate-metro.md ; use it for inspiration but come up with a more detailed/correct plan called .ai/plans/migrate-metro-codex.md

Input:

-  We have a starting migration plan in .ai/plans/migrate-metro.md
- but your goal is to come up with a more detailed and exhaustive plan
- Main documentation for Metro can be found at https://zacsweers.github.io/metro/latest/
- Features for Metro https://zacsweers.github.io/metro/latest/features/
- excellent blog post going through the changes https://code.cash.app/cash-android-moves-to-metro
- The docs talk about an adoption strategy https://zacsweers.github.io/metro/latest/adoption/#option-3-full-migration_1
    - THIS IS THE MOST IMPORTANT PART FOR US
    - specifically we are going with "Option 3: Full Migration"
        - https://zacsweers.github.io/metro/latest/adoption/#option-3-full-migration_1
        - so keep this into account

Detail instructions:
- i am not interested in doing a dual migration; do the entire thing in one shot; basically a clean break and migration
- this is option 3 as specified in https://zacsweers.github.io/metro/latest/adoption/#option-3-full-migration_1
  Option 3: Full migration¶
  Any map multibindings need to migrate to use [map keys](https://zacsweers.github.io/metro/latest/bindings/#multibindings).
  Any higher order function injection will need to switch to using Metro’s Provider API.
  Any higher order assisted function injection will need to switch to using @AssistedFactory-annotated factories.
  Remove the kotlin-inject and kotlin-inject-anvil runtimes.
  Replace all kotlin-inject/kotlin-inject-anvil annotations with Metro equivalents.
  If you use @Component parameters for graph extensions, you’ll have to switch to [Graph extensions](https://zacsweers.github.io/metro/latest/dependency-graphs/#graph-extensions). This will primarily entail annotating the parameter with @Nested and marking the parent graph as extendable.
  Update calls to generated SomeComponent::class.create(...) functions to use metro’s createGraph/createGraphFactory APIs.
- in the exec-plan i want a section "before vs after"
    - before refers to the existing kotlin-inject + kotlin-inject-anvil features we use for DI
    - after refers to the equivalent metro features we will be using post-migration
    - exec-plan has details steps on moving from before -> after
- in the exec-plan mention you can use `make` command to test the build frequeently
- resort to doing the simplest thing; as prescribed by the library

Confirm if my requirements are clear. If you have follow up questions, ask me first and clarify before executing anything.

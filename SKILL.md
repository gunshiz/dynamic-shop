**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## 5. Project Architecture

The project has been refactored into specific packages to maintain a clean and organized codebase. Always place new classes in their appropriate package:

- **`commands/`**: Contains all command executors (e.g., `ShopCommand`, `SellCommand`, `StockCommand`, `ShopAdminCommand`).
- **`managers/`**: Handles core business logic and background tasks (e.g., `DatabaseManager`, `EconomyManager`, `MarketEventManager`, `ShopManager`).
- **`listeners/`**: Contains Spigot event listeners (e.g., `ShopGUIListener`).
- **`utils/`**: Utility classes and helpers (e.g., `GUIUtils`, `PricingEngine`).
- **`models/`**: Data structures, models, and enums (e.g., `ShopItem`, `SortType`).
- **Root**: Contains the main plugin entry point (`DynamicShopPlugin`).

## 6. Build and Deploy Workflow

Whenever you build the project (`mvn clean package`), you **must** also execute the upload script (`bun upload.ts`) to ensure the compiled plugin is automatically pushed to the server for testing.
**Standard Command:** `mvn clean package && bun upload.ts`

## 7. Purpur 26.1.2 API

This project targets **Purpur 26.1.2** (Minecraft 1.21.6+). Key notes:

### Version Format
Purpur has moved away from `1.X.X-R0.1-SNAPSHOT`. The new format is build-based:
```xml
<version>26.1.2.build.2591-stable</version>
```
To find latest builds: `curl https://repo.purpurmc.org/snapshots/org/purpurmc/purpur/purpur-api/maven-metadata.xml`

### Maven Repository
```xml
<repository>
    <id>purpur</id>
    <url>https://repo.purpurmc.org/snapshots</url>
</repository>
```

## 8. Dialog API (Native Custom Screens)

Minecraft 1.21.6+ introduced a native **Dialog API** for server-side custom screens with text inputs, buttons, labels, and more. This replaces the need for AnvilGUI or Sign GUI hacks.

### Key Packages
- `io.papermc.paper.dialog.Dialog` — Main Dialog interface, created via `Dialog.create()`
- `io.papermc.paper.registry.data.dialog.DialogBase` — Title, body text, and inputs
- `io.papermc.paper.registry.data.dialog.body.DialogBody` — Body content (e.g., `plainMessage()`)
- `io.papermc.paper.registry.data.dialog.input.DialogInput` — Input fields (`text()`, `bool()`, `numberRange()`, `singleOption()`)
- `io.papermc.paper.registry.data.dialog.type.DialogType` — Dialog types (e.g., `notice()`)
- `io.papermc.paper.registry.data.dialog.ActionButton` — Buttons with labels and actions
- `io.papermc.paper.registry.data.dialog.action.DialogAction` — Action handlers (`customClick()`, `CommandTemplateAction`, `StaticAction`)

### Usage Pattern
```java
Dialog dialog = Dialog.create(factory -> {
    factory.builder(Key.key("dynamicshop", "my_dialog"))
        .base(DialogBase.builder(Component.text("Title"))
            .body(List.of(DialogBody.plainMessage(Component.text("Body text"))))
            .inputs(List.of(DialogInput.text("input_key", 200, Component.text("Label"), true, "", 128, null)))
            .build())
        .type(DialogType.notice(
            ActionButton.builder(Component.text("OK"))
                .width(200)
                .action(DialogAction.customClick((audience, ctx) -> {
                    String value = ctx.input("input_key");
                    // handle input
                }))
                .build()
        ));
});
player.showDialog(dialog);
```

### Notes
- The Dialog API is marked `@Experimental` — method signatures may change between versions.
- Dialogs render as **native Minecraft client screens** (not inventory GUIs), with the ⚠ "custom screen" warning icon.
- Supports: text input fields, boolean toggles, number range sliders, single-option dropdowns, buttons.
- For **Bedrock Edition** players (via Floodgate/Geyser), continue using `Cumulus` forms as the Dialog API is Java Edition only.
- JavaDocs: `https://jd.papermc.io/paper/26.1.2/`

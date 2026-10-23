# Compose Desktop API design reference

Retrieved: 2026-10-22 01:19 UTC. Scope: official documentation and JetBrains core's jb-main source, not the exact Compose 1.11.1 release source used by this repository or a compatibility guarantee.

## Official design evidence

- [`application` source](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/desktopMain/kotlin/androidx/compose/ui/window/Application.desktop.kt): the entry accepts composable ApplicationScope content. Its scope exposes exitApplication and owns composition/effect lifetime, not business-service registration.
- [`Window` source](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/desktopMain/kotlin/androidx/compose/ui/window/Window.desktop.kt): window properties and close requests belong to the window API. Composition entry/exit creates/disposes the window. WindowState supports control and observation rather than requiring callers to assemble native host objects.
- In the same source, singleWindowApplication is a convenience wrapper around application + Window whose close handler exits the application. The [official single-window guide](https://kotlinlang.org/docs/multiplatform/compose-desktop-top-level-windows-management.html#create-a-single-window-application) distinguishes this shortcut from the more flexible general entry.
- [Platform entry points](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-entry-points.html#on-desktop): Desktop integrates with the JVM; Web UI has its own entry. An ordinary Native closure is not Wasm-side composable content.

## Assessment of Kauri's former API

- The single entry removed an external assembly chain, but reducing eight public symbols to one was not the final design criterion: it still combined application identity, window attributes and a backend factory.
- The user subsequently required atomic capabilities with a caller-owned composition root. Learning Compose's responsibility boundaries does not justify another Kauri assembly DSL.
- The former single-entry ablation is implementation history, not the target API. Resource and window capabilities can be independent; servers use Ktor/kRPC directly instead of being evaluated by public-symbol count or uniform wrapping.

## Confirmed direction, now implemented

- Do not adopt the earlier proposed Kauri application { rpc(...); window(...) } integration DSL.
- app/desktop creates business services, selects assets, starts the local server, passes its endpoint to the window and arranges shutdown. No new ApplicationScope, Builder or mandatory configuration object is required.
- Each capability retains its own safety and ownership rules. A window does not implicitly start/stop business servers, and a server does not implicitly create windows; ordinary Kotlin control flow and resource scopes compose them.
- The user later approved the atomic specification and source rewrite. Names, lifetime contracts and historical execution evidence are in resources/atomic-framework-api.md and resources/atomic-rewrite.md. No single-entry compatibility layer remains.

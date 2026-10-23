# Task Tree

- `Inspect platform adapters and public APIs`()
- `Probe Kotlin Native ABI and callbacks on Mac`()
- `Review feasibility and build simplification boundaries`()
- `Record evidence and remove temporary probes`()

# Details

- Explore removing maintained C/C++ sources; do not rewrite the production hosts yet.
- “Pure Kotlin/Native” permits official SDK headers and compiler-generated interop stubs, but must distinguish those from authored C/C++ or prebuilt native loaders.
- Preserve four targets, a single shipped executable, system WebViews, typed kRPC, and runtime security/lifecycle behavior.
- Main risks: WebView2 COM vtables/callback ownership and its loader's public versus internal API boundary.
- Run any compiler probes only on the remote Mac, serialized under the project lock. Do not activate GUI windows, clear shared tool caches, or run Gradle on Linux.
- Temporary probe sources/results belong under ignored `build/exploration/`; durable conclusions belong in `resources/`.

## Results

- No Gradle invocation or production code/build edits. Compiler probes used the existing Mac Kotlin/Native CLI, explicit Java 21, project lock, one command at a time, lowered priority and at most 1 GiB Java heap.
- Linux x64/arm64 compile; x64 GLib callback/state/destroy notify runtime passes, GTK/WebKitGTK symbols resolve without GUI initialization.
- Windows SDK C interop and Kotlin COM vtable/callback/WNDPROC/RECT method calls compile/link. Initial GUID namespace error fixed using generated metadata; no Windows runtime or loader test.
- Mac extern linker-array access compiles/runs without handwritten C function bodies.
- Reviewed official documentation and pinned loader source; current internal loader API risk remains if ported to Kotlin. Alternative official loaders retain a native dependency or change packaging.
- agy Gemini 3.8 Flash completed a read-only review. Applied relevant clarifications; rejected expansion to out-of-scope Windows arm64 and SDK-wide COM abstraction.
- Durable report: `resources/pure-native-feasibility.md`. Probe sources, logs, import audit and artifact fingerprints archived under ignored `build/review/pure-native-*`.
- Removed local/Mac `build/exploration/pure-native` after archiving. No Gradle daemon started/stopped, GUI resource acquired, or probe process remains; unrelated caches were untouched.

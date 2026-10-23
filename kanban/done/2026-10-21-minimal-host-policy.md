# Task Tree

- `Inspect pinned host defaults and official data APIs`()
- `Separate confirmed first-release scope from proposed policy`()
- `Record findings without changing runtime implementation`()

# Details

- User confirmed Compose Wasm owns application navigation and in-content dialogs; native popup/multiwindow support is out of scope for this release.
- Analyze browser-data storage (profile) and offscreen testing, without implementing a migration or starting builds.
- Preserve the distinction between application routing and the native boundary preventing document escape.
- Cache official sources and implementation constraints; do not elevate proposed profile defaults into confirmed decisions.

## Results

- Recorded `resources/minimal-host-policy.md` and the confirmed Compose routing/dialog scope in the architecture checklist.
- Distinguished product profile defaults from test data isolation; identified creation-time API constraints and the existing smoke's default persistent browser store.
- agy Flash reviewed the document read-only; clarified that initialization customization may need a small source patch, not an already exposed C API hook. Did not adopt its incorrect aside implying four-platform static linking was untested: that evidence already exists in the spike report.
- No runtime/build changes, new compilation, GUI control, or commit. Profile defaults remain proposals.

# Build ownership

- Build on `stream@macbook`, not the Linux workstation. Use `scripts/gradle-mac.sh` with qualified paths such as `:framework:macosArm64Test`; unqualified selectors can unexpectedly select subproject checks.
- Reuse an idle daemon belonging to the current user, wrapper version, and Gradle user home. Read its JVM and arguments; refuse overrides or incompatible/busy environments instead of starting competing application builds.
- If no daemon exists, bootstrap one using an explicit JDK and a temporary minimal build. The JDK runs Gradle only; the application remains Kotlin/Native.
- The init-script `ValueSource` checks the actual daemon PID/JVM, including configuration-cache validation. Do not replace this with a post-build log check alone.
- Keep the repository lock, client log, and owned-daemon marker under `.gradle/kauri/`, outside `clean` outputs. Release and Mac smoke scripts hold the same lock across compilation and runtime checks; only direct child scripts may adopt a parent's lock.
- Do not automatically break a stale lock or stop an unrelated daemon. Check `owner.pid` and process ownership before manual recovery; relinquish control if the user takes over.

# Sources of truth

- `gradle/libs.versions.toml`: library/plugin versions and Binaryen.
- `gradle/native-toolchains.properties`: native preparation's Kotlin-compatible LLVM/sysroots, MinGW C++ headers, WebView2 SDK version and SDK SHA-256.
- `gradle/linux-sdk.lock`: Debian bookworm package paths/SHA-256 for the two cross-link SDKs.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle version and distribution checksum; the helper reads the version from this file.
- `.nvmrc`: nvm Node version. Web tooling uses its absolute nvm executable and does not download another Node.
- Settings own Gradle repositories and reject project repositories. Binaryen uses a module-restricted Ivy endpoint; its automatic repository insertion is disabled through the supported `EnvSpec`. Native SDK downloads belong to explicit preparation, not Gradle link actions.

# Build logic and data flow

- Use ordinary module scripts, KGP targets/binaries and standard cinterop. No included convention build or custom task classes.
- Root only shares KGP's classpath with one `apply false`; no `base` or lifecycle aggregation. The classpath declaration avoids an observed KGP SwiftPM build-service classloader conflict between module plugin sets.
- IDE binding/commonization must need repository public headers only, not Mac-prepared archives or link SDKs. Final Native binaries declare only the required system SDK link inputs; Linux SDK inputs remain target-specific.
- Declare the GTK compatibility header as an input of its existing cinterop tasks; a header edit must regenerate bindings rather than leave a stale up-to-date result. This input remains repository-only.
- Agent-triggered workstation IDEA import can generate cinterop bindings; obtain user approval for that limited exception to Mac-only execution. Do not treat it as permission to compile the app/WebView locally or stop user-owned daemons.
- Run `prepare-native-mac.sh` explicitly after native policy/SDK changes or if prepared inputs were removed. It verifies source/download hashes, prepares target SDK link inputs sequentially on the Mac, and never installs system packages.
- Native preparation uses KGP's distribution download task if needed, then `konanc -Xcheck-dependencies` to install target toolchains in `KONAN_DATA_DIR` (default `~/.konan`). These are build inputs, not packaged application files; cold bootstrap still needs network, JDK 21, nvm Node and Xcode.
- `app/frontend` uses the standard Wasm distribution task. `app/desktop` has one standard `Exec` archive task and one standard `Exec` embedding task per target; script/asset/compiler inputs and output objects are declared.
- Executable links depend on embedding; Native unit tests do not. Resource objects must be link inputs, not merely undeclared linker-option strings.
- Use normal KGP binary output paths. Release scripts copy, strip/sign on Mac and generate checksums; do not wrap these operations in bespoke task types.
- Configuration cache remains strict. Standard `Exec` embedding supports up-to-date checks; do not claim build-cache restoration or cross-machine reproducibility for these local transformations.
- Preserve application/framework dependency direction and all four single-file targets. Native SDK preparation is a documented prerequisite, not an implicit application-build side effect.

# Commands

Run from the remote repository root:

```bash
bash scripts/prepare-native-mac.sh
bash scripts/gradle-mac.sh :app:shared:macosArm64Test \
  :app:desktop:macosArm64Test :framework:macosArm64Test
bash scripts/gradle-mac.sh :app:desktop:linkReleaseExecutableMacosArm64
bash scripts/release-all-mac.sh
bash scripts/release-mac.sh
bash scripts/test-build-policy.sh
```

- Native checks use the three module test paths above. Browser tests require their own explicit path; they are not part of this gate.
- Root defines no `:check` / `:assemble`. Release scripts select the required existing module release-link and test tasks directly; do not reintroduce a root wrapper.
- Release scripts additionally run Node archive tests and Mac non-GUI licenses/HTTP security/streaming checks. Native GUI smoke is opt-in on an owned graphical runner (`KAURI_ISOLATED_DISPLAY=1`), uses ordinary visible windows and is explicitly recorded NOT RUN by default.
- Embedded HTTP assets use openEmbeddedResource and Source.use around pinned CIO respondSource without contentLength. Retain bounded-read/backpressure/cancellation tests and interrupted-client/exact-byte HTTP regression; a streaming type alone does not prove bounded buffering.
- Mac-only releases use the same canonical `build/release/macosArm64/Kauri` path and invalidate a potentially stale four-target manifest.
- `SHA256SUMS` records integrity. `VALIDATION.txt` is replaced only after the script's checks pass and states which platforms were not runtime-tested.
- The full release manifest must contain one executable for each target; partial releases remove the full manifest. Compare validation-receipt hashes with current artifacts before relying on an older receipt.
- `--offline` needs a warmed dependency/tool cache; it is not a promise that a fresh machine can build without downloads.
- Mac/Linux GUI smoke requires an owned isolated graphical runner and `KAURI_ISOLATED_DISPLAY=1`; the flag is an assertion of runner ownership, not a mechanism that creates isolation. Never use the user's active desktop.

# Maintenance gates

- Keep configuration-cache problems fatal; test both storage and reuse, not just a successful uncached build.
- Verify clean application-output recovery, embedding producer scheduling, unchanged-build up-to-date behavior and failure/lock handling after build changes. Removing root `build/` also removes prepared native inputs and requires explicit preparation.
- Run all four release links and Mac non-GUI runtime/security checks after linkage or packaging changes. Run native GUI checks only with an owned runner; record missing runners, including Mac, explicitly.
- Keep two workers and the existing memory budget until measurements justify a change.
- Record upstream warnings rather than hide them. kRPC 0.10.4 still calls Gradle's deprecated `Project.getProperties`; a Gradle 10 upgrade needs an upstream-compatible resolution.

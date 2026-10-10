# Peridot → Android Spike Results

**Date:** 2026-10-10 · **Verdict: ✅ SUCCESS — the pipeline is proven.**

The original, **unmodified** `peridot-sync` (path dep on `~/projects/peridot`)
cross-compiles for `aarch64-linux-android`, links into a cdylib with UniFFI
scaffolding, and UniFFI generates Kotlin bindings from the resulting Android
`.so` — no vendored fallback needed.

## Artifacts

| Artifact | Status |
|---|---|
| `target/aarch64-linux-android/debug/libperidot_ffi.so` | ✅ ELF 64-bit LSB, ARM aarch64, `NEEDED: libdl.so, libc.so` (bionic), 149 MB debug build |
| `kotlin-out/uniffi/peridot/peridot.kt` | ✅ 1458 lines, package `uniffi.peridot`, `PeridotEngine` with `ping()` / `engineVersion()` / `pathIsClean()` |
| `spike-results.md` | this file |

The `.so` exports all expected UniFFI symbols (`uniffi_peridot_ffi_fn_constructor_peridotengine_new`,
`UNIFFI_META_NAMESPACE_PERIDOT`, checksum fns, rustbuffer fns).

## Versions

- rustc / cargo: **1.95.0** (`rustc 1.95.0 (59807616e 2026-04-14)`)
- target: `aarch64-linux-android` (rust-std installed via `rustup target add`)
- NDK: **28.2.13676358** at `~/Android/Sdk/ndk/28.2.13676358`, clang wrapper `aarch64-linux-android24-clang` (API 24)
- uniffi: **0.29.5** (locked), proc-macro mode + library-mode bindgen
- peridot-sync: **0.2.7** at `../peridot/crates/peridot-sync` (untouched)

## The one real blocker (and its fix)

**First error** (attempt 1, before any fix — captured from
`build-android-attempt1.log`):

```
error: failed to run custom build command for `ring v0.17.14`
...
cargo:warning=Compiler family detection failed due to error:
  ToolNotFound: failed to find tool "aarch64-linux-android-clang":
  No such file or directory (os error 2)
error occurred in cc-rs: failed to find tool "aarch64-linux-android-clang"
```

Cause: `cc-rs` (used by `ring`'s build script) does **not** read Cargo's
`[target.aarch64-linux-android] linker` key, and NDK r19+ only ships
API-suffixed clang wrappers (`aarch64-linux-android24-clang`), not the bare
name cc-rs guesses.

Fix: point cc-rs at the NDK wrapper via `[env]` in `.cargo/config.toml`
(cargo applies `[env]` to build scripts). Set `CC`/`CXX`/`AR` with the
`aarch64_linux_android` suffix (underscore form is what cc-rs reads; dash
form set as belt-and-braces). After this single fix, **everything compiled
and linked clean** — `ring`, `secp256k1`, rusqlite (sqlite linkage came for
free via feature unification with nostr-database's libsqlite3-sys), and
peridot-sync itself including the `rustix`-heavy `apply.rs`.

Notable negative results (things we expected to break but didn't):
- `rusqlite 0.40.2` (non-bundled): linked fine — `nostr-sdk`'s sqlite dep
  enabled `libsqlite3-sys/bundled`, which compiled sqlite3.c with the NDK
  clang (after the CC fix) and satisfied rusqlite's link via feature
  unification. If nostr-sdk ever drops sqlite, add
  `features = ["bundled"]` to rusqlite.
- `rustix` `fs` feature (openat2 etc. in `apply.rs`): compiles as-is for
  aarch64-linux-android.
- `notify 8.2`: compiles (and peridot-sync's src doesn't even reference it).

## Copy-pasteable recipe

```bash
# 0) Toolchain
export PATH="$HOME/.cargo/bin:$PATH"
NDK="$HOME/Android/Sdk/ndk/28.2.13676358"
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
rustup target add aarch64-linux-android

# 1) Spike crate
mkdir -p ~/projects/peridot-android-spike && cd ~/projects/peridot-android-spike
cargo init --lib --name peridot_ffi
mkdir -p .cargo

# 2) .cargo/config.toml — linker + cc-rs env (the critical fix)
cat > .cargo/config.toml <<'EOF'
[target.aarch64-linux-android]
linker = "/home/moltbot/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang"

[env]
CC_aarch64_linux_android = "/home/moltbot/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang"
CXX_aarch64_linux_android = "/home/moltbot/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang++"
AR_aarch64_linux_android = "/home/moltbot/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar"
CC_aarch64-linux-android = "/home/moltbot/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang"
CXX_aarch64-linux-android = "/home/moltbot/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang++"
AR_aarch64-linux-android = "/home/moltbot/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar"
EOF

# 3) Cargo.toml — path dep on the UNMODIFIED peridot checkout
cat > Cargo.toml <<'EOF'
[package]
name = "peridot_ffi"
version = "0.1.0"
edition = "2024"

[lib]
crate-type = ["cdylib", "lib"]
name = "peridot_ffi"

# Bin shim so `cargo run --bin uniffi-bindgen` works from this crate
# (cargo can't run bin targets of dependencies directly).
[[bin]]
name = "uniffi-bindgen"
path = "uniffi-bindgen.rs"

[dependencies]
peridot-sync = { path = "../peridot/crates/peridot-sync" }
uniffi = { version = "0.29", features = ["build"] }
EOF

# 4) uniffi-bindgen bin shim
cat > uniffi-bindgen.rs <<'EOF'
fn main() {
    uniffi::uniffi_bindgen_main()
}
EOF

# 5) src/lib.rs — tiny proc-macro facade (real peridot-sync call included
#    so the dep tree is genuinely linked, not dead-stripped)
cat > src/lib.rs <<'EOF'
uniffi::setup_scaffolding!("peridot");

#[derive(uniffi::Object)]
pub struct PeridotEngine;

#[uniffi::export]
impl PeridotEngine {
    #[uniffi::constructor]
    pub fn new() -> Self { PeridotEngine }

    pub fn ping(&self) -> String { "pong".to_string() }

    pub fn engine_version(&self) -> String { "peridot-sync spike".to_string() }

    /// Calls real peridot-sync code (manifest::is_clean_relative).
    pub fn path_is_clean(&self, path: String) -> bool {
        peridot_sync::manifest::is_clean_relative(&path)
    }
}
EOF

# 6) Cross-compile  →  libperidot_ffi.so
cargo build -p peridot_ffi --target aarch64-linux-android

# 7) Kotlin bindings (library mode — uniffi 0.29 reads metadata straight
#    from the ELF via goblin, so the Android .so works on an x86_64 host)
cargo run --features uniffi/cli --bin uniffi-bindgen -- \
  generate --library target/aarch64-linux-android/debug/libperidot_ffi.so \
  --language kotlin --out-dir ./kotlin-out
# → kotlin-out/uniffi/peridot/peridot.kt  (ktlint warning is cosmetic)
```

## Notes for the real integration

- **No vendoring required.** The task's fallback plan (feature-gating
  `scan.rs`/`apply.rs`) never became necessary — both compile for Android.
- Kotlin uses JNA (`com.sun.jna`) — the app will need `net.java.dev.jna`
  plus the uniffi runtime pattern from the generated file, or a switch to
  `--config kn_lane`-style alternatives later. No app integration was done
  (out of scope).
- Debug `.so` is 149 MB (debuginfo, not stripped). A release build +
  `strip` will land in the low single-digit MB; not measured in this spike.
- Two cosmetic deviations from the task's suggested commands: (a) the
  `uniffi-bindgen` bin had to be a local shim because cargo cannot run a
  dependency's bin target; (b) ktlint isn't installed on this host, so the
  generated .kt is unformatted (add `--no-format` to silence).

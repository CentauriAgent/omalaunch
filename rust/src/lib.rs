//! Spike FFI facade over `peridot-sync`, built to prove the Android
//! cross-compile + UniFFI Kotlin pipeline. The surface is intentionally
//! tiny: `ping`/`engine_version` prove the pipeline, `path_is_clean`
//! forces real peridot-sync code (and its whole dependency tree) to be
//! linked into the cdylib, not dead-stripped away.

uniffi::setup_scaffolding!("peridot");

#[derive(uniffi::Object)]
pub struct PeridotEngine;

#[uniffi::export]
impl PeridotEngine {
    #[uniffi::constructor]
    pub fn new() -> Self {
        PeridotEngine
    }

    /// Round-trip check: callable from Kotlin, returns "pong".
    pub fn ping(&self) -> String {
        "pong".to_string()
    }

    /// Hardcoded for the spike — real version wiring comes later.
    pub fn engine_version(&self) -> String {
        "peridot-sync spike".to_string()
    }

    /// Calls real peridot-sync code (`manifest::is_clean_relative`).
    pub fn path_is_clean(&self, path: String) -> bool {
        peridot_sync::manifest::is_clean_relative(&path)
    }
}

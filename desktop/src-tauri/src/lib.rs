// Learn more about Tauri commands at https://tauri.app/develop/calling-rust/

use serde::Serialize;
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Debug, Serialize)]
#[serde(rename_all = "lowercase")]
enum AppPlatform {
    Windows,
    Macos,
    Linux,
}

#[derive(Debug, Serialize)]
struct AppInfo {
    name: String,
    version: String,
    build_type: String,
    platform: AppPlatform,
}

#[derive(Debug, Serialize)]
pub struct PingResult {
    pub ok: bool,
    pub ts: u128,
}

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[tauri::command]
fn ping() -> PingResult {
    PingResult {
        ok: true,
        ts: SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_millis())
            .unwrap_or(0),
    }
}

#[tauri::command]
fn app_info() -> AppInfo {
    build_app_info()
}

fn build_app_info() -> AppInfo {
    AppInfo {
        name: env!("CARGO_PKG_NAME").to_string(),
        version: env!("CARGO_PKG_VERSION").to_string(),
        build_type: if cfg!(debug_assertions) {
            "debug".to_string()
        } else {
            "release".to_string()
        },
        platform: current_platform(),
    }
}

fn current_platform() -> AppPlatform {
    if cfg!(target_os = "windows") {
        AppPlatform::Windows
    } else if cfg!(target_os = "macos") {
        AppPlatform::Macos
    } else {
        AppPlatform::Linux
    }
}

// `app_info` returns a strongly-typed `AppInfo`. For integration tests we
// want a stable JSON shape; convert the enum to a string here so the
// public API surface matches what `IpcCommands["app_info"]["result"]`
// declares on the TS side ("windows" | "macos" | "linux").
#[derive(Debug, Serialize)]
pub struct AppInfoJson {
    pub name: String,
    pub version: String,
    pub build_type: String,
    pub platform: String,
}

/// Test-only helper used by `tests/builtin.rs`. Builds the same JSON
/// shape that the IPC layer returns to the renderer.
pub fn app_info_for_test() -> AppInfoJson {
    let info = build_app_info();
    AppInfoJson {
        name: info.name,
        version: info.version,
        build_type: info.build_type,
        platform: match info.platform {
            AppPlatform::Windows => "windows",
            AppPlatform::Macos => "macos",
            AppPlatform::Linux => "linux",
        }
        .to_string(),
    }
}

/// Test-only helper used by `tests/builtin.rs`.
pub fn ping_response_for_test() -> PingResult {
    ping()
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![greet, ping, app_info])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

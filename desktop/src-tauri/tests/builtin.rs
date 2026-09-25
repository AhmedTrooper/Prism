//! Smoke tests for the built-in Tauri commands that ship with the
//! desktop scaffold. Each test calls the underlying implementation
//! without going through the Tauri runtime so they stay fast and
//! independent of the OS event loop.

use prism_lib::app_info_for_test;
use prism_lib::ping_response_for_test;

#[test]
fn ping_reports_ok_with_positive_timestamp() {
    let resp = ping_response_for_test();
    assert!(resp.ok, "ping should always succeed");
    assert!(resp.ts > 0, "ping should return a positive timestamp");
}

#[test]
fn app_info_matches_cargo_metadata() {
    let info = app_info_for_test();
    assert_eq!(info.name, env!("CARGO_PKG_NAME"));
    assert_eq!(info.version, env!("CARGO_PKG_VERSION"));
    // Debug builds report "debug"; release builds report "release".
    let expected = if cfg!(debug_assertions) {
        "debug"
    } else {
        "release"
    };
    assert_eq!(info.build_type, expected);
}

#[test]
fn app_info_platform_is_one_of_known_values() {
    let info = app_info_for_test();
    let valid = matches!(info.platform.as_str(), "windows" | "macos" | "linux");
    assert!(valid, "platform must be one of windows/macos/linux");
}

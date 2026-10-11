use std::ffi::c_void;
use std::sync::atomic::{AtomicU64, Ordering};

static SEQUENCE: AtomicU64 = AtomicU64::new(0x4E43_4C49_434B_4552);

fn mix64(mut value: u64) -> u64 {
    value = value.wrapping_add(0x9E37_79B9_7F4A_7C15);
    value = (value ^ (value >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
    value = (value ^ (value >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
    value ^ (value >> 31)
}

/// JNI method used by com.nclicker.engine.NativeEngine.
#[no_mangle]
pub extern "system" fn Java_com_nclicker_engine_NativeEngine_nativeTicket(
    _env: *mut c_void,
    _object: *mut c_void,
    seed: i64,
    bound: i32,
) -> i32 {
    if bound <= 0 {
        return 0;
    }
    let sequence = SEQUENCE.fetch_add(0x9E37_79B9_7F4A_7C15, Ordering::Relaxed);
    let mixed = mix64((seed as u64) ^ sequence ^ (bound as u64));
    (mixed % bound as u64) as i32
}

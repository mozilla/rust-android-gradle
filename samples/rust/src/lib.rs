extern crate jni;

use std::ffi::CString;
use std::os::raw::c_char;

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JValue};

pub type Callback = unsafe extern "C" fn(*const c_char) -> ();

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn invokeCallbackViaJNA(callback: Callback) {
    let s = CString::new("Hello from Rust").unwrap();
    unsafe { callback(s.as_ptr()); }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_nishtahir_androidrust_MainActivity_invokeCallbackViaJNI(
    env: JNIEnv,
    _class: JClass,
    callback: JObject
) {
    let s = String::from("Hello from Rust");
    let response = env.new_string(&s)
        .expect("Couldn't create java string!");
    env.call_method(callback, "callback", "(Ljava/lang/String;)V",
                    &[JValue::from(JObject::from(response))]).unwrap();
}

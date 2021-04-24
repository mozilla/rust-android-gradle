extern crate jni;

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JValue};

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_nishtahir_androidrust_MainActivity_startRequestFromJni(env: JNIEnv,
                                                                                  _class: JClass,
                                                                                  callback: JObject) {
    env.call_method(callback, "hello", "()V", &[]).unwrap();

    let s = String::from("hello from Rust");
    let response = env.new_string(&s)
                    .expect("Couldn't create java string!");
    env.call_method(callback, "appendToTextView", "(Ljava/lang/String;)V",
    &[JValue::from(JObject::from(response))]).unwrap();
}

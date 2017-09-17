extern crate jni;
extern crate futures;
extern crate hyper;
extern crate tokio_core;

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JValue};

use futures::{Future, Stream};
use futures::future;
use hyper::{Client, Error};

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_nishtahir_androidrust_MainActivity_startRequestFromJni(env: JNIEnv,
                                                                                  _class: JClass,
                                                                                  callback: JObject) {
    env.call_method(callback, "hello", "()V", &[]).unwrap();

    let url = "http://echo.jsontest.com/title/ipsum/content/blah".parse::<hyper::Uri>().unwrap();
    let mut core = tokio_core::reactor::Core::new().unwrap();
    let handle = core.handle();
    let client = Client::new(&handle);

    let work = client.get(url).and_then(|res| {

        res.body().fold(Vec::new(), |mut v, chunk| {
                        v.extend(&chunk[..]);
                        future::ok::<_, Error>(v)
                    }).and_then(|chunks| {
                        let s = String::from_utf8(chunks).unwrap();

                        let response = env.new_string(&s)
                                        .expect("Couldn't create java string!");
                        env.call_method(callback, "appendToTextView", "(Ljava/lang/String;)V",
                        &[JValue::from(JObject::from(response))]).unwrap();


                        future::ok::<_, Error>(s)
                    })
    });
    let _ = core.run(work).unwrap();
}
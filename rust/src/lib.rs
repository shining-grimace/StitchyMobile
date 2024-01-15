
use jni::{JNIEnv, objects::{JClass, JObject, JValue, JString}};
use std::ffi::CStr;

#[derive(Debug)]
enum Error {
    Jni(jni::errors::Error),
    Unknown(String)
}

impl From<jni::errors::Error> for Error {
    fn from(value: jni::errors::Error) -> Self {
        Error::Jni(value)
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_com_shininggrimace_stitchy_MainActivity_runStitchy(
    mut env: JNIEnv,
    _: JClass,
    uri: JString
) {
    if let Err(e) = process_image(&mut env, uri) {
        println!("Error logging message: {:?}", e);
    };
}

fn log_message(
    env: &mut JNIEnv,
    message: &str
) -> Result<(), jni::errors::Error> {
    let log_class = env.find_class("android/util/Log")?;
    let tag_string = JObject::from(env.new_string("StitchyMobile")?);
    let message_string = JObject::from(env.new_string(message)?);
    env.call_static_method(log_class, "d", "(Ljava/lang/String;Ljava/lang/String;)I", &[
        JValue::Object(&tag_string),
        JValue::Object(&message_string)
    ])?;
    Ok(())
}

fn process_image(
    env: &mut JNIEnv,
    uri: JString
) -> Result<(), Error> {
    let uri_ptr = env.get_string(&uri)?.as_ptr();
    let uri_cstr = unsafe { CStr::from_ptr(uri_ptr) };
    let uri_str = uri_cstr.to_str().unwrap();
    log_message(env, uri_str)?;
    Ok(())
}

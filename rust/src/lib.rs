
use jni::{JNIEnv, objects::{JClass, JObject, JValue, JString, JIntArray, ReleaseMode}, sys::jstring};
use std::{fs::File, os::fd::FromRawFd};

#[derive(Debug)]
enum Error {
    Jni(jni::errors::Error),
    Io(std::io::Error)
}

impl From<jni::errors::Error> for Error {
    fn from(value: jni::errors::Error) -> Self {
        Error::Jni(value)
    }
}

impl From<std::io::Error> for Error {
    fn from(value: std::io::Error) -> Self {
        Error::Io(value)
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_com_shininggrimace_stitchy_MainActivity_runStitchy(
    mut env: JNIEnv,
    _: JClass,
    input_fds: JIntArray
) -> jstring {
    let message = match process_images(&mut env, input_fds) {
        Ok(()) => "Files processed".to_owned(),
        Err(Error::Jni(e)) => format!("JNI error: {:?}", e),
        Err(Error::Io(e)) => format!("IO error: {:?}", e)
    };
    env.new_string(message).unwrap_or(JString::default()).into_raw()
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

fn process_images(
    env: &mut JNIEnv,
    input_fds: JIntArray
) -> Result<(), Error> {
    let fds = unsafe { env.get_array_elements(&input_fds, ReleaseMode::NoCopyBack)? };
    for fd in fds.into_iter() {
        let file = unsafe { File::from_raw_fd(*fd) };
        let metadata = file.metadata()?;
        let message = format!("File size: {}", metadata.len());
        log_message(env, &message)?;
    }
    Ok(())
}

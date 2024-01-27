
mod error;
mod log;
mod options;
mod stitch;

pub(crate) use error::Error;
pub(crate) use log::Logger;
pub(crate) use options::Options;

use jni::{JNIEnv, objects::{JClass, JString, JObjectArray, JIntArray, ReleaseMode}, sys::{jstring, jint}};

#[no_mangle]
pub unsafe extern "C" fn Java_com_shininggrimace_stitchy_MainActivity_runStitchy(
    mut env: JNIEnv,
    _: JClass,
    input_options_json: JString,
    input_fds: JIntArray,
    input_mime_types: JObjectArray,
    output_fd: jint,
    output_mime_type: JString
) -> jstring {
    let message = match run(&mut env, input_options_json, input_fds, input_mime_types, output_fd, output_mime_type) {
        Ok(()) => return JString::default().into_raw(),
        Err(Error::Jni(e)) => format!("JNI error: {:?}", e),
        Err(Error::Io(e)) => format!("IO error: {:?}", e),
        Err(Error::Image(e)) => format!("Image error: {:?}", e),
        Err(Error::Json(e)) => format!("JSON error: {:?}", e),
        Err(Error::Unknown(s)) => s
    };
    env.new_string(message).unwrap_or(JString::default()).into_raw()
}

fn run(
    env: &mut JNIEnv,
    input_options_json: JString,
    input_fds: JIntArray,
    input_mime_types: JObjectArray,
    output_fd: jint,
    output_mime_type: JString
) -> Result<(), Error> {
    let in_files = unsafe { env.get_array_elements(&input_fds, ReleaseMode::NoCopyBack)? };
    let mimes_length = env.get_array_length(&input_mime_types)?;
    let mut in_mimes: Vec<String> = vec![];
    for i in 0..mimes_length {
        let j_mime: JString = env.get_object_array_element(&input_mime_types, i)?.into();
        let mime = unsafe { env.get_string_unchecked(&j_mime)?.into() };
        in_mimes.push(mime)
    }
    let json_string: String = unsafe { env.get_string_unchecked(&input_options_json)?.into() };
    let mut options: Options = serde_json::from_str(json_string.as_str())?;
    options.prepare_for_use();
    let out_mime = unsafe { env.get_string_unchecked(&output_mime_type)?.into() };
    let logger = Logger::new(env)?;
    stitch::run_stitchy(logger, options, in_files, in_mimes, output_fd, out_mime)?;
    Ok(())
}

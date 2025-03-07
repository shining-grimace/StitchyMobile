mod error;
mod log;
mod options;
mod stitch;

pub(crate) use error::Error;
pub(crate) use log::Logger;
pub(crate) use options::Options;

use core::slice;
use jni::{
    objects::{JByteBuffer, JClass, JObjectArray, JString},
    sys::{jint, jstring},
    JNIEnv,
};

#[no_mangle]
pub unsafe extern "C" fn Java_com_shininggrimace_stitchy_MainActivity_runStitchy(
    mut env: JNIEnv,
    _: JClass,
    input_options_json: JString,
    input_buffers: JObjectArray,
    input_mime_types: JObjectArray,
    output_fd: jint,
    output_mime_type: JString,
) -> jstring {
    let message = match run(
        &mut env,
        input_options_json,
        input_buffers,
        input_mime_types,
        output_fd,
        output_mime_type,
    ) {
        Ok(()) => return JString::default().into_raw(),
        Err(Error::Jni(e)) => format!("JNI error: {:?}", e),
        Err(Error::Io(e)) => format!("IO error: {:?}", e),
        Err(Error::Image(e)) => format!("Image error: {:?}", e),
        Err(Error::Json(e)) => format!("JSON error: {:?}", e),
        Err(Error::Unknown(s)) => s,
    };
    let raw_message = env
        .new_string(message)
        .unwrap_or(JString::default())
        .into_raw();
    raw_message
}

fn run(
    env: &mut JNIEnv,
    input_options_json: JString,
    input_buffers: JObjectArray,
    input_mime_types: JObjectArray,
    output_fd: jint,
    output_mime_type: JString,
) -> Result<(), Error> {
    let (json_string, in_buffers, in_mimes, out_mime) = convert_jni_args(
        env,
        input_options_json,
        input_buffers,
        input_mime_types,
        output_mime_type,
    )?;
    let mut options: Options = serde_json::from_str(json_string.as_str())?;
    options.prepare_for_use();
    let logger = Logger::new(env)?;
    stitch::run_stitchy(logger, options, in_buffers, in_mimes, output_fd, out_mime)?;
    Ok(())
}

fn convert_jni_args<'a>(
    env: &mut JNIEnv,
    input_options_json: JString,
    input_buffers: JObjectArray,
    input_mime_types: JObjectArray,
    output_mime_type: JString,
) -> Result<(String, Vec<&'a [u8]>, Vec<String>, String), Error> {
    let mut in_buffers: Vec<&'a [u8]> = vec![];
    let count = env
        .get_array_length(&input_buffers)
        .map_err(|e| format!("Internal error: {:?}", e))?;
    for i in 0..count {
        let byte_buffer: JByteBuffer = env
            .get_object_array_element(&input_buffers, i)
            .map_err(|e| format!("Internal error: {:?}", e))?
            .into();
        let capacity = env
            .call_method(&byte_buffer, "remaining", "()I", &[])
            .map_err(|e| format!("Internal error: {:?}", e))?
            .i()
            .map_err(|e| format!("Internal error: {:?}", e))?;
        let buffer_ptr = env
            .get_direct_buffer_address(&byte_buffer)
            .map_err(|e| format!("Internal error: {:?}", e))?;
        let slice = unsafe { slice::from_raw_parts(buffer_ptr as *const u8, capacity as usize) };
        in_buffers.push(slice);
    }

    let mimes_length = env.get_array_length(&input_mime_types)?;
    let mut in_mimes: Vec<String> = vec![];
    for i in 0..mimes_length {
        let j_mime: JString = env.get_object_array_element(&input_mime_types, i)?.into();
        let mime = unsafe { env.get_string_unchecked(&j_mime)?.into() };
        in_mimes.push(mime)
    }
    let json_string: String = unsafe { env.get_string_unchecked(&input_options_json)?.into() };
    let out_mime = unsafe { env.get_string_unchecked(&output_mime_type)?.into() };

    Ok((json_string, in_buffers, in_mimes, out_mime))
}

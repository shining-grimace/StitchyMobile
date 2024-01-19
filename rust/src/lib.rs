
mod error;
mod log;

pub(crate) use error::Error;
pub(crate) use log::Logger;

use jni::{JNIEnv, objects::{AutoElements, JClass, JString, JObjectArray, JIntArray, ReleaseMode}, sys::{jstring, jint}};
use std::{fs::File, os::fd::FromRawFd};

#[no_mangle]
pub unsafe extern "C" fn Java_com_shininggrimace_stitchy_MainActivity_runStitchy(
    mut env: JNIEnv,
    _: JClass,
    input_fds: JIntArray,
    input_mime_types: JObjectArray
) -> jstring {
    let message = match run(&mut env, input_fds, input_mime_types) {
        Ok(()) => "Files processed".to_owned(),
        Err(Error::Jni(e)) => format!("JNI error: {:?}", e),
        Err(Error::Io(e)) => format!("IO error: {:?}", e),
        Err(Error::Unknown(s)) => s
    };
    env.new_string(message).unwrap_or(JString::default()).into_raw()
}

fn run(env: &mut JNIEnv, input_fds: JIntArray, input_mime_types: JObjectArray) -> Result<(), Error> {
    let fds = unsafe { env.get_array_elements(&input_fds, ReleaseMode::NoCopyBack)? };
    let mimes_length = env.get_array_length(&input_mime_types)?;
    let mut mime_types: Vec<String> = vec![];
    for i in 0..mimes_length {
        let j_mime: JString = env.get_object_array_element(&input_mime_types, i)?.into();
        let mime = unsafe { env.get_string_unchecked(&j_mime)?.into() };
        mime_types.push(mime)
    }
    let logger = Logger::new(env)?;
    process_images(logger, fds, mime_types)?;
    Ok(())
}

fn process_images(
    mut logger: Logger,
    fds: AutoElements<jint>,
    mimes: Vec<String>
) -> Result<(), Error> {

    let files: Vec<File> = fds.into_iter().map(|fd| unsafe { File::from_raw_fd(*fd) }).collect();
    if files.len() != mimes.len() {
        return Err(Error::Unknown("Internal error: mismatch in file data".to_owned()));
    }

    for (i, fd) in fds.into_iter().enumerate() {
        let file = unsafe { File::from_raw_fd(*fd) };
        let metadata = file.metadata()?;
        let message = format!("File {} has size {} bytes and MIME {}", i, metadata.len(), mimes[i]);
        logger.log_message(&message)?;
    }
    Ok(())
}

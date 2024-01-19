
mod error;
mod log;

pub(crate) use error::Error;
pub(crate) use log::Logger;

use jni::{JNIEnv, objects::{AutoElements, JClass, JString, JIntArray, ReleaseMode}, sys::{jstring, jint}};
use std::{fs::File, os::fd::FromRawFd};

#[no_mangle]
pub unsafe extern "C" fn Java_com_shininggrimace_stitchy_MainActivity_runStitchy(
    mut env: JNIEnv,
    _: JClass,
    input_fds: JIntArray
) -> jstring {
    let message = match run(&mut env, input_fds) {
        Ok(()) => "Files processed".to_owned(),
        Err(Error::Jni(e)) => format!("JNI error: {:?}", e),
        Err(Error::Io(e)) => format!("IO error: {:?}", e)
    };
    env.new_string(message).unwrap_or(JString::default()).into_raw()
}

fn run(env: &mut JNIEnv, input_fds: JIntArray) -> Result<(), Error> {
    let fds = unsafe { env.get_array_elements(&input_fds, ReleaseMode::NoCopyBack)? };
    let logger = Logger::new(env)?;
    process_images(logger, fds)?;
    Ok(())
}

fn process_images(
    mut logger: Logger,
    fds: AutoElements<jint>
) -> Result<(), Error> {
    for fd in fds.into_iter() {
        let file = unsafe { File::from_raw_fd(*fd) };
        let metadata = file.metadata()?;
        let message = format!("File size: {}", metadata.len());
        logger.log_message(&message)?;
    }
    Ok(())
}

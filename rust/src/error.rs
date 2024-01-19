
#[derive(Debug)]
pub enum Error {
    Jni(jni::errors::Error),
    Io(std::io::Error),
    Unknown(String)
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

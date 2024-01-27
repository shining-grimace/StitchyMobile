
#[derive(Debug)]
pub enum Error {
    Jni(jni::errors::Error),
    Io(std::io::Error),
    Image(stitchy_core::image::ImageError),
    Json(serde_json::Error),
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

impl From<stitchy_core::image::ImageError> for Error {
    fn from(value: stitchy_core::image::ImageError) -> Self {
        Error::Image(value)
    }
}

impl From<serde_json::Error> for Error {
    fn from(value: serde_json::Error) -> Self {
        Error::Json(value)
    }
}

impl From<String> for Error {
    fn from(value: String) -> Self {
        Error::Unknown(value)
    }
}


use jni::{JNIEnv, objects::{JClass, JObject, JValue}};

pub struct Logger<'a, 'b> {
    env: &'a mut JNIEnv<'b>,
    log_class: JClass<'b>,
    tag_string: JObject<'b>
}

impl<'a, 'b> Logger<'a, 'b> {

    pub fn new(env: &'a mut JNIEnv<'b>) -> Result<Self, jni::errors::Error> {
        let log_class = env.find_class("android/util/Log")?;
        let tag_string = JObject::from(env.new_string("StitchyMobile")?);
        Ok(Self {
            env,
            log_class,
            tag_string
        })
    }

    pub fn log_message(&mut self, message: &str) -> Result<(), jni::errors::Error> {
        let message_string = JObject::from(self.env.new_string(message)?);
        self.env.call_static_method(&self.log_class, "d", "(Ljava/lang/String;Ljava/lang/String;)I", &[
            JValue::Object(&self.tag_string),
            JValue::Object(&message_string)
        ])?;
        Ok(())
    }
}

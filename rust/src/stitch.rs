use crate::{Error, Logger, Options};
use jni::sys::jint;
use std::fs::File;
use std::io::BufWriter;
use std::time::SystemTime;
use stitchy_core::{
    image::{
        DynamicImage, FilterType, Frame, GifEncoder, ImageFormat, JpegEncoder, PngCompressionType,
        PngEncoder, PngFilterType,
    },
    FileLocation, FileProperties, ImageFiles, OwnedRawFdLocation, RawBufferLocation, Stitch,
};

pub fn run_stitchy<'a>(
    mut logger: Logger,
    options: Options,
    input_buffers: Vec<&'a [u8]>,
    input_mimes: Vec<String>,
    output_fd: jint,
    output_mime: String,
) -> Result<(), Error> {

    let mut images_builder = ImageFiles::builder();
    let now = SystemTime::now();

    for (i, buffer) in input_buffers.into_iter().enumerate() {
        if i >= input_mimes.len() {
            return Err(Error::Unknown(
                "Internal error: mismatch in file data".to_owned(),
            ));
        }
        let location = RawBufferLocation::new(buffer, input_mimes[i].clone(), now.clone());
        images_builder = images_builder.add_file(location)?;
        logger.log_message("File added to ImageFiles")?;
    }

    let images = images_builder.build().map_err(|e| Error::Unknown(e))?;

    let resize_filter = match options.fast {
        true => FilterType::Nearest,
        false => FilterType::Lanczos3,
    };

    let stitch_output = Stitch::builder()
        .alignment(options.get_alignment())
        .width_limit(options.maxw as u32)
        .height_limit(options.maxh as u32)
        .resize_filter(resize_filter)
        .image_files(images)?
        .stitch()?;

    logger.log_message("Stitch completed")?;

    let mut output_properties = OwnedRawFdLocation::new(output_fd, output_mime)
        .into_properties()
        .map_err(|e| Error::Unknown(e))?;
    let requested_output_format = output_properties
        .infer_format()
        .ok_or_else(|| Error::Unknown("Could not determine output format".to_owned()))?;

    let output_file = output_properties.borrow_file_mut();
    write_image_to_file(
        stitch_output,
        output_file,
        requested_output_format,
        options.quality,
        options.small,
    )?;

    logger.log_message("Output written; Stitchy completed successfully")?;

    Ok(())
}

pub fn write_image_to_file(
    image: DynamicImage,
    file: &File,
    format: ImageFormat,
    quality: u32,
    encode_smallest: bool,
) -> Result<(), String> {
    let mut file_writer = BufWriter::new(file);
    let result = match format {
        ImageFormat::Jpeg => {
            JpegEncoder::new_with_quality(file_writer, quality as u8).encode_image(&image)
        }
        ImageFormat::Png => {
            let mode = match encode_smallest {
                true => PngCompressionType::Best,
                false => PngCompressionType::Fast,
            };
            let encoder = PngEncoder::new_with_quality(file_writer, mode, PngFilterType::default());
            image.write_with_encoder(encoder)
        }
        ImageFormat::Gif => {
            let speed = match encode_smallest {
                true => 1,
                false => 10,
            };
            let mut encoder = GifEncoder::new_with_speed(file_writer, speed);
            encoder.encode_frame(Frame::new(image.to_rgba8()))
        }
        ImageFormat::Bmp => image.write_to(&mut file_writer, ImageFormat::Bmp),
        ImageFormat::WebP => image.write_to(&mut file_writer, ImageFormat::WebP),
        other_format => {
            panic!("Internal error: found format {:?}", other_format)
        }
    };
    result.map_err(|e| format!("Failed to generate output file - {}", e))
}

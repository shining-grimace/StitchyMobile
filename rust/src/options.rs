
use stitchy_core::AlignmentMode;
use serde::Deserialize;

#[derive(Deserialize)]
pub struct Options {
    pub horizontal: bool,
    pub vertical: bool,
    pub quality: u32,
    pub small: bool,
    pub fast: bool,
    pub maxd: u32,
    pub maxw: u32,
    pub maxh: u32
}

impl Options {
    pub fn prepare_for_use(&mut self) {
        if self.maxd > 0 {
            self.maxw = self.maxd;
            self.maxh = self.maxd;
        }
    }

    pub fn get_alignment(&self) -> AlignmentMode {
        match (self.horizontal, self.vertical) {
            (true, false) => AlignmentMode::Horizontal,
            (false, true) => AlignmentMode::Vertical,
            _ => AlignmentMode::Grid
        }
    }
}

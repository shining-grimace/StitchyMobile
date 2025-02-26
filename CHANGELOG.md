
### Unreleased

- Update `stitchy-core` dependency to 0.1.4, which brings these updates:
  - Fixed more failures to process files
  - Apply rotation metadata so images are oriented correctly (JPEG and WebP)
- Support WebP (lossless only)
- Add 'small mode' that encodes the smallest file possible (at the expense of speed); applies only
  to PNG and GIF
- Add 'fast mode' that uses nearest-neighbour filtering when copying files (fast but lower quality)

### 1.0.1 (May 11, 2024)

- Changed app display name to "Stitchy"
- Fixed quality setting reverting to 0 after saving a non-JPEG format then changing to JPEG
- Bumped `stitchy-core` dependency to 0.1.2, with these updates:
  - Fixed black lines sometimes appearing across images after resizing output
  - Fixed output sometimes failing to generate after resizing

### 1.0.0 (February 20, 2024)

- Initial release

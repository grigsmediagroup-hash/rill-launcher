# Rill identity

The mint three-line mark echoes the launcher's existing app icon: a small list with room around it. These are original project graphics, MIT licensed with the repository.

- `icon.svg`: scalable mark for project listings.
- `icon.png`: 512 × 512 raster icon.
- `banner.png`: 1280 × 640 README banner.
- `social-preview.png`: 1280 × 640 image ready for GitHub's social-preview setting.

The branding refresh does not change the Android APK or its icon. That binary remains the signed preview release.

To apply the share image, open the repository's Settings → General → Social preview and upload `social-preview.png`. Including an image in the README does not set GitHub's social-preview image automatically.

## Product showcase

The README now leads with `showcase-hero.png`: a real colorful wallpaper home screen, framed beside the product headline. `showcase-navigation.png` and `showcase-customization.png` continue the demo. Screenshots live in `docs/images/showcase/`; `tools/render-showcase.py` composes the surrounding original graphics with Pillow. It preserves the entire captured screen and does not synthesize app UI. These images replace the earlier text-only banner in the README. The APK is unchanged.

package io.deskpilot.engine.ocr;

public final class OcrConfig {

    public enum Preset {
        DEFAULT,
        TEXT_UI,
        LIGHT_BG
    }

    public final Preset preset;

    public final boolean grayscale;
    public final double scaleFactor;

    // optional future knobs
    public final Integer threshold01_255; // null = no threshold

    private OcrConfig(Preset preset, boolean grayscale, double scaleFactor, Integer threshold01_255) {
        this.preset = preset;
        this.grayscale = grayscale;
        this.scaleFactor = scaleFactor;
        this.threshold01_255 = threshold01_255;
    }

    public static OcrConfig defaults() {
        return preset(Preset.DEFAULT);
    }

    public static OcrConfig preset(Preset p) {
        if (p == null) throw new IllegalArgumentException("preset is null");

        return switch (p) {
            case DEFAULT  -> new OcrConfig(p, true, 2.5, null);
            case TEXT_UI  -> new OcrConfig(p, true, 3.0, 165);
            case LIGHT_BG -> new OcrConfig(p, true, 2.5, 200);
        };
    }

    public OcrConfig withScale(double factor) {
        if (factor <= 0) throw new IllegalArgumentException("scaleFactor must be > 0");
        return new OcrConfig(this.preset, this.grayscale, factor, this.threshold01_255);
    }

    public OcrConfig withThreshold(int t) {
        if (t < 0 || t > 255) throw new IllegalArgumentException("threshold must be 0..255");
        return new OcrConfig(this.preset, this.grayscale, this.scaleFactor, t);
    }
}

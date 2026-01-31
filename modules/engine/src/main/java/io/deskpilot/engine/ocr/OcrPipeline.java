package io.deskpilot.engine.ocr;

import io.deskpilot.engine.ImagePreprocess;

import java.awt.image.BufferedImage;

public final class OcrPipeline {

    private OcrPipeline() {}

   public static Result preprocess(BufferedImage cropped, OcrConfig cfg) {
    if (cropped == null) throw new IllegalArgumentException("cropped is null");
    if (cfg == null) cfg = OcrConfig.defaults();

    BufferedImage img = cropped;

    if (cfg.grayscale) {
        img = ImagePreprocess.toGray(img);
    }

    // ✅ enable thresholding now
    if (cfg.threshold01_255 != null) {
        img = ImagePreprocess.threshold(img, cfg.threshold01_255);
    }

    if (cfg.scaleFactor != 1.0) {
        img = ImagePreprocess.scale(img, cfg.scaleFactor);
    }

    return new Result(img, cfg.scaleFactor);
}


    public static final class Result {
        public final BufferedImage preprocessed;
        public final double scaleFactor;

        public Result(BufferedImage preprocessed, double scaleFactor) {
            this.preprocessed = preprocessed;
            this.scaleFactor = scaleFactor;
        }
    }
}

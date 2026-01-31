package io.deskpilot.engine.recording;

import io.deskpilot.engine.NormalizedRegion;

import java.awt.Rectangle;

public class RecorderValidation {

    private final RecorderPolicy policy;

    public RecorderValidation(RecorderPolicy policy) {
        this.policy = policy;
    }

    public record ValidatedRegion(NormalizedRegion region, Rectangle clampedWin32, ValidationResult result) {}

    public ValidatedRegion validateRegionSelection(Rectangle selectedWin32, Rectangle clientRectWin32, NormalizedRegion normalized) {
        if (selectedWin32 == null) {
            return new ValidatedRegion(null, null, ValidationResult.reject("Selected rect is null."));
        }
        if (clientRectWin32 == null || clientRectWin32.width <= 0 || clientRectWin32.height <= 0) {
            return new ValidatedRegion(normalized, null, ValidationResult.reject("Client rect invalid: " + clientRectWin32));
        }

        ValidationResult res = ValidationResult.ok();

        Rectangle clamped = intersect(selectedWin32, clientRectWin32);

        if (!rectEquals(selectedWin32, clamped)) {
            res.addWarn("Selection was partially outside the client area; clamped into client bounds.");
        }

        if (clamped.width <= 0 || clamped.height <= 0) {
            return new ValidatedRegion(normalized, clamped, res.addReject("Selection collapsed to empty after clamp."));
        }

        if (clamped.width < policy.minRegionPxW || clamped.height < policy.minRegionPxH) {
            return new ValidatedRegion(
                    normalized,
                    clamped,
                    res.addReject("Region too small: " + clamped.width + "x" + clamped.height +
                            "px (min " + policy.minRegionPxW + "x" + policy.minRegionPxH + "px).")
            );
        }

        // Safety on normalized values (should already be safe, but guard anyway)
        if (!isFinite01(normalized.xPct) || !isFinite01(normalized.yPct)
                || !isFinite01(normalized.wPct) || !isFinite01(normalized.hPct)) {
            return new ValidatedRegion(normalized, clamped, res.addReject("NormalizedRegion contains invalid percentages: " + normalized));
        }

        if (!(normalized.wPct > 0 && normalized.hPct > 0)) {
            return new ValidatedRegion(normalized, clamped, res.addReject("NormalizedRegion has non-positive size: " + normalized));
        }

        // OCR usability warning: very tiny normalized regions often produce garbage OCR (like single letters)
if (normalized.wPct < 0.08 || normalized.hPct < 0.03) {
    res.addWarn(
        "Region is very small in normalized size (wPct=" + normalized.wPct + ", hPct=" + normalized.hPct + "). " +
        "OCR may return junk (e.g., 'a'). Consider capturing a wider/taller region around the full text."
    );
}


        return new ValidatedRegion(normalized, clamped, res);
    }

    private static boolean isFinite01(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v >= 0.0 && v <= 1.0;
    }

    private static Rectangle intersect(Rectangle a, Rectangle b) {
        int x1 = Math.max(a.x, b.x);
        int y1 = Math.max(a.y, b.y);
        int x2 = Math.min(a.x + a.width, b.x + b.width);
        int y2 = Math.min(a.y + a.height, b.y + b.height);
        return new Rectangle(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }

    private static boolean rectEquals(Rectangle r1, Rectangle r2) {
        return r1.x == r2.x && r1.y == r2.y && r1.width == r2.width && r1.height == r2.height;
    }
}

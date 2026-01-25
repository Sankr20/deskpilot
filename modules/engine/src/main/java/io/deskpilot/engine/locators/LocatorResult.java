package io.deskpilot.engine.locators;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LocatorResult {

    public final LocatorKind kind;
    public final String label;

    public final LocateStatus status;

    // Optional geometry
    public final Point point;         // for POINT or click point for TEMPLATE/REGION when applicable
    public final Rectangle bounds;     // match bounds / region bounds
    public final double score;         // template confidence or OCR confidence (0..1), -1 if N/A

    // Diagnostics (kept lightweight; artifact writer can persist details)
    public final Map<String, String> diag;

   private LocatorResult(
        LocatorKind kind,
        String label,
        LocateStatus status,
        Point point,
        Rectangle bounds,
        double score,
        Map<String, String> diag
) {
    if (kind == null) throw new IllegalArgumentException("kind is null");
    if (status == null) throw new IllegalArgumentException("status is null");
    if (label == null || label.isBlank()) throw new IllegalArgumentException("label is blank");

    this.kind = kind;
    this.label = label;
    this.status = status;
    this.point = point;
    this.bounds = bounds;
    this.score = score;
    this.diag = diag == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(diag));
}


    public static LocatorResult found(LocatorKind kind, String label, Point point, Rectangle bounds, double score, Map<String,String> diag) {
        return new LocatorResult(kind, label, LocateStatus.FOUND, point, bounds, score, diag);
    }

    public static LocatorResult nearMiss(LocatorKind kind, String label, Point point, Rectangle bounds, double score, Map<String,String> diag) {
        return new LocatorResult(kind, label, LocateStatus.NEAR_MISS, point, bounds, score, diag);
    }

    public static LocatorResult notFound(LocatorKind kind, String label, Map<String,String> diag) {
        return new LocatorResult(kind, label, LocateStatus.NOT_FOUND, null, null, -1, diag);
    }

    public boolean isFound() { return status == LocateStatus.FOUND; }

    @Override
    public String toString() {
        return "LocatorResult{" +
                "kind=" + kind +
                ", label='" + label + '\'' +
                ", status=" + status +
                ", point=" + point +
                ", bounds=" + bounds +
                ", score=" + score +
                ", diagKeys=" + diag.keySet() +
                '}';
    }
public boolean exists() {
    return isFound();
}

public Point pointOrNull() {
    return point;
}

public Rectangle boundsOrNull() {
    return bounds;
}

public String summary() {
    return "status=" + status
            + ", point=" + point
            + ", bounds=" + bounds
            + ", score=" + score
            + ", diagKeys=" + diag.keySet();
}

    
}

package io.deskpilot.engine.recording;

import io.deskpilot.engine.NormalizedRegion;
import io.deskpilot.engine.locators.LocatorKind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public final class RegistryIndex {

    private static final String START = "// --- DESKPILOT:AUTOGEN:START";
    private static final String END   = "// --- DESKPILOT:AUTOGEN:END";
    private final Map<String, LocatorKind> locatorKindByConst;
    private final Set<String> uiMapConsts;
    private final Set<String> uiRegionConsts;
    private final Set<String> uiTemplateConsts;
    private final Set<String> locatorConsts;

    private final Map<String, double[]> pointsByConst;          // const -> [xPct,yPct]
    private final Map<String, NormalizedRegion> regionsByConst; // const -> region

private RegistryIndex(
        Set<String> uiMapConsts,
        Set<String> uiRegionConsts,
        Set<String> uiTemplateConsts,
        Set<String> locatorConsts,
        Map<String, LocatorKind> locatorKindByConst,
        Map<String, double[]> pointsByConst,
        Map<String, NormalizedRegion> regionsByConst
) {
    this.uiMapConsts = uiMapConsts;
    this.uiRegionConsts = uiRegionConsts;
    this.uiTemplateConsts = uiTemplateConsts;
    this.locatorConsts = locatorConsts;
    this.locatorKindByConst = locatorKindByConst;
    this.pointsByConst = pointsByConst;
    this.regionsByConst = regionsByConst;
}



    public static RegistryIndex load(EnginePaths paths) throws IOException {
        requireNonNull(paths, "paths");

        // UiMap
        ParsedFile uiMap = parseAutogen(paths.uiMap());
        Set<String> uiMapConsts = uiMap.constNames;
        Map<String, double[]> points = parseUiTargets(uiMap.blocksByConst);

        // UiRegions
        ParsedFile uiRegions = parseAutogen(paths.uiRegions());
        Set<String> uiRegionConsts = uiRegions.constNames;
        Map<String, NormalizedRegion> regions = parseRegions(uiRegions.blocksByConst);

        // UiTemplates
        ParsedFile uiTemplates = parseAutogen(paths.uiTemplates());
        Set<String> uiTemplateConsts = uiTemplates.constNames;

        // Locators
       ParsedFile locators = parseAutogen(paths.locators());
Set<String> locatorConsts = locators.constNames;
Map<String, LocatorKind> locatorKinds = parseLocatorKinds(locators.blocksByConst);

return new RegistryIndex(
        uiMapConsts,
        uiRegionConsts,
        uiTemplateConsts,
        locatorConsts,
        locatorKinds,
        points,
        regions
);


    }

    // -------------------------
    // Collision checks
    // -------------------------

    public boolean existsPointConst(String constName) {
        return uiMapConsts.contains(constName) || locatorConsts.contains(constName);
    }

    public boolean existsRegionConst(String constName) {
        return uiRegionConsts.contains(constName) || locatorConsts.contains(constName);
    }

    public boolean existsTemplateConst(String constName) {
        return uiTemplateConsts.contains(constName) || locatorConsts.contains(constName);
    }

    public boolean existsPointOrTemplateLocator(String constName) {
    return existsPointConst(constName) || existsTemplateConst(constName) || existsLocatorConst(constName);
}

public boolean existsRegionLocator(String constName) {
    return existsRegionConst(constName) || existsLocatorConst(constName);
}


    // -------------------------
    // Near-duplicate checks
    // -------------------------

    public Optional<String> findNearDuplicatePoint(String constName, double xPct, double yPct, double eps) {
        for (var e : pointsByConst.entrySet()) {
            String other = e.getKey();
            if (other.equals(constName)) continue;

            double[] p = e.getValue();
            if (Math.abs(p[0] - xPct) <= eps && Math.abs(p[1] - yPct) <= eps) {
                return Optional.of(other);
            }
        }
        return Optional.empty();
    }

    // -------------------------
// 13D: verbose near-duplicate hits
// -------------------------

public enum NearDupType { STRICT, IOU }

public record NearDupRegionHit(
        String otherConst,
        NearDupType type,
        double dx, double dy, double dw, double dh,
        double iou
) {}

public Optional<NearDupRegionHit> findNearDuplicateRegionVerbose(NormalizedRegion r, double eps, double minIou) {
    double minIouClamped = clamp01Allow0(minIou);

    for (var e : regionsByConst.entrySet()) {
        String other = e.getKey();
        NormalizedRegion o = e.getValue();

        double dx = Math.abs(o.xPct - r.xPct);
        double dy = Math.abs(o.yPct - r.yPct);
        double dw = Math.abs(o.wPct - r.wPct);
        double dh = Math.abs(o.hPct - r.hPct);

        boolean strict = dx <= eps && dy <= eps && dw <= eps && dh <= eps;
        if (strict) {
            return Optional.of(new NearDupRegionHit(other, NearDupType.STRICT, dx, dy, dw, dh, iou(o, r)));
        }

        if (minIouClamped > 0) {
            double i = iou(o, r);
            if (i >= minIouClamped) {
                return Optional.of(new NearDupRegionHit(other, NearDupType.IOU, dx, dy, dw, dh, i));
            }
        }
    }
    return Optional.empty();
}


    /**
     * Region near-duplicate:
     * - strict eps check on x,y,w,h (fast)
     * - OR IoU overlap check (human-friendly)
     *
     * @param eps    strict tolerance for x/y/w/h (e.g. 0.003)
     * @param minIou overlap threshold (e.g. 0.85). Set <=0 to disable IoU.
     */
    public Optional<String> findNearDuplicateRegion(NormalizedRegion r, double eps, double minIou) {
        double minIouClamped = clamp01Allow0(minIou); // allow 0 (disabled threshold handled below)

        for (var e : regionsByConst.entrySet()) {
            String other = e.getKey();
            NormalizedRegion o = e.getValue();

            // 1) strict (old behavior)
            boolean strict =
                    Math.abs(o.xPct - r.xPct) <= eps &&
                    Math.abs(o.yPct - r.yPct) <= eps &&
                    Math.abs(o.wPct - r.wPct) <= eps &&
                    Math.abs(o.hPct - r.hPct) <= eps;

            if (strict) return Optional.of(other);

            // 2) IoU overlap (better behavior)
            if (minIouClamped > 0) {
                double iou = iou(o, r);
                if (iou >= minIouClamped) return Optional.of(other);
            }
        }
        return Optional.empty();
    }

    /** Backwards-compatible wrapper (keeps your existing call sites working). */
    public Optional<String> findNearDuplicateRegion(NormalizedRegion r, double eps) {
        return findNearDuplicateRegion(r, eps, 0.85);
    }

    // IoU helpers (normalized space)
    private static double iou(NormalizedRegion a, NormalizedRegion b) {
        // clamp endpoints into [0..1] to stay safe even if a/b are slightly out-of-range
        double ax1 = clamp01(a.xPct);
        double ay1 = clamp01(a.yPct);
        double ax2 = clamp01(a.xPct + a.wPct);
        double ay2 = clamp01(a.yPct + a.hPct);

        double bx1 = clamp01(b.xPct);
        double by1 = clamp01(b.yPct);
        double bx2 = clamp01(b.xPct + b.wPct);
        double by2 = clamp01(b.yPct + b.hPct);

        // ensure ordering (just in case)
        double a1x = Math.min(ax1, ax2), a2x = Math.max(ax1, ax2);
        double a1y = Math.min(ay1, ay2), a2y = Math.max(ay1, ay2);
        double b1x = Math.min(bx1, bx2), b2x = Math.max(bx1, bx2);
        double b1y = Math.min(by1, by2), b2y = Math.max(by1, by2);

        double ix1 = Math.max(a1x, b1x);
        double iy1 = Math.max(a1y, b1y);
        double ix2 = Math.min(a2x, b2x);
        double iy2 = Math.min(a2y, b2y);

        double iw = Math.max(0, ix2 - ix1);
        double ih = Math.max(0, iy2 - iy1);
        double inter = iw * ih;

        double areaA = Math.max(0, a2x - a1x) * Math.max(0, a2y - a1y);
        double areaB = Math.max(0, b2x - b1x) * Math.max(0, b2y - b1y);
        double union = areaA + areaB - inter;

        if (union <= 0) return 0;
        return inter / union;
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private static double clamp01Allow0(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    // -------------------------
    // Parsing
    // -------------------------

    public record EnginePaths(Path uiMap, Path uiRegions, Path uiTemplates, Path locators) {
        public static EnginePaths fromRepoLayout() {
            Path base1 = Paths.get("modules", "engine", "src", "main", "java", "io", "deskpilot", "engine");
            Path base2 = Paths.get("src", "main", "java", "io", "deskpilot", "engine");

            Path base = Files.exists(base1) ? base1 : base2;

            return new EnginePaths(
                    base.resolve("UiMap.java"),
                    base.resolve("UiRegions.java"),
                    base.resolve("UiTemplates.java"),
                    base.resolve("Locators.java")
            );
        }
    }

    private static final class ParsedFile {
        final Set<String> constNames = new LinkedHashSet<>();
        final Map<String, List<String>> blocksByConst = new LinkedHashMap<>();
    }

    private static ParsedFile parseAutogen(Path file) throws IOException {
        ParsedFile out = new ParsedFile();
        if (file == null || !Files.exists(file)) return out;

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        int s = indexOfTrim(lines, START);
        int e = indexOfTrim(lines, END);
        if (s < 0 || e < 0 || e <= s) return out;

        List<String> autogen = new ArrayList<>(lines.subList(s + 1, e));
        List<Block> blocks = parseBlocks(autogen);

        for (Block b : blocks) {
            if (b.constName != null) {
                out.constNames.add(b.constName);
                out.blocksByConst.put(b.constName, b.lines);
            }
        }
        return out;
    }

    private static int indexOfTrim(List<String> lines, String exactTrimmed) {
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).trim().equals(exactTrimmed)) return i;
        return -1;
    }

    private static final class Block {
        final String constName;
        final List<String> lines;
        Block(String constName, List<String> lines) { this.constName = constName; this.lines = lines; }
    }

    private static List<Block> parseBlocks(List<String> autogenLines) {
        List<Block> out = new ArrayList<>();
        int i = 0;

        while (i < autogenLines.size()) {
            if (autogenLines.get(i).trim().isEmpty()) { i++; continue; }

            List<String> block = new ArrayList<>();

            // Optional Javadoc
            if (autogenLines.get(i).trim().startsWith("/**")) {
                while (i < autogenLines.size()) {
                    block.add(autogenLines.get(i));
                    if (autogenLines.get(i).trim().endsWith("*/")) { i++; break; }
                    i++;
                }
                while (i < autogenLines.size() && autogenLines.get(i).trim().isEmpty()) {
                    block.add(autogenLines.get(i));
                    i++;
                }
            }

            if (i >= autogenLines.size()) break;
            if (!autogenLines.get(i).trim().startsWith("public static final ")) { i++; continue; }

            while (i < autogenLines.size()) {
                block.add(autogenLines.get(i));
                if (autogenLines.get(i).trim().endsWith(";")) { i++; break; }
                i++;
            }

            String constName = parseConstName(block);
            if (constName != null) out.add(new Block(constName, block));
        }
        return out;
    }

    private static String parseConstName(List<String> lines) {
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("public static final ")) {
                String[] parts = t.split("\\s+");
                if (parts.length >= 5) {
                    String name = parts[4];
                    int eq = name.indexOf('=');
                    if (eq >= 0) name = name.substring(0, eq).trim();
                    return name;
                }
            }
        }
        return null;
    }

    private static final Pattern P_UITARGET =
            Pattern.compile("new\\s+UiTarget\\(\"[^\"]*\"\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*\\)");

    private static Map<String, double[]> parseUiTargets(Map<String, List<String>> blocksByConst) {
        Map<String, double[]> out = new LinkedHashMap<>();
        for (var e : blocksByConst.entrySet()) {
            String joined = String.join("\n", e.getValue());
            Matcher m = P_UITARGET.matcher(joined);
            if (m.find()) {
                try {
                    double x = Double.parseDouble(m.group(1));
                    double y = Double.parseDouble(m.group(2));
                    out.put(e.getKey(), new double[]{x, y});
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    private static final Pattern P_REGION =
            Pattern.compile("new\\s+NormalizedRegion\\(([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*\\)");

    private static Map<String, NormalizedRegion> parseRegions(Map<String, List<String>> blocksByConst) {
        Map<String, NormalizedRegion> out = new LinkedHashMap<>();
        for (var e : blocksByConst.entrySet()) {
            String joined = String.join("\n", e.getValue());
            Matcher m = P_REGION.matcher(joined);
            if (m.find()) {
                try {
                    double x = Double.parseDouble(m.group(1));
                    double y = Double.parseDouble(m.group(2));
                    double w = Double.parseDouble(m.group(3));
                    double h = Double.parseDouble(m.group(4));
                    out.put(e.getKey(), new NormalizedRegion(x, y, w, h));
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    

// -------------------------
// Locator existence + suggestions
// -------------------------

/** True if const exists specifically in Locators.java AUTOGEN. */
public boolean existsLocatorConst(String constName) {
    if (constName == null || constName.isBlank()) return false;
    return locatorConsts.contains(constName);
}

/** True if const exists in ANY registry (UiMap/UiRegions/UiTemplates/Locators). */
public boolean existsAnyLocatorConst(String constName) {
    if (constName == null || constName.isBlank()) return false;

    // NOTE: existsPointConst/existsRegionConst/existsTemplateConst already include locatorConsts
    return existsPointConst(constName)
            || existsRegionConst(constName)
            || existsTemplateConst(constName)
            || locatorConsts.contains(constName);
}

/** True only if const exists as a REGION const in UiRegions (not just a locator name). */
public boolean existsRegionConstOnly(String constName) {
    if (constName == null || constName.isBlank()) return false;
    return uiRegionConsts.contains(constName);
}

/**
 * Suggest a closest known const name (across UiMap/UiRegions/UiTemplates/Locators).
 * Returns empty if nothing is close enough.
 */
public Optional<String> suggestClosestLocator(String input) {
    if (input == null || input.isBlank()) return Optional.empty();

    String needle = input.trim()
            .replace("Locators.", "")
            .toUpperCase(java.util.Locale.ROOT);

    // Build candidate list from all registries
    List<String> all = new ArrayList<>();
    all.addAll(uiMapConsts);
    all.addAll(uiRegionConsts);
    all.addAll(uiTemplateConsts);
    all.addAll(locatorConsts);

    // 1) Exact
    for (String c : all) if (c.equals(needle)) return Optional.of(c);

    // 2) Contains / prefix heuristics
    for (String c : all) if (c.contains(needle)) return Optional.of(c);
    for (String c : all) if (needle.contains(c)) return Optional.of(c);
    for (String c : all) if (c.startsWith(needle) || needle.startsWith(c)) return Optional.of(c);

    // 3) Levenshtein best match
    int bestScore = Integer.MAX_VALUE;
    String best = null;

    for (String c : all) {
        int d = levenshtein(needle, c);
        if (d < bestScore) {
            bestScore = d;
            best = c;
        }
    }

    // Accept only if "close enough"
    if (best != null && bestScore <= Math.max(2, needle.length() / 4)) {
        return Optional.of(best);
    }

    return Optional.empty();
}

private static int levenshtein(String a, String b) {
    if (a == null) a = "";
    if (b == null) b = "";
    int n = a.length(), m = b.length();
    int[] prev = new int[m + 1];
    int[] cur = new int[m + 1];

    for (int j = 0; j <= m; j++) prev[j] = j;

    for (int i = 1; i <= n; i++) {
        cur[0] = i;
        char ca = a.charAt(i - 1);
        for (int j = 1; j <= m; j++) {
            int cost = (ca == b.charAt(j - 1)) ? 0 : 1;
            cur[j] = Math.min(
                    Math.min(cur[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
            );
        }
        int[] tmp = prev; prev = cur; cur = tmp;
    }
    return prev[m];
}
private static Map<String, LocatorKind> parseLocatorKinds(Map<String, List<String>> blocksByConst) {
    Map<String, LocatorKind> out = new LinkedHashMap<>();

    for (var e : blocksByConst.entrySet()) {
        String constName = e.getKey();
        String joined = String.join("\n", e.getValue());

        LocatorKind kind = LocatorKind.UNKNOWN;

        // Match your Locators factory usage:
        // point(...), region(...), template(...)
       // Detect OCR first (so it doesn't get misclassified)
if (joined.contains("ocrContains(") || joined.contains("new OcrContainsLocator")) kind = LocatorKind.OCR;
else if (joined.contains("point(")) kind = LocatorKind.POINT;
else if (joined.contains("region(")) kind = LocatorKind.REGION;
else if (joined.contains("template(")) kind = LocatorKind.TEMPLATE;


        out.put(constName, kind);
    }
    return out;
}

public boolean existsRegionLocatorConst(String constName) {
    if (constName == null || constName.isBlank()) return false;

    if (uiRegionConsts.contains(constName)) return true; // raw region const allowed
    return locatorKindByConst.getOrDefault(constName, LocatorKind.UNKNOWN) == LocatorKind.REGION;
}

public boolean existsPointLocatorConst(String constName) {
    if (constName == null || constName.isBlank()) return false;

    if (uiMapConsts.contains(constName)) return true; // raw point const allowed
    return locatorKindByConst.getOrDefault(constName, LocatorKind.UNKNOWN) == LocatorKind.POINT;
}

public boolean existsTemplateLocatorConst(String constName) {
    if (constName == null || constName.isBlank()) return false;

    if (uiTemplateConsts.contains(constName)) return true; // raw template const allowed
    return locatorKindByConst.getOrDefault(constName, LocatorKind.UNKNOWN) == LocatorKind.TEMPLATE;
}


// helper reused by all suggestion paths
private Optional<String> suggestClosestFromList(String needle, List<String> candidates) {
    if (candidates.isEmpty()) return Optional.empty();

    // 1) exact
    for (String c : candidates) if (c.equals(needle)) return Optional.of(c);

    // 2) contains / prefix
    for (String c : candidates) if (c.contains(needle)) return Optional.of(c);
    for (String c : candidates) if (needle.contains(c)) return Optional.of(c);
    for (String c : candidates) if (c.startsWith(needle) || needle.startsWith(c)) return Optional.of(c);

    // 3) Levenshtein
    int bestScore = Integer.MAX_VALUE;
    String best = null;

    for (String c : candidates) {
        int d = levenshtein(needle, c);
        if (d < bestScore) {
            bestScore = d;
            best = c;
        }
    }

    if (best != null && bestScore <= Math.max(2, needle.length() / 4)) {
        return Optional.of(best);
    }

    return Optional.empty();
}

public Optional<String> suggestClosestConstOfKind(String input, LocatorKind requiredKind) {
    if (input == null || input.isBlank()) return Optional.empty();
    if (requiredKind == null || requiredKind == LocatorKind.UNKNOWN) return Optional.empty();

    String needle = input.trim()
            .replace("Locators.", "")
            .toUpperCase(java.util.Locale.ROOT);

    List<String> candidates = new ArrayList<>();

    // Include raw Ui* consts as valid inputs too (universal)
    switch (requiredKind) {
        case POINT -> {
            candidates.addAll(uiMapConsts);
            for (String c : locatorConsts) {
                if (locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.POINT) {
                    candidates.add(c);
                }
            }
        }
        case REGION -> {
            candidates.addAll(uiRegionConsts);
            for (String c : locatorConsts) {
                if (locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.REGION) {
                    candidates.add(c);
                }
            }
        }
        case TEMPLATE -> {
            candidates.addAll(uiTemplateConsts);
            for (String c : locatorConsts) {
                if (locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.TEMPLATE) {
                    candidates.add(c);
                }
            }
        }
        case OCR -> {
    for (String c : locatorConsts) {
        if (locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.OCR) {
            candidates.add(c);
        }
    }
}

        default -> { return Optional.empty(); }
    }

    // 1) exact
    for (String c : candidates) if (c.equals(needle)) return Optional.of(c);

    // 2) contains / prefix
    for (String c : candidates) if (c.contains(needle)) return Optional.of(c);
    for (String c : candidates) if (needle.contains(c)) return Optional.of(c);
    for (String c : candidates) if (c.startsWith(needle) || needle.startsWith(c)) return Optional.of(c);

    // 3) Levenshtein
    int bestScore = Integer.MAX_VALUE;
    String best = null;
    for (String c : candidates) {
        int d = levenshtein(needle, c);
        if (d < bestScore) {
            bestScore = d;
            best = c;
        }
    }

    if (best != null && bestScore <= Math.max(2, needle.length() / 4)) {
        return Optional.of(best);
    }

    return Optional.empty();
}

// -------------------------
// Locator const existence
// -------------------------

/** True if the const exists and is an OCR locator const (kind=OCR). */
public boolean existsOcrLocatorConst(String constName) {
    if (constName == null || constName.isBlank()) return false;

    String c = constName.trim()
            .replace("Locators.", "")
            .toUpperCase(java.util.Locale.ROOT);

    return locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.OCR;
}


// --- helpers ---

private static String normalizeConst(String input) {
    if (input == null) return "";
    String s = input.trim();
    if (s.startsWith("Locators.")) s = s.substring("Locators.".length());
    return s.trim().toUpperCase(java.util.Locale.ROOT);
}


}

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
    // Const existence (typed)
    // -------------------------

    public boolean existsPointConst(String constName) {
        String c = normConst(constName);
        if (c.isEmpty()) return false;
        return uiMapConsts.contains(c) || locatorConsts.contains(c);
    }

    public boolean existsRegionConst(String constName) {
        String c = normConst(constName);
        if (c.isEmpty()) return false;
        return uiRegionConsts.contains(c) || locatorConsts.contains(c);
    }

    public boolean existsTemplateConst(String constName) {
        String c = normConst(constName);
        if (c.isEmpty()) return false;
        return uiTemplateConsts.contains(c) || locatorConsts.contains(c);
    }

    /** Exists in Locators.java AUTOGEN only. */
    public boolean existsLocatorConst(String constName) {
        String c = normConst(constName);
        if (c.isEmpty()) return false;
        return locatorConsts.contains(c);
    }

    /** ✅ REQUIRED by EngineRecordMode */
    public boolean existsAnyLocatorConst(String constName) {
        String c = normConst(constName);
        if (c.isEmpty()) return false;

        return uiMapConsts.contains(c)
                || uiRegionConsts.contains(c)
                || uiTemplateConsts.contains(c)
                || locatorConsts.contains(c);
    }

    public boolean existsOcrLocatorConst(String constName) {
        String c = normConst(constName);
        if (c.isEmpty()) return false;
        return locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.OCR;
    }

    /** Accepts raw UiMap const OR locator const that is POINT. */
    public boolean existsPointLocatorConst(String constName) {
        String c = normConst(constName);
        if (c.isEmpty()) return false;
        if (uiMapConsts.contains(c)) return true;
        return locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.POINT;
    }

    /** Accepts raw UiRegions const OR locator const that is REGION. */
    public boolean existsRegionLocatorConst(String constName) {
        String c = normConst(constName);
        if (c.isEmpty()) return false;
        if (uiRegionConsts.contains(c)) return true;
        return locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.REGION;
    }

    /** Accepts raw UiTemplates const OR locator const that is TEMPLATE. */
    public boolean existsTemplateLocatorConst(String constName) {
        String c = normConst(constName);
        if (c.isEmpty()) return false;
        if (uiTemplateConsts.contains(c)) return true;
        return locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.TEMPLATE;
    }

    // -------------------------
    // Near-duplicate checks (existing behavior)
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

    // IoU helpers (normalized space)
    private static double iou(NormalizedRegion a, NormalizedRegion b) {
        double ax1 = clamp01(a.xPct);
        double ay1 = clamp01(a.yPct);
        double ax2 = clamp01(a.xPct + a.wPct);
        double ay2 = clamp01(a.yPct + a.hPct);

        double bx1 = clamp01(b.xPct);
        double by1 = clamp01(b.yPct);
        double bx2 = clamp01(b.xPct + b.wPct);
        double by2 = clamp01(b.yPct + b.hPct);

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
    // Suggestions (✅ REQUIRED by EngineRecordMode)
    // -------------------------

    /** Suggest a closest known const (across UiMap/UiRegions/UiTemplates/Locators). */
    public Optional<String> suggestClosestLocator(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        String needle = normConst(input);
        if (needle.isEmpty()) return Optional.empty();

        List<String> all = new ArrayList<>(uiMapConsts.size() + uiRegionConsts.size() + uiTemplateConsts.size() + locatorConsts.size());
        all.addAll(uiMapConsts);
        all.addAll(uiRegionConsts);
        all.addAll(uiTemplateConsts);
        all.addAll(locatorConsts);

        // exact
        for (String c : all) if (c.equals(needle)) return Optional.of(c);

        // contains/prefix
        for (String c : all) if (c.contains(needle)) return Optional.of(c);
        for (String c : all) if (needle.contains(c)) return Optional.of(c);
        for (String c : all) if (c.startsWith(needle) || needle.startsWith(c)) return Optional.of(c);

        // Levenshtein best match
        int bestScore = Integer.MAX_VALUE;
        String best = null;
        for (String c : all) {
            int d = levenshtein(needle, c);
            if (d < bestScore) {
                bestScore = d;
                best = c;
            }
        }

        if (best != null && bestScore <= Math.max(2, needle.length() / 4)) return Optional.of(best);
        return Optional.empty();
    }

    public Optional<String> suggestClosestConstOfKind(String input, LocatorKind requiredKind) {
        if (input == null || input.isBlank()) return Optional.empty();
        if (requiredKind == null || requiredKind == LocatorKind.UNKNOWN) return Optional.empty();

        String needle = normConst(input);
        if (needle.isEmpty()) return Optional.empty();

        List<String> candidates = new ArrayList<>();

        switch (requiredKind) {
            case POINT -> {
                candidates.addAll(uiMapConsts);
                for (String c : locatorConsts) {
                    if (locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.POINT) candidates.add(c);
                }
            }
            case REGION -> {
                candidates.addAll(uiRegionConsts);
                for (String c : locatorConsts) {
                    if (locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.REGION) candidates.add(c);
                }
            }
            case TEMPLATE -> {
                candidates.addAll(uiTemplateConsts);
                for (String c : locatorConsts) {
                    if (locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.TEMPLATE) candidates.add(c);
                }
            }
            case OCR -> {
                for (String c : locatorConsts) {
                    if (locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.OCR) candidates.add(c);
                }
            }
            default -> { return Optional.empty(); }
        }

        // exact
        for (String c : candidates) if (c.equals(needle)) return Optional.of(c);

        // contains/prefix
        for (String c : candidates) if (c.contains(needle)) return Optional.of(c);
        for (String c : candidates) if (needle.contains(c)) return Optional.of(c);
        for (String c : candidates) if (c.startsWith(needle) || needle.startsWith(c)) return Optional.of(c);

        // Levenshtein
        int bestScore = Integer.MAX_VALUE;
        String best = null;
        for (String c : candidates) {
            int d = levenshtein(needle, c);
            if (d < bestScore) {
                bestScore = d;
                best = c;
            }
        }

        if (best != null && bestScore <= Math.max(2, needle.length() / 4)) return Optional.of(best);
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

    private static Map<String, LocatorKind> parseLocatorKinds(Map<String, List<String>> blocksByConst) {
        Map<String, LocatorKind> out = new LinkedHashMap<>();
        for (var e : blocksByConst.entrySet()) {
            String joined = String.join("\n", e.getValue());

            LocatorKind kind;
            if (joined.contains("ocrContains(") || joined.contains("new OcrContainsLocator")) kind = LocatorKind.OCR;
            else if (joined.contains("point(")) kind = LocatorKind.POINT;
            else if (joined.contains("region(")) kind = LocatorKind.REGION;
            else if (joined.contains("template(")) kind = LocatorKind.TEMPLATE;
            else kind = LocatorKind.UNKNOWN;

            out.put(e.getKey(), kind);
        }
        return out;
    }

    // -------------------------
    // Normalization helper
    // -------------------------

    private static String normConst(String s) {
        if (s == null) return "";
        String c = s.trim();
        if (c.startsWith("Locators.")) c = c.substring("Locators.".length());
        return c.trim().toUpperCase(Locale.ROOT);
    }

   public List<String> listOcrLocatorConsts() {
    List<String> out = new ArrayList<>();
    for (String c : locatorConsts) {
        if (locatorKindByConst.getOrDefault(c, LocatorKind.UNKNOWN) == LocatorKind.OCR) out.add(c);
    }
    out.sort(String::compareTo);
    return out;
}

public boolean existsUiRegionConst(String constName) {
    String c = normConst(constName);
    if (c.isEmpty()) return false;
    return uiRegionConsts.contains(c);
}

public List<String> listUiRegionConsts() {
    List<String> out = new ArrayList<>(uiRegionConsts);
    out.sort(String::compareTo);
    return out;
}




}

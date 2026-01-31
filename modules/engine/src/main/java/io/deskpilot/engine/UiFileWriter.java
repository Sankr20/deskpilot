package io.deskpilot.engine;

import io.deskpilot.engine.locators.Locator;
import io.deskpilot.engine.targets.SearchAreaPct;
import io.deskpilot.engine.targets.TemplateTarget;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes and reads AUTOGEN constants for:
 * - UiMap.java      (UiTarget points)
 * - UiRegions.java  (NormalizedRegion)
 * - UiTemplates.java(TemplateTarget)
 * - Locators.java   (Locator constants that reference UiMap/UiRegions/UiTemplates)
 *
 * Design:
 * - AUTOGEN blocks are parsed as "javadoc + public static final ... ;"
 * - upserts are stable and sorted by const name
 * - normalization pass keeps labels consistent and snake_case-friendly
 */
public final class UiFileWriter {
    private UiFileWriter() {}

    static final String START = "// --- DESKPILOT:AUTOGEN:START";
    static final String END   = "// --- DESKPILOT:AUTOGEN:END";

    // -------------------------
    // Public API (write)
    // -------------------------

    public static void upsertTarget(UiTarget t) throws IOException {
        Objects.requireNonNull(t, "UiTarget is null");

        Path file = resolveEngineJavaFile("UiMap.java");
        ensureParent(file);
        if (!Files.exists(file)) Files.writeString(file, defaultUiMapSkeleton(), StandardCharsets.UTF_8);

        String label = UiNaming.normalizeLabel(t.name(), UiNaming.Kind.POINT);
        String constName = UiNaming.toConst(label);

        List<String> newBlock = List.of(
                "",
                "    /** " + escapeJava(label) + " */",
                "    public static final UiTarget " + constName + " =",
                "            new UiTarget(\"" + escapeJava(label) + "\", " + fmt6(t.xPct()) + ", " + fmt6(t.yPct()) + ");"
        );

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> updated = upsertConstBlock(lines, "UiTarget", constName, newBlock);
        updated = normalizeAutogenFile("UiMap.java", updated);
        Files.write(file, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void upsertRegion(String rawName, NormalizedRegion r) throws IOException {
        Objects.requireNonNull(rawName, "Region name is null");
        Objects.requireNonNull(r, "NormalizedRegion is null");

        Path file = resolveEngineJavaFile("UiRegions.java");
        ensureParent(file);
        if (!Files.exists(file)) Files.writeString(file, defaultUiRegionsSkeleton(), StandardCharsets.UTF_8);

        String label = UiNaming.normalizeLabel(rawName, UiNaming.Kind.REGION);
        String constName = UiNaming.toConst(label);

        List<String> newBlock = List.of(
                "",
                "    /** " + escapeJava(label) + " */",
                "    public static final NormalizedRegion " + constName + " =",
                "            new NormalizedRegion(" + fmt6(r.xPct) + ", " + fmt6(r.yPct) + ", " + fmt6(r.wPct) + ", " + fmt6(r.hPct) + ");"
        );

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> updated = upsertConstBlock(lines, "NormalizedRegion", constName, newBlock);
        updated = normalizeAutogenFile("UiRegions.java", updated);
        Files.write(file, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void upsertTemplate(TemplateTarget t) throws IOException {
        Objects.requireNonNull(t, "TemplateTarget is null");

        Path file = resolveEngineJavaFile("UiTemplates.java");
        ensureParent(file);
        if (!Files.exists(file)) Files.writeString(file, defaultUiTemplatesSkeleton(), StandardCharsets.UTF_8);

        String label = UiNaming.normalizeLabel(t.name, UiNaming.Kind.TEMPLATE);
        String constName = UiNaming.toConst(label);

        String[] paths = cleanedPaths(t);
        String ofLine = buildTemplateOfLine(label, paths);

        List<String> newBlock = new ArrayList<>();
        newBlock.add("");
        newBlock.add("    /** " + escapeJava(label) + " */");
        newBlock.add("    public static final TemplateTarget " + constName + " =");
        newBlock.add("            " + ofLine);

        if (t.searchAreaPct != null) {
            newBlock.add("                    .withSearchAreaPct(new io.deskpilot.engine.targets.SearchAreaPct(" +
                    fmt6(t.searchAreaPct.xPct) + ", " + fmt6(t.searchAreaPct.yPct) + ", " +
                    fmt6(t.searchAreaPct.wPct) + ", " + fmt6(t.searchAreaPct.hPct) + "))");
        } else if (t.searchArea != null) {
            newBlock.add("                    .withSearchArea(new java.awt.Rectangle(" +
                    t.searchArea.x + ", " + t.searchArea.y + ", " +
                    t.searchArea.width + ", " + t.searchArea.height + "))");
        }

        newBlock.add("                    .withMinScore(" + String.format(Locale.US, "%.2f", t.minScore) + ");");

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> updated = upsertConstBlock(lines, "TemplateTarget", constName, List.copyOf(newBlock));
        updated = normalizeAutogenFile("UiTemplates.java", updated);
        Files.write(file, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Adds an image path variant to an existing template constant by:
     * - parsing existing TemplateTarget.of("label","p1","p2"...)
     * - merging newResourcePath in insertion-order
     * - preserving minScore + searchAreaPct/searchArea
     */
    public static void addTemplateVariant(String rawName, String newResourcePath) throws IOException {
        String label = UiNaming.normalizeLabel(rawName, UiNaming.Kind.TEMPLATE);
        String constName = UiNaming.toConst(label);

        Path file = resolveEngineJavaFile("UiTemplates.java");
        if (!Files.exists(file)) throw new RuntimeException("UiTemplates.java not found at: " + file.toAbsolutePath());

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        int s = indexOfTrim(lines, START);
        int e = indexOfTrim(lines, END);
        if (s < 0 || e < 0 || e <= s) throw new RuntimeException("UiTemplates missing AUTOGEN markers");

        List<String> autogen = new ArrayList<>(lines.subList(s + 1, e));
        List<Block> blocks = parseBlocks(autogen);

        Block existing = null;
        for (Block b : blocks) {
            if (constName.equals(b.constName)) { existing = b; break; }
        }

        if (existing == null) {
            TemplateTarget t = TemplateTarget.of(label, newResourcePath).withMinScore(0.85);
            upsertTemplate(t);
            return;
        }

        ParsedTemplate pt = parseTemplateBlock(existing.lines);

        LinkedHashSet<String> merged = new LinkedHashSet<>(pt.paths);
        merged.add(newResourcePath);

        TemplateTarget out =
                TemplateTarget.of(label, merged.toArray(new String[0]))
                        .withMinScore(pt.minScore);

        if (pt.searchAreaPct != null) out = out.withSearchAreaPct(pt.searchAreaPct);
        else if (pt.searchArea != null) out = out.withSearchArea(pt.searchArea);

        upsertTemplate(out);
    }

    public static void upsertLocatorPoint(String rawName) throws IOException {
        String label = UiNaming.normalizeLabel(rawName, UiNaming.Kind.POINT);
        String constName = UiNaming.toConst(label);

        Path file = resolveEngineJavaFile("Locators.java");
        ensureParent(file);
        if (!Files.exists(file)) throw new RuntimeException("Locators.java not found at: " + file.toAbsolutePath());

        List<String> newBlock = List.of(
                "",
                "    /** " + escapeJava(label) + " */",
                "    public static final Locator " + constName + " =",
                "            point(UiMap." + constName + ");"
        );

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> updated = upsertConstBlock(lines, "Locator", constName, newBlock);
        updated = normalizeAutogenFile("Locators.java", updated);
        Files.write(file, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void upsertLocatorRegion(String rawName) throws IOException {
        String label = UiNaming.normalizeLabel(rawName, UiNaming.Kind.REGION);
        String constName = UiNaming.toConst(label);

        Path file = resolveEngineJavaFile("Locators.java");
        ensureParent(file);
        if (!Files.exists(file)) throw new RuntimeException("Locators.java not found at: " + file.toAbsolutePath());

        List<String> newBlock = List.of(
                "",
                "    /** " + escapeJava(label) + " */",
                "    public static final Locator " + constName + " =",
                "            region(\"" + escapeJava(label) + "\", UiRegions." + constName + ");"
        );

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> updated = upsertConstBlock(lines, "Locator", constName, newBlock);
        updated = normalizeAutogenFile("Locators.java", updated);
        Files.write(file, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void upsertLocatorTemplate(String rawName) throws IOException {
        String label = UiNaming.normalizeLabel(rawName, UiNaming.Kind.TEMPLATE);
        String constName = UiNaming.toConst(label);

        Path file = resolveEngineJavaFile("Locators.java");
        ensureParent(file);
        if (!Files.exists(file)) throw new RuntimeException("Locators.java not found at: " + file.toAbsolutePath());

        List<String> newBlock = List.of(
                "",
                "    /** " + escapeJava(label) + " */",
                "    public static final Locator " + constName + " =",
                "            template(UiTemplates." + constName + ");"
        );

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> updated = upsertConstBlock(lines, "Locator", constName, newBlock);
        updated = normalizeAutogenFile("Locators.java", updated);
        Files.write(file, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Writes an OCR locator constant into Locators AUTOGEN:
     *   ocrContains("label", UiRegions.REGION_CONST, "containsText")
     *
     * Caller may pass uiRegionConst as "UiRegions.X" or just "X".
     */
    public static void upsertLocatorOcrContains(String rawName,
                                               String uiRegionConst,
                                               String containsText) throws IOException {
        Objects.requireNonNull(rawName, "rawName is null");
        Objects.requireNonNull(uiRegionConst, "uiRegionConst is null");
        Objects.requireNonNull(containsText, "containsText is null");

        // Normalize OCR locator label as REGION-kind (it is tied to a region)
        String label = UiNaming.normalizeLabel(rawName, UiNaming.Kind.REGION);
        String constName = UiNaming.toConst(label);

        String regionConst = uiRegionConst.trim();
        if (regionConst.startsWith("UiRegions.")) regionConst = regionConst.substring("UiRegions.".length());
        regionConst = regionConst.trim();

        Path file = resolveEngineJavaFile("Locators.java");
        ensureParent(file);
        if (!Files.exists(file)) throw new RuntimeException("Locators.java not found at: " + file.toAbsolutePath());

        List<String> newBlock = List.of(
                "",
                "    /** " + escapeJava(label) + " */",
                "    public static final Locator " + constName + " =",
                "            ocrContains(\"" + escapeJava(label) + "\", UiRegions." + regionConst + ", \"" + escapeJava(containsText) + "\");"
        );

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> updated = upsertConstBlock(lines, "Locator", constName, newBlock);
        updated = normalizeAutogenFile("Locators.java", updated);
        Files.write(file, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // -------------------------
    // Public API (read helpers)
    // -------------------------

    /** Checks if const exists in AUTOGEN for the given file (UiMap/UiRegions/UiTemplates/Locators). */
    public static boolean existsConstInFile(String fileName, String constName) throws IOException {
        Path file = resolveEngineJavaFile(fileName);
        if (!Files.exists(file)) return false;

        Map<String, List<String>> blocks = readAutogenBlocks(file);
        return blocks.containsKey(constName);
    }

    /** Reads AUTOGEN blocks keyed by constName -> blockLines. */
    public static Map<String, List<String>> readAutogenBlocks(String fileName) throws IOException {
        return readAutogenBlocks(resolveEngineJavaFile(fileName));
    }

    /** UiRegions constants parsed into a map. */
    public static Map<String, NormalizedRegion> readUiRegionsMap() throws IOException {
        Path file = resolveEngineJavaFile("UiRegions.java");
        if (!Files.exists(file)) return Map.of();

        Map<String, List<String>> blocks = readAutogenBlocks(file);

        Map<String, NormalizedRegion> out = new LinkedHashMap<>();
        for (var e : blocks.entrySet()) {
            NormalizedRegion r = parseRegionFromBlock(e.getValue());
            if (r != null) out.put(e.getKey(), r);
        }
        return out;
    }

    /** UiMap UiTarget points parsed into const -> [xPct,yPct]. */
    public static Map<String, double[]> readUiMapPoints() throws IOException {
        Path file = resolveEngineJavaFile("UiMap.java");
        if (!Files.exists(file)) return Map.of();

        Map<String, List<String>> blocks = readAutogenBlocks(file);

        Map<String, double[]> out = new LinkedHashMap<>();
        for (var e : blocks.entrySet()) {
            double[] p = parsePointFromBlock(e.getValue());
            if (p != null) out.put(e.getKey(), p);
        }
        return out;
    }

    // -------------------------
    // Core upsert (by CONST NAME)
    // -------------------------

    private static List<String> upsertConstBlock(List<String> fileLines,
                                                 String typeName,
                                                 String constName,
                                                 List<String> newBlock) {
        int s = indexOfTrim(fileLines, START);
        int e = indexOfTrim(fileLines, END);

        List<String> lines = fileLines;
        if (s < 0 || e < 0 || e <= s) {
            lines = injectMarkers(lines);
            s = indexOfTrim(lines, START);
            e = indexOfTrim(lines, END);
        }

        List<String> autogen = new ArrayList<>(lines.subList(s + 1, e));
        List<Block> blocks = parseBlocks(autogen);

        blocks.removeIf(b -> constName.equals(b.constName));
        blocks.add(new Block(typeName, constName, new ArrayList<>(newBlock)));
        blocks.sort(Comparator.comparing(b -> b.constName));

        List<String> rebuilt = new ArrayList<>();
        for (Block b : blocks) rebuilt.addAll(b.lines);

        List<String> out = new ArrayList<>();
        out.addAll(lines.subList(0, s + 1));
        out.addAll(rebuilt);
        out.addAll(lines.subList(e, lines.size()));
        return out;
    }

    // -------------------------
    // AUTOGEN block parsing (shared)
    // -------------------------

    private static Map<String, List<String>> readAutogenBlocks(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        int s = indexOfTrim(lines, START);
        int e = indexOfTrim(lines, END);
        if (s < 0 || e < 0 || e <= s) return Map.of();

        List<String> autogen = new ArrayList<>(lines.subList(s + 1, e));
        List<Block> blocks = parseBlocks(autogen);

        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Block b : blocks) {
            if (b.constName != null) out.put(b.constName, List.copyOf(b.lines));
        }
        return out;
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

            ConstMeta meta = parseConstMeta(block);
            if (meta.constName != null) out.add(new Block(meta.typeName, meta.constName, block));
        }

        return out;
    }

    private static ConstMeta parseConstMeta(List<String> lines) {
        String type = null;
        String name = null;

        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("public static final ")) {
                String[] parts = t.split("\\s+");
                if (parts.length >= 5) {
                    type = parts[3];
                    name = parts[4];
                    int eq = name.indexOf('=');
                    if (eq >= 0) name = name.substring(0, eq).trim();
                }
                break;
            }
        }
        return new ConstMeta(type, name);
    }

    private record ConstMeta(String typeName, String constName) {}

    private static final class Block {
        final String typeName;
        final String constName;
        final List<String> lines;

        Block(String typeName, String constName, List<String> lines) {
            this.typeName = typeName;
            this.constName = constName;
            this.lines = lines;
        }
    }

    // -------------------------
    // Point/Region parsing helpers
    // -------------------------

    private static final Pattern P_UITARGET =
            Pattern.compile("new\\s+UiTarget\\(\"[^\"]*\"\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*\\)");
    private static final Pattern P_REGION =
            Pattern.compile("new\\s+NormalizedRegion\\(([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*\\)");

    private static double[] parsePointFromBlock(List<String> blockLines) {
        String joined = String.join("\n", blockLines);
        Matcher m = P_UITARGET.matcher(joined);
        if (!m.find()) return null;
        try {
            return new double[]{ Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)) };
        } catch (Exception e) {
            return null;
        }
    }

    private static NormalizedRegion parseRegionFromBlock(List<String> blockLines) {
        String joined = String.join("\n", blockLines);
        Matcher m = P_REGION.matcher(joined);
        if (!m.find()) return null;
        try {
            return new NormalizedRegion(
                    Double.parseDouble(m.group(1)),
                    Double.parseDouble(m.group(2)),
                    Double.parseDouble(m.group(3)),
                    Double.parseDouble(m.group(4))
            );
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------
    // Template helpers
    // -------------------------

    private static String[] cleanedPaths(TemplateTarget t) {
        String[] raw = (t.imagePaths != null && t.imagePaths.length > 0)
                ? t.imagePaths
                : new String[]{ t.imagePath };

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String p : raw) {
            if (p == null) continue;
            String s = p.trim();
            if (!s.isEmpty()) set.add(s);
        }
        if (set.isEmpty()) throw new IllegalArgumentException("TemplateTarget has no valid image paths: " + t.name);
        return set.toArray(new String[0]);
    }

    /** Builds: TemplateTarget.of("label","p1") OR TemplateTarget.of("label","p1","p2",...) */
    private static String buildTemplateOfLine(String label, String[] paths) {
        if (paths.length == 1) {
            return "TemplateTarget.of(\"" + escapeJava(label) + "\", \"" + escapeJava(paths[0]) + "\")";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("TemplateTarget.of(\"").append(escapeJava(label)).append("\"");
        for (String p : paths) sb.append(", \"").append(escapeJava(p)).append("\"");
        sb.append(")");
        return sb.toString();
    }

    // -------------------------
    // Template variant parsing (preserve older behavior)
    // -------------------------

    private static final class ParsedTemplate {
        final List<String> paths;
        final double minScore;
        final SearchAreaPct searchAreaPct;
        final java.awt.Rectangle searchArea;

        ParsedTemplate(List<String> paths, double minScore, SearchAreaPct searchAreaPct, java.awt.Rectangle searchArea) {
            this.paths = paths;
            this.minScore = minScore;
            this.searchAreaPct = searchAreaPct;
            this.searchArea = searchArea;
        }
    }

    private static ParsedTemplate parseTemplateBlock(List<String> blockLines) {
        List<String> paths = new ArrayList<>();
        double minScore = 0.85;
        SearchAreaPct areaPct = null;
        java.awt.Rectangle areaPx = null;

        String joined = String.join("\n", blockLines);

        int ofIdx = joined.indexOf("TemplateTarget.of(");
        if (ofIdx >= 0) {
            String tail = joined.substring(ofIdx);
            List<String> quoted = extractQuotedStrings(tail);

            // quoted[0] is label, quoted[1..] are paths
            if (quoted.size() >= 2) {
                for (int i = 1; i < quoted.size(); i++) {
                    String p = unescapeJava(quoted.get(i));
                    if (!p.isBlank()) paths.add(p);
                }
            }
        }

        Matcher mScore = Pattern.compile("\\.withMinScore\\(([-0-9.]+)\\)").matcher(joined);
        if (mScore.find()) {
            try { minScore = Double.parseDouble(mScore.group(1)); } catch (Exception ignored) {}
        }

        Matcher mPct = Pattern
                .compile("\\.withSearchAreaPct\\(new\\s+[^\\(]*SearchAreaPct\\(([^\\)]*)\\)\\)")
                .matcher(joined);
        if (mPct.find()) {
            String[] nums = mPct.group(1).split(",");
            if (nums.length >= 4) {
                double x = Double.parseDouble(nums[0].trim());
                double y = Double.parseDouble(nums[1].trim());
                double w = Double.parseDouble(nums[2].trim());
                double h = Double.parseDouble(nums[3].trim());
                areaPct = new SearchAreaPct(x, y, w, h);
            }
        }

        Matcher mPx = Pattern
                .compile("\\.withSearchArea\\(new\\s+java\\.awt\\.Rectangle\\(([^\\)]*)\\)\\)")
                .matcher(joined);
        if (mPx.find()) {
            String[] nums = mPx.group(1).split(",");
            if (nums.length >= 4) {
                int x = Integer.parseInt(nums[0].trim());
                int y = Integer.parseInt(nums[1].trim());
                int w = Integer.parseInt(nums[2].trim());
                int h = Integer.parseInt(nums[3].trim());
                areaPx = new java.awt.Rectangle(x, y, w, h);
            }
        }

        // fallback for older formats
        if (paths.isEmpty()) {
            for (String l : blockLines) {
                if (l.contains("\"")) {
                    List<String> q = extractQuotedStrings(l);
                    for (String s : q) {
                        String u = unescapeJava(s);
                        if (!u.isBlank()) paths.add(u);
                    }
                }
            }
        }

        return new ParsedTemplate(paths, minScore, areaPct, areaPx);
    }

    private static List<String> extractQuotedStrings(String s) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(s);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static String unescapeJava(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    // -------------------------
    // File + marker helpers
    // -------------------------

    private static List<String> injectMarkers(List<String> original) {
        List<String> injected = new ArrayList<>(original);
        int closeIdx = lastIndexOfTrim(injected, "}");
        if (closeIdx < 0) closeIdx = injected.size();

        injected.add(closeIdx, "    " + START);
        injected.add(closeIdx + 1, "    " + END);
        return injected;
    }

    private static int indexOfTrim(List<String> lines, String exactTrimmed) {
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).trim().equals(exactTrimmed)) return i;
        return -1;
    }

    private static int lastIndexOfTrim(List<String> lines, String exactTrimmed) {
        for (int i = lines.size() - 1; i >= 0; i--) if (lines.get(i).trim().equals(exactTrimmed)) return i;
        return -1;
    }

    private static Path resolveEngineJavaFile(String fileName) {
        Path p1 = Paths.get("modules", "engine", "src", "main", "java", "io", "deskpilot", "engine", fileName);
        if (Files.exists(p1.getParent())) return p1;

        Path p2 = Paths.get("src", "main", "java", "io", "deskpilot", "engine", fileName);
        if (Files.exists(p2.getParent())) return p2;

        return p1;
    }

    private static void ensureParent(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
    }

    private static String fmt6(double v) {
        return String.format(Locale.US, "%.6f", v);
    }

    private static String escapeJava(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String defaultUiMapSkeleton() {
        return """
                package io.deskpilot.engine;

                public final class UiMap {
                    private UiMap() {}

                    %s
                    %s
                }
                """.formatted(START, END);
    }

    private static String defaultUiRegionsSkeleton() {
        return """
                package io.deskpilot.engine;

                public final class UiRegions {
                    private UiRegions() {}

                    %s
                    // (Record Mode will write NormalizedRegion constants here)
                    %s
                }
                """.formatted(START, END);
    }

    private static String defaultUiTemplatesSkeleton() {
        return """
                package io.deskpilot.engine;

                import io.deskpilot.engine.targets.TemplateTarget;

                public final class UiTemplates {
                    private UiTemplates() {}

                    %s
                    // (Record Mode will write TemplateTarget constants here)
                    %s
                }
                """.formatted(START, END);
    }

    // -------------------------
    // Milestone 11: AUTOGEN normalization pass
    // -------------------------

    private static final Pattern P_INLINE_JAVADOC = Pattern.compile("^\\s*/\\*\\*\\s*(.*?)\\s*\\*/\\s*$");
    private static final Pattern P_UITARGET_LABEL = Pattern.compile("new\\s+UiTarget\\(\"([^\"]*)\"");
    private static final Pattern P_REGION_LABEL   = Pattern.compile("region\\(\"([^\"]*)\"");
    private static final Pattern P_TEMPLATE_OF_LABEL = Pattern.compile("TemplateTarget\\.of\\(\"([^\"]*)\"");
    private static final Pattern P_OCRCONTAINS_LABEL = Pattern.compile("ocrContains\\(\"([^\"]*)\"");

    static List<String> normalizeAutogenFile(String fileName, List<String> lines) {
        if (lines == null || lines.isEmpty()) return lines;

        int s = indexOfTrim(lines, START);
        int e = indexOfTrim(lines, END);
        if (s < 0 || e < 0 || e <= s) return lines;

        UiNaming.Kind defaultKind = UiNaming.Kind.POINT;
        if ("UiRegions.java".equals(fileName)) defaultKind = UiNaming.Kind.REGION;
        else if ("UiTemplates.java".equals(fileName)) defaultKind = UiNaming.Kind.TEMPLATE;
        else if ("Locators.java".equals(fileName)) defaultKind = UiNaming.Kind.REGION; // safe default

        List<String> out = new ArrayList<>(lines.size());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (i <= s || i >= e) {
                out.add(line);
                continue;
            }

            // Normalize inline javadoc: /** something */
            Matcher jd = P_INLINE_JAVADOC.matcher(line);
            if (jd.matches()) {
                String raw = jd.group(1);
                UiNaming.Kind kind = inferKindForLocators(fileName, defaultKind, lines, i);
                String norm;
                try { norm = UiNaming.normalizeLabel(raw, kind); }
                catch (Exception ex) { norm = raw; }

                String indent = line.substring(0, line.indexOf('/'));
                out.add(indent + "/** " + escapeJava(norm) + " */");
                continue;
            }

            if ("UiMap.java".equals(fileName)) {
                out.add(replaceFirstStringLiteral(line, P_UITARGET_LABEL, UiNaming.Kind.POINT));
                continue;
            }

            if ("UiRegions.java".equals(fileName)) {
                // region labels are not embedded in NormalizedRegion ctor; keep line as-is
                out.add(line);
                continue;
            }

            if ("UiTemplates.java".equals(fileName)) {
                out.add(replaceFirstStringLiteral(line, P_TEMPLATE_OF_LABEL, UiNaming.Kind.TEMPLATE));
                continue;
            }

            if ("Locators.java".equals(fileName)) {
                // normalize label in region("...") and ocrContains("...") string literal
                String v = replaceFirstStringLiteral(line, P_REGION_LABEL, UiNaming.Kind.REGION);
                v = replaceFirstStringLiteral(v, P_OCRCONTAINS_LABEL, UiNaming.Kind.REGION);
                out.add(v);
                continue;
            }

            out.add(line);
        }

        return out;
    }

    private static String replaceFirstStringLiteral(String line, Pattern p, UiNaming.Kind kind) {
        Matcher m = p.matcher(line);
        if (!m.find()) return line;

        String raw = m.group(1);
        String norm;
        try { norm = UiNaming.normalizeLabel(raw, kind); }
        catch (Exception ex) { return line; }

        return line.substring(0, m.start(1)) + escapeJava(norm) + line.substring(m.end(1));
    }

    /**
     * For Locators.java: decide the label kind based on the factory used in the following lines.
     * OCR locators behave like REGION-kind labels.
     */
    private static UiNaming.Kind inferKindForLocators(String fileName, UiNaming.Kind fallback, List<String> lines, int idx) {
        if (!"Locators.java".equals(fileName)) return fallback;

        for (int j = idx; j < Math.min(lines.size(), idx + 10); j++) {
            String l = lines.get(j);
            if (l.contains("ocrContains(") || l.contains("new OcrContainsLocator")) return UiNaming.Kind.REGION;
            if (l.contains("region(")) return UiNaming.Kind.REGION;
            if (l.contains("template(")) return UiNaming.Kind.TEMPLATE;
            if (l.contains("point(")) return UiNaming.Kind.POINT;
        }
        return fallback;
    }
}

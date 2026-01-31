package io.deskpilot.engine.text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic, OCR-friendly parsing helpers.
 * This class is app-agnostic: it doesn't assume any domain rules.
 */
public final class TextParsers {

    private TextParsers() {}

    // Finds a candidate numeric "token" inside messy OCR text.
    // Examples matched:
    //  "status: searched 1,234.56 CAD" -> "1,234.56"
    //  "Total (1.234,56)"              -> "(1.234,56)"
    //  "-99.50"                        -> "-99.50"
    //  "99-"                           -> "99-"
    private static final Pattern NUMBER_TOKEN = Pattern.compile(
            "(\\(?\\s*[-+]?\\s*\\d[\\d\\s,.'`]*\\d(?:[\\.,]\\d+)?\\s*\\)?\\s*%?\\s*[-]?)"
    );

    /**
     * Try to parse the first number-like token found in a string.
     * Returns Optional.empty() if none found or parsing fails.
     */
    public static Optional<BigDecimal> tryParseNumber(String text) {
        return tryParseNumber(text, NumberParseConfig.defaults());
    }

    public static Optional<BigDecimal> tryParseNumber(String text, NumberParseConfig cfg) {
        if (text == null) return Optional.empty();
        if (cfg == null) cfg = NumberParseConfig.defaults();

        String s = text.trim();
        if (s.isEmpty()) return Optional.empty();

        String token = findFirstNumberToken(s);
        if (token == null) return Optional.empty();

        try {
            return Optional.of(parseNumberToken(token, cfg));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Parse first number-like token found; throws if not found or invalid.
     */
    public static BigDecimal parseNumber(String text) {
        return parseNumber(text, NumberParseConfig.defaults());
    }

    public static BigDecimal parseNumber(String text, NumberParseConfig cfg) {
        return tryParseNumber(text, cfg)
                .orElseThrow(() -> new IllegalArgumentException("No parseable number found in: '" + text + "'"));
    }

    /**
     * Money parsing is just "number parsing" with currency tolerated.
     * This method exists for readability in tests/usage.
     */
    public static Optional<BigDecimal> tryParseMoney(String text) {
        return tryParseMoney(text, NumberParseConfig.defaults());
    }

    public static Optional<BigDecimal> tryParseMoney(String text, NumberParseConfig cfg) {
        if (cfg == null) cfg = NumberParseConfig.defaults();
        // money typically needs currency allowed
        if (!cfg.allowCurrency) cfg = NumberParseConfig.defaults();
        return tryParseNumber(text, cfg);
    }

    public static BigDecimal parseMoney(String text) {
        return parseMoney(text, NumberParseConfig.defaults());
    }

    public static BigDecimal parseMoney(String text, NumberParseConfig cfg) {
        return tryParseMoney(text, cfg)
                .orElseThrow(() -> new IllegalArgumentException("No parseable money/number found in: '" + text + "'"));
    }

    /**
     * Normalize text for comparisons:
     * - lowercase
     * - strip non-alphanumerics
     * - collapse whitespace
     */
    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    // -------------------------
    // Internals
    // -------------------------

    private static String findFirstNumberToken(String s) {
        Matcher m = NUMBER_TOKEN.matcher(s);
        if (!m.find()) return null;
        String token = m.group(1);
        return (token == null) ? null : token.trim();
    }

    private static BigDecimal parseNumberToken(String rawToken, NumberParseConfig cfg) {
        if (rawToken == null) throw new IllegalArgumentException("token is null");

        String t = rawToken.trim();

        // Strip currency if allowed (generic: symbols + 3-letter codes often appear around)
        // We keep it permissive; the NUMBER_TOKEN already isolates mostly numeric content.
        if (cfg.allowCurrency) {
            t = t.replaceAll("(?i)\\b(usd|cad|eur|gbp|inr|aud|nzd|sgd|jpy|cny)\\b", "");
            t = t.replaceAll("[$€£¥₹]", "");
        }

        // Percent handling (keep numeric portion, caller can decide meaning)
        boolean isPercent = false;
        if (cfg.allowPercent && t.contains("%")) {
            isPercent = true;
            t = t.replace("%", "");
        } else {
            t = t.replace("%", "");
        }

        // Handle parentheses negative: "(123.45)"
        boolean neg = false;
        if (cfg.allowParenthesesNegative) {
            String trimmed = t.replaceAll("\\s+", "");
            if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
                neg = true;
                t = trimmed.substring(1, trimmed.length() - 1);
            }
        }

        // Remove spaces/apostrophes/backticks that OCR often inserts as group separators
        t = t.replaceAll("[\\s'`]", "");

        // Handle trailing negative "123-"
        if (cfg.allowTrailingNegative && t.endsWith("-")) {
            neg = true;
            t = t.substring(0, t.length() - 1);
        }

        // Leading sign
        if (t.startsWith("-")) {
            neg = true;
            t = t.substring(1);
        } else if (t.startsWith("+")) {
            if (!cfg.allowLeadingPlus) throw new IllegalArgumentException("leading plus not allowed");
            t = t.substring(1);
        }

        t = t.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("empty token after cleanup");

        // Now we have digits + separators. Decide decimal separator.
        // Heuristic:
        // - If both '.' and ',' appear: rightmost one is decimal, the other is grouping.
        // - If only one appears:
        //   - If it appears multiple times: treat as grouping, decimal = none.
        //   - If it appears once: treat as decimal IF 1-6 digits after it, else grouping.
        char decimalSep = detectDecimalSeparator(t);
        String normalized = normalizeNumberString(t, decimalSep);

        BigDecimal bd = new BigDecimal(normalized);
        if (neg) bd = bd.negate();

        // Clamp scale to avoid OCR weirdness like 12.34567890123
        if (bd.scale() > cfg.maxScale) {
            bd = bd.setScale(cfg.maxScale, RoundingMode.HALF_UP);
        }

        // If percent, leave as numeric (12.5%), caller can divide by 100 if they want.
        // We don't change semantics here to stay app-agnostic.
        return bd;
    }

    private static char detectDecimalSeparator(String t) {
        int lastDot = t.lastIndexOf('.');
        int lastComma = t.lastIndexOf(',');

        if (lastDot >= 0 && lastComma >= 0) {
            // both present -> rightmost is decimal
            return (lastDot > lastComma) ? '.' : ',';
        }

        if (lastDot >= 0) {
            // dot only
            if (countChar(t, '.') > 1) return 0; // grouping only
            int digitsAfter = digitsAfterSeparator(t, lastDot);
            return (digitsAfter >= 1 && digitsAfter <= 6) ? '.' : 0;
        }

        if (lastComma >= 0) {
            // comma only
            if (countChar(t, ',') > 1) return 0; // grouping only
            int digitsAfter = digitsAfterSeparator(t, lastComma);
            return (digitsAfter >= 1 && digitsAfter <= 6) ? ',' : 0;
        }

        return 0; // no decimal
    }

    private static String normalizeNumberString(String t, char decimalSep) {
        StringBuilder out = new StringBuilder(t.length() + 4);

        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isDigit(c)) {
                out.append(c);
                continue;
            }

            if (c == '.' || c == ',') {
                if (decimalSep != 0 && c == decimalSep) {
                    out.append('.'); // normalize decimal to dot
                }
                // else: skip grouping separators
                continue;
            }

            // ignore everything else (should be rare after cleanup)
        }

        // Edge cases: ".5" style isn't expected from OCR, but just in case:
        String s = out.toString();
        if (s.isEmpty()) throw new IllegalArgumentException("no digits after normalization");
        if (s.startsWith(".")) s = "0" + s;
        if (s.endsWith(".")) s = s + "0";

        return s;
    }

    private static int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    private static int digitsAfterSeparator(String s, int sepIndex) {
        int n = 0;
        for (int i = sepIndex + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c)) break;
            n++;
        }
        return n;
    }

    public static List<BigDecimal> findAllNumbers(String text) {
    return findAllNumbers(text, NumberParseConfig.defaults());
}

public static List<BigDecimal> findAllNumbers(String text, NumberParseConfig cfg) {
    if (text == null || text.trim().isEmpty()) return List.of();
    if (cfg == null) cfg = NumberParseConfig.defaults();

    List<BigDecimal> out = new ArrayList<>();
    Matcher m = NUMBER_TOKEN.matcher(text);

    while (m.find()) {
        String token = m.group(1);
        try {
            out.add(parseNumberToken(token, cfg));
        } catch (Exception ignored) {
            // skip bad OCR fragments
        }
    }
    return out;
}
public static boolean containsNumber(String text) {
    if (text == null) return false;
    return NUMBER_TOKEN.matcher(text).find();
}
public static Optional<BigDecimal> tryParsePercent(String text) {
    var cfg = NumberParseConfig.defaults();
    Optional<BigDecimal> n = tryParseNumber(text, cfg);
    if (n.isEmpty()) return Optional.empty();

    if (!text.contains("%")) return Optional.empty();
    return Optional.of(n.get().divide(BigDecimal.valueOf(100)));
}

}

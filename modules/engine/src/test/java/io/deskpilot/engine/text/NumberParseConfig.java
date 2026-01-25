package io.deskpilot.engine.text;

import java.util.Objects;

/**
 * Generic number parsing config (app-agnostic).
 * Keep it permissive by default; tighten only if you need.
 */
public final class NumberParseConfig {

    public final boolean allowParenthesesNegative;
    public final boolean allowTrailingNegative;     // e.g. "123-" (seen in accounting systems)
    public final boolean allowLeadingPlus;
    public final boolean allowPercent;              // e.g. "12.5%"
    public final boolean allowCurrency;             // e.g. "$", "CAD", "USD"
    public final int maxScale;                      // clamp scale to avoid insane OCR artifacts

    private NumberParseConfig(boolean allowParenthesesNegative,
                              boolean allowTrailingNegative,
                              boolean allowLeadingPlus,
                              boolean allowPercent,
                              boolean allowCurrency,
                              int maxScale) {
        this.allowParenthesesNegative = allowParenthesesNegative;
        this.allowTrailingNegative = allowTrailingNegative;
        this.allowLeadingPlus = allowLeadingPlus;
        this.allowPercent = allowPercent;
        this.allowCurrency = allowCurrency;
        this.maxScale = maxScale;
    }

    public static NumberParseConfig defaults() {
        return new NumberParseConfig(
                true,   // allowParenthesesNegative
                true,   // allowTrailingNegative
                true,   // allowLeadingPlus
                true,   // allowPercent
                true,   // allowCurrency
                6       // maxScale (safe default)
        );
    }

    public NumberParseConfig withMaxScale(int scale) {
        if (scale < 0) throw new IllegalArgumentException("maxScale must be >= 0");
        return new NumberParseConfig(
                allowParenthesesNegative,
                allowTrailingNegative,
                allowLeadingPlus,
                allowPercent,
                allowCurrency,
                scale
        );
    }

    @Override
    public String toString() {
        return "NumberParseConfig{" +
                "allowParenthesesNegative=" + allowParenthesesNegative +
                ", allowTrailingNegative=" + allowTrailingNegative +
                ", allowLeadingPlus=" + allowLeadingPlus +
                ", allowPercent=" + allowPercent +
                ", allowCurrency=" + allowCurrency +
                ", maxScale=" + maxScale +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NumberParseConfig)) return false;
        NumberParseConfig other = (NumberParseConfig) o;
        return allowParenthesesNegative == other.allowParenthesesNegative
                && allowTrailingNegative == other.allowTrailingNegative
                && allowLeadingPlus == other.allowLeadingPlus
                && allowPercent == other.allowPercent
                && allowCurrency == other.allowCurrency
                && maxScale == other.maxScale;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                allowParenthesesNegative,
                allowTrailingNegative,
                allowLeadingPlus,
                allowPercent,
                allowCurrency,
                maxScale
        );
    }
}

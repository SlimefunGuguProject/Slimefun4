package io.github.thebusybiscuit.slimefun4.core.services.stability;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Server-independent text helpers used by the Slimefun item doctor. */
public final class ItemDoctorText {

    private static final Pattern DYNAMIC_TOKEN = Pattern.compile(
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|(?<!§)[+-]?\\d+(?:[.,]\\d+)?");
    private static final Pattern LEGACY_CHARGE = Pattern.compile(
            "(?i)(?<!§)([+-]?\\d+(?:[.,]\\d+)?)\\s*/\\s*([+-]?\\d+(?:[.,]\\d+)?)\\s*J");

    private ItemDoctorText() {}

    public static boolean containsCjk(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (isCjkCodePoint(codePoint)) {
                return true;
            }
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    public static boolean containsCjk(@Nullable List<String> lines) {
        if (lines == null) {
            return false;
        }

        for (String line : lines) {
            if (containsCjk(line)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces CJK presentation lines with the registered English template while retaining
     * every non-CJK line in its original slot. This is deliberately position-preserving
     * because legacy items and third-party addons may keep hidden state in lore.
     */
    public static @Nonnull List<String> mergeEnglishLore(
            @Nullable List<String> currentLore, @Nullable List<String> canonicalLore) {
        List<String> canonical = canonicalLore == null ? List.of() : canonicalLore;
        if (currentLore == null || currentLore.isEmpty()) {
            return new ArrayList<>(canonical);
        }

        List<String> result = new ArrayList<>(currentLore.size() + canonical.size());
        List<Boolean> removablePlaceholders = new ArrayList<>(currentLore.size() + canonical.size());
        for (int i = 0; i < currentLore.size(); i++) {
            String currentLine = currentLore.get(i);
            if (!containsCjk(currentLine)) {
                result.add(currentLine);
                removablePlaceholders.add(false);
            } else if (i < canonical.size()) {
                result.add(carryDynamicTokens(currentLine, canonical.get(i)));
                removablePlaceholders.add(false);
            } else {
                // Keep later hidden/state lines at their original indexes. A blank placeholder
                // is safer than moving legacy lore-backed data to a different slot.
                result.add("");
                removablePlaceholders.add(true);
            }
        }

        // A translated template may contain more lines than the stored legacy item.
        // Append only missing lines so hidden or dynamic lines never change position.
        for (String canonicalLine : canonical) {
            if (!normalize(canonicalLine).isEmpty() && !containsEquivalent(result, canonicalLine)) {
                result.add(canonicalLine);
                removablePlaceholders.add(false);
            }
        }

        while (!result.isEmpty() && removablePlaceholders.get(removablePlaceholders.size() - 1)) {
            int lastIndex = result.size() - 1;
            result.remove(lastIndex);
            removablePlaceholders.remove(lastIndex);
        }
        return result;
    }

    static @Nullable String carryDynamicTokens(
            @Nullable String currentLine, @Nullable String canonicalLine) {
        if (currentLine == null || canonicalLine == null) {
            return canonicalLine;
        }

        List<Token> currentTokens = extractTokens(currentLine);
        List<Token> canonicalTokens = extractTokens(canonicalLine);
        if (currentTokens.size() != canonicalTokens.size() || currentTokens.isEmpty()) {
            return canonicalLine;
        }

        for (int i = 0; i < currentTokens.size(); i++) {
            if (currentTokens.get(i).uuid() != canonicalTokens.get(i).uuid()) {
                return canonicalLine;
            }
        }

        StringBuilder rebuilt = new StringBuilder(canonicalLine.length() + 16);
        int previousEnd = 0;
        for (int i = 0; i < canonicalTokens.size(); i++) {
            Token canonicalToken = canonicalTokens.get(i);
            rebuilt.append(canonicalLine, previousEnd, canonicalToken.start());
            rebuilt.append(currentTokens.get(i).value());
            previousEnd = canonicalToken.end();
        }
        rebuilt.append(canonicalLine, previousEnd, canonicalLine.length());
        return rebuilt.toString();
    }

    static @Nullable Float findLegacyCharge(@Nullable List<String> lore) {
        if (lore == null) {
            return null;
        }

        Float result = null;
        for (String line : lore) {
            if (!containsCjk(line)) {
                continue;
            }

            Matcher matcher = LEGACY_CHARGE.matcher(line);
            if (!matcher.find()) {
                continue;
            }

            try {
                float value = Float.parseFloat(matcher.group(1).replace(',', '.'));
                if (!Float.isFinite(value) || result != null) {
                    return null;
                }
                result = value;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result;
    }

    static @Nullable Integer findSingleLegacyInteger(@Nullable List<String> lore) {
        if (lore == null) {
            return null;
        }

        Integer result = null;
        for (String line : lore) {
            if (!containsCjk(line)) {
                continue;
            }

            List<Token> tokens = extractTokens(line);
            if (tokens.size() != 1 || tokens.get(0).uuid() || tokens.get(0).value().contains(".")
                    || tokens.get(0).value().contains(",")) {
                continue;
            }

            try {
                int value = Integer.parseInt(tokens.get(0).value());
                if (result != null) {
                    return null;
                }
                result = value;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result;
    }

    private static List<Token> extractTokens(String line) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = DYNAMIC_TOKEN.matcher(line);
        while (matcher.find()) {
            String value = matcher.group();
            tokens.add(new Token(matcher.start(), matcher.end(), value, isUuidToken(value)));
        }
        return tokens;
    }

    private static boolean isUuidToken(String value) {
        return value.length() == 36
                && value.charAt(8) == '-'
                && value.charAt(13) == '-'
                && value.charAt(18) == '-'
                && value.charAt(23) == '-';
    }

    private static boolean containsEquivalent(List<String> lines, String candidate) {
        String normalizedCandidate = normalize(candidate);
        for (String line : lines) {
            if (normalize(line).equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(@Nullable String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?i)§[0-9A-FK-ORX]", "")
                .replaceAll("\\d+(?:[.,]\\d+)?", "#")
                .trim();
    }

    private record Token(int start, int end, String value, boolean uuid) {}

    private static boolean isCjkCodePoint(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }
}

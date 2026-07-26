package io.github.thebusybiscuit.slimefun4.core.services.stability;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestItemDoctorText {

    @Test
    void detectsCommonAndSupplementaryCjkCharacters() {
        Assertions.assertTrue(ItemDoctorText.containsCjk("Machine \u4E2D\u6587"));
        Assertions.assertTrue(ItemDoctorText.containsCjk("Item \uD840\uDC00"));
        Assertions.assertFalse(ItemDoctorText.containsCjk("English item name"));
        Assertions.assertFalse(ItemDoctorText.containsCjk((String) null));
    }

    @Test
    void rebuildsBaseLoreAndPreservesEnglishStateLines() {
        List<String> current = List.of(
                "\u00A77\u4E2D\u6587\u63CF\u8FF0",
                "\u00A77Charge: 64 J",
                "\u00A7bCustom marker");
        List<String> canonical = List.of("\u00A77English description", "\u00A77Charge: 0 J");

        List<String> repaired = ItemDoctorText.mergeEnglishLore(current, canonical);

        Assertions.assertEquals(
                List.of("\u00A77English description", "\u00A77Charge: 64 J", "\u00A7bCustom marker"), repaired);
        Assertions.assertFalse(ItemDoctorText.containsCjk(repaired));
    }

    @Test
    void preservesHiddenStateAtItsOriginalIndex() {
        String hiddenUuid = "\u00A70cc5e8e27-7e4e-45cd-9396-62b41ecfd717";
        List<String> repaired = ItemDoctorText.mergeEnglishLore(
                List.of("\u00A77\u6240\u6709\u8005", hiddenUuid, "\u00A77\u4f7f\u7528\u8bf4\u660e"),
                List.of("\u00A77Owner: None", "", "\u00A77Usage instructions"));

        Assertions.assertEquals(List.of("\u00A77Owner: None", hiddenUuid, "\u00A77Usage instructions"), repaired);
    }

    @Test
    void carriesDynamicNumbersFromTranslatedLines() {
        List<String> repaired = ItemDoctorText.mergeEnglishLore(
                List.of("§7\u5269\u4F59\u4F7F\u7528\u6B21\u6570: 7", "§7\u5F53\u524D\u7535\u91CF: 64 / 128 J"),
                List.of("§7Uses left: 20", "§7Charge: 0 / 128 J"));

        Assertions.assertEquals(List.of("§7Uses left: 7", "§7Charge: 64 / 128 J"), repaired);
    }

    @Test
    void doesNotTreatLegacyColorCodesAsDynamicNumbers() {
        Assertions.assertEquals(
                "§7Uses left: 4",
                ItemDoctorText.carryDynamicTokens(
                        "§e\u5269\u4F59\u4F7F\u7528\u6B21\u6570: 4", "§7Uses left: 20"));
    }

    @Test
    void doesNotDuplicateEquivalentCanonicalLines() {
        List<String> repaired = ItemDoctorText.mergeEnglishLore(
                List.of("\u00A7aEnglish description", "\u00A77Extra state"),
                List.of("\u00A77English description"));

        Assertions.assertEquals(List.of("\u00A7aEnglish description", "\u00A77Extra state"), repaired);
    }

    @Test
    void removesCjkLoreWithoutMovingHiddenState() {
        List<String> repaired = ItemDoctorText.mergeEnglishLore(
                List.of("§7\u4E2D\u6587\u63CF\u8FF0", "§7Charge: 19 J", "§0hidden-state"), null);

        Assertions.assertEquals(List.of("", "§7Charge: 19 J", "§0hidden-state"), repaired);
        Assertions.assertEquals(List.of(), ItemDoctorText.mergeEnglishLore(List.of("§7\u4E2D\u6587"), null));
    }

    @Test
    void carriesUuidAndSignedDynamicValues() {
        String owner = "cc5e8e27-7e4e-45cd-9396-62b41ecfd717";
        Assertions.assertEquals(
                "§7Owner UUID: " + owner,
                ItemDoctorText.carryDynamicTokens(
                        "§7\u6240\u6709\u8005: " + owner,
                        "§7Owner UUID: 00000000-0000-0000-0000-000000000000"));
        Assertions.assertEquals(
                "§7Temperature: -12.5 C",
                ItemDoctorText.carryDynamicTokens("§7\u6E29\u5EA6: -12.5 C", "§7Temperature: 0.0 C"));
    }

    @Test
    void recoversLegacyChargeAndSingleUseValues() {
        Assertions.assertEquals(
                64.5F,
                ItemDoctorText.findLegacyCharge(List.of("§7\u5F53\u524D\u7535\u91CF: 64.5 / 128 J")));
        Assertions.assertEquals(
                7,
                ItemDoctorText.findSingleLegacyInteger(List.of("§7\u5269\u4F59\u4F7F\u7528\u6B21\u6570: 7")));
        Assertions.assertNull(ItemDoctorText.findSingleLegacyInteger(List.of(
                "§7\u7B49\u7EA7: 2", "§7\u5269\u4F59\u4F7F\u7528\u6B21\u6570: 7")));
    }
}

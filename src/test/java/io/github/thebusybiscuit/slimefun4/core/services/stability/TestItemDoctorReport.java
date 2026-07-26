package io.github.thebusybiscuit.slimefun4.core.services.stability;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestItemDoctorReport {

    @Test
    void recordsProgressAndLimitsDuplicateSamples() {
        ItemDoctorReport report = new ItemDoctorReport(true);
        report.inventoryScanned();
        report.backpackScanned();
        report.stackScanned();
        report.slimefunStackFound();
        report.cjkStackFound();
        report.stackRepaired();
        report.unknownIdFound("UNKNOWN_ITEM");
        report.unknownIdFound("UNKNOWN_ITEM");
        report.unresolvedTemplateFound("CHINESE_TEMPLATE");
        report.failure();
        report.markComplete();

        Assertions.assertTrue(report.isRepairMode());
        Assertions.assertEquals("repair", report.getModeName());
        Assertions.assertTrue(report.isComplete());
        Assertions.assertEquals(1, report.getInventories());
        Assertions.assertEquals(1, report.getBackpacks());
        Assertions.assertEquals(1, report.getScannedStacks());
        Assertions.assertEquals(1, report.getSlimefunStacks());
        Assertions.assertEquals(1, report.getCjkStacks());
        Assertions.assertEquals(1, report.getRepairedStacks());
        Assertions.assertEquals(2, report.getUnknownIds());
        Assertions.assertEquals(1, report.getUnresolvedTemplates());
        Assertions.assertEquals(1, report.getFailures());
        Assertions.assertEquals(1, report.getUnknownIdSamples().size());
        Assertions.assertEquals("UNKNOWN_ITEM", report.getUnknownIdSamples().get(0));
        Assertions.assertEquals("CHINESE_TEMPLATE", report.getUnresolvedTemplateSamples().get(0));
        Assertions.assertTrue(report.getDurationMillis() >= 0);
    }
}

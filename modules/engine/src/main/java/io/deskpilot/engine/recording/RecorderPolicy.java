package io.deskpilot.engine.recording;

public class RecorderPolicy {
    public final int minRegionPxW;
    public final int minRegionPxH;

    public final int minTemplatePxW;
    public final int minTemplatePxH;

    public RecorderPolicy(int minRegionPxW, int minRegionPxH, int minTemplatePxW, int minTemplatePxH) {
        this.minRegionPxW = minRegionPxW;
        this.minRegionPxH = minRegionPxH;
        this.minTemplatePxW = minTemplatePxW;
        this.minTemplatePxH = minTemplatePxH;
    }

    public static RecorderPolicy defaults() {
        // region can be smaller than templates (OCR can read narrow labels)
       int minRW = Integer.getInteger("deskpilot.record.minRegionW", 12);
int minRH = Integer.getInteger("deskpilot.record.minRegionH", 12);
int minTW = Integer.getInteger("deskpilot.record.minTemplateW", 16);
int minTH = Integer.getInteger("deskpilot.record.minTemplateH", 16);
return new RecorderPolicy(minRW, minRH, minTW, minTH);

    }
}

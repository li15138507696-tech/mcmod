package net.guzhenren.tianfu.client;

import net.guzhenren.tianfu.v2.TianfuPage;

/** Client-only navigation steps for the opening selection wizard. */
public enum TianfuWizardStep {
    DIFFICULTY(TianfuPage.DIFFICULTY, "难度"),
    ORIGIN(TianfuPage.ORIGIN, "出身"),
    APTITUDE(TianfuPage.APTITUDE, "资质根基"),
    LIUPAI(TianfuPage.LIUPAI, "流派"),
    PHYSIQUE(TianfuPage.PHYSIQUE, "体质"),
    MIND(TianfuPage.MIND, "心智"),
    QIYUN(TianfuPage.QIYUN, "气运"),
    REVIEW(null, "总复核");

    public final TianfuPage page;
    public final String label;

    TianfuWizardStep(TianfuPage page, String label) {
        this.page = page;
        this.label = label;
    }

    /** The category page represented by this step; the review step has no category page. */
    public TianfuPage page() {
        return page;
    }

    /** Move forward by one step, staying on review at the end. */
    public TianfuWizardStep next() {
        TianfuWizardStep[] steps = values();
        return steps[Math.min(ordinal() + 1, steps.length - 1)];
    }

    /** Move backward by one step, staying on difficulty at the beginning. */
    public TianfuWizardStep previous() {
        TianfuWizardStep[] steps = values();
        return steps[Math.max(ordinal() - 1, 0)];
    }

    /** Whether this step may be left without selecting anything. */
    public boolean canSkip(int kongqiao, boolean anchorSatisfied) {
        return switch (this) {
            case ORIGIN, LIUPAI, PHYSIQUE, MIND, QIYUN -> true;
            case APTITUDE -> anchorSatisfied || kongqiao >= 1 && kongqiao <= 10;
            case DIFFICULTY, REVIEW -> false;
        };
    }

    /** Choose the blocking step after a fresh login, based on the saved gameplay state. */
    public static TianfuWizardStep initial(boolean difficultyChosen, boolean anchorSatisfied) {
        if (!difficultyChosen) {
            return DIFFICULTY;
        }
        return anchorSatisfied ? REVIEW : APTITUDE;
    }
}

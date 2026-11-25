package net.guzhenren.tianfu.client;

/** Shared geometry keeps recommendation text outside interactive controls. */
public record UniversalLayout(int titleY, int pointsY, int rowsY, int rowHeight, int rowStep,
                              int recommendationY, int buttonsY, int buttonHeight) {
    public static UniversalLayout forHeight(int height, int count) {
        int rowStep = 22;
        int rowHeight = 20;
        int buttonHeight = 20;
        int total = 28 + count * rowStep + 12 + buttonHeight;
        int top = Math.max(0, (height - total) / 2);
        int rows = top + 28;
        int recommendation = rows + count * rowStep;
        return new UniversalLayout(top, top + 14, rows, rowHeight, rowStep,
                recommendation, recommendation + 12, buttonHeight);
    }
    public static int rowsPerPage(int height) { return Math.max(1, (height - 68) / 22); }
}

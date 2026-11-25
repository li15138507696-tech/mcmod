package net.guzhenren.tianfu.client;

import net.guzhenren.tianfu.UniversalRules;
import net.guzhenren.tianfu.UniversalState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UniversalScreen extends Screen {
    private final UniversalState state;
    private String recommendation = "";
    private int page;
    private UniversalLayout layout;
    private UniversalScreen(UniversalState state) { super(Component.literal("天命通用版")); this.state = state; }
    public static void open(UniversalState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof UniversalScreen old) {
            UniversalScreen updated = new UniversalScreen(state);
            updated.page = old.page;
            mc.setScreen(updated);
        } else if (state.open() && mc.screen == null) mc.setScreen(new UniversalScreen(state));
    }
    @Override protected void init() {
        int pageSize = UniversalLayout.rowsPerPage(height);
        int maxPage = (UniversalRules.CATALOG.size() - 1) / pageSize;
        page = Math.min(page, maxPage);
        int start = page * pageSize;
        int end = Math.min(UniversalRules.CATALOG.size(), start + pageSize);
        layout = UniversalLayout.forHeight(height, end - start);
        int y = layout.rowsY();
        int panelWidth = Math.min(210, Math.max(40, width - 8));
        int panelX = (width - panelWidth) / 2;
        for (var t : UniversalRules.CATALOG.subList(start, end)) {
            boolean has = state.owned().contains(t.id());
            Button button = Button.builder(Component.literal(t.name() + " " + UniversalRules.attributeName(t) + UniversalRules.effectText(t) + " " + (has ? "已拥有" : t.cost()+"点")), b -> {
                if (minecraft != null && minecraft.player != null) minecraft.player.connection.sendCommand("tianfu buy " + t.id());
            }).bounds(panelX, y, panelWidth, layout.rowHeight()).build();
            button.active = !has && state.points() >= t.cost(); addRenderableWidget(button); y += layout.rowStep();
        }
        addRenderableWidget(Button.builder(Component.literal("随机推荐"), b -> {
            var options = UniversalRules.CATALOG.stream().filter(t -> !state.owned().contains(t.id()) && t.cost() <= state.points()).toList();
            recommendation = options.isEmpty() ? "没有可负担的新天赋" : "推荐：" + options.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(options.size())).name();
        }).bounds(panelX, layout.buttonsY(), panelWidth / 2 - 2, layout.buttonHeight()).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), b -> onClose()).bounds(panelX + panelWidth / 2 + 2, layout.buttonsY(), panelWidth / 2 - 2, layout.buttonHeight()).build());
        if (maxPage > 0) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); clearWidgets(); init(); }).bounds(panelX, layout.pointsY(), 20, 12).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = Math.min(maxPage, page + 1); clearWidgets(); init(); }).bounds(panelX + panelWidth - 20, layout.pointsY(), 20, 12).build());
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, layout.titleY(), 0xffd080);
        graphics.drawCenteredString(font, Component.literal("点数：" + state.points()), width / 2, layout.pointsY(), 0xffffff);
        if (!recommendation.isEmpty()) graphics.drawCenteredString(font, Component.literal(font.plainSubstrByWidth(recommendation, Math.max(0, width - 8))), width / 2, layout.recommendationY(), 0xffd080);
    }
    @Override public boolean isPauseScreen() { return false; }
}

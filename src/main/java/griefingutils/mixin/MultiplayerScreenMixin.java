package griefingutils.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.network.LanServerQueryManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultiplayerScreen.class, priority = 900)
public abstract class MultiplayerScreenMixin extends Screen {
    @Shadow
    @Final
    private Screen parent;
    @Shadow
    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow
    protected MultiplayerServerListWidget serverListWidget;
    @Shadow
    private ServerList serverList;
    @Shadow
    private ButtonWidget buttonEdit;
    @Shadow
    private ButtonWidget buttonJoin;
    @Shadow
    private ButtonWidget buttonDelete;
    private ButtonWidget buttonQuickDelete;
    @Shadow
    private ServerInfo selectedEntry;
    @Shadow
    private LanServerQueryManager.LanServerEntryList lanServers;
    @Shadow
    @Nullable
    private LanServerQueryManager.LanServerDetector lanServerDetector;
    @Shadow
    private boolean initialized;

    @Shadow
    @Final
    public abstract void refresh();
    @Shadow protected abstract void removeEntry(boolean confirmedAction);
    @Shadow protected abstract void editEntry(boolean confirmedAction);
    @Shadow protected abstract void addEntry(boolean confirmedAction);
    @Shadow protected abstract void directConnect(boolean confirmedAction);
    @Shadow public abstract void connect();
    @Shadow protected abstract void updateButtonActivationStates();

    public MultiplayerScreenMixin(Text text) {
        super(text);
    }

    /**
     * @author Puyodead1
     * @reason add new button to axis grid
     */
    @Overwrite()
    public void init() {
        if (this.initialized) {
            this.serverListWidget.setDimensionsAndPosition(this.width, this.height - 64 - 32, 0, 32);
        } else {
            this.initialized = true;
            this.serverList = new ServerList(this.client);
            this.serverList.loadFile();
            this.lanServers = new LanServerQueryManager.LanServerEntryList();

            try {
                this.lanServerDetector = new LanServerQueryManager.LanServerDetector(this.lanServers);
                this.lanServerDetector.start();
            } catch (Exception var8) {
                Exception exception = var8;
                LOGGER.warn("Unable to start LAN server detection: {}", exception.getMessage());
            }

            this.serverListWidget = new MultiplayerServerListWidget((MultiplayerScreen) ((Object)this), this.client, this.width, this.height - 64 - 32, 32, 36);
            this.serverListWidget.setServers(this.serverList);
        }

        this.addSelectableChild(this.serverListWidget);
        this.buttonJoin = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.select"), (button) -> {
            this.connect();
        }).width(100).build());
        ButtonWidget buttonWidget = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.direct"), (button) -> {
            this.selectedEntry = new ServerInfo(I18n.translate("selectServer.defaultName", new Object[0]), "", ServerInfo.ServerType.OTHER);
            this.client.setScreen(new DirectConnectScreen(this, this::directConnect, this.selectedEntry));
        }).width(100).build());
        ButtonWidget buttonWidget2 = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.add"), (button) -> {
            this.selectedEntry = new ServerInfo(I18n.translate("selectServer.defaultName", new Object[0]), "", ServerInfo.ServerType.OTHER);
            this.client.setScreen(new AddServerScreen(this, this::addEntry, this.selectedEntry));
        }).width(100).build());
        this.buttonQuickDelete = this.addDrawableChild(ButtonWidget.builder(Text.of("Quick Delete"), (button) -> {
            MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
            if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
                ServerInfo serverInfo = ((MultiplayerServerListWidget.ServerEntry)entry).getServer();
                this.selectedEntry = new ServerInfo(serverInfo.name, serverInfo.address, ServerInfo.ServerType.OTHER);
                this.selectedEntry.copyWithSettingsFrom(serverInfo);
                this.removeEntry(true);
            }
        }).width(100).build());

        this.buttonEdit = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.edit"), (button) -> {
            MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
            if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
                ServerInfo serverInfo = ((MultiplayerServerListWidget.ServerEntry)entry).getServer();
                this.selectedEntry = new ServerInfo(serverInfo.name, serverInfo.address, ServerInfo.ServerType.OTHER);
                this.selectedEntry.copyWithSettingsFrom(serverInfo);
                this.client.setScreen(new AddServerScreen(this, this::editEntry, this.selectedEntry));
            }

        }).width(100).build());
        this.buttonDelete = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.delete"), (button) -> {
            MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
            if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
                String string = ((MultiplayerServerListWidget.ServerEntry)entry).getServer().name;
                if (string != null) {
                    Text text = Text.translatable("selectServer.deleteQuestion");
                    Text text2 = Text.translatable("selectServer.deleteWarning", new Object[]{string});
                    Text text3 = Text.translatable("selectServer.deleteButton");
                    Text text4 = ScreenTexts.CANCEL;
                    this.client.setScreen(new ConfirmScreen(this::removeEntry, text, text2, text3, text4));
                }
            }

        }).width(100).build());
        ButtonWidget buttonWidget3 = this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.refresh"), (button) -> {
            this.refresh();
        }).width(100).build());
        ButtonWidget buttonWidget4 = this.addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, (button) -> {
            this.client.setScreen(this.parent);
        }).width(100).build());

        DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.vertical();
        AxisGridWidget axisGridWidget = directionalLayoutWidget.add(new AxisGridWidget(408, 20, AxisGridWidget.DisplayAxis.HORIZONTAL));
        axisGridWidget.add(this.buttonJoin);
        axisGridWidget.add(this.buttonQuickDelete);
        axisGridWidget.add(buttonWidget);
        axisGridWidget.add(buttonWidget2);
        directionalLayoutWidget.add(EmptyWidget.ofHeight(4));

        AxisGridWidget axisGridWidget2 = directionalLayoutWidget.add(new AxisGridWidget(408, 20, AxisGridWidget.DisplayAxis.HORIZONTAL));
        axisGridWidget2.add(this.buttonEdit);
        axisGridWidget2.add(this.buttonDelete);
        axisGridWidget2.add(buttonWidget3);
        axisGridWidget2.add(buttonWidget4);
        directionalLayoutWidget.refreshPositions();
        SimplePositioningWidget.setPos(directionalLayoutWidget, 0, this.height - 64, this.width, 64);

        this.updateButtonActivationStates();
    }

    @Inject(method = "updateButtonActivationStates", at = @At("TAIL"))
    private void onUpdateButtonActivationStates(CallbackInfo info) {
        MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
        this.buttonQuickDelete.active = entry instanceof MultiplayerServerListWidget.ServerEntry;
    }
}

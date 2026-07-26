package com.hirowassan.bufinder;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.File;

/**
 * BUFinder Modのエントリポイント。
 *
 * <p>役割:</p>
 * <ul>
 *   <li>{@link OutboundChatFilter} を実装し、{@code /bu on|off|clear} という
 *       疑似チャットコマンドを {@link BUCommandHandler} に委譲する。</li>
 *   <li>{@link PostRenderListener} を実装し、毎フレームの描画を
 *       {@link BUBoxRenderer} に委譲する。</li>
 * </ul>
 *
 * <p>Chunk Population中のBlock Update検知そのものは、このクラスではなく
 * {@code com.hirowassan.bufinder.mixin} パッケージ内のMixinクラスと
 * {@link PopulationTracker} / {@link BUDetectionManager} が担当する
 * (MixinはmixinConfigに列挙するだけで自動的に有効化されるため、
 * このクラスから明示的に呼び出す必要はない)。</p>
 */
public class LiteModBUFinder implements LiteMod, OutboundChatFilter, PostRenderListener, Tickable {

    public static final String MOD_NAME = "BUFinder";
    public static final String MOD_VERSION = "1.0.0";

    /** ESP表示のON/OFFを切り替えるキーバインドの説明(ローカライズキー)。Controls画面に表示される。 */
    private static final String KEY_DESCRIPTION = "key.bufinder.toggleesp";

    /** キーバインドが属するカテゴリ。既存のバニラ"その他"カテゴリを利用する。 */
    private static final String KEY_CATEGORY = "key.categories.misc";

    /** ESP表示切り替えキーのデフォルト割り当て(Bキー。バニラのデフォルトでは未使用)。 */
    private static final int DEFAULT_TOGGLE_KEY = Keyboard.KEY_B;

    private BUCommandHandler commandHandler;
    private BUBoxRenderer boxRenderer;
    private BUDetectionManager detectionManager;
    private KeyBinding toggleEspKeyBinding;

    // ---- LiteMod ----

    @Override
    public void init(File configPath) {
        // 要件: 設定の永続化(config)は不要なため、ここではコンポーネントの生成のみ行う。
        // なお、この時点ではMinecraft本体が未初期化のため、Minecraftクラスへは触れない。
        this.detectionManager = BUDetectionManager.getInstance();
        this.commandHandler = new BUCommandHandler(this.detectionManager);
        this.boxRenderer = new BUBoxRenderer(this.detectionManager);

        // ESP表示切り替え用のキーバインドを生成し、LiteLoaderのInputマネージャに登録する。
        // 登録することでバニラのControls画面(その他カテゴリ)にも表示され、
        // プレイヤーが自由にキーを再割り当てできるようになる。
        this.toggleEspKeyBinding = new KeyBinding(KEY_DESCRIPTION, DEFAULT_TOGGLE_KEY, KEY_CATEGORY);
        LiteLoader.getInput().registerKeyBinding(this.toggleEspKeyBinding);
    }

    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath) {
        // 永続設定を持たないため何もしない
    }

    @Override
    public String getName() {
        return MOD_NAME;
    }

    @Override
    public String getVersion() {
        return MOD_VERSION;
    }

    // ---- OutboundChatFilter ----

    @Override
    public boolean onSendChatMessage(String message) {
        // /bu コマンドとして処理できた場合は、実サーバーへの送信をキャンセルする(false を返す)
        boolean handled = this.commandHandler.tryHandle(message);
        return !handled;
    }

    // ---- PostRenderListener ----

    @Override
    public void onPostRenderEntities(float partialTicks) {
        // 【実験】水などの半透明ブロックより前のこの段階で描画すると、
        // 後から描かれる水の色がESPの上に重なって青く見えてしまう。
        // そのため描画は onPostRender (ワールド描画の最後) 側に移動し、
        // ここでは何もしない。もし onPostRender 側で位置がズレる/表示されない
        // 等の問題が起きた場合は、このメソッドの中身を元に戻し、
        // onPostRender 側を空にすることで元の挙動に戻せる。
    }

    @Override
    public void onPostRender(float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        // ワールド・プレイヤーが存在しない場面(タイトル画面など)では描画しない
        if (minecraft.world == null || minecraft.player == null) {
            return;
        }
        this.boxRenderer.render(minecraft, partialTicks);
    }

    // ---- Tickable ----

    @Override
    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) {
        // メニュー画面(inGame == false)ではキー入力を処理しない
        if (!inGame || this.toggleEspKeyBinding == null) {
            return;
        }

        // KeyBinding#isPressed() は「前回呼び出し以降に新たに押された回数」を消費しながら
        // 判定するため、毎フレーム呼び出しても二重に反応することはない。
        if (this.toggleEspKeyBinding.isPressed()) {
            boolean nowVisible = this.detectionManager.toggleVisibility();
            String state = nowVisible
                    ? TextFormatting.GREEN + "ON"
                    : TextFormatting.YELLOW + "OFF";
            ChatFeedback.send("ESP display: " + state + TextFormatting.RESET);
        }
    }
}

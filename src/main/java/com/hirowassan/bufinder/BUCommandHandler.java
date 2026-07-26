package com.hirowassan.bufinder;

import net.minecraft.util.text.TextFormatting;

/**
 * チャット欄に入力された {@code /bu} コマンドを解釈するクラス。
 *
 * <p>LiteLoaderの {@code OutboundChatFilter} からメッセージ送信前に呼び出される。
 * {@code /bu} はサーバー側に存在しないクライアント専用の疑似コマンドであるため、
 * 該当した場合は呼び出し元でサーバーへの送信自体をキャンセルする必要がある
 * (このクラスはtrue/falseの戻り値でそれを呼び出し元に伝える)。</p>
 */
public final class BUCommandHandler {

    private static final String COMMAND_ROOT = "/bu";

    private final BUDetectionManager detectionManager;

    public BUCommandHandler(BUDetectionManager detectionManager) {
        this.detectionManager = detectionManager;
    }

    /**
     * 送信しようとしたチャットメッセージが {@code /bu} コマンドであれば処理する。
     *
     * @param message 送信しようとしたチャットメッセージ(先頭・末尾の空白は未トリム)
     * @return {@code /bu} コマンドとして処理した場合はtrue(呼び出し元は送信をキャンセルすること)。
     *         コマンドに該当しない通常のチャットメッセージの場合はfalse。
     */
    public boolean tryHandle(String message) {
        if (message == null) {
            return false;
        }

        String trimmed = message.trim();
        boolean isCommand = trimmed.equals(COMMAND_ROOT) || trimmed.startsWith(COMMAND_ROOT + " ");
        if (!isCommand) {
            return false;
        }

        String[] tokens = trimmed.split("\\s+");
        String subCommand = tokens.length >= 2 ? tokens[1].toLowerCase() : "";

        if ("on".equals(subCommand)) {
            this.handleOn();
        } else if ("off".equals(subCommand)) {
            this.handleOff();
        } else if ("clear".equals(subCommand)) {
            this.handleClear();
        } else {
            ChatFeedback.send("Usage: /bu <on|off|clear>");
        }

        // /bu コマンドとして解釈したので、実サーバーへは送信させない
        return true;
    }

    private void handleOn() {
        this.detectionManager.enable();
        ChatFeedback.send(TextFormatting.GREEN + "BU detection ENABLED."
                + TextFormatting.RESET + " (Populate中のBlock Updateを記録します)");
    }

    private void handleOff() {
        this.detectionManager.disable();
        ChatFeedback.send(TextFormatting.YELLOW + "BU detection DISABLED."
                + TextFormatting.RESET + " (記録済みの座標は保持されます)");
    }

    private void handleClear() {
        int removedCount = this.detectionManager.clear();
        ChatFeedback.send("Cleared " + removedCount + " stored coordinate(s).");
    }
}

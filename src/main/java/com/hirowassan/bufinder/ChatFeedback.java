package com.hirowassan.bufinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * ローカルチャット欄への通知メッセージ送信を一元化するユーティリティクラス。
 *
 * <p>{@code /bu} コマンドの実行結果と、ESP表示切り替えキーバインドの
 * トグル結果の両方でこのクラスを使用することで、プレフィックスの表示形式や
 * メッセージ送信ロジックを1箇所にまとめている。</p>
 */
public final class ChatFeedback {

    private static final String PREFIX = TextFormatting.GRAY + "["
            + TextFormatting.RED + "BUFinder" + TextFormatting.GRAY + "] " + TextFormatting.RESET;

    private ChatFeedback() {
        // ユーティリティクラスのためインスタンス化しない
    }

    /**
     * ローカルチャット欄にのみメッセージを表示する(サーバーへは送信しない)。
     */
    public static void send(String text) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null) {
            player.sendMessage(new TextComponentString(PREFIX + text));
        }
    }
}

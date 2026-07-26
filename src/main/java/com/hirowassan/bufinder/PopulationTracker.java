package com.hirowassan.bufinder;

/**
 * 現在 Chunk Population 処理中かどうかを管理するクラス。
 *
 * <p>{@code MixinChunk} が {@code Chunk#populate(IChunkProvider, IChunkGenerator)} の
 * 開始・終了を検知した際にのみ、このクラスの状態が更新される。</p>
 *
 * <p>Worldごとに区別せず、単純なグローバルな再入カウンタとして実装している。
 * vanillaのChunk Populationはサーバーのメインスレッド上で同期的に実行され、
 * 複数ディメンションのPopulate処理が同時に(並行して)走ることはないため、
 * Worldごとに状態を分ける必要はない。この単純化により、Mixinで
 * {@code Chunk#world} フィールドを {@code @Shadow} する必要がなくなり、
 * 難読化環境でのフィールド名解決に依存しない、より堅牢な実装になっている。</p>
 *
 * <p>1つのチャンクのPopulate処理中に、隣接チャンクのロードなどをきっかけとして
 * 別チャンクのPopulate処理が入れ子になって呼び出される可能性があるため、
 * 単純なbooleanフラグではなく再入カウンタで管理する。</p>
 *
 * <p>書き込み(enter/exit)・読み取り(isPopulating)はいずれもサーバーの
 * メインスレッドからのみ行われるため、同期処理は行っていない。</p>
 */
public final class PopulationTracker {

    /** 現在アクティブなpopulate呼び出しの再入カウンタ。0は「Populate中ではない」を意味する。 */
    private static int populatingDepth = 0;

    private PopulationTracker() {
        // ユーティリティクラスのためインスタンス化しない
    }

    /**
     * Chunk Population処理が1つ開始したことを記録する。
     */
    public static void enterPopulation() {
        populatingDepth++;
    }

    /**
     * Chunk Population処理が1つ終了したことを記録する。
     */
    public static void exitPopulation() {
        if (populatingDepth > 0) {
            populatingDepth--;
        }
    }

    /**
     * 現在Chunk Population処理中かどうかを判定する。
     *
     * @return Populate処理中(入れ子を含む)であればtrue
     */
    public static boolean isPopulating() {
        return populatingDepth > 0;
    }
}

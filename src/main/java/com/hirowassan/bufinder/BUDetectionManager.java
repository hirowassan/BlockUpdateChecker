package com.hirowassan.bufinder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 検知したBU座標の保存、および検知の有効/無効状態を一元管理するクラス。
 *
 * <p>{@link #recordBlockUpdate(World, BlockPos)} はサーバースレッド
 * (MixinWorldのneighborChangedフック内)から呼び出され、
 * {@link #getSnapshot()} はクライアントの描画スレッドから呼び出される。
 * スレッドをまたいだアクセスが発生するため、内部コレクションには
 * スレッドセーフな実装({@link ConcurrentHashMap} ベースのSet)を使用している。</p>
 */
public final class BUDetectionManager {

    private static final BUDetectionManager INSTANCE = new BUDetectionManager();

    /**
     * 検知済み座標の集合。
     * ConcurrentHashMapのkeySetを利用することで、
     * ・追加時の重複排除(同一座標は1件のみ保持)
     * ・書き込みスレッドと読み取りスレッドが異なっても安全
     * ・読み取り側でConcurrentModificationExceptionが発生しない(弱一貫性イテレータ)
     * という3点を同時に満たす。
     */
    private final Set<BUCoordinate> coordinates =
            Collections.newSetFromMap(new ConcurrentHashMap<BUCoordinate, Boolean>());

    /** BU検知が有効かどうか。 /bu on / /bu off で切り替わる。 */
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    /**
     * ESP(ボックス描画)を表示するかどうか。キーバインドで切り替わる。
     * 検知の有効/無効({@link #enabled}）とは独立した状態であり、
     * 検知を止めていても記録済みの座標の表示/非表示だけを切り替えられる。
     */
    private final AtomicBoolean espVisible = new AtomicBoolean(true);

    private BUDetectionManager() {
    }

    public static BUDetectionManager getInstance() {
        return INSTANCE;
    }

    /**
     * BU検知を開始する({@code /bu on}）。
     */
    public void enable() {
        this.enabled.set(true);
    }

    /**
     * BU検知を停止する({@code /bu off}）。
     * 既に記録済みの座標はクリアされず、そのまま描画され続ける。
     */
    public void disable() {
        this.enabled.set(false);
    }

    /**
     * 現在BU検知が有効かどうかを返す。
     */
    public boolean isEnabled() {
        return this.enabled.get();
    }

    /**
     * ESP(ボックス描画)の表示/非表示を反転させる。キーバインドから呼び出される。
     *
     * @return 切り替え後の表示状態(trueなら表示)
     */
    public boolean toggleVisibility() {
        // AtomicBoolean には(AtomicInteger等と異なり)updateAndGet(UnaryOperator)が
        // 存在しないため、compareAndSetによるCASループで安全に反転させる。
        boolean oldValue;
        boolean newValue;
        do {
            oldValue = this.espVisible.get();
            newValue = !oldValue;
        } while (!this.espVisible.compareAndSet(oldValue, newValue));
        return newValue;
    }

    /**
     * 現在ESPが表示状態かどうかを返す。
     */
    public boolean isVisible() {
        return this.espVisible.get();
    }

    /**
     * 保存済み座標をすべて削除する({@code /bu clear}）。
     *
     * @return 削除された座標の件数
     */
    public int clear() {
        int removedCount = this.coordinates.size();
        this.coordinates.clear();
        return removedCount;
    }

    /**
     * 現在保存されている座標の件数を返す。
     */
    public int size() {
        return this.coordinates.size();
    }

    /**
     * Chunk Population中に発生したBUの座標を記録する。
     *
     * <p>検知が無効な場合は何もしない。同一座標(X, Y, Z, Dimension)は
     * {@link Set} の性質により自動的に重複排除される。</p>
     *
     * @param world BUが発生したWorld
     * @param pos   BUを受け取ったブロックの座標
     */
    public void recordBlockUpdate(World world, BlockPos pos) {
        if (!this.enabled.get()) {
            return;
        }
        int dimension = world.provider.getDimensionType().getId();
        this.coordinates.add(new BUCoordinate(pos.getX(), pos.getY(), pos.getZ(), dimension));
    }

    /**
     * 描画などで参照するための、現在保存されている座標集合を返す。
     * 内部の実体をそのまま返すため、呼び出し側で変更しないこと。
     */
    public Set<BUCoordinate> getSnapshot() {
        return this.coordinates;
    }
}

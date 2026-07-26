package com.hirowassan.bufinder;

/**
 * Chunk Population中に発生したBlock Update(BU)の発生座標を表す不変クラス。
 *
 * <p>X, Y, Z, Dimension の4値の組み合わせで同一性(equals/hashCode)を判定するため、
 * {@link java.util.Set} に格納することで同一座標の重複保存を自動的に防止できる。</p>
 */
public final class BUCoordinate {

    private final int x;
    private final int y;
    private final int z;
    private final int dimension;

    public BUCoordinate(int x, int y, int z, int dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getDimension() {
        return this.dimension;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BUCoordinate)) {
            return false;
        }
        BUCoordinate other = (BUCoordinate) obj;
        return this.x == other.x
                && this.y == other.y
                && this.z == other.z
                && this.dimension == other.dimension;
    }

    @Override
    public int hashCode() {
        // X, Y, Z, Dimension の4値から一意なハッシュを生成する
        int result = this.x;
        result = 31 * result + this.y;
        result = 31 * result + this.z;
        result = 31 * result + this.dimension;
        return result;
    }

    @Override
    public String toString() {
        return "BUCoordinate{x=" + this.x + ", y=" + this.y + ", z=" + this.z
                + ", dimension=" + this.dimension + '}';
    }
}

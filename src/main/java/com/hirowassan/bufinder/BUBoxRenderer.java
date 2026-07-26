package com.hirowassan.bufinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.util.Set;

/**
 * {@link BUDetectionManager} に記録された座標を、
 * 壁越しでも視認できる半透明の赤いボックス(ESP風)としてワールド上に描画するクラス。
 *
 * <p>現在プレイヤーがいるディメンションかつ、現在の描画距離設定(視点からの距離)の
 * 範囲内にある座標のみを描画対象とする。</p>
 */
public final class BUBoxRenderer {

    /** 塗りつぶし面の色 (薄い赤・半透明。真っ赤すぎて主張しすぎないよう彩度・不透明度を抑えている) */
    private static final float FILL_RED = 1.0f;
    private static final float FILL_GREEN = 0.35f;
    private static final float FILL_BLUE = 0.35f;
    private static final float FILL_ALPHA = 0.16f;

    /** 輪郭線の色 (塗りつぶしより少し濃いめの赤だが、こちらも彩度を抑えている) */
    private static final float LINE_RED = 1.0f;
    private static final float LINE_GREEN = 0.4f;
    private static final float LINE_BLUE = 0.4f;
    private static final float LINE_ALPHA = 0.7f;
    private static final float LINE_WIDTH = 2.0f;

    /** 1ブロック分のボックスの一辺の長さ */
    private static final double BOX_EDGE_LENGTH = 1.0;

    /** 1チャンクあたりのブロック数(描画距離をブロック単位に変換するために使用) */
    private static final int BLOCKS_PER_CHUNK = 16;

    /**
     * lightmap(ブロック光・スカイ光の合成テクスチャ)を強制的に最大輝度にするための座標値。
     * ブロック光・スカイ光ともに最大値である15をテクスチャ座標系(15 * 16 = 240)に変換した値。
     */
    private static final float FULL_BRIGHTNESS = 240.0F;

    private final BUDetectionManager detectionManager;

    /**
     * 描画前のGL_LIGHTING / GL_FOGの有効状態を一時保存するためのフィールド。
     * setupRenderState()で保存し、restoreRenderState()で元の状態に正確に戻すために使う。
     * (レンダースレッドからのみ使用されるため、フィールドで使い回しても問題ない)
     *
     * 注: lightmapの輝度座標(setLightmapTextureCoordsで設定する値)については、
     * vanillaのOpenGlHelperに「直前に設定した値」を取得する手段が存在しないため、
     * 同様の保存・復元は行わない。地形やパーティクルなど他のほとんどの描画は
     * 頂点データ自体にlightmap座標を含む形式(BLOCK, PARTICLE_POSITION_TEX_COLOR_LMAP等)を
     * 使っており、そちらは頂点ごとの値が優先されるため、ここで設定した値が
     * 後続の描画に悪影響を与えることはない。
     */
    private boolean wasLightingEnabled;
    private boolean wasFogEnabled;

    public BUBoxRenderer(BUDetectionManager detectionManager) {
        this.detectionManager = detectionManager;
    }

    /**
     * 記録済みのBU座標をすべて描画する。
     *
     * @param minecraft   Minecraftインスタンス(worldとplayerがnullでないことを呼び出し元で保証すること)
     * @param partialTicks パーシャルティック(未使用だが将来の拡張のために保持)
     */
    public void render(Minecraft minecraft, float partialTicks) {
        // キーバインドでESP表示がOFFにされている場合は何も描画しない
        if (!this.detectionManager.isVisible()) {
            return;
        }

        Set<BUCoordinate> snapshot = this.detectionManager.getSnapshot();
        if (snapshot.isEmpty()) {
            return;
        }

        EntityPlayer player = minecraft.player;
        int currentDimension = player.world.provider.getDimensionType().getId();

        // 描画距離をブロック単位に変換して、範囲外の座標は描画しないようにする
        double renderDistanceBlocks = minecraft.gameSettings.renderDistanceChunks * (double) BLOCKS_PER_CHUNK;
        double renderDistanceSq = renderDistanceBlocks * renderDistanceBlocks;

        // このフレームで実際にワールドが描画されているカメラの補間座標を取得する
        RenderManager renderManager = minecraft.getRenderManager();
        double cameraX = renderManager.viewerPosX;
        double cameraY = renderManager.viewerPosY;
        double cameraZ = renderManager.viewerPosZ;

        this.setupRenderState();
        try {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            for (BUCoordinate coordinate : snapshot) {
                if (coordinate.getDimension() != currentDimension) {
                    continue;
                }

                double blockCenterX = coordinate.getX() + 0.5;
                double blockCenterY = coordinate.getY() + 0.5;
                double blockCenterZ = coordinate.getZ() + 0.5;

                double distanceSq = squaredDistance(
                        blockCenterX, blockCenterY, blockCenterZ,
                        cameraX, cameraY, cameraZ
                );
                if (distanceSq > renderDistanceSq) {
                    continue;
                }

                // カメラを原点とした相対座標に変換する
                double relativeX = coordinate.getX() - cameraX;
                double relativeY = coordinate.getY() - cameraY;
                double relativeZ = coordinate.getZ() - cameraZ;

                this.drawFilledBox(tessellator, buffer, relativeX, relativeY, relativeZ);
                this.drawWireBox(tessellator, buffer, relativeX, relativeY, relativeZ);
            }
        } finally {
            this.restoreRenderState();
        }
    }

    private static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * 壁越しに見えるように深度テストを無効化し、
     * 半透明描画用のブレンド設定・ライン幅を適用する。
     */
    private void setupRenderState() {
        // 描画前のライティング/フォグの状態を記憶しておく(restoreRenderStateで正確に復元するため)
        this.wasLightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        this.wasFogEnabled = GL11.glIsEnabled(GL11.GL_FOG);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        // メインのテクスチャユニット(unit 0)を確実にアクティブにしてから無効化する。
        // 直前の描画処理でアクティブなユニットがlightmap側(unit 1)のままになっていることがあり、
        // その状態でdisableTexture2D()を呼ぶとunit 0側が無効化されず、
        // 地形アトラス等の残留テクスチャがESPの色に誤って合成されてしまう。
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableTexture2D();

        // 深度テストを無効化することで壁越しでも常時視認できる(ESP風)描画にする
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        // 直前のエンティティ描画等でGL_LIGHTINGが有効なまま残っていることがあり、
        // 法線を持たないPOSITION_COLOR頂点はその場合に真っ黒に計算されてしまう。
        // (「ESPが黒くなる」問題の原因)
        GlStateManager.disableLighting();

        // 水中/溶岩中などフォグが濃い状況では、フォグの影響で色が本来の赤から
        // 大きく変化してしまう(「液体の場合に変になる」問題の原因)。
        // ESPは常に一定の見た目であるべきなのでフォグの影響を受けないようにする。
        GlStateManager.disableFog();

        // lightmap(ブロック光・スカイ光の合成テクスチャ、unit 1)の参照座標を
        // 強制的に最大輝度に固定する。Night Visionはこのlightmapのテクスチャ行列を
        // 書き換えることで暗闇を明るく見せているため、GL_LIGHTINGとは別にこちらも
        // 対処しないと、Night Visionの有無によってESPの見え方が変わってしまう
        // (Night Vision使用時に正しい色にならない問題の原因)。
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, FULL_BRIGHTNESS, FULL_BRIGHTNESS);

        GlStateManager.glLineWidth(LINE_WIDTH);
    }

    /**
     * GL状態を描画前の状態に戻す。他の描画処理(水などの半透明ブロックやパーティクル)に
     * 影響を与えないよう、setupRenderStateで変更した内容を正確に元に戻す。
     */
    private void restoreRenderState() {
        // 色の状態を白(1,1,1,1)にリセットし、以降の描画に色情報が残らないようにする
        GlStateManager.resetColor();

        // ライティング/フォグは、描画前に実際に有効だった場合のみ再度有効化する
        // (無条件にenableすると、逆に元々無効だった描画パスに影響を与えてしまうため)
        if (this.wasFogEnabled) {
            GlStateManager.enableFog();
        }
        if (this.wasLightingEnabled) {
            GlStateManager.enableLighting();
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 半透明の塗りつぶし立方体(6面)を描画する。
     *
     * @param x カメラ基準の相対X座標(ボックスの最小X)
     * @param y カメラ基準の相対Y座標(ボックスの最小Y)
     * @param z カメラ基準の相対Z座標(ボックスの最小Z)
     */
    private void drawFilledBox(Tessellator tessellator, BufferBuilder buffer, double x, double y, double z) {
        double minX = x;
        double minY = y;
        double minZ = z;
        double maxX = x + BOX_EDGE_LENGTH;
        double maxY = y + BOX_EDGE_LENGTH;
        double maxZ = z + BOX_EDGE_LENGTH;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // 下面 (Y-)
        buffer.pos(minX, minY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, minY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, minY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(minX, minY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();

        // 上面 (Y+)
        buffer.pos(minX, maxY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, maxY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(minX, maxY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();

        // 北面 (Z-)
        buffer.pos(minX, maxY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, maxY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, minY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(minX, minY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();

        // 南面 (Z+)
        buffer.pos(minX, minY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, minY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(minX, maxY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();

        // 西面 (X-)
        buffer.pos(minX, maxY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(minX, maxY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(minX, minY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(minX, minY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();

        // 東面 (X+)
        buffer.pos(maxX, minY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, minY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, maxY, minZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();

        tessellator.draw();
    }

    /**
     * 立方体の輪郭線(12辺)を描画する。塗りつぶしだけよりも視認性が上がる。
     */
    private void drawWireBox(Tessellator tessellator, BufferBuilder buffer, double x, double y, double z) {
        double minX = x;
        double minY = y;
        double minZ = z;
        double maxX = x + BOX_EDGE_LENGTH;
        double maxY = y + BOX_EDGE_LENGTH;
        double maxZ = z + BOX_EDGE_LENGTH;

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // 下面4辺
        line(buffer, minX, minY, minZ, maxX, minY, minZ);
        line(buffer, maxX, minY, minZ, maxX, minY, maxZ);
        line(buffer, maxX, minY, maxZ, minX, minY, maxZ);
        line(buffer, minX, minY, maxZ, minX, minY, minZ);

        // 上面4辺
        line(buffer, minX, maxY, minZ, maxX, maxY, minZ);
        line(buffer, maxX, maxY, minZ, maxX, maxY, maxZ);
        line(buffer, maxX, maxY, maxZ, minX, maxY, maxZ);
        line(buffer, minX, maxY, maxZ, minX, maxY, minZ);

        // 垂直4辺
        line(buffer, minX, minY, minZ, minX, maxY, minZ);
        line(buffer, maxX, minY, minZ, maxX, maxY, minZ);
        line(buffer, maxX, minY, maxZ, maxX, maxY, maxZ);
        line(buffer, minX, minY, maxZ, minX, maxY, maxZ);

        tessellator.draw();
    }

    private static void line(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2, double z2) {
        buffer.pos(x1, y1, z1).color(LINE_RED, LINE_GREEN, LINE_BLUE, LINE_ALPHA).endVertex();
        buffer.pos(x2, y2, z2).color(LINE_RED, LINE_GREEN, LINE_BLUE, LINE_ALPHA).endVertex();
    }
}

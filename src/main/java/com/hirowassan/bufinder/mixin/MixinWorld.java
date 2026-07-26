package com.hirowassan.bufinder.mixin;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.hirowassan.bufinder.BUDetectionManager;
import com.hirowassan.bufinder.PopulationTracker;

/**
 * {@code net.minecraft.world.World#neighborChanged(BlockPos, Block, BlockPos)} は、
 * あるブロックへのBlock Update(BU)がバニラ内部で実際に配送される、
 * 単一の集約ポイントである(呼び出し原因が装飾生成・レッドストーン・ピストン・
 * 水/溶岩流・プレイヤー操作のいずれであっても、最終的に必ずここを通る)。
 *
 * <p>このMixinはこのメソッドのHEADにフックし、{@link PopulationTracker#isPopulating()}が
 * trueの場合のみ座標を記録する。</p>
 *
 * <p>クライアント側Worldとサーバー側Worldを区別する {@code isRemote} チェックは
 * あえて行っていない。Chunk Population(チャンク生成の装飾処理)はサーバー側でのみ
 * 発生する処理であり、クライアントが自らチャンクをPopulateすることはないため、
 * {@link PopulationTracker#isPopulating()} が true になるのは実質的にサーバー側の
 * Worldに対してのみである。{@code isRemote} フィールドを {@code @Shadow} すると
 * 難読化された本番環境でrefmap解決に失敗することが実機で確認されたため、
 * このチェックを省略することで {@code @Shadow} フィールドへの依存自体を無くしている。</p>
 *
 * <p>この設計により、「通常プレイ中のBU」「プレイヤー操作・レッドストーン・
 * ピストンなどによるBU」は対象外となり、「Chunk Population中に発生したBUのみ」を
 * 正しく判定できる。</p>
 */
@Mixin(World.class)
public abstract class MixinWorld {

    @Inject(
            method = "neighborChanged(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;"
                    + "Lnet/minecraft/util/math/BlockPos;)V",
            at = @At("HEAD")
    )
    private void bufinder$onNeighborChanged(BlockPos pos, Block blockIn, BlockPos fromPos, CallbackInfo ci) {
        // Chunk Population処理中でなければ、通常プレイ中のBUとみなして無視する
        if (!PopulationTracker.isPopulating()) {
            return;
        }

        World self = (World) (Object) this;
        BUDetectionManager.getInstance().recordBlockUpdate(self, pos);
    }
}

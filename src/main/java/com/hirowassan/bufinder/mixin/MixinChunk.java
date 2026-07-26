package com.hirowassan.bufinder.mixin;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.hirowassan.bufinder.PopulationTracker;

/**
 * {@code net.minecraft.world.chunk.Chunk#populate(IChunkProvider, IChunkGenerator)} は、
 * バニラにおいて「このチャンクの装飾(Chunk Population)処理」を実行する実際のエントリポイントである
 * (Forgeにおける {@code PopulateChunkEvent} も本来はこのメソッド呼び出しの前後で発火する)。
 *
 * <p>このMixinは当該メソッドの開始(HEAD)と終了(RETURN)にフックし、
 * 「現在Populate処理中」であることを {@link PopulationTracker} に伝える役割のみを持つ。
 * 実際のBU座標の記録は {@code MixinWorld} 側で行う。</p>
 *
 * <p>意図的に {@code @Shadow} フィールドを一切使用していない。難読化された
 * 本番環境ではフィールドのrefmap解決が失敗する場合があるため
 * (実機で {@code Shadow field ... was not located} エラーとして確認済み)、
 * より広くテストされている {@code @Inject}(メソッド)のみで実装している。</p>
 *
 * <p>本Mixinはメソッドの実行タイミングを検知するだけで、
 * 処理内容そのものは一切変更しない(バニラの挙動に影響を与えない)。</p>
 */
@Mixin(Chunk.class)
public abstract class MixinChunk {

    @Inject(
            method = "populate(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/gen/IChunkGenerator;)V",
            at = @At("HEAD")
    )
    private void bufinder$onPopulateStart(IChunkProvider chunkProvider, IChunkGenerator chunkGenerator,
                                           CallbackInfo ci) {
        PopulationTracker.enterPopulation();
    }

    @Inject(
            method = "populate(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/gen/IChunkGenerator;)V",
            at = @At("RETURN")
    )
    private void bufinder$onPopulateEnd(IChunkProvider chunkProvider, IChunkGenerator chunkGenerator,
                                         CallbackInfo ci) {
        PopulationTracker.exitPopulation();
    }
}

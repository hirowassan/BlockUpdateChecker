# BUFinder

Minecraft 1.12.2 / LiteLoader 用Mod。
Chunk Population(チャンク生成のPopulate処理)中に発生したBlock Update(BU)の座標を検知し、
壁越しに視認できる赤色半透明ボックス(ESP風)としてワールド上に可視化します。
MansionのBUを描画すると非常に重くなってしまうため注意してください。

lite-mod-template (https://github.com/CrazyHPi/lite-mod-template) をベースに実装しています。

全部Claudeが書きました。感謝ですね。

## コマンド
- `/bu on` : BU検知を開始
- `/bu off` : BU検知を停止(記録済み座標は保持)
- `/bu clear` : 記録済み座標をすべて削除

いずれもクライアント側でのみ処理される疑似コマンドであり、サーバーには送信されません。

## ビルド
```
./gradlew build
```
`build/libs/` にlitemodファイルが生成されます。

## 実行
```
./gradlew runClient
```

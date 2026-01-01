# Jack Programs - Nand2Tetris Project 9

Nand2Tetris第9章のJackプログラム集です。
Jack言語で書かれたサンプルプログラムを含んでいます。

## ディレクトリ構成

```
Jack/
├── docs/
│   └── Chapter9.md      # Jack言語の解説
├── src/
│   ├── HelloWorld/      # Hello Worldプログラム
│   │   └── Main.jack
│   ├── Average/         # 平均値計算プログラム
│   │   └── Main.jack
│   ├── Square/          # 四角形操作ゲーム
│   │   ├── Main.jack
│   │   ├── Square.jack
│   │   └── SquareGame.jack
│   └── Pong/            # Pongゲーム
│       ├── Main.jack
│       ├── Ball.jack
│       ├── Paddle.jack
│       └── PongGame.jack
└── README.md
```

## サンプルプログラム

### HelloWorld

最もシンプルなJackプログラム。

```jack
class Main {
    function void main() {
        do Output.printString("Hello, World!");
        do Output.println();
        return;
    }
}
```

### Average

ユーザーから数値を入力してもらい、平均値を計算します。
- 配列の使用
- キーボード入力
- ループ処理

### Square

キーボードで四角形を操作するプログラム。
- 矢印キー: 移動
- Z/X: サイズ変更
- Q: 終了

クラス構成:
- `Square`: 四角形の描画と移動
- `SquareGame`: ゲームループとキー入力

### Pong

シンプルなPongゲーム。
- 上下矢印: パドル移動
- Q: 終了

クラス構成:
- `Ball`: ボールの移動と壁での反射
- `Paddle`: プレイヤー操作のパドル
- `PongGame`: ゲームループと衝突判定

## コンパイルと実行

### 1. JackCompilerでコンパイル

```bash
# Nand2Tetrisのツールを使用
JackCompiler src/HelloWorld/
```

これにより `.vm` ファイルが生成されます。

### 2. VMエミュレータで実行

```bash
VMEmulator
```

1. File → Load Program
2. 生成された `.vm` ファイルを含むディレクトリを選択
3. Run

### 3. または、完全な変換チェーン

```bash
# Jack → VM
JackCompiler src/HelloWorld/

# VM → ASM（自作のVMトランスレータを使用）
cd ../VM
java -cp bin VMTranslator ../Jack/src/HelloWorld/

# ASM → HACK（自作のアセンブラを使用）
cd ../ASMBR
java -cp bin Assembler ../Jack/src/HelloWorld/HelloWorld.asm

# HACKファイルをCPUエミュレータで実行
```

## Jack言語の基本

### データ型

| 型 | 説明 |
|----|------|
| `int` | 16ビット整数 |
| `char` | 文字 |
| `boolean` | true/false |
| `クラス名` | オブジェクト参照 |

### サブルーチンの種類

| 種類 | キーワード | 説明 |
|------|-----------|------|
| コンストラクタ | `constructor` | オブジェクト生成 |
| メソッド | `method` | インスタンスメソッド |
| 関数 | `function` | 静的メソッド |

### 変数の種類

| 種類 | キーワード | スコープ |
|------|-----------|---------|
| フィールド | `field` | インスタンス |
| 静的変数 | `static` | クラス全体 |
| ローカル | `var` | サブルーチン内 |

### 標準ライブラリ

- `Math`: 算術演算
- `String`: 文字列操作
- `Array`: 配列
- `Output`: 画面出力
- `Keyboard`: キーボード入力
- `Screen`: グラフィックス
- `Memory`: メモリ操作
- `Sys`: システム関数

詳細は `docs/Chapter9.md` を参照してください。

## 注意事項

- オブジェクトは必ず `dispose()` で解放する
- 配列も `dispose()` で解放する
- メモリリークに注意

## 参考資料

- [Nand2Tetris公式サイト](https://www.nand2tetris.org/)
- docs/Chapter9.md - Jack言語の詳細解説

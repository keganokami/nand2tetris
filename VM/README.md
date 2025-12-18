# VM Translator - Nand2Tetris Project 7 & 8

Nand2TetrisプロジェクトのVMトランスレータ実装です。
スタックベースのVMコードをHackアセンブリ言語に変換します。

## プロジェクト概要

このVMトランスレータは、高級言語（Jack）とHackアセンブリの中間層となる仮想マシンコードを処理します。

### アーキテクチャ

```
高級言語 (Jack)
      ↓
  コンパイラ
      ↓
   VMコード (.vm)
      ↓
VMトランスレータ ← このプロジェクト
      ↓
アセンブリコード (.asm)
      ↓
   アセンブラ
      ↓
機械語 (.hack)
```

## 実装機能（プロジェクト7）

### スタック演算
- `push segment index` - 指定セグメントの値をスタックにpush
- `pop segment index` - スタックから値をpopして指定セグメントに格納

### 算術/論理演算
- `add` - 加算
- `sub` - 減算
- `neg` - 符号反転
- `eq` - 等しい
- `gt` - より大きい
- `lt` - より小さい
- `and` - ビット単位のAND
- `or` - ビット単位のOR
- `not` - ビット単位のNOT

### メモリセグメント
- `constant` - 定数（0-32767）
- `local` - ローカル変数
- `argument` - 関数の引数
- `this` - オブジェクトのフィールド
- `that` - 配列の要素
- `temp` - 一時変数（8個）
- `pointer` - thisとthatのポインタ
- `static` - 静的変数

## クラス構成

### Parser.java
VMコマンドを解析するクラス
- `.vmファイルを1行ずつ読み込み
- コメントと空白を除去
- コマンドタイプと引数を抽出

主なメソッド：
- `hasMoreCommands()` - まだコマンドがあるか
- `advance()` - 次のコマンドを読み込む
- `commandType()` - コマンドタイプを返す
- `arg1()` - 第1引数を返す
- `arg2()` - 第2引数を返す

### CodeWriter.java
VMコマンドをHackアセンブリコードに変換するクラス
- 各VMコマンドに対応するアセンブリコードを生成
- `.asmファイルに書き込む

主なメソッド：
- `setFileName(String)` - 現在のファイル名を設定
- `writeArithmetic(String)` - 算術コマンドを変換
- `writePushPop(CommandType, String, int)` - push/popコマンドを変換

### VMTranslator.java
メインクラス
- コマンドライン引数を処理
- ParserとCodeWriterを連携させて変換を実行

## メモリマップ

Hackコンピュータのメモリ構成：

| アドレス | 用途 |
|---------|------|
| RAM[0] | SP - Stack Pointer（スタックの次の空き位置） |
| RAM[1] | LCL - Local（localセグメントのベース） |
| RAM[2] | ARG - Argument（argumentセグメントのベース） |
| RAM[3] | THIS（thisセグメントのベース） |
| RAM[4] | THAT（thatセグメントのベース） |
| RAM[5-12] | Temp（一時変数、8個） |
| RAM[13-15] | 汎用レジスタ |
| RAM[16-255] | Static（静的変数） |
| RAM[256-2047] | Stack（スタック領域） |

## 使用方法

### コンパイル

```bash
cd VM
javac VMTranslator.java Parser.java CodeWriter.java
```

### 実行

単一の.vmファイルを変換：
```bash
java VMTranslator SimpleAdd.vm
```

ディレクトリ内の全.vmファイルを変換：
```bash
java VMTranslator MyProgram/
```

### テスト（SimpleAdd）

```bash
# 1. コンパイル
javac *.java

# 2. SimpleAdd.vmを変換
java VMTranslator SimpleAdd.vm

# 3. SimpleAdd.asmが生成される
# 4. Nand2TetrisのCPUエミュレータで実行して結果を確認
```

期待される結果：
- スタックトップに15（7+8）が格納される

## SimpleAdd.vmの動作

```
push constant 7   // スタック: [7]
push constant 8   // スタック: [7, 8]
add              // スタック: [15]
```

生成されるアセンブリコードの概要：
```assembly
// push constant 7
@7
D=A
@SP
A=M
M=D
@SP
M=M+1

// push constant 8
@8
D=A
@SP
A=M
M=D
@SP
M=M+1

// add
@SP
AM=M-1
D=M
A=A-1
M=D+M
```

## 実装機能（プロジェクト8）

### プログラムフロー制御
- `label ラベル名` - ラベルを定義
- `goto ラベル名` - 無条件ジャンプ
- `if-goto ラベル名` - スタックトップが0以外ならジャンプ

### 関数呼び出し
- `function 関数名 ローカル変数数` - 関数定義
- `call 関数名 引数数` - 関数呼び出し
- `return` - 関数からの復帰

### ブートストラップコード
- SP=256で初期化
- Sys.init関数を呼び出し

詳細は[PROJECT8.md](PROJECT8.md)を参照してください。

## 参考資料

- [Nand2Tetris公式サイト](https://www.nand2tetris.org/)
- プロジェクト7の課題：
  - SimpleAdd
  - StackTest
  - BasicTest
  - PointerTest
  - StaticTest
- プロジェクト8の課題：
  - BasicLoop
  - FibonacciSeries
  - SimpleFunction
  - NestedCall
  - FibonacciElement
  - StaticsTest

## ライセンス

このコードはNand2Tetrisコースの学習目的で作成されています。

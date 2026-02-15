# Nand2Tetris Projects

Nand2Tetrisコース（コンピュータシステムの理論と実装）のプロジェクト実装集です。

## プロジェクト構成

### ASMBR - Hackアセンブラ（プロジェクト6）

Hack機械語のアセンブラ実装。アセンブリ言語(.asm)をバイナリ機械語(.hack)に変換します。

```
ASMBR/
├── src/          # Javaソースコード
├── bin/          # コンパイル済みクラスファイル
├── tasks/        # 課題用.asm/.hackファイル
├── README.md     # 詳細なドキュメント
└── Makefile
```

**実装機能:**
- A命令とC命令のパース
- シンボルテーブル管理
- 2パスアセンブル
- ラベルと変数の解決

**使用方法:**
```bash
cd ASMBR
javac -d bin src/*.java
java -cp bin Assembler tasks/Add.asm
```

### VM - VMトランスレータ（プロジェクト7 & 8）

スタックベースのVM言語をHackアセンブリ言語に変換するトランスレータ実装。

```
VM/
├── src/              # Javaソースコード
│   ├── Parser.java       # VMコマンド解析
│   ├── CodeWriter.java   # アセンブリ生成
│   ├── VMTranslator.java # メインクラス
│   ├── Parser.md         # 解説ドキュメント
│   ├── CodeWriter.md
│   ├── VMTranslator.md
│   └── Chapter8.md       # 8章の解説
├── bin/              # コンパイル済みクラスファイル
├── tasks/            # 課題フォルダ
│   ├── StackArithmetic/  # プロジェクト7
│   ├── MemoryAccess/     # プロジェクト7
│   ├── ProgramFlow/      # プロジェクト8
│   └── FunctionCalls/    # プロジェクト8
├── README.md
└── PROJECT8.md       # プロジェクト8の詳細解説
```

**実装機能（プロジェクト7）:**
- スタック演算（push/pop）
- 算術/論理演算（add, sub, neg, eq, gt, lt, and, or, not）
- メモリセグメント（constant, local, argument, this, that, temp, pointer, static）

**実装機能（プロジェクト8）:**
- プログラムフロー制御（label, goto, if-goto）
- 関数呼び出し（function, call, return）
- ブートストラップコード（SP初期化、Sys.init呼び出し）

**使用方法:**
```bash
cd VM
javac -d bin src/*.java

# 単一ファイル変換
java -cp bin VMTranslator tasks/StackArithmetic/SimpleAdd/SimpleAdd.vm

# ディレクトリ変換（複数.vmファイル → 1つの.asm）
java -cp bin VMTranslator tasks/FunctionCalls/FibonacciElement
```

### Jack - Jackプログラム（プロジェクト9）

Jack言語で書かれたサンプルプログラム集。

```
Jack/
├── docs/
│   └── Chapter9.md      # Jack言語の解説
├── src/
│   ├── HelloWorld/      # Hello World
│   ├── Average/         # 平均値計算
│   ├── Square/          # 四角形操作ゲーム
│   └── Pong/            # Pongゲーム
└── README.md
```

**サンプルプログラム:**
- HelloWorld: 最初のJackプログラム
- Average: 配列と入出力の練習
- Square: キーボードで四角形を操作
- Pong: シンプルなPongゲーム

### JackCompiler - Jackコンパイラ（プロジェクト10 & 11）

Jack言語をVM言語にコンパイルするコンパイラ実装。

```
JackCompiler/
├── src/                          # Javaソースコード
│   ├── JackTokenizer.java        # 字句解析器
│   ├── CompilationEngine.java    # 構文解析器（XML出力）
│   ├── JackAnalyzer.java         # 第10章メイン
│   ├── SymbolTable.java          # シンボルテーブル
│   ├── VMWriter.java             # VMコード出力
│   ├── CompilationEngineVM.java  # コード生成器
│   └── JackCompiler.java         # 第11章メイン
├── bin/                          # コンパイル済みクラスファイル
├── docs/                         # ドキュメント
│   ├── Chapter10.md              # 第10章（構文解析）解説
│   └── Chapter11.md              # 第11章（コード生成）解説
├── README.md
└── Makefile
```

**実装機能（プロジェクト10）:**
- 字句解析（トークナイザ）
- 再帰下降構文解析
- XML形式の構文木出力

**実装機能（プロジェクト11）:**
- シンボルテーブル（変数管理）
- VMコード生成
- 式、文、サブルーチンのコンパイル

**使用方法:**
```bash
cd JackCompiler
make compile

# 構文解析（XML出力）
java -cp bin JackAnalyzer ../Jack/src/Square/

# コンパイル（VMコード出力）
java -cp bin JackCompiler ../Jack/src/Square/
```

### OS - Jack OS（プロジェクト12）

Jack言語で実装されたオペレーティングシステム。Hackコンピューター上でJackプログラムを実行するための基盤を提供します。

```
OS/
├── src/                    # Jackソースコード
│   ├── Math.jack           # 数学関数
│   ├── String.jack         # 文字列操作
│   ├── Array.jack          # 配列操作
│   ├── Memory.jack         # メモリ管理
│   ├── Screen.jack         # グラフィック描画
│   ├── Output.jack         # テキスト出力
│   ├── Keyboard.jack       # キーボード入力
│   └── Sys.jack            # システムサービス
├── docs/
│   └── Chapter12.md        # 第12章の解説
└── README.md
```

**実装機能:**
- 数学演算（乗算、除算、平方根）
- 文字列・配列操作
- ヒープメモリ管理（動的割り当て）
- グラフィック描画（点、線、矩形、円）
- テキスト出力（8x11ピクセルフォント）
- キーボード入力

**使用方法:**
```bash
# JackCompilerでOSをコンパイル
cd OS/src
JackCompiler .

# Jackプログラムと統合
cp *.vm ../Jack/src/Square/
JackCompiler ../Jack/src/Square/
```

## コース概要

[Nand2Tetris](https://www.nand2tetris.org/)は、NANDゲートからテトリスまで、コンピュータシステムを下から上まで構築する教育コースです。

### プロジェクトの進行

- ✅ プロジェクト1: ブール論理
- ✅ プロジェクト2: ブール演算
- ✅ プロジェクト3: 順序回路
- ✅ プロジェクト4: 機械語
- ✅ プロジェクト5: コンピュータアーキテクチャ
- ✅ プロジェクト6: アセンブラ（ASMBR）
- ✅ プロジェクト7: VMトランスレータ I - スタック演算（VM）
- ✅ プロジェクト8: VMトランスレータ II - プログラムフロー（VM）
- ✅ プロジェクト9: 高級言語（Jack）
- ✅ プロジェクト10: コンパイラ I - 構文解析（JackCompiler）
- ✅ プロジェクト11: コンパイラ II - コード生成（JackCompiler）
- ✅ プロジェクト12: オペレーティングシステム（OS）

## 開発環境

- Java 8以上
- Nand2Tetrisソフトウェアスイート

## ライセンス

このコードはNand2Tetrisコースの学習目的で作成されています。

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

**実行方法:**
```bash
# Nand2TetrisのJackCompilerでコンパイル
JackCompiler Jack/src/HelloWorld/

# VMEmulatorで実行
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
- ⬜ プロジェクト10: コンパイラ I - 構文解析
- ⬜ プロジェクト11: コンパイラ II - コード生成
- ⬜ プロジェクト12: オペレーティングシステム

## 開発環境

- Java 8以上
- Nand2Tetrisソフトウェアスイート

## ライセンス

このコードはNand2Tetrisコースの学習目的で作成されています。

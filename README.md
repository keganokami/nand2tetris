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

### VM - VMトランスレータ（プロジェクト7）

スタックベースのVM言語をHackアセンブリ言語に変換するトランスレータ実装。

```
VM/
├── src/          # Javaソースコード
├── bin/          # コンパイル済みクラスファイル
├── tasks/        # 課題フォルダ
│   ├── StackArithmetic/
│   └── MemoryAccess/
└── README.md     # 詳細なドキュメント
```

**実装機能:**
- スタック演算（push/pop）
- 算術/論理演算（add, sub, neg, eq, gt, lt, and, or, not）
- メモリセグメント（constant, local, argument, this, that, temp, pointer, static）

**使用方法:**
```bash
cd VM
javac -d bin src/*.java
java -cp bin VMTranslator tasks/StackArithmetic/SimpleAdd/SimpleAdd.vm
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
- ⬜ プロジェクト8: VMトランスレータ II - プログラムフロー
- ⬜ プロジェクト9: 高級言語
- ⬜ プロジェクト10: コンパイラ I - 構文解析
- ⬜ プロジェクト11: コンパイラ II - コード生成
- ⬜ プロジェクト12: オペレーティングシステム

## 開発環境

- Java 8以上
- Nand2Tetrisソフトウェアスイート

## ライセンス

このコードはNand2Tetrisコースの学習目的で作成されています。

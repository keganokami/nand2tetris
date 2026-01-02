# Jack Compiler

Nand2Tetris プロジェクト10 & 11: Jack言語コンパイラ

## 概要

Jack言語のソースコードをVM言語にコンパイルするコンパイラです。

- **第10章**: 構文解析器（JackAnalyzer）- XML出力
- **第11章**: コンパイラ（JackCompiler）- VMコード出力

## ディレクトリ構成

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
├── bin/                          # コンパイル済みクラス
├── docs/                         # ドキュメント
│   ├── Chapter10.md              # 第10章解説
│   └── Chapter11.md              # 第11章解説
├── Makefile
└── README.md
```

## 使い方

### コンパイル

```bash
make compile
```

### 構文解析（第10章）

```bash
# 単一ファイル
java -cp bin JackAnalyzer path/to/File.jack

# ディレクトリ
java -cp bin JackAnalyzer path/to/directory/
```

出力：
- `FileT.xml` - トークン列
- `File.xml` - 構文木

### コンパイル（第11章）

```bash
# 単一ファイル
java -cp bin JackCompiler path/to/File.jack

# ディレクトリ
java -cp bin JackCompiler path/to/directory/
```

出力：
- `File.vm` - VMコード

### テスト

```bash
# 構文解析器のテスト
make test-analyzer

# コンパイラのテスト
make test-compiler
```

## モジュール説明

### JackTokenizer（字句解析器）

ソースコードをトークン列に分解します。

```
class Main { ... }
  ↓
[class] [Main] [{] ... [}]
```

トークンの種類：
- KEYWORD（予約語）
- SYMBOL（記号）
- INT_CONST（整数定数）
- STRING_CONST（文字列定数）
- IDENTIFIER（識別子）

### CompilationEngine（構文解析器）

再帰下降構文解析でトークン列を構文木に変換します。

```
let x = 1 + 2;
  ↓
<letStatement>
  <keyword> let </keyword>
  <identifier> x </identifier>
  ...
</letStatement>
```

### SymbolTable（シンボルテーブル）

変数の情報を管理します。

| 変数 | 型 | 種類 | インデックス |
|------|-----|------|-------------|
| x | int | VAR | 0 |
| y | int | VAR | 1 |
| count | int | STATIC | 0 |

### VMWriter（VMコード出力）

VMコマンドを出力します。

```
push constant 1
push constant 2
add
pop local 0
```

### CompilationEngineVM（コード生成器）

構文解析しながらVMコードを生成します。

## Jack言語の文法

### プログラム構造

```
class className {
    field/static declarations
    subroutine declarations
}
```

### サブルーチン

```
constructor/function/method type name(parameters) {
    var declarations
    statements
}
```

### 文

- `let varName = expression;`
- `if (expression) { statements } else { statements }`
- `while (expression) { statements }`
- `do subroutineCall;`
- `return expression;`

## 関連プロジェクト

- **ASMBR**: Hackアセンブラ（プロジェクト6）
- **VM**: VMトランスレータ（プロジェクト7-8）
- **Jack**: Jackプログラム集（プロジェクト9）

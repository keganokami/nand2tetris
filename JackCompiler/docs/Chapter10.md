# 第10章: コンパイラI - 構文解析

## 概要

第10章では、Jack言語の**構文解析器（Syntax Analyzer）**を実装します。
構文解析器は、ソースコードを読み込み、その構造を解析して**構文木（Syntax Tree）**を生成します。

### コンパイルの全体像

```
Jack Source Code
       ↓
┌──────────────────┐
│  JackTokenizer   │  ← 字句解析（Lexical Analysis）
│  （トークナイザ）   │
└────────┬─────────┘
         ↓
    Token Stream
         ↓
┌──────────────────┐
│ CompilationEngine│  ← 構文解析（Syntax Analysis）
│ （パーサー）       │
└────────┬─────────┘
         ↓
    XML Output（構文木）
```

## 1. 字句解析（Lexical Analysis）

### JackTokenizerの役割

JackTokenizerは、ソースコードを**トークン（Token）**の列に分解します。

```java
// Jack Source Code
class Main {
    function void main() {
        var int x;
        let x = 42;
    }
}

// Token Stream
[class] [Main] [{] [function] [void] [main] [(] [)] [{] [var] [int] [x] [;] [let] [x] [=] [42] [;] [}] [}]
```

### トークンの種類

Jack言語には5種類のトークンがあります：

| 種類 | 説明 | 例 |
|------|------|-----|
| **KEYWORD** | 予約語 | `class`, `function`, `let`, `if`, `while`, ... |
| **SYMBOL** | 記号 | `{`, `}`, `(`, `)`, `+`, `-`, `*`, `/`, ... |
| **INT_CONST** | 整数定数 | `0`, `42`, `32767` |
| **STRING_CONST** | 文字列定数 | `"Hello"`, `"World"` |
| **IDENTIFIER** | 識別子 | `main`, `x`, `Point`, `myVar` |

### 予約語（21個）

```
class, constructor, function, method,
field, static, var,
int, char, boolean, void,
true, false, null, this,
let, do, if, else, while, return
```

### コメントの処理

JackTokenizerは3種類のコメントを除去します：

```java
// 行コメント - 行末まで

/* ブロックコメント
   複数行にまたがる */

/** APIドキュメンテーション
 *  Javadocスタイル */
```

### JackTokenizerの実装ポイント

```java
public class JackTokenizer {
    // 主要なメソッド
    public boolean hasMoreTokens()   // 次のトークンがあるか
    public void advance()            // 次のトークンに進む
    public TokenType tokenType()     // 現在のトークン種類
    public Keyword keyword()         // 予約語を取得
    public char symbol()             // 記号を取得
    public String identifier()       // 識別子を取得
    public int intVal()              // 整数値を取得
    public String stringVal()        // 文字列値を取得
}
```

## 2. 構文解析（Syntax Analysis）

### CompilationEngineの役割

CompilationEngineは、トークン列を解析して**構文木**を構築します。
再帰下降構文解析（Recursive Descent Parsing）を使用します。

### Jack言語の文法（BNF形式）

#### プログラム構造

```
class:          'class' className '{' classVarDec* subroutineDec* '}'

classVarDec:    ('static'|'field') type varName (',' varName)* ';'

type:           'int' | 'char' | 'boolean' | className

subroutineDec:  ('constructor'|'function'|'method') ('void'|type)
                subroutineName '(' parameterList ')' subroutineBody

parameterList:  ((type varName) (',' type varName)*)?

subroutineBody: '{' varDec* statements '}'

varDec:         'var' type varName (',' varName)* ';'
```

#### 文（Statements）

```
statements:     statement*

statement:      letStatement | ifStatement | whileStatement |
                doStatement | returnStatement

letStatement:   'let' varName ('[' expression ']')? '=' expression ';'

ifStatement:    'if' '(' expression ')' '{' statements '}'
                ('else' '{' statements '}')?

whileStatement: 'while' '(' expression ')' '{' statements '}'

doStatement:    'do' subroutineCall ';'

returnStatement: 'return' expression? ';'
```

#### 式（Expressions）

```
expression:     term (op term)*

term:           integerConstant | stringConstant | keywordConstant |
                varName | varName '[' expression ']' | subroutineCall |
                '(' expression ')' | unaryOp term

subroutineCall: subroutineName '(' expressionList ')' |
                (className|varName) '.' subroutineName '(' expressionList ')'

expressionList: (expression (',' expression)*)?

op:             '+' | '-' | '*' | '/' | '&' | '|' | '<' | '>' | '='

unaryOp:        '-' | '~'

keywordConstant: 'true' | 'false' | 'null' | 'this'
```

### 再帰下降構文解析

各文法規則に対応するメソッドを実装します：

```java
public class CompilationEngine {
    // プログラム構造
    public void compileClass()
    public void compileClassVarDec()
    public void compileSubroutine()
    public void compileParameterList()
    public void compileSubroutineBody()
    public void compileVarDec()

    // 文
    public void compileStatements()
    public void compileLet()
    public void compileIf()
    public void compileWhile()
    public void compileDo()
    public void compileReturn()

    // 式
    public void compileExpression()
    public void compileTerm()
    public int compileExpressionList()
}
```

### 例: let文の解析

```java
// Jack Code
let x = 5 + y;

// 文法規則
// letStatement: 'let' varName '=' expression ';'

public void compileLet() {
    // 'let'
    writeKeyword();

    // varName
    tokenizer.advance();
    writeIdentifier();

    // '='
    tokenizer.advance();
    writeSymbol();

    // expression
    tokenizer.advance();
    compileExpression();  // 再帰呼び出し

    // ';'
    writeSymbol();
}
```

### XML出力例

```xml
<letStatement>
  <keyword> let </keyword>
  <identifier> x </identifier>
  <symbol> = </symbol>
  <expression>
    <term>
      <integerConstant> 5 </integerConstant>
    </term>
    <symbol> + </symbol>
    <term>
      <identifier> y </identifier>
    </term>
  </expression>
  <symbol> ; </symbol>
</letStatement>
```

## 3. 先読み（Lookahead）

構文解析で重要なのが**先読み**です。現在のトークンだけでは判断できない場合、
次のトークンを見て判断します。

### 例: term の解析

`term`は以下のいずれかになりえます：
- `varName` : 変数
- `varName[expr]` : 配列アクセス
- `subroutineName(args)` : 関数呼び出し
- `className.subroutineName(args)` : メソッド呼び出し

識別子を見ただけでは判断できないので、**次のトークン**を見ます：

```java
public void compileTerm() {
    if (tokenizer.tokenType() == IDENTIFIER) {
        String next = tokenizer.peekNext();  // 先読み

        if ("[".equals(next)) {
            // 配列アクセス: arr[index]
        } else if ("(".equals(next) || ".".equals(next)) {
            // サブルーチン呼び出し
        } else {
            // 単純な変数
        }
    }
}
```

## 4. JackAnalyzerの使い方

### コンパイルと実行

```bash
cd JackCompiler
make compile

# 単一ファイル
java -cp bin JackAnalyzer ../Jack/src/Square/Main.jack

# ディレクトリ全体
java -cp bin JackAnalyzer ../Jack/src/Square/
```

### 出力ファイル

- `XxxT.xml` : トークン列（字句解析結果）
- `Xxx.xml` : 構文木（構文解析結果）

## 5. モジュール構成

```
JackCompiler/
├── src/
│   ├── JackTokenizer.java      # 字句解析器
│   ├── CompilationEngine.java  # 構文解析器（XML出力）
│   └── JackAnalyzer.java       # メインクラス
├── bin/                        # コンパイル済みクラス
├── docs/
│   ├── Chapter10.md            # この文書
│   └── Chapter11.md            # 第11章の解説
└── Makefile
```

## まとめ

第10章で実装したもの：

1. **JackTokenizer**: ソースコードをトークンに分解
2. **CompilationEngine**: トークンを構文木に変換（XML出力）
3. **JackAnalyzer**: メインプログラム

これらは第11章でVMコード生成に拡張されます。
構文解析の仕組みを理解することで、コンパイラの核心部分を学べます。

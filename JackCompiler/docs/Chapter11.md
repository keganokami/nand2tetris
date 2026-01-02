# 第11章: コンパイラII - コード生成

## 概要

第11章では、第10章で作成した構文解析器を拡張して、**VMコードを生成**するコンパイラを完成させます。

### コンパイルの全体像

```
Jack Source Code
       ↓
┌──────────────────┐
│  JackTokenizer   │  ← 字句解析
└────────┬─────────┘
         ↓
    Token Stream
         ↓
┌──────────────────────────────────────┐
│       CompilationEngineVM            │  ← 構文解析 + コード生成
│  ┌─────────────┐  ┌────────────┐    │
│  │SymbolTable  │  │ VMWriter   │    │
│  │（変数管理）  │  │（VM出力）   │    │
│  └─────────────┘  └────────────┘    │
└────────┬─────────────────────────────┘
         ↓
    VM Code (.vm)
```

## 1. シンボルテーブル（Symbol Table）

### 役割

シンボルテーブルは、プログラム内の**変数を管理**します。
各変数について以下の情報を保持します：

- **name**: 変数名
- **type**: 型（int, char, boolean, またはクラス名）
- **kind**: 種類（static, field, argument, local）
- **index**: 同じ種類内でのインデックス

### 変数の種類とVMセグメント

| Kind | スコープ | VMセグメント | 説明 |
|------|----------|-------------|------|
| **STATIC** | クラス | `static` | 静的変数（クラス全体で共有） |
| **FIELD** | クラス | `this` | フィールド（インスタンス変数） |
| **ARG** | サブルーチン | `argument` | 引数 |
| **VAR** | サブルーチン | `local` | ローカル変数 |

### 例: 変数のマッピング

```java
class Point {
    field int x, y;       // FIELD 0, FIELD 1 → this 0, this 1
    static int count;     // STATIC 0 → static 0

    method int distance(Point other) {
        // メソッドの場合、thisが暗黙の第1引数
        // ARG 0 = this, ARG 1 = other
        var int dx, dy;   // VAR 0, VAR 1 → local 0, local 1
        ...
    }
}
```

### スコープの管理

シンボルテーブルには2つのスコープがあります：

1. **クラススコープ**: static, field変数（クラス全体で有効）
2. **サブルーチンスコープ**: argument, var変数（サブルーチン内で有効）

```java
public class SymbolTable {
    private Map<String, Symbol> classScope;      // クラスレベル
    private Map<String, Symbol> subroutineScope; // サブルーチンレベル

    public void startSubroutine() {
        // 新しいサブルーチンを開始
        // サブルーチンスコープをクリア
        subroutineScope.clear();
        argCount = 0;
        varCount = 0;
    }

    public void define(String name, String type, Kind kind) {
        // 変数を定義
    }

    public Kind kindOf(String name) {
        // 変数の種類を取得（サブルーチン → クラスの順で検索）
    }
}
```

## 2. VMWriter

### 役割

VMWriterは、VMコマンドを出力します。

### 主要なメソッド

```java
public class VMWriter {
    void writePush(Segment segment, int index)  // push segment index
    void writePop(Segment segment, int index)   // pop segment index
    void writeArithmetic(Command command)       // add, sub, neg, etc.
    void writeLabel(String label)               // label LABEL
    void writeGoto(String label)                // goto LABEL
    void writeIf(String label)                  // if-goto LABEL
    void writeCall(String name, int nArgs)      // call name nArgs
    void writeFunction(String name, int nLocals) // function name nLocals
    void writeReturn()                          // return
}
```

## 3. 式のコンパイル

### 中置記法から後置記法へ

Jack（中置記法）: `a + b * c`
VM（後置記法）:   `push a`, `push b`, `push c`, `call Math.multiply 2`, `add`

```java
public void compileExpression() {
    // term (op term)*

    compileTerm();  // 最初の項

    while (isOp()) {
        char op = tokenizer.symbol();
        tokenizer.advance();
        compileTerm();  // 次の項

        // 演算子を出力（両オペランドの後）
        switch (op) {
            case '+': writer.writeArithmetic(ADD); break;
            case '-': writer.writeArithmetic(SUB); break;
            case '*': writer.writeCall("Math.multiply", 2); break;
            case '/': writer.writeCall("Math.divide", 2); break;
            // ...
        }
    }
}
```

### 算術演算のマッピング

| Jack演算子 | VMコード |
|-----------|----------|
| `+` | `add` |
| `-` | `sub` |
| `*` | `call Math.multiply 2` |
| `/` | `call Math.divide 2` |
| `&` | `and` |
| `\|` | `or` |
| `<` | `lt` |
| `>` | `gt` |
| `=` | `eq` |
| `-` (単項) | `neg` |
| `~` | `not` |

## 4. 文のコンパイル

### let文

```java
// Jack
let x = expression;

// VM
// expressionをコンパイル（結果がスタックトップに）
pop segment index  // xに対応するセグメント/インデックス
```

### 配列アクセス

```java
// Jack
let arr[i] = expression;

// VM
push arr           // 配列のベースアドレス
push i             // インデックス
add                // arr + i
// expressionをコンパイル
pop temp 0         // 値を一時保存
pop pointer 1      // that = arr + i
push temp 0
pop that 0         // arr[i] = 値
```

### if文

```java
// Jack
if (condition) {
    statements1
} else {
    statements2
}

// VM
// conditionをコンパイル
not
if-goto IF_FALSE
// statements1をコンパイル
goto IF_END
label IF_FALSE
// statements2をコンパイル
label IF_END
```

### while文

```java
// Jack
while (condition) {
    statements
}

// VM
label WHILE_EXP
// conditionをコンパイル
not
if-goto WHILE_END
// statementsをコンパイル
goto WHILE_EXP
label WHILE_END
```

## 5. サブルーチンのコンパイル

### function（静的関数）

```java
// Jack
class Main {
    function void main() {
        var int x, y;
        ...
    }
}

// VM
function Main.main 2  // 2つのローカル変数
// 関数本体
```

### method（インスタンスメソッド）

```java
// Jack
class Point {
    method int getX() {
        return x;
    }
}

// VM
function Point.getX 0
push argument 0      // thisをpush
pop pointer 0        // pointer 0 (this) を設定
// これでthis 0, this 1, ...でフィールドにアクセス可能
// 関数本体
```

### constructor

```java
// Jack
class Point {
    field int x, y;
    constructor Point new(int ax, int ay) {
        let x = ax;
        let y = ay;
        return this;
    }
}

// VM
function Point.new 0
push constant 2       // フィールド数
call Memory.alloc 1   // メモリを確保
pop pointer 0         // this = 確保したアドレス
// コンストラクタ本体
push pointer 0        // thisを返す
return
```

## 6. サブルーチン呼び出し

### 関数呼び出し

```java
// Jack
let y = Math.sqrt(x);

// VM
push x                // 引数
call Math.sqrt 1      // 呼び出し
pop y                 // 戻り値を格納
```

### メソッド呼び出し

```java
// Jack
let d = p.distance(q);

// VM
push p                // thisとしてpをpush
push q                // 引数
call Point.distance 2 // 呼び出し（引数数 = 明示的引数 + 1）
pop d
```

### 同クラス内のメソッド呼び出し

```java
// Jack (Point クラス内)
do draw();

// VM
push pointer 0        // 現在のthisをpush
call Point.draw 1
pop temp 0            // do文なので戻り値を破棄
```

## 7. 定数とキーワード

| Jack | VMコード |
|------|----------|
| 整数 n | `push constant n` |
| `true` | `push constant 0`, `not` (= -1) |
| `false` | `push constant 0` |
| `null` | `push constant 0` |
| `this` | `push pointer 0` |

### 文字列定数

```java
// Jack
let s = "Hello";

// VM
push constant 5       // 文字列長
call String.new 1     // Stringオブジェクトを作成
push constant 72      // 'H'
call String.appendChar 2
push constant 101     // 'e'
call String.appendChar 2
// ... 各文字を追加
```

## 8. 完全なコンパイル例

### Jack ソースコード

```java
class Main {
    function void main() {
        var int x;
        let x = 1 + 2 * 3;
        do Output.printInt(x);
        return;
    }
}
```

### 生成されるVMコード

```
function Main.main 1      // 1つのローカル変数
push constant 1           // 1
push constant 2           // 2
push constant 3           // 3
call Math.multiply 2      // 2 * 3 = 6
add                       // 1 + 6 = 7
pop local 0               // x = 7
push local 0              // xをpush
call Output.printInt 1    // 出力
pop temp 0                // 戻り値を破棄
push constant 0           // void関数は0を返す
return
```

## 9. JackCompilerの使い方

### コンパイルと実行

```bash
cd JackCompiler
make compile

# 単一ファイル
java -cp bin JackCompiler ../Jack/src/Square/Main.jack

# ディレクトリ全体
java -cp bin JackCompiler ../Jack/src/Square/
```

### 出力

- `Xxx.vm` : 各.jackファイルに対応するVMコード

## 10. モジュール構成

```
JackCompiler/
├── src/
│   ├── JackTokenizer.java       # 字句解析器
│   ├── SymbolTable.java         # シンボルテーブル
│   ├── VMWriter.java            # VMコード出力
│   ├── CompilationEngineVM.java # コード生成
│   └── JackCompiler.java        # メインクラス
├── bin/
├── docs/
│   ├── Chapter10.md
│   └── Chapter11.md
└── Makefile
```

## まとめ

第11章で追加実装したもの：

1. **SymbolTable**: 変数の管理（名前→型/種類/インデックスのマッピング）
2. **VMWriter**: VMコードの出力
3. **CompilationEngineVM**: 構文解析しながらVMコードを生成
4. **JackCompiler**: メインプログラム

これで、Jack言語からVM言語へのコンパイラが完成しました。
第7-8章のVMトランスレータと組み合わせることで、
Jack → VM → Hackアセンブリ → 機械語 の全行程が実現できます。

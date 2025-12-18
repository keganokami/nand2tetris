# Parser.java 解説

## 概要

`Parser`クラスは、VMコード（`.vm`ファイル）を1行ずつ読み込み、
各コマンドを解析してコマンドタイプと引数を抽出するクラスです。

```
VMファイル (.vm)
      │
      ▼
┌─────────────┐
│   Parser    │  ← コマンドを解析
└─────────────┘
      │
      ▼
コマンドタイプ + 引数
```

---

## クラス構造

### フィールド

```java
private BufferedReader reader;      // ファイル読み込み用
private String currentCommand;      // 現在処理中のコマンド
private String nextCommand;         // 次に処理するコマンド（先読み）
```

**先読み方式を採用している理由**:
- `hasMoreCommands()`で次のコマンドの存在を確認できる
- ファイル終端の判定が容易

### CommandType列挙型

```java
public enum CommandType {
    C_ARITHMETIC,  // add, sub, neg, eq, gt, lt, and, or, not
    C_PUSH,        // push segment index
    C_POP,         // pop segment index
    C_LABEL,       // label ラベル名
    C_GOTO,        // goto ラベル名
    C_IF,          // if-goto ラベル名
    C_FUNCTION,    // function 関数名 ローカル変数数
    C_RETURN,      // return
    C_CALL         // call 関数名 引数数
}
```

---

## メソッド詳細

### コンストラクタ

```java
public Parser(File file) throws IOException
```

**処理**:
1. `BufferedReader`でファイルを開く
2. `readNextCommand()`で最初のコマンドを先読み

### hasMoreCommands()

```java
public boolean hasMoreCommands()
```

**戻り値**: まだ処理するコマンドがあれば`true`

**実装**:
```java
return nextCommand != null;
```

### advance()

```java
public void advance() throws IOException
```

**処理**:
1. `nextCommand`を`currentCommand`に移動
2. 次のコマンドを`nextCommand`に読み込み

**使用例**:
```java
while (parser.hasMoreCommands()) {
    parser.advance();
    // currentCommandを処理
}
```

### readNextCommand()（private）

```java
private void readNextCommand() throws IOException
```

**処理**:
1. ファイルから1行読み込む
2. コメント（`//`以降）を除去
3. 前後の空白を除去
4. 空行ならスキップして次の行へ
5. 有効なコマンドなら`nextCommand`に保存

**コメント除去の例**:
```
入力: "push constant 7  // 定数をpush"
      ↓
出力: "push constant 7"
```

### commandType()

```java
public CommandType commandType()
```

**処理**: コマンドの最初の単語で判定

```java
String[] parts = currentCommand.split("\\s+");
String command = parts[0];  // 最初の単語

switch (command) {
    case "push":     return C_PUSH;
    case "pop":      return C_POP;
    case "label":    return C_LABEL;
    case "goto":     return C_GOTO;
    case "if-goto":  return C_IF;
    case "function": return C_FUNCTION;
    case "call":     return C_CALL;
    case "return":   return C_RETURN;
    default:         return C_ARITHMETIC;  // add, sub等
}
```

### arg1()

```java
public String arg1()
```

**戻り値**: コマンドの第1引数

**コマンドタイプ別の動作**:

| コマンドタイプ | 例 | 戻り値 |
|--------------|-----|-------|
| C_ARITHMETIC | `add` | `"add"` |
| C_PUSH | `push local 0` | `"local"` |
| C_POP | `pop argument 1` | `"argument"` |
| C_LABEL | `label LOOP` | `"LOOP"` |
| C_GOTO | `goto END` | `"END"` |
| C_IF | `if-goto LOOP` | `"LOOP"` |
| C_FUNCTION | `function Foo.bar 2` | `"Foo.bar"` |
| C_CALL | `call Math.add 2` | `"Math.add"` |
| C_RETURN | `return` | **呼び出し不可** |

### arg2()

```java
public int arg2()
```

**戻り値**: コマンドの第2引数（整数）

**呼び出し可能なコマンドタイプ**:
- `C_PUSH` - セグメントのインデックス
- `C_POP` - セグメントのインデックス
- `C_FUNCTION` - ローカル変数の数
- `C_CALL` - 引数の数

**例**:
```
"push constant 7"  → 7
"pop local 0"      → 0
"function Foo 3"   → 3
"call Bar 2"       → 2
```

---

## 処理フロー図

```
┌────────────────────────────────────────┐
│           VMファイル                    │
│  push constant 7                        │
│  push constant 8  // コメント           │
│  add                                    │
│                                         │
└────────────────────────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────┐
│         readNextCommand()               │
│  1. コメント除去                         │
│  2. 空白トリム                           │
│  3. 空行スキップ                         │
└────────────────────────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────┐
│            advance()                    │
│  nextCommand → currentCommand           │
└────────────────────────────────────────┘
                    │
        ┌──────────┴──────────┐
        ▼                     ▼
┌──────────────┐      ┌──────────────┐
│ commandType()│      │    arg1()    │
│   → C_PUSH   │      │  → "constant"│
└──────────────┘      └──────────────┘
                              │
                              ▼
                      ┌──────────────┐
                      │    arg2()    │
                      │     → 7      │
                      └──────────────┘
```

---

## 使用例

```java
Parser parser = new Parser(new File("Test.vm"));

while (parser.hasMoreCommands()) {
    parser.advance();

    switch (parser.commandType()) {
        case C_ARITHMETIC:
            String op = parser.arg1();  // "add", "sub"等
            // 算術演算を処理
            break;

        case C_PUSH:
            String segment = parser.arg1();  // "constant", "local"等
            int index = parser.arg2();       // インデックス
            // push処理
            break;

        case C_FUNCTION:
            String funcName = parser.arg1(); // "Main.main"等
            int nLocals = parser.arg2();     // ローカル変数数
            // 関数定義処理
            break;

        // ... 他のコマンドタイプ
    }
}

parser.close();
```

---

## 正規表現について

```java
currentCommand.split("\\s+")
```

- `\\s` - 空白文字（スペース、タブ等）
- `+` - 1文字以上の繰り返し

**例**:
```
"push  constant   7"  // 複数スペースがあっても
      ↓
["push", "constant", "7"]  // 正しく分割される
```

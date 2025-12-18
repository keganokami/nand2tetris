# VMTranslator.java 解説

## 概要

`VMTranslator`クラスは、VMトランスレータのメインクラスです。
コマンドライン引数を処理し、`Parser`と`CodeWriter`を連携させて
VMコードをHackアセンブリに変換します。

```
┌────────────────────────────────────────────────────────────┐
│                      VMTranslator                          │
│                                                            │
│  ┌──────────┐     コマンド解析      ┌──────────┐          │
│  │  Parser  │ ──────────────────▶ │CodeWriter│          │
│  └──────────┘                      └──────────┘          │
│       ▲                                  │                │
│       │                                  ▼                │
│   .vmファイル                        .asmファイル          │
└────────────────────────────────────────────────────────────┘
```

---

## 使用方法

### 単一ファイルの変換

```bash
java VMTranslator SimpleAdd.vm
```

**出力**: `SimpleAdd.asm`

### ディレクトリの変換

```bash
java VMTranslator FibonacciElement/
```

**出力**: `FibonacciElement/FibonacciElement.asm`

---

## mainメソッド

```java
public static void main(String[] args)
```

### 処理フロー

```
┌─────────────────┐
│ 引数チェック     │  引数が1つでなければエラー
└────────┬────────┘
         ▼
┌─────────────────┐
│ ファイル存在確認 │  存在しなければエラー
└────────┬────────┘
         ▼
    ┌────┴────┐
    ▼         ▼
 ファイル?  ディレクトリ?
    │         │
    ▼         ▼
translateFile  translateDirectory
```

### コード

```java
public static void main(String[] args) {
    // 引数チェック
    if (args.length != 1) {
        System.err.println("使用方法: java VMTranslator source.vm");
        System.exit(1);
    }

    String sourcePath = args[0];
    File source = new File(sourcePath);

    // 存在確認
    if (!source.exists()) {
        System.err.println("エラー: ファイルが見つかりません");
        System.exit(1);
    }

    // ファイル or ディレクトリで分岐
    if (source.isFile() && sourcePath.endsWith(".vm")) {
        translateFile(source);
    } else if (source.isDirectory()) {
        translateDirectory(source);
    }
}
```

---

## translateFile メソッド

単一の`.vm`ファイルを変換します。

### 処理フロー

```
┌───────────────────┐
│ 出力ファイル名生成 │  .vm → .asm
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ Parser/CodeWriter │  初期化
│     の初期化      │
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ ファイル名を設定   │  staticセグメント用
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ コマンドを順次処理 │  ← メインループ
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ 無限ループを追加   │  プログラム終了処理
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ リソースをクローズ │
└───────────────────┘
```

### メインループ

```java
while (parser.hasMoreCommands()) {
    parser.advance();
    Parser.CommandType commandType = parser.commandType();

    switch (commandType) {
        case C_ARITHMETIC:
            codeWriter.writeArithmetic(parser.arg1());
            break;

        case C_PUSH:
        case C_POP:
            codeWriter.writePushPop(commandType, parser.arg1(), parser.arg2());
            break;

        case C_LABEL:
            codeWriter.writeLabel(parser.arg1());
            break;

        case C_GOTO:
            codeWriter.writeGoto(parser.arg1());
            break;

        case C_IF:
            codeWriter.writeIf(parser.arg1());
            break;

        case C_FUNCTION:
            codeWriter.writeFunction(parser.arg1(), parser.arg2());
            break;

        case C_CALL:
            codeWriter.writeCall(parser.arg1(), parser.arg2());
            break;

        case C_RETURN:
            codeWriter.writeReturn();
            break;
    }
}
```

---

## translateDirectory メソッド

ディレクトリ内の全`.vm`ファイルを1つの`.asm`ファイルに変換します。

### 処理フロー

```
┌───────────────────┐
│ .vmファイルを検索  │  FilenameFilterを使用
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ 出力ファイル名生成 │  ディレクトリ名.asm
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ Sys.vmの存在確認   │
└─────────┬─────────┘
          ▼
    ┌─────┴─────┐
    ▼           ▼
 存在する     存在しない
    │
    ▼
┌───────────────────┐
│ブートストラップ出力│
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ 各.vmファイルを処理│  ← ループ
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ 無限ループを追加   │
└───────────────────┘
```

### .vmファイルの検索

```java
File[] vmFiles = directory.listFiles(new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
        return name.endsWith(".vm");
    }
});
```

### ブートストラップコードの出力条件

```java
// Sys.vmが存在する場合のみブートストラップを出力
boolean hasSysFile = false;
for (File vmFile : vmFiles) {
    if (vmFile.getName().equals("Sys.vm")) {
        hasSysFile = true;
        break;
    }
}
if (hasSysFile) {
    codeWriter.writeBootstrap();
}
```

**理由**:
- `Sys.vm`があるプログラムは、OSの`Sys.init`関数から開始する
- ブートストラップコードは`Sys.init`を呼び出す
- 単純なテストプログラムにはブートストラップは不要

---

## 複数ファイル処理時の注意点

### staticセグメントのスコープ

各`.vm`ファイルには独立した`static`変数がある:

```
Class1.vm:  push static 0  →  @Class1.0
Class2.vm:  push static 0  →  @Class2.0
```

`setFileName()`を各ファイルの処理前に呼び出して、
正しいシンボル名が生成されるようにする:

```java
for (File vmFile : vmFiles) {
    String fileName = vmFile.getName();
    fileName = fileName.substring(0, fileName.length() - 3);
    codeWriter.setFileName(fileName);  // ← ここで設定

    // ファイルを処理...
}
```

### 関数名のスコープ

関数名はグローバルで一意:
- `Class1.foo`
- `Class2.foo`
- `Main.main`

ファイルを跨いで`call`できる。

---

## 出力ファイルの構造

### 単一ファイル (例: BasicLoop.vm)

```asm
// push constant 0
@0
D=A
...

// label LOOP_START
($LOOP_START)
...

// 無限ループ（プログラム終了）
(END)
@END
0;JMP
```

### ディレクトリ (例: FibonacciElement/)

```asm
// Bootstrap code
@256
D=A
@SP
M=D
// call Sys.init 0
...

// === Main.vm ===
// function Main.fibonacci 0
(Main.fibonacci)
...

// === Sys.vm ===
// function Sys.init 0
(Sys.init)
...

// 無限ループ（プログラム終了）
(END)
@END
0;JMP
```

---

## エラーハンドリング

```java
try {
    if (source.isFile()) {
        translateFile(source);
    } else if (source.isDirectory()) {
        translateDirectory(source);
    }
} catch (IOException e) {
    System.err.println("エラー: " + e.getMessage());
    e.printStackTrace();
    System.exit(1);
}
```

---

## 実行例

### 入力: SimpleAdd.vm

```
push constant 7
push constant 8
add
```

### 出力: SimpleAdd.asm

```asm
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
// 無限ループ（プログラム終了）
(END)
@END
0;JMP
```

---

## クラス間の関係

```
┌─────────────────────────────────────────────────────────┐
│                     VMTranslator                        │
│                                                         │
│  mainメソッド                                            │
│    │                                                    │
│    ├──▶ translateFile()                                │
│    │      │                                            │
│    │      ├──▶ Parser (ファイル読み込み、コマンド解析)   │
│    │      │                                            │
│    │      └──▶ CodeWriter (アセンブリ生成、ファイル出力) │
│    │                                                    │
│    └──▶ translateDirectory()                           │
│           │                                            │
│           ├──▶ CodeWriter.writeBootstrap() [条件付き]   │
│           │                                            │
│           └──▶ 各.vmファイルに対して:                   │
│                 ├──▶ Parser                            │
│                 └──▶ CodeWriter                        │
└─────────────────────────────────────────────────────────┘
```

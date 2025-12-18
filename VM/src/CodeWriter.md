# CodeWriter.java 解説

## 概要

`CodeWriter`クラスは、VMコマンドを受け取り、
対応するHackアセンブリコードを生成して`.asm`ファイルに書き込むクラスです。

```
VMコマンド
    │
    ▼
┌─────────────┐
│ CodeWriter  │  ← アセンブリに変換
└─────────────┘
    │
    ▼
Hackアセンブリ (.asm)
```

---

## クラス構造

### フィールド

```java
private PrintWriter writer;           // 出力ファイル書き込み用
private String currentFileName;       // 現在処理中のファイル名（static変数用）
private String currentFunctionName;   // 現在の関数名（ラベルスコープ用）
private int labelCounter;             // 比較演算用ラベルカウンタ
private int returnCounter;            // call用戻りラベルカウンタ
```

---

## Hackコンピュータのメモリマップ

```
RAM アドレス    用途
─────────────────────────────────
  0          SP   (Stack Pointer)
  1          LCL  (Local)
  2          ARG  (Argument)
  3          THIS
  4          THAT
  5-12       Temp セグメント
 13-15       汎用レジスタ (R13-R15)
 16-255      Static セグメント
 256-2047    Stack 領域
```

---

## プロジェクト7: スタック演算とメモリアクセス

### 算術/論理演算 (writeArithmetic)

#### 二項演算 (add, sub, and, or)

```
スタック: [x, y] (yがトップ)
    ↓
[x op y]
```

**生成されるアセンブリ（add の場合）**:
```asm
@SP
AM=M-1      // SP-- して A=SP (yの位置)
D=M         // D = y
A=A-1       // A = SP-1 (xの位置)
M=D+M       // x = x + y (結果を上書き)
```

#### 単項演算 (neg, not)

```
スタック: [x]
    ↓
[op x]
```

**生成されるアセンブリ（neg の場合）**:
```asm
@SP
A=M-1       // A = スタックトップの位置
M=-M        // 値を反転
```

#### 比較演算 (eq, gt, lt)

```
スタック: [x, y]
    ↓
[-1 (true) または 0 (false)]
```

**生成されるアセンブリ（eq の場合）**:
```asm
@SP
AM=M-1      // SP--
D=M         // D = y
A=A-1       // xの位置
D=M-D       // D = x - y
@TRUE_0
D;JEQ       // x == y ならジャンプ
@SP
A=M-1
M=0         // false (0)
@END_0
0;JMP
(TRUE_0)
@SP
A=M-1
M=-1        // true (-1)
(END_0)
```

### Push操作 (writePush)

#### push constant n

```asm
@n
D=A         // D = 定数値
@SP
A=M
M=D         // *SP = D
@SP
M=M+1       // SP++
```

#### push local/argument/this/that n

```asm
@LCL        // (または ARG, THIS, THAT)
D=M         // D = ベースアドレス
@n
A=D+A       // A = base + index
D=M         // D = *(base + index)
@SP
A=M
M=D         // *SP = D
@SP
M=M+1       // SP++
```

#### push temp n

```asm
@(5+n)      // temp は RAM[5-12]
D=M
// ... pushの共通処理
```

#### push pointer 0/1

```asm
@3          // pointer 0 = THIS (RAM[3])
// または
@4          // pointer 1 = THAT (RAM[4])
D=M
// ... pushの共通処理
```

#### push static n

```asm
@ファイル名.n   // 例: Main.0
D=M
// ... pushの共通処理
```

### Pop操作 (writePop)

#### pop local/argument/this/that n

```asm
@LCL
D=M         // D = ベースアドレス
@n
D=D+A       // D = base + index
@R13
M=D         // R13 = 保存先アドレス

@SP
AM=M-1      // SP--
D=M         // D = スタックトップの値

@R13
A=M
M=D         // *(base + index) = D
```

**R13を使う理由**:
- popでは保存先アドレスとスタックの値の両方が必要
- レジスタが足りないため、R13に一時保存

---

## プロジェクト8: プログラムフロー

### label (writeLabel)

```
label LOOP
    ↓
(関数名$LOOP)
```

**関数スコープの理由**:
- 異なる関数で同じラベル名を使えるようにする
- `Foo.bar$LOOP` と `Baz.qux$LOOP` は別のラベル

### goto (writeGoto)

```
goto LOOP
    ↓
@関数名$LOOP
0;JMP
```

### if-goto (writeIf)

```
if-goto LOOP
```

**動作**:
1. スタックトップをpop
2. 値が0以外ならラベルへジャンプ

```asm
@SP
AM=M-1      // SP--
D=M         // D = 条件値
@関数名$LOOP
D;JNE       // D != 0 ならジャンプ
```

---

## プロジェクト8: 関数呼び出し

### 関数呼び出し時のスタック構造

```
呼び出し前:          呼び出し後:
┌─────────┐         ┌─────────┐
│  arg 0  │         │  arg 0  │ ← ARG
│  arg 1  │         │  arg 1  │
└─────────┘ ← SP    ├─────────┤
                    │ret addr │ ← 戻りアドレス
                    │saved LCL│
                    │saved ARG│
                    │saved THIS│
                    │saved THAT│
                    ├─────────┤
                    │ local 0 │ ← LCL
                    │ local 1 │
                    └─────────┘ ← SP
```

### function (writeFunction)

```
function Foo.bar 2
```

**処理**:
1. 関数ラベルを宣言
2. ローカル変数を0で初期化

```asm
(Foo.bar)           // 関数エントリポイント
@SP                 // local 0 = 0
A=M
M=0
@SP
M=M+1
@SP                 // local 1 = 0
A=M
M=0
@SP
M=M+1
```

### call (writeCall)

```
call Foo.bar 2
```

**処理**:
1. return-address をpush
2. LCL をpush
3. ARG をpush
4. THIS をpush
5. THAT をpush
6. ARG = SP - 5 - nArgs
7. LCL = SP
8. 関数へジャンプ
9. return-addressラベルを配置

```asm
// 1. return-addressをpush
@RETURN_0
D=A
@SP
A=M
M=D
@SP
M=M+1

// 2-5. LCL, ARG, THIS, THATをpush（省略）

// 6. ARG = SP - 5 - nArgs
@SP
D=M
@7              // 5 + 2(nArgs)
D=D-A
@ARG
M=D

// 7. LCL = SP
@SP
D=M
@LCL
M=D

// 8. 関数へジャンプ
@Foo.bar
0;JMP

// 9. return-addressラベル
(RETURN_0)
```

### return (writeReturn)

**処理**:
1. endFrame = LCL (R13に保存)
2. retAddr = *(endFrame - 5) (R14に保存)
3. *ARG = pop() (戻り値を設定)
4. SP = ARG + 1
5. THAT = *(endFrame - 1)
6. THIS = *(endFrame - 2)
7. ARG = *(endFrame - 3)
8. LCL = *(endFrame - 4)
9. retAddrへジャンプ

```asm
// 1. endFrame = LCL
@LCL
D=M
@R13
M=D

// 2. retAddr = *(endFrame - 5)
@5
A=D-A
D=M
@R14
M=D

// 3. *ARG = pop()
@SP
AM=M-1
D=M
@ARG
A=M
M=D

// 4. SP = ARG + 1
@ARG
D=M+1
@SP
M=D

// 5-8. THAT, THIS, ARG, LCL を復元
@R13
AM=M-1
D=M
@THAT
M=D
// ... 同様にTHIS, ARG, LCLも復元

// 9. retAddrへジャンプ
@R14
A=M
0;JMP
```

---

## ブートストラップコード (writeBootstrap)

**VMプログラム起動時の初期化処理**:

```asm
// SP = 256
@256
D=A
@SP
M=D

// call Sys.init
// ... (callと同じ処理)
```

---

## メソッド一覧

| メソッド | 機能 | プロジェクト |
|---------|------|-------------|
| `setFileName(String)` | ファイル名設定 | 7 |
| `writeArithmetic(String)` | 算術演算 | 7 |
| `writePushPop(...)` | push/pop | 7 |
| `writeLabel(String)` | ラベル定義 | 8 |
| `writeGoto(String)` | 無条件ジャンプ | 8 |
| `writeIf(String)` | 条件付きジャンプ | 8 |
| `writeFunction(String, int)` | 関数定義 | 8 |
| `writeCall(String, int)` | 関数呼び出し | 8 |
| `writeReturn()` | 関数復帰 | 8 |
| `writeBootstrap()` | 初期化コード | 8 |
| `writeInfiniteLoop()` | 終了処理 | 7 |

---

## ヘルパーメソッド

### pushD()

Dレジスタの値をスタックにpushする共通処理:

```java
private void pushD() {
    writer.println("@SP");
    writer.println("A=M");
    writer.println("M=D");
    writer.println("@SP");
    writer.println("M=M+1");
}
```

### writeBinaryOperation(String operation)

二項演算の共通処理:

```java
private void writeBinaryOperation(String operation) {
    writer.println("@SP");
    writer.println("AM=M-1");
    writer.println("D=M");
    writer.println("A=A-1");
    writer.println("M=" + operation);  // "D+M", "M-D"等
}
```

---

## 一意なラベル生成

```java
private int labelCounter = 0;
private int returnCounter = 0;

// 比較演算用 (eq, gt, lt)
String trueLabel = "TRUE_" + labelCounter;    // TRUE_0, TRUE_1, ...
labelCounter++;

// call用
String returnLabel = "RETURN_" + returnCounter;  // RETURN_0, RETURN_1, ...
returnCounter++;
```

これにより、同じコマンドを複数回使ってもラベルが衝突しない。

# プロジェクト8: VMトランスレータ（完全版）

## 概要

プロジェクト8では、プロジェクト7で作成したVMトランスレータに以下の機能を追加します：

1. **プログラムフロー制御** - `label`, `goto`, `if-goto`
2. **関数呼び出し** - `function`, `call`, `return`
3. **ブートストラップコード** - VM初期化処理

これらの機能により、VMトランスレータは完全なVM言語をサポートし、
高水準言語（Jackなど）からコンパイルされたプログラムを実行できるようになります。

---

## 1. プログラムフロー制御

### 1.1 label コマンド

```
label LOOP
```

**目的**: プログラム内の特定位置にラベル（名前）を付ける

**生成されるアセンブリコード**:
```asm
// label LOOP
(関数名$LOOP)
```

**ポイント**:
- ラベルは関数スコープを持つため、`関数名$ラベル名` の形式で生成
- これにより異なる関数で同じラベル名を使っても衝突しない

### 1.2 goto コマンド

```
goto LOOP
```

**目的**: 指定したラベルに無条件でジャンプ

**生成されるアセンブリコード**:
```asm
// goto LOOP
@関数名$LOOP
0;JMP
```

**動作の流れ**:
```
[goto LOOP 実行]
     |
     v
(関数名$LOOP) ラベルの位置へジャンプ
```

### 1.3 if-goto コマンド

```
if-goto LOOP
```

**目的**: スタックトップの値が `true`（0以外）ならジャンプ

**生成されるアセンブリコード**:
```asm
// if-goto LOOP
@SP
AM=M-1      // SP-- して、その位置に移動
D=M         // D = スタックトップの値
@関数名$LOOP
D;JNE       // D != 0 ならジャンプ
```

**動作の流れ**:
```
スタック: [..., 条件値]
              ↓
[条件値をpop]
              ↓
条件値 != 0 ? → Yes → ラベルへジャンプ
              ↓ No
         次の命令へ
```

---

## 2. 関数呼び出し

VMの関数呼び出しは、呼び出し元の状態を保存し、
呼び出し先関数を実行後、元の状態を復元するメカニズムです。

### 2.1 コールスタックの構造

関数呼び出し時、スタックは以下の構造になります：

```
      ┌─────────────────┐
      │    argument 0   │ ← ARG
      │    argument 1   │
      │       ...       │
      ├─────────────────┤
      │  return address │ ← 呼び出し元に戻るアドレス
      │   saved LCL     │
      │   saved ARG     │
      │   saved THIS    │
      │   saved THAT    │
      ├─────────────────┤
      │    local 0      │ ← LCL
      │    local 1      │
      │       ...       │
      ├─────────────────┤
      │  作業用スタック  │
      │       ...       │ ← SP
      └─────────────────┘
```

### 2.2 function コマンド

```
function SimpleFunction.test 2
```

**目的**: 関数の開始点を定義し、ローカル変数を初期化

**引数**:
- `SimpleFunction.test` - 関数名
- `2` - ローカル変数の数

**生成されるアセンブリコード**:
```asm
// function SimpleFunction.test 2
(SimpleFunction.test)   // 関数エントリポイント
@SP                     // local 0 = 0
A=M
M=0
@SP
M=M+1
@SP                     // local 1 = 0
A=M
M=0
@SP
M=M+1
```

### 2.3 call コマンド

```
call Mult.mult 2
```

**目的**: 関数を呼び出す（引数は既にスタックにpush済み）

**引数**:
- `Mult.mult` - 呼び出す関数名
- `2` - 引数の数

**処理の流れ**:

```
[call 実行前のスタック]        [call 実行後のスタック]
┌─────────────┐               ┌─────────────┐
│     ...     │               │     ...     │
├─────────────┤               ├─────────────┤
│    arg 0    │               │    arg 0    │ ← 新ARG
│    arg 1    │               │    arg 1    │
└─────────────┘ ← SP          ├─────────────┤
                              │return addr  │
                              │ saved LCL   │
                              │ saved ARG   │
                              │ saved THIS  │
                              │ saved THAT  │
                              └─────────────┘ ← SP, 新LCL
```

**生成されるアセンブリコード**（簡略版）:
```asm
// call Mult.mult 2
@RETURN_0           // 1. return-addressをpush
D=A
[push D]
@LCL                // 2. LCLをpush
D=M
[push D]
@ARG                // 3. ARGをpush
D=M
[push D]
@THIS               // 4. THISをpush
D=M
[push D]
@THAT               // 5. THATをpush
D=M
[push D]
@SP                 // 6. ARG = SP - 5 - nArgs
D=M
@7                  //    (5 + 2 = 7)
D=D-A
@ARG
M=D
@SP                 // 7. LCL = SP
D=M
@LCL
M=D
@Mult.mult          // 8. 関数へジャンプ
0;JMP
(RETURN_0)          // 9. 戻りアドレスラベル
```

### 2.4 return コマンド

```
return
```

**目的**: 関数から呼び出し元に戻る

**処理の流れ**:

```
[return 実行前]                [return 実行後]
┌─────────────┐               ┌─────────────┐
│    arg 0    │ ← ARG         │  戻り値     │
│    arg 1    │               └─────────────┘ ← SP
├─────────────┤
│return addr  │               LCL, ARG, THIS, THAT
│ saved LCL   │               は呼び出し元の値に復元
│ saved ARG   │
│ saved THIS  │               return addressへジャンプ
│ saved THAT  │
├─────────────┤
│   local 0   │ ← LCL
│   local 1   │
├─────────────┤
│   戻り値    │
└─────────────┘ ← SP
```

**生成されるアセンブリコード**（簡略版）:
```asm
// return
@LCL                // 1. endFrame = LCL
D=M
@R13                //    R13 = endFrame
M=D
@5                  // 2. retAddr = *(endFrame - 5)
A=D-A
D=M
@R14                //    R14 = retAddr
M=D
@SP                 // 3. *ARG = pop() (戻り値を設定)
AM=M-1
D=M
@ARG
A=M
M=D
@ARG                // 4. SP = ARG + 1
D=M+1
@SP
M=D
@R13                // 5. THAT = *(endFrame - 1)
AM=M-1
D=M
@THAT
M=D
// ... (THIS, ARG, LCL も同様に復元)
@R14                // 9. 戻りアドレスへジャンプ
A=M
0;JMP
```

---

## 3. ブートストラップコード

VMプログラムの起動時に最初に実行されるコードです。

### 3.1 ブートストラップの役割

1. **スタックポインタの初期化**: SP = 256
2. **Sys.init の呼び出し**: OSの初期化関数を呼び出す

### 3.2 生成されるアセンブリコード

```asm
// Bootstrap code
@256            // SP = 256
D=A
@SP
M=D
// call Sys.init 0
@RETURN_0       // return-addressをpush
D=A
[push D]
// ... (call の残りの処理)
@Sys.init
0;JMP
(RETURN_0)
```

### 3.3 Sys.init の役割

`Sys.init` は通常、以下を行います：
1. OSの各種モジュールを初期化
2. `Main.main` 関数を呼び出す
3. プログラム終了後は無限ループ

---

## 4. メモリ使用の詳細

### 4.1 汎用レジスタの使用

| レジスタ | 用途 |
|---------|------|
| R13 | return時のendFrame一時保存 |
| R14 | return時のretAddr一時保存 |
| R15 | （予備） |

### 4.2 スタックの初期状態

```
RAM[0]   SP    = 256
RAM[1]   LCL   = 未定義（関数呼び出し時に設定）
RAM[2]   ARG   = 未定義（関数呼び出し時に設定）
RAM[3]   THIS  = 未定義
RAM[4]   THAT  = 未定義
RAM[5-12]      = temp セグメント
RAM[13-15]     = 汎用レジスタ
RAM[16-255]    = static 変数
RAM[256-]      = スタック領域
```

---

## 5. 実行例：フィボナッチ計算

### 5.1 VM コード（抜粋）

```
// Fibonacci(n) を計算
function Main.fibonacci 0
  push argument 0
  push constant 2
  lt
  if-goto IF_TRUE
  goto IF_FALSE
label IF_TRUE
  push argument 0
  return
label IF_FALSE
  push argument 0
  push constant 2
  sub
  call Main.fibonacci 1
  push argument 0
  push constant 1
  sub
  call Main.fibonacci 1
  add
  return
```

### 5.2 実行の流れ（n=3 の場合）

```
fibonacci(3)
├─ fibonacci(1) → return 1
└─ fibonacci(2)
   ├─ fibonacci(0) → return 0
   └─ fibonacci(1) → return 1
   └─ return 0 + 1 = 1
└─ return 1 + 1 = 2
```

---

## 6. ファイル構成

```
VM/
├── src/
│   ├── VMTranslator.java  # メインクラス
│   ├── Parser.java        # VMコード解析
│   └── CodeWriter.java    # アセンブリ生成
├── bin/                   # コンパイル済みクラス
├── tasks/                 # テストファイル
└── PROJECT8.md            # この解説
```

---

## 7. 使用方法

### 7.1 コンパイル

```bash
cd VM
javac -d bin src/*.java
```

### 7.2 実行

```bash
# 単一ファイル
java -cp bin VMTranslator tasks/ProgramFlow/BasicLoop/BasicLoop.vm

# ディレクトリ（複数ファイル）
java -cp bin VMTranslator tasks/FunctionCalls/FibonacciElement
```

### 7.3 出力

- 単一ファイル: `ファイル名.asm`
- ディレクトリ: `ディレクトリ名/ディレクトリ名.asm`

---

## 8. 参考：主要メソッド一覧

### CodeWriter クラス

| メソッド | 機能 |
|---------|------|
| `writeLabel(label)` | ラベル定義 |
| `writeGoto(label)` | 無条件ジャンプ |
| `writeIf(label)` | 条件付きジャンプ |
| `writeFunction(name, nLocals)` | 関数定義 |
| `writeCall(name, nArgs)` | 関数呼び出し |
| `writeReturn()` | 関数からの復帰 |
| `writeBootstrap()` | 初期化コード |

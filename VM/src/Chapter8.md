# Nand2Tetris 第8章: 仮想マシン II - プログラム制御

## 概要

第8章では、第7章で作成したVMトランスレータを拡張し、以下の機能を追加します：

1. **プログラムフロー制御** - 分岐とループ
2. **関数呼び出し** - サブルーチンの実装
3. **ブートストラップ** - プログラムの起動処理

これにより、VMトランスレータは完全なVM言語をサポートし、
高水準言語からコンパイルされた実用的なプログラムを実行できるようになります。

---

## 1. プログラムフロー制御

### 1.1 なぜプログラムフローが必要か

第7章のVMは命令を順番に実行するだけでした。
しかし、実用的なプログラムには以下が必要です：

- **条件分岐** - if文の実装
- **ループ** - while/for文の実装

```
高水準言語:                    VMコード:
─────────────                  ─────────────
if (x > 0) {                   push local 0
    y = 1;                     push constant 0
}                              gt
                               if-goto IF_TRUE
                               goto IF_END
                             label IF_TRUE
                               push constant 1
                               pop local 1
                             label IF_END
```

### 1.2 label コマンド

**構文**: `label ラベル名`

**目的**: プログラム内の特定位置に名前を付ける

```
VMコード:              アセンブリ:
──────────             ──────────
label LOOP             (関数名$LOOP)
```

**ラベルのスコープ**:

VMのラベルは関数スコープを持ちます。つまり、同じラベル名でも
異なる関数内では別のラベルとして扱われます。

```
function Foo.bar 0         function Baz.qux 0
  label LOOP                 label LOOP
  ...                        ...
  goto LOOP                  goto LOOP

↓ アセンブリ変換後

(Foo.bar)                  (Baz.qux)
(Foo.bar$LOOP)  ← 別々の  (Baz.qux$LOOP)
@Foo.bar$LOOP      ラベル  @Baz.qux$LOOP
0;JMP                      0;JMP
```

### 1.3 goto コマンド

**構文**: `goto ラベル名`

**目的**: 無条件でラベル位置にジャンプ

```
VMコード:              アセンブリ:
──────────             ──────────
goto LOOP              @関数名$LOOP
                       0;JMP
```

**使用例（無限ループ）**:
```
label LOOP
  // 処理
  goto LOOP    ← 無条件でLOOPに戻る
```

### 1.4 if-goto コマンド

**構文**: `if-goto ラベル名`

**目的**: スタックトップの値が `true`（0以外）ならジャンプ

```
スタック: [..., 条件値]
              │
              ▼
         条件値をpop
              │
     ┌────────┴────────┐
     ▼                 ▼
 0以外(true)         0(false)
     │                 │
     ▼                 ▼
 ラベルへ          次の命令へ
 ジャンプ
```

```
VMコード:              アセンブリ:
──────────             ──────────
if-goto LOOP           @SP
                       AM=M-1      // pop
                       D=M         // D = 条件値
                       @関数名$LOOP
                       D;JNE       // D≠0ならジャンプ
```

**使用例（whileループ）**:
```
// while (i < 10) { ... i++ }

label WHILE_START
  push local 0        // i
  push constant 10
  lt                  // i < 10 ?
  not                 // ループ終了条件に反転
  if-goto WHILE_END   // 条件が偽ならループ終了

  // ループ本体
  ...

  // i++
  push local 0
  push constant 1
  add
  pop local 0

  goto WHILE_START    // ループの先頭へ

label WHILE_END
```

---

## 2. 関数呼び出し

### 2.1 関数とは

関数（サブルーチン）は、再利用可能なコードブロックです。

```
┌─────────────────────────────────────────┐
│ 呼び出し元 (Caller)                      │
│                                         │
│   push argument 0    // 引数を準備       │
│   push argument 1                       │
│   call Foo.bar 2     // 関数呼び出し     │
│   // ここに戻ってくる                    │
│   // スタックトップに戻り値がある         │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│ 呼び出し先 (Callee) - Foo.bar           │
│                                         │
│   function Foo.bar 2  // 関数定義       │
│   // ローカル変数2個                     │
│   ...                                   │
│   push local 0        // 戻り値を準備    │
│   return              // 呼び出し元に戻る │
└─────────────────────────────────────────┘
```

### 2.2 コールスタック

関数呼び出し時、スタックには以下の情報が積まれます：

```
メモリ
  │
  ▼
┌─────────────────┐
│   argument 0    │ ← ARG (引数の先頭)
│   argument 1    │
│      ...        │
├─────────────────┤
│  return address │   呼び出し元に戻るアドレス
│   saved LCL     │   呼び出し元のLCL
│   saved ARG     │   呼び出し元のARG
│   saved THIS    │   呼び出し元のTHIS
│   saved THAT    │   呼び出し元のTHAT
├─────────────────┤
│    local 0      │ ← LCL (ローカル変数の先頭)
│    local 1      │
│      ...        │
├─────────────────┤
│  作業用スタック  │
│      ...        │ ← SP
└─────────────────┘
```

この構造を **フレーム (Frame)** と呼びます。
関数が呼び出されるたびに新しいフレームがスタックに積まれます。

### 2.3 function コマンド

**構文**: `function 関数名 ローカル変数数`

**目的**: 関数のエントリポイントを定義し、ローカル変数を初期化

```
VMコード:                      アセンブリ:
──────────                     ──────────
function Foo.bar 2             (Foo.bar)        // ラベル
                               // local 0 = 0
                               @SP
                               A=M
                               M=0
                               @SP
                               M=M+1
                               // local 1 = 0
                               @SP
                               A=M
                               M=0
                               @SP
                               M=M+1
```

**ローカル変数の初期化**:
- ローカル変数は全て0で初期化される
- これはスタックに0をpushすることで実現

### 2.4 call コマンド

**構文**: `call 関数名 引数数`

**目的**: 関数を呼び出す

**前提**: 引数は既にスタックにpushされている

```
call前のスタック:        call後のスタック:
┌─────────┐             ┌─────────┐
│  arg 0  │             │  arg 0  │ ← 新ARG
│  arg 1  │             │  arg 1  │
└─────────┘ ← SP        ├─────────┤
                        │ret addr │
                        │saved LCL│
                        │saved ARG│
                        │saved THIS│
                        │saved THAT│
                        └─────────┘ ← 新LCL, SP
```

**処理手順**:

```
1. return-address をpush     // 戻り先アドレス
2. LCL をpush                // 呼び出し元のLCLを保存
3. ARG をpush                // 呼び出し元のARGを保存
4. THIS をpush               // 呼び出し元のTHISを保存
5. THAT をpush               // 呼び出し元のTHATを保存
6. ARG = SP - 5 - nArgs      // 新しいARGを設定
7. LCL = SP                  // 新しいLCLを設定
8. goto 関数名               // 関数へジャンプ
9. (return-address)          // ここに戻ってくる
```

**ARG計算の図解**:

```
         SP (call実行前)
          │
          ▼
┌────┬────┬────┬────┬────┬────┬────┐
│arg0│arg1│ ret│LCL │ARG │THIS│THAT│
└────┴────┴────┴────┴────┴────┴────┘
  ▲                                ▲
  │                                │
新ARG = SP - 5 - 2              新LCL = SP
      = SP - 7
```

### 2.5 return コマンド

**構文**: `return`

**目的**: 関数から呼び出し元に戻る

**前提**: スタックトップに戻り値がある

```
return前:                    return後:
┌─────────────┐             ┌─────────────┐
│    arg 0    │ ← ARG       │   戻り値    │
│    arg 1    │             └─────────────┘ ← SP
├─────────────┤
│  ret addr   │             LCL, ARG, THIS, THAT
│  saved LCL  │             は呼び出し元の値に復元
│  saved ARG  │
│  saved THIS │
│  saved THAT │
├─────────────┤
│   local 0   │ ← LCL
│   local 1   │
├─────────────┤
│   戻り値    │
└─────────────┘ ← SP
```

**処理手順**:

```
1. endFrame = LCL            // フレームの終端を保存 (R13)
2. retAddr = *(endFrame-5)   // 戻りアドレスを取得 (R14)
3. *ARG = pop()              // 戻り値をARG[0]に格納
4. SP = ARG + 1              // SPを戻り値の次に設定
5. THAT = *(endFrame-1)      // THATを復元
6. THIS = *(endFrame-2)      // THISを復元
7. ARG = *(endFrame-3)       // ARGを復元
8. LCL = *(endFrame-4)       // LCLを復元
9. goto retAddr              // 呼び出し元へジャンプ
```

**なぜ戻り値をARG[0]に置くか**:
- 呼び出し元から見ると、引数をpushした位置に結果が返る
- SPが正しい位置（引数の次）に戻る

### 2.6 再帰呼び出し

関数は自分自身を呼び出せます（再帰）。
各呼び出しで新しいフレームが作られるため、
ローカル変数と引数は独立しています。

```
fibonacci(3)の呼び出し:

┌─────────────────┐
│ fibonacci(3)    │
│   arg0 = 3      │
├─────────────────┤
│ fibonacci(1)    │  ← 再帰呼び出し1
│   arg0 = 1      │
├─────────────────┤
│ fibonacci(2)    │  ← 再帰呼び出し2
│   arg0 = 2      │
├─────────────────┤
│ fibonacci(0)    │  ← 再帰呼び出し3
│   arg0 = 0      │
├─────────────────┤
│ fibonacci(1)    │  ← 再帰呼び出し4
│   arg0 = 1      │
└─────────────────┘
```

---

## 3. ブートストラップコード

### 3.1 ブートストラップとは

VMプログラムが起動する際に、最初に実行される初期化コードです。

**役割**:
1. スタックポインタを初期化（SP = 256）
2. `Sys.init`関数を呼び出す

```asm
// ブートストラップコード
@256
D=A
@SP
M=D          // SP = 256

// call Sys.init 0
@RETURN_0
D=A
// ... (call処理)
@Sys.init
0;JMP
(RETURN_0)
```

### 3.2 Sys.init関数

`Sys.init`はOSの初期化関数です。通常、以下を行います：

1. 各種OSモジュールを初期化
2. `Main.main`関数を呼び出す
3. プログラム終了後は無限ループ

```
function Sys.init 0
  // OSの初期化（省略）
  call Main.main 0    // ユーザープログラムを呼び出し
label END
  goto END            // 無限ループ
```

### 3.3 ブートストラップの出力条件

VMトランスレータは、`Sys.vm`ファイルが存在する場合のみ
ブートストラップコードを出力します。

```
ディレクトリ内に Sys.vm がある
         ↓
ブートストラップを出力 → call Sys.init → Main.main実行

Sys.vm がない（単純なテスト）
         ↓
ブートストラップなし → コードをそのまま実行
```

---

## 4. 実装の詳細

### 4.1 使用するレジスタ

| レジスタ | 用途 |
|---------|------|
| R13 | return時のendFrame一時保存 |
| R14 | return時のretAddr一時保存 |
| R15 | （予備） |

### 4.2 ラベルの命名規則

| 用途 | 形式 | 例 |
|------|------|-----|
| 関数ラベル | `関数名` | `Main.fibonacci` |
| VMラベル | `関数名$ラベル名` | `Main.fibonacci$LOOP` |
| 比較演算 | `TRUE_n`, `END_n` | `TRUE_0`, `END_0` |
| call戻り | `RETURN_n` | `RETURN_0` |

---

## 5. テストプログラム

### 5.1 BasicLoop

1から引数[0]までの合計を計算するループ。

```
// sum = 1 + 2 + ... + n

push constant 0
pop local 0           // sum = 0

label LOOP
  push argument 0     // n
  push local 0        // sum
  add
  pop local 0         // sum = sum + n

  push argument 0
  push constant 1
  sub
  pop argument 0      // n = n - 1

  push argument 0
  if-goto LOOP        // n > 0 ならループ

push local 0          // 結果をスタックに
```

### 5.2 FibonacciSeries

フィボナッチ数列をメモリに書き込む。

```
// that[0] = 0, that[1] = 1
// that[i] = that[i-1] + that[i-2] for i >= 2
```

### 5.3 SimpleFunction

単純な関数呼び出しのテスト。

```
function SimpleFunction.test 2
  push local 0
  push local 1
  add
  not
  push argument 0
  add
  push argument 1
  sub
  return
```

### 5.4 FibonacciElement

再帰を使ってフィボナッチ数を計算。

```
// fib(n) = n                    if n < 2
//        = fib(n-1) + fib(n-2)  otherwise

function Main.fibonacci 0
  push argument 0
  push constant 2
  lt
  if-goto BASE_CASE

  // 再帰ケース
  push argument 0
  push constant 1
  sub
  call Main.fibonacci 1    // fib(n-1)

  push argument 0
  push constant 2
  sub
  call Main.fibonacci 1    // fib(n-2)

  add                      // fib(n-1) + fib(n-2)
  return

label BASE_CASE
  push argument 0
  return
```

### 5.5 NestedCall

ネストした関数呼び出しのテスト。

### 5.6 StaticsTest

複数クラス間でのstatic変数のテスト。

---

## 6. VMから高水準言語への対応

### 6.1 if文

```java
if (x > 0) {
    y = 1;
}
```

```
push local 0        // x
push constant 0
gt                  // x > 0 ?
not
if-goto IF_END
  push constant 1
  pop local 1       // y = 1
label IF_END
```

### 6.2 while文

```java
while (i < 10) {
    sum = sum + i;
    i = i + 1;
}
```

```
label WHILE_START
  push local 0      // i
  push constant 10
  lt                // i < 10 ?
  not
  if-goto WHILE_END

  // sum = sum + i
  push local 1
  push local 0
  add
  pop local 1

  // i = i + 1
  push local 0
  push constant 1
  add
  pop local 0

  goto WHILE_START
label WHILE_END
```

### 6.3 関数呼び出し

```java
int result = Math.multiply(3, 4);
```

```
push constant 3     // 引数1
push constant 4     // 引数2
call Math.multiply 2
pop local 0         // result = 戻り値
```

---

## 7. まとめ

第8章で追加した機能:

| 機能 | コマンド | 用途 |
|------|---------|------|
| ラベル | `label` | 分岐先の定義 |
| 無条件ジャンプ | `goto` | ループ、無条件分岐 |
| 条件付きジャンプ | `if-goto` | 条件分岐、ループ |
| 関数定義 | `function` | サブルーチンの開始 |
| 関数呼び出し | `call` | サブルーチンの呼び出し |
| 関数復帰 | `return` | 呼び出し元への戻り |

これにより、VMトランスレータは完全なVM言語をサポートし、
第9章以降で作成するJackコンパイラの出力を実行できるようになります。

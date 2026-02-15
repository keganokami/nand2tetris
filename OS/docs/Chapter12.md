# Nand2Tetris 第12章: オペレーティングシステム

## 概要

第12章では、Jack言語で書かれたオペレーティングシステム（OS）を実装します。
このOSは、Hackコンピューター上でJackプログラムを実行するために必要な
基本サービスを提供します。

```
ユーザープログラム（Jack）
         │
         ▼
┌─────────────────────────────────┐
│      Jack OS ライブラリ          │
│  Math, String, Array, Memory,   │
│  Screen, Output, Keyboard, Sys  │
└─────────────────────────────────┘
         │
         ▼
    Hack ハードウェア
```

---

## 1. OSの構成

Jack OSは8つのクラスで構成されます：

| クラス | 役割 |
|--------|------|
| `Math` | 数学関数（乗算、除算、平方根など） |
| `String` | 文字列操作 |
| `Array` | 配列の生成と解放 |
| `Memory` | メモリ管理（ヒープ割り当て） |
| `Screen` | グラフィック描画 |
| `Output` | テキスト出力 |
| `Keyboard` | キーボード入力 |
| `Sys` | システム初期化とユーティリティ |

---

## 2. Hackコンピューターのメモリマップ

```
アドレス範囲        用途
────────────────────────────────────
0-15              特殊レジスタ（SP, LCL, ARG, THIS, THAT, R5-R15）
16-255            静的変数
256-2047          スタック
2048-16383        ヒープ（動的メモリ割り当て）
16384-24575       スクリーンメモリマップ
24576             キーボードメモリマップ
```

### 2.1 スクリーンメモリマップ

- 解像度: 512 x 256 ピクセル
- 各ワード（16ビット）が16ピクセルを表す
- 行あたり 32 ワード（512 / 16）
- 合計 8192 ワード（256 x 32）

```
RAM[16384] = 最初の行の最初の16ピクセル
RAM[16415] = 最初の行の最後の16ピクセル
RAM[16416] = 2番目の行の最初の16ピクセル
...
```

### 2.2 キーボードメモリマップ

- RAM[24576] に現在押されているキーのスキャンコードが格納
- キーが押されていない場合は 0

---

## 3. 各クラスの詳細

### 3.1 Math クラス

数学演算を提供します。Hackコンピューターには乗算・除算命令がないため、
ソフトウェアで実装します。

```jack
Math.abs(x)        // 絶対値
Math.multiply(x,y) // 乗算（シフト加算アルゴリズム）
Math.divide(x,y)   // 除算（再帰アルゴリズム）
Math.min(x,y)      // 最小値
Math.max(x,y)      // 最大値
Math.sqrt(x)       // 平方根（二分探索）
```

**乗算アルゴリズム（シフト加算）:**

```
multiply(x, y):
    sum = 0
    shiftedX = x
    for i = 0 to 15:
        if (y の i番目のビット == 1):
            sum = sum + shiftedX
        shiftedX = shiftedX * 2
    return sum
```

**除算アルゴリズム（再帰）:**

```
divide(x, y):
    if (y > x): return 0
    q = divide(x, 2*y)
    if (x - q*2*y < y):
        return 2*q
    else:
        return 2*q + 1
```

### 3.2 String クラス

可変長文字列を提供します。

```jack
String.new(maxLength)  // コンストラクタ
str.dispose()          // メモリ解放
str.length()           // 現在の長さ
str.charAt(i)          // i番目の文字を取得
str.setCharAt(i, c)    // i番目の文字を設定
str.appendChar(c)      // 文字を末尾に追加
str.eraseLastChar()    // 最後の文字を削除
str.intValue()         // 文字列を整数に変換
str.setInt(n)          // 整数を文字列に設定
```

### 3.3 Array クラス

配列の動的生成と解放を提供します。

```jack
Array.new(size)    // 配列を生成
arr.dispose()      // 配列を解放
```

注: 配列の要素アクセス `arr[i]` はコンパイラが直接処理します。

### 3.4 Memory クラス

ヒープメモリの管理を行います。フリーリストアルゴリズムを使用。

```jack
Memory.peek(address)         // メモリを読み取り
Memory.poke(address, value)  // メモリに書き込み
Memory.alloc(size)           // メモリを確保
Memory.deAlloc(obj)          // メモリを解放
```

**フリーリストの構造:**

```
フリーブロック:
┌─────────────┬─────────────┬───────────────────┐
│  サイズ     │  次→ポインタ  │   空き領域...     │
└─────────────┴─────────────┴───────────────────┘

確保済みブロック:
┌─────────────┬───────────────────────────────────┐
│  サイズ     │   ユーザーデータ...                │
└─────────────┴───────────────────────────────────┘
              ↑
              alloc() が返すアドレス
```

### 3.5 Screen クラス

グラフィック描画を提供します。

```jack
Screen.clearScreen()               // 画面クリア
Screen.setColor(b)                 // 描画色設定（true=黒, false=白）
Screen.drawPixel(x, y)             // 点を描画
Screen.drawLine(x1, y1, x2, y2)    // 線を描画
Screen.drawRectangle(x1, y1, x2, y2)  // 矩形を描画
Screen.drawCircle(x, y, r)         // 円を描画
```

**Bresenhamの線描画アルゴリズム:**

整数演算のみで効率的に線を描画します。

```
drawLine(x1, y1, x2, y2):
    dx = x2 - x1
    dy = y2 - y1
    a = 0, b = 0
    while a <= dx and b <= dy:
        drawPixel(x1+a, y1+b)
        if adyMinusbdx < 0:
            a = a + 1
            adyMinusbdx = adyMinusbdx + dy
        else:
            b = b + 1
            adyMinusbdx = adyMinusbdx - dx
```

### 3.6 Output クラス

テキスト出力を提供します。8x11ピクセルのビットマップフォントを使用。

```jack
Output.moveCursor(row, col)  // カーソル移動（行: 0-22, 列: 0-63）
Output.printChar(c)          // 文字を出力
Output.printString(s)        // 文字列を出力
Output.printInt(n)           // 整数を出力
Output.println()             // 改行
Output.backSpace()           // バックスペース
```

**画面のテキスト領域:**

- 512 / 8 = 64 列
- 256 / 11 = 23 行

### 3.7 Keyboard クラス

キーボード入力を提供します。

```jack
Keyboard.keyPressed()    // 現在押されているキー（0=なし）
Keyboard.readChar()      // 1文字入力
Keyboard.readLine(msg)   // 1行入力
Keyboard.readInt(msg)    // 整数入力
```

**特殊キーのスキャンコード:**

| キー | コード |
|-----|-------|
| Enter | 128 |
| Backspace | 129 |
| 左矢印 | 130 |
| 上矢印 | 131 |
| 右矢印 | 132 |
| 下矢印 | 133 |
| Esc | 140 |
| F1-F12 | 141-152 |

### 3.8 Sys クラス

システムサービスを提供します。

```jack
Sys.init()       // OS初期化（ブートストラップから呼ばれる）
Sys.halt()       // プログラム停止
Sys.wait(ms)     // ミリ秒待機
Sys.error(code)  // エラー表示して停止
```

**初期化順序:**

```
Sys.init():
    1. Memory.init()   // 最初に初期化
    2. Math.init()
    3. Screen.init()
    4. Output.init()
    5. Keyboard.init()
    6. Main.main()     // ユーザープログラム開始
    7. Sys.halt()      // 終了後は停止
```

---

## 4. エラーコード

| コード | 説明 |
|--------|------|
| 1 | Sys.wait の引数が負 |
| 2 | Array.new のサイズが不正 |
| 3 | Math.divide で0除算 |
| 4 | Math.sqrt の引数が負 |
| 5 | Memory.alloc のサイズが不正 |
| 6 | Memory.alloc でメモリ不足 |
| 7 | Screen.drawPixel の座標が範囲外 |
| 8 | Screen.drawLine の座標が範囲外 |
| 9 | Screen.drawRectangle の座標が範囲外 |
| 12 | Screen.drawCircle の中心座標が範囲外 |
| 13 | Screen.drawCircle の半径が不正 |
| 14 | String.new の最大長が負 |
| 15 | String.charAt のインデックスが範囲外 |
| 16 | String.setCharAt のインデックスが範囲外 |
| 17 | String.appendChar で文字列が満杯 |
| 18 | String.eraseLastChar で文字列が空 |
| 20 | Output.moveCursor のカーソル位置が範囲外 |

---

## 5. テスト方法

各OSクラスは、Nand2Tetrisのテストプログラムでテストできます。

### 5.1 個別テスト

```bash
# JackCompilerでOSクラスをコンパイル
cd OS/src
JackCompiler .

# VMEmulatorでテストプログラムを実行
# 例: MathTest
1. VMEmulatorを起動
2. テストプログラム（MathTest）を読み込み
3. OS VMファイルを同じディレクトリに配置
4. 実行して結果を確認
```

### 5.2 統合テスト

すべてのOSクラスをJackプログラムと一緒にコンパイルして実行：

```bash
# JackプログラムとOSをまとめてコンパイル
JackCompiler ../Jack/src/Square/

# VMEmulatorで実行
```

---

## 6. 実装のポイント

### 6.1 効率性

- **Math.multiply**: シフト加算でO(n)（nはビット数）
- **Screen.drawHorizontalLine**: ワード単位で高速描画
- **Memory.alloc**: First-fitアルゴリズム

### 6.2 初期化順序

Memoryは他のすべてのクラスが使用するため、最初に初期化する必要があります。

### 6.3 循環依存の回避

OSクラス間の依存関係に注意：
- Math は Memory を使用（twoToThe配列）
- String は Math, Memory を使用
- Output は Math, Screen, String, Memory を使用

---

## 7. まとめ

Jack OSは以下の機能を提供します：

| 機能カテゴリ | クラス | 主な機能 |
|-------------|--------|----------|
| 数学演算 | Math | 乗算、除算、平方根 |
| データ構造 | String, Array | 文字列、配列 |
| メモリ管理 | Memory | 動的割り当て |
| 入出力 | Screen, Output, Keyboard | グラフィック、テキスト、入力 |
| システム | Sys | 初期化、エラー処理 |

これで、Nand2Tetrisの全12章が完了し、NANDゲートからオペレーティングシステムまで、
コンピューターシステムの全体像を構築しました。

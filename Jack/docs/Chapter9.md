# Nand2Tetris 第9章: 高水準言語 (Jack)

## 概要

第9章では、Nand2Tetrisで使用する高水準言語「Jack」を学びます。
Jackは、Java/C#に似たオブジェクト指向言語で、シンプルながらも
実用的なプログラムを書くことができます。

```
Jack言語
    │
    ▼ (10-11章で実装するコンパイラ)
VMコード
    │
    ▼ (7-8章で実装したVMトランスレータ)
アセンブリ
    │
    ▼ (6章で実装したアセンブラ)
機械語
```

---

## 1. Jack言語の特徴

### 1.1 基本的な特徴

- **オブジェクト指向**: クラスベース
- **静的型付け**: 変数は型を持つ
- **シンプルな構文**: Java/C#に似ている
- **メモリ管理**: 手動（new/dispose）

### 1.2 データ型

| 型 | 説明 | 例 |
|----|------|-----|
| `int` | 16ビット整数 (-32768〜32767) | `0`, `123`, `-456` |
| `char` | 1文字（Unicode） | `'A'`, `'あ'` |
| `boolean` | 真偽値 | `true`, `false` |
| `クラス名` | オブジェクト参照 | `String`, `Array` |

### 1.3 演算子

**算術演算子**:
```
+  加算
-  減算（二項）、負号（単項）
*  乗算
/  除算
```

**比較演算子**:
```
=  等しい
>  より大きい
<  より小さい
```

**論理演算子**:
```
&  AND
|  OR
~  NOT
```

---

## 2. プログラム構造

### 2.1 クラス

Jackプログラムはクラスの集合です。
各`.jack`ファイルには1つのクラスを定義します。

```jack
class Main {
    // フィールド（インスタンス変数）
    field int x, y;

    // 静的変数
    static int count;

    // コンストラクタ
    constructor Main new() {
        let x = 0;
        let y = 0;
        return this;
    }

    // メソッド
    method void move(int dx, int dy) {
        let x = x + dx;
        let y = y + dy;
        return;
    }

    // 関数（静的メソッド）
    function void main() {
        var Main obj;
        let obj = Main.new();
        do obj.move(10, 20);
        do obj.dispose();
        return;
    }
}
```

### 2.2 サブルーチンの種類

| 種類 | キーワード | 説明 |
|------|-----------|------|
| コンストラクタ | `constructor` | オブジェクトを生成して返す |
| メソッド | `method` | オブジェクトに対して操作（thisを使用） |
| 関数 | `function` | 静的メソッド（thisなし） |

### 2.3 変数の種類

| 種類 | キーワード | スコープ |
|------|-----------|---------|
| フィールド | `field` | クラスインスタンス内 |
| 静的変数 | `static` | クラス全体で共有 |
| ローカル変数 | `var` | サブルーチン内 |
| 引数 | なし | サブルーチン内 |

---

## 3. 文（Statement）

### 3.1 let文（代入）

```jack
let x = 10;
let arr[i] = 5;      // 配列への代入
let name = "Hello";
```

### 3.2 if文

```jack
if (x > 0) {
    let y = 1;
}

if (x > 0) {
    let y = 1;
} else {
    let y = -1;
}
```

### 3.3 while文

```jack
while (i < 10) {
    let sum = sum + i;
    let i = i + 1;
}
```

### 3.4 do文（サブルーチン呼び出し）

```jack
do Output.printString("Hello");
do obj.move(10, 20);
do Memory.deAlloc(this);
```

### 3.5 return文

```jack
return;           // void関数
return x + 1;     // 値を返す
return this;      // コンストラクタ
```

---

## 4. 標準ライブラリ (OS)

Jackには標準ライブラリ（OS）が付属しています。

### 4.1 Math

```jack
Math.abs(x)        // 絶対値
Math.multiply(x,y) // 乗算
Math.divide(x,y)   // 除算
Math.min(x,y)      // 最小値
Math.max(x,y)      // 最大値
Math.sqrt(x)       // 平方根
```

### 4.2 String

```jack
String.new(maxLength)  // 文字列生成
str.dispose()          // 解放
str.length()           // 長さ
str.charAt(i)          // i番目の文字
str.setCharAt(i, c)    // i番目を変更
str.appendChar(c)      // 文字を追加
str.eraseLastChar()    // 最後を削除
str.intValue()         // 整数に変換
str.setInt(n)          // 整数を設定
```

### 4.3 Array

```jack
Array.new(size)    // 配列生成
arr.dispose()      // 解放
arr[i]             // 要素アクセス
```

### 4.4 Output

```jack
Output.moveCursor(row, col)  // カーソル移動
Output.printChar(c)          // 文字出力
Output.printString(s)        // 文字列出力
Output.printInt(n)           // 整数出力
Output.println()             // 改行
Output.backSpace()           // バックスペース
```

### 4.5 Keyboard

```jack
Keyboard.keyPressed()    // 押されているキー（0=なし）
Keyboard.readChar()      // 1文字入力
Keyboard.readLine(msg)   // 1行入力
Keyboard.readInt(msg)    // 整数入力
```

### 4.6 Screen

```jack
Screen.clearScreen()           // 画面クリア
Screen.setColor(b)             // 色設定（true=黒, false=白）
Screen.drawPixel(x, y)         // 点を描画
Screen.drawLine(x1,y1,x2,y2)   // 線を描画
Screen.drawRectangle(x1,y1,x2,y2)  // 矩形を描画
Screen.drawCircle(x, y, r)     // 円を描画
```

### 4.7 Memory

```jack
Memory.peek(addr)         // メモリ読み取り
Memory.poke(addr, value)  // メモリ書き込み
Memory.alloc(size)        // メモリ確保
Memory.deAlloc(obj)       // メモリ解放
```

### 4.8 Sys

```jack
Sys.halt()       // プログラム停止
Sys.error(code)  // エラー終了
Sys.wait(ms)     // ミリ秒待機
```

---

## 5. 画面とキーボード

### 5.1 画面仕様

- 解像度: 512 x 256 ピクセル
- 白黒（1ビット）
- 左上が (0, 0)

```
(0,0) ─────────────────────▶ (511,0)
  │                              x
  │
  │
  ▼
(0,255)                    (511,255)
  y
```

### 5.2 キーボードコード

| キー | コード |
|-----|-------|
| Enter | 128 |
| Backspace | 129 |
| 左矢印 | 130 |
| 上矢印 | 131 |
| 右矢印 | 132 |
| 下矢印 | 133 |
| Home | 134 |
| End | 135 |
| Page Up | 136 |
| Page Down | 137 |
| Insert | 138 |
| Delete | 139 |
| Esc | 140 |
| F1-F12 | 141-152 |

---

## 6. メモリ管理

### 6.1 オブジェクトの生成と解放

```jack
// 生成
var Point p;
let p = Point.new(10, 20);

// 使用
do p.move(5, 5);

// 解放（必須！）
do p.dispose();
```

### 6.2 disposeメソッドの実装

```jack
class Point {
    field int x, y;

    constructor Point new(int ax, int ay) {
        let x = ax;
        let y = ay;
        return this;
    }

    method void dispose() {
        do Memory.deAlloc(this);
        return;
    }
}
```

### 6.3 メモリリーク

解放を忘れるとメモリリークが発生します。
特にループ内でオブジェクトを生成する場合は注意：

```jack
// 悪い例（メモリリーク）
while (true) {
    let str = String.new(10);  // 毎回生成
    // strを使う
    // disposeを忘れている！
}

// 良い例
while (true) {
    let str = String.new(10);
    // strを使う
    do str.dispose();  // 必ず解放
}
```

---

## 7. プログラム例

### 7.1 Hello World

```jack
class Main {
    function void main() {
        do Output.printString("Hello, World!");
        do Output.println();
        return;
    }
}
```

### 7.2 簡単な計算

```jack
class Main {
    function void main() {
        var int a, b, sum;

        do Output.printString("Enter first number: ");
        let a = Keyboard.readInt("");

        do Output.printString("Enter second number: ");
        let b = Keyboard.readInt("");

        let sum = a + b;

        do Output.printString("Sum = ");
        do Output.printInt(sum);
        do Output.println();

        return;
    }
}
```

### 7.3 配列の使用

```jack
class Main {
    function void main() {
        var Array arr;
        var int i, sum;

        let arr = Array.new(5);
        let i = 0;

        while (i < 5) {
            let arr[i] = i * i;  // 0, 1, 4, 9, 16
            let i = i + 1;
        }

        let sum = 0;
        let i = 0;
        while (i < 5) {
            let sum = sum + arr[i];
            let i = i + 1;
        }

        do Output.printString("Sum of squares: ");
        do Output.printInt(sum);  // 30
        do Output.println();

        do arr.dispose();
        return;
    }
}
```

---

## 8. ゲーム開発の基本

### 8.1 ゲームループ

```jack
class Game {
    field int x, y;       // プレイヤー位置
    field boolean exit;   // 終了フラグ

    constructor Game new() {
        let x = 256;      // 画面中央
        let y = 128;
        let exit = false;
        return this;
    }

    method void run() {
        var char key;

        while (~exit) {
            // 入力処理
            let key = Keyboard.keyPressed();
            do processInput(key);

            // 描画
            do draw();

            // ウェイト
            do Sys.wait(50);
        }
        return;
    }

    method void processInput(char key) {
        if (key = 130) { let x = x - 5; }  // 左
        if (key = 132) { let x = x + 5; }  // 右
        if (key = 131) { let y = y - 5; }  // 上
        if (key = 133) { let y = y + 5; }  // 下
        if (key = 140) { let exit = true; } // ESC
        return;
    }

    method void draw() {
        do Screen.clearScreen();
        do Screen.setColor(true);
        do Screen.drawRectangle(x-5, y-5, x+5, y+5);
        return;
    }

    method void dispose() {
        do Memory.deAlloc(this);
        return;
    }
}

class Main {
    function void main() {
        var Game game;
        let game = Game.new();
        do game.run();
        do game.dispose();
        return;
    }
}
```

---

## 9. コンパイルと実行

### 9.1 コンパイル手順

```
1. JackCompiler で .jack → .vm に変換
2. VMEmulator で実行（または）
3. VMTranslator で .vm → .asm に変換
4. Assembler で .asm → .hack に変換
5. CPUEmulator で実行
```

### 9.2 JackCompilerの使用

```bash
# Nand2Tetrisのツールを使用
JackCompiler MyProgram/

# または
JackCompiler Main.jack
```

---

## 10. まとめ

Jack言語の主な特徴:

| 特徴 | 説明 |
|------|------|
| クラスベース | 各ファイルに1クラス |
| 3種類のサブルーチン | constructor, method, function |
| 4種類の変数 | field, static, var, 引数 |
| 手動メモリ管理 | new/dispose |
| 標準ライブラリ | Math, String, Array, Screen, Keyboard等 |

次の10-11章では、このJack言語のコンパイラを実装します。

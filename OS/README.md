# Jack OS - オペレーティングシステム（プロジェクト12）

Nand2Tetrisコース第12章のJack OS実装です。
Jack言語で書かれた8つのOSクラスを提供し、Hackコンピューター上でJackプログラムを実行するための基盤となります。

## ディレクトリ構成

```
OS/
├── src/                    # ソースコード
│   ├── Math.jack           # 数学関数
│   ├── String.jack         # 文字列操作
│   ├── Array.jack          # 配列操作
│   ├── Memory.jack         # メモリ管理
│   ├── Screen.jack         # グラフィック描画
│   ├── Output.jack         # テキスト出力
│   ├── Keyboard.jack       # キーボード入力
│   └── Sys.jack            # システムサービス
├── docs/
│   └── Chapter12.md        # 詳細ドキュメント
└── README.md
```

## OSクラス一覧

| クラス | 説明 | 主なメソッド |
|--------|------|--------------|
| **Math** | 数学演算 | `multiply`, `divide`, `sqrt`, `abs`, `min`, `max` |
| **String** | 文字列 | `new`, `charAt`, `appendChar`, `intValue`, `setInt` |
| **Array** | 配列 | `new`, `dispose` |
| **Memory** | ヒープ管理 | `alloc`, `deAlloc`, `peek`, `poke` |
| **Screen** | グラフィック | `drawPixel`, `drawLine`, `drawRectangle`, `drawCircle` |
| **Output** | テキスト出力 | `printChar`, `printString`, `printInt`, `println` |
| **Keyboard** | キーボード | `keyPressed`, `readChar`, `readLine`, `readInt` |
| **Sys** | システム | `init`, `halt`, `wait`, `error` |

## 使用方法

### コンパイル

JackCompilerを使用してOSクラスをVMコードにコンパイルします。

```bash
# Nand2Tetrisのツールを使用
cd OS/src
JackCompiler .
```

### Jackプログラムとの統合

JackプログラムとOSを一緒にコンパイルして実行します。

```bash
# 例: Squareゲームを実行
# 1. OSファイルをプログラムディレクトリにコピー
cp OS/src/*.jack Jack/src/Square/

# 2. コンパイル
JackCompiler Jack/src/Square/

# 3. VMEmulatorで実行
```

### テスト

各OSクラスはNand2Tetrisのテストプログラムでテストできます。

```bash
# 例: Mathクラスのテスト
# 1. テストディレクトリにMath.vmをコピー
# 2. VMEmulatorでMathTestを実行
# 3. 比較ファイルと照合
```

## API リファレンス

### Math

```jack
function int abs(int x)           // 絶対値
function int multiply(int x, int y) // 乗算
function int divide(int x, int y)   // 除算
function int min(int x, int y)      // 最小値
function int max(int x, int y)      // 最大値
function int sqrt(int x)            // 平方根
```

### String

```jack
constructor String new(int maxLength)
method void dispose()
method int length()
method char charAt(int j)
method void setCharAt(int j, char c)
method String appendChar(char c)
method char eraseLastChar()
method int intValue()
method void setInt(int val)
function char newLine()      // 128
function char backSpace()    // 129
function char doubleQuote()  // 34
```

### Array

```jack
function Array new(int size)
method void dispose()
```

### Memory

```jack
function int peek(int address)
function void poke(int address, int value)
function int alloc(int size)
function void deAlloc(Array o)
```

### Screen

```jack
function void clearScreen()
function void setColor(boolean b)  // true=黒, false=白
function void drawPixel(int x, int y)
function void drawLine(int x1, int y1, int x2, int y2)
function void drawRectangle(int x1, int y1, int x2, int y2)
function void drawCircle(int x, int y, int r)
```

### Output

```jack
function void moveCursor(int i, int j)  // 行: 0-22, 列: 0-63
function void printChar(char c)
function void printString(String s)
function void printInt(int i)
function void println()
function void backSpace()
```

### Keyboard

```jack
function char keyPressed()
function char readChar()
function String readLine(String message)
function int readInt(String message)
```

### Sys

```jack
function void init()
function void halt()
function void error(int errorCode)
function void wait(int duration)
```

## 実装の特徴

### 効率的なアルゴリズム

- **Math.multiply**: シフト加算アルゴリズム（O(n)、n=ビット数）
- **Math.sqrt**: 二分探索
- **Screen.drawLine**: Bresenhamのアルゴリズム
- **Memory.alloc**: First-fitフリーリスト

### メモリレイアウト

```
RAM[0-15]:      特殊レジスタ
RAM[16-255]:    静的変数
RAM[256-2047]:  スタック
RAM[2048-16383]: ヒープ
RAM[16384-24575]: スクリーン（512x256ピクセル）
RAM[24576]:     キーボード
```

## 関連ドキュメント

- [Chapter12.md](docs/Chapter12.md) - 詳細な実装解説

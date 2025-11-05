# Java Project

このディレクトリでJavaコードを開発・実行できます。

## 基本的な使い方

### Javaファイルをコンパイルして実行する

```bash
# Javaファイルをコンパイル
javac HelloWorld.java

# 実行
java HelloWorld
```

### クラスパスを指定する場合

```bash
# 特定のクラスパスでコンパイル
javac -cp . HelloWorld.java

# 特定のクラスパスで実行
java -cp . HelloWorld
```

### 複数のJavaファイルをまとめてコンパイル

```bash
# 全ての.javaファイルをコンパイル
javac *.java

# パッケージ構造がある場合
javac -d . src/**/*.java
```

## プロジェクト構造の例

```
/Users/taisei/Hack/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   └── Main.java
├── lib/
│   └── (外部ライブラリ)
└── README.md
```

## 便利なコマンド

- `java -version` - Javaのバージョン確認
- `javac -version` - コンパイラのバージョン確認
- `java -cp .` - 現在のディレクトリをクラスパスに設定

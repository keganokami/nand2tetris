# Java Development Makefile
# 基本的なJavaプロジェクトのビルドとテスト用

# 設定
JAVA_SRC_DIR = src/main/java
BUILD_DIR = build
MAIN_CLASS = com.example.Main

# デフォルトターゲット
all: compile

# ディレクトリ作成
setup:
	mkdir -p $(BUILD_DIR)

# コンパイル
compile: setup
	javac -d $(BUILD_DIR) -cp $(BUILD_DIR) $(JAVA_SRC_DIR)/**/*.java

# 実行
run: compile
	java -cp $(BUILD_DIR) $(MAIN_CLASS)

# 引数付きで実行
run-with-args: compile
	java -cp $(BUILD_DIR) $(MAIN_CLASS) Hello World Java

# シンプルなプログラム用
compile-simple:
	javac HelloWorld.java

run-simple: compile-simple
	java HelloWorld

# クリーンアップ
clean:
	rm -rf $(BUILD_DIR)
	rm -f *.class

# ヘルプ
help:
	@echo "利用可能なコマンド:"
	@echo "  make compile       - パッケージ構造のJavaファイルをコンパイル"
	@echo "  make run          - メインプログラムを実行"
	@echo "  make run-with-args - 引数付きでプログラムを実行"
	@echo "  make compile-simple - HelloWorld.javaをコンパイル"
	@echo "  make run-simple    - HelloWorldを実行"
	@echo "  make clean        - 生成されたファイルを削除"
	@echo "  make help         - このヘルプを表示"

.PHONY: all setup compile run run-with-args compile-simple run-simple clean help

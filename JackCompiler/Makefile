# Jack Compiler Makefile
# プロジェクト10 & 11: 構文解析とコード生成

SRCDIR = src
BINDIR = bin
TESTDIR = ../Jack/src

# Javaソースファイル
SOURCES = $(wildcard $(SRCDIR)/*.java)
CLASSES = $(SOURCES:$(SRCDIR)/%.java=$(BINDIR)/%.class)

# デフォルトターゲット
all: $(BINDIR) compile

# binディレクトリを作成
$(BINDIR):
	mkdir -p $(BINDIR)

# コンパイル
compile: $(BINDIR)
	javac -d $(BINDIR) $(SRCDIR)/*.java

# クリーン
clean:
	rm -rf $(BINDIR)/*.class

# 構文解析器のテスト（第10章）
test-analyzer: compile
	@echo "=== JackAnalyzer テスト ==="
	@echo ""
	@echo "--- Square/Main.jack ---"
	java -cp $(BINDIR) JackAnalyzer $(TESTDIR)/Square/Main.jack
	@echo ""
	@echo "--- Square ディレクトリ全体 ---"
	java -cp $(BINDIR) JackAnalyzer $(TESTDIR)/Square/

# コンパイラのテスト（第11章）
test-compiler: compile
	@echo "=== JackCompiler テスト ==="
	@echo ""
	@echo "--- HelloWorld ---"
	java -cp $(BINDIR) JackCompiler $(TESTDIR)/HelloWorld/
	@echo ""
	@echo "--- Average ---"
	java -cp $(BINDIR) JackCompiler $(TESTDIR)/Average/
	@echo ""
	@echo "--- Square ---"
	java -cp $(BINDIR) JackCompiler $(TESTDIR)/Square/
	@echo ""
	@echo "--- Pong ---"
	java -cp $(BINDIR) JackCompiler $(TESTDIR)/Pong/

# 単一ファイルをコンパイル
compile-file: compile
	@if [ -z "$(FILE)" ]; then \
		echo "使用方法: make compile-file FILE=<path/to/file.jack>"; \
	else \
		java -cp $(BINDIR) JackCompiler $(FILE); \
	fi

# 単一ファイルを解析（XML出力）
analyze-file: compile
	@if [ -z "$(FILE)" ]; then \
		echo "使用方法: make analyze-file FILE=<path/to/file.jack>"; \
	else \
		java -cp $(BINDIR) JackAnalyzer $(FILE); \
	fi

# ヘルプ
help:
	@echo "Jack Compiler (プロジェクト10 & 11)"
	@echo ""
	@echo "ターゲット:"
	@echo "  all           - コンパイル（デフォルト）"
	@echo "  compile       - Javaソースをコンパイル"
	@echo "  clean         - クラスファイルを削除"
	@echo "  test-analyzer - 構文解析器をテスト（第10章）"
	@echo "  test-compiler - コンパイラをテスト（第11章）"
	@echo "  compile-file  - 単一ファイルをコンパイル (FILE=<path>)"
	@echo "  analyze-file  - 単一ファイルを解析 (FILE=<path>)"
	@echo ""
	@echo "例:"
	@echo "  make"
	@echo "  make test-compiler"
	@echo "  make compile-file FILE=../Jack/src/Square/Main.jack"

.PHONY: all compile clean test-analyzer test-compiler compile-file analyze-file help

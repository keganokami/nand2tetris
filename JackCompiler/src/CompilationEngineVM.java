import java.io.*;

/**
 * CompilationEngineVM - VMコードを生成する構文解析エンジン
 *
 * 第11章の中核モジュール。JackTokenizerからトークンを受け取り、
 * 再帰下降構文解析を行いながらVMコードを生成します。
 *
 * CompilationEngine（第10章）との違い:
 * - XMLを出力する代わりにVMコードを出力
 * - シンボルテーブルで変数を管理
 * - VMWriterでVMコードを出力
 *
 * コード生成のポイント:
 * 1. 変数: シンボルテーブルで管理し、push/popで操作
 * 2. 式: 後置記法に変換（演算子は両オペランドの後に出力）
 * 3. 関数呼び出し: 引数をpushしてからcall
 * 4. メソッド呼び出し: 最初にthisをpush
 * 5. 制御構造: ラベルとジャンプ命令で実装
 */
public class CompilationEngineVM {

    private JackTokenizer tokenizer;
    private VMWriter writer;
    private SymbolTable symbolTable;

    // 現在のクラス名
    private String className;

    // ラベル生成用カウンタ
    private int labelCounter;

    // 演算子
    private static final String OPS = "+-*/&|<>=";
    private static final String UNARY_OPS = "-~";

    /**
     * コンストラクタ
     * @param tokenizer JackTokenizerインスタンス
     * @param outputFile 出力VMファイル
     */
    public CompilationEngineVM(JackTokenizer tokenizer, File outputFile) throws IOException {
        this.tokenizer = tokenizer;
        this.writer = new VMWriter(outputFile);
        this.symbolTable = new SymbolTable();
        this.className = "";
        this.labelCounter = 0;
    }

    /**
     * ユニークなラベルを生成
     */
    private String newLabel(String prefix) {
        return prefix + labelCounter++;
    }

    /**
     * クラス全体をコンパイル
     * 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() {
        // 'class'
        tokenizer.advance();

        // className
        tokenizer.advance();
        className = tokenizer.identifier();

        // '{'
        tokenizer.advance();

        // classVarDec* subroutineDec*
        tokenizer.advance();
        while (isClassVarDec() || isSubroutineDec()) {
            if (isClassVarDec()) {
                compileClassVarDec();
            } else {
                compileSubroutine();
            }
        }

        // '}'
        writer.close();
    }

    /**
     * クラス変数宣言をコンパイル
     * ('static'|'field') type varName (',' varName)* ';'
     */
    public void compileClassVarDec() {
        // 'static' | 'field'
        SymbolTable.Kind kind;
        if (tokenizer.keyword() == JackTokenizer.Keyword.STATIC) {
            kind = SymbolTable.Kind.STATIC;
        } else {
            kind = SymbolTable.Kind.FIELD;
        }

        // type
        tokenizer.advance();
        String type = getCurrentType();

        // varName
        tokenizer.advance();
        symbolTable.define(tokenizer.identifier(), type, kind);

        // (',' varName)*
        tokenizer.advance();
        while (isSymbol(',')) {
            tokenizer.advance();
            symbolTable.define(tokenizer.identifier(), type, kind);
            tokenizer.advance();
        }

        // ';'
        tokenizer.advance();
    }

    /**
     * サブルーチンをコンパイル
     * ('constructor'|'function'|'method') ('void'|type)
     * subroutineName '(' parameterList ')' subroutineBody
     */
    public void compileSubroutine() {
        // 新しいサブルーチンスコープを開始
        symbolTable.startSubroutine();

        // 'constructor' | 'function' | 'method'
        JackTokenizer.Keyword subroutineType = tokenizer.keyword();

        // メソッドの場合、thisを最初の引数として登録
        if (subroutineType == JackTokenizer.Keyword.METHOD) {
            symbolTable.define("this", className, SymbolTable.Kind.ARG);
        }

        // 'void' | type
        tokenizer.advance();

        // subroutineName
        tokenizer.advance();
        String subroutineName = tokenizer.identifier();

        // '('
        tokenizer.advance();

        // parameterList
        tokenizer.advance();
        compileParameterList();

        // ')'

        // subroutineBody
        tokenizer.advance();
        compileSubroutineBody(subroutineType, subroutineName);

        tokenizer.advance();
    }

    /**
     * パラメータリストをコンパイル
     * ((type varName) (',' type varName)*)?
     */
    public void compileParameterList() {
        // パラメータがある場合
        if (!isSymbol(')')) {
            // type
            String type = getCurrentType();

            // varName
            tokenizer.advance();
            symbolTable.define(tokenizer.identifier(), type, SymbolTable.Kind.ARG);

            // (',' type varName)*
            tokenizer.advance();
            while (isSymbol(',')) {
                // type
                tokenizer.advance();
                type = getCurrentType();

                // varName
                tokenizer.advance();
                symbolTable.define(tokenizer.identifier(), type, SymbolTable.Kind.ARG);

                tokenizer.advance();
            }
        }
    }

    /**
     * サブルーチン本体をコンパイル
     * '{' varDec* statements '}'
     */
    public void compileSubroutineBody(JackTokenizer.Keyword subroutineType, String subroutineName) {
        // '{'

        // varDec*
        tokenizer.advance();
        while (isVarDec()) {
            compileVarDec();
        }

        // function宣言を出力（ローカル変数の数が分かった後）
        String functionName = className + "." + subroutineName;
        int nLocals = symbolTable.varCount(SymbolTable.Kind.VAR);
        writer.writeFunction(functionName, nLocals);

        // コンストラクタ: メモリを確保してthisを設定
        if (subroutineType == JackTokenizer.Keyword.CONSTRUCTOR) {
            int nFields = symbolTable.varCount(SymbolTable.Kind.FIELD);
            writer.writePush(VMWriter.Segment.CONSTANT, nFields);
            writer.writeCall("Memory.alloc", 1);
            writer.writePop(VMWriter.Segment.POINTER, 0);
        }
        // メソッド: thisを設定
        else if (subroutineType == JackTokenizer.Keyword.METHOD) {
            writer.writePush(VMWriter.Segment.ARGUMENT, 0);
            writer.writePop(VMWriter.Segment.POINTER, 0);
        }

        // statements
        compileStatements();

        // '}'
    }

    /**
     * ローカル変数宣言をコンパイル
     * 'var' type varName (',' varName)* ';'
     */
    public void compileVarDec() {
        // 'var'

        // type
        tokenizer.advance();
        String type = getCurrentType();

        // varName
        tokenizer.advance();
        symbolTable.define(tokenizer.identifier(), type, SymbolTable.Kind.VAR);

        // (',' varName)*
        tokenizer.advance();
        while (isSymbol(',')) {
            tokenizer.advance();
            symbolTable.define(tokenizer.identifier(), type, SymbolTable.Kind.VAR);
            tokenizer.advance();
        }

        // ';'
        tokenizer.advance();
    }

    /**
     * 文の列をコンパイル
     */
    public void compileStatements() {
        while (isStatement()) {
            if (isKeyword("let")) {
                compileLet();
            } else if (isKeyword("if")) {
                compileIf();
            } else if (isKeyword("while")) {
                compileWhile();
            } else if (isKeyword("do")) {
                compileDo();
            } else if (isKeyword("return")) {
                compileReturn();
            }
        }
    }

    /**
     * let文をコンパイル
     * 'let' varName ('[' expression ']')? '=' expression ';'
     */
    public void compileLet() {
        // 'let'

        // varName
        tokenizer.advance();
        String varName = tokenizer.identifier();
        SymbolTable.Kind kind = symbolTable.kindOf(varName);
        int index = symbolTable.indexOf(varName);
        String segment = SymbolTable.kindToSegment(kind);

        // ('[' expression ']')?
        tokenizer.advance();
        boolean isArrayAccess = isSymbol('[');

        if (isArrayAccess) {
            // 配列アクセス: arr[expr]
            // arr + exprをthatのベースに設定

            // arrのベースアドレスをpush
            writer.writePush(segment, index);

            // '['
            tokenizer.advance();
            compileExpression();
            // ']'

            // arr + expr
            writer.writeArithmetic(VMWriter.Command.ADD);

            // '='
            tokenizer.advance();

            // 右辺の式を評価
            tokenizer.advance();
            compileExpression();

            // スタック上: [arr+expr値, 右辺値]
            // 右辺値を一時保存
            writer.writePop(VMWriter.Segment.TEMP, 0);

            // thatをarr+exprに設定
            writer.writePop(VMWriter.Segment.POINTER, 1);

            // 右辺値をthat[0]に格納
            writer.writePush(VMWriter.Segment.TEMP, 0);
            writer.writePop(VMWriter.Segment.THAT, 0);
        } else {
            // 単純な変数アクセス
            // '='

            // 右辺の式を評価
            tokenizer.advance();
            compileExpression();

            // 変数に格納
            writer.writePop(segment, index);
        }

        // ';'
        tokenizer.advance();
    }

    /**
     * if文をコンパイル
     * 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     */
    public void compileIf() {
        String labelElse = newLabel("IF_FALSE");
        String labelEnd = newLabel("IF_END");

        // 'if'

        // '('
        tokenizer.advance();

        // expression
        tokenizer.advance();
        compileExpression();

        // ')'

        // 条件が偽ならelseへジャンプ
        writer.writeArithmetic(VMWriter.Command.NOT);
        writer.writeIf(labelElse);

        // '{'
        tokenizer.advance();

        // statements (if-true)
        tokenizer.advance();
        compileStatements();

        // '}'

        // elseをスキップ
        writer.writeGoto(labelEnd);

        // else部分
        writer.writeLabel(labelElse);

        // ('else' '{' statements '}')?
        tokenizer.advance();
        if (isKeyword("else")) {
            // '{'
            tokenizer.advance();

            // statements (else)
            tokenizer.advance();
            compileStatements();

            // '}'
            tokenizer.advance();
        }

        writer.writeLabel(labelEnd);
    }

    /**
     * while文をコンパイル
     * 'while' '(' expression ')' '{' statements '}'
     */
    public void compileWhile() {
        String labelLoop = newLabel("WHILE_EXP");
        String labelEnd = newLabel("WHILE_END");

        // ループの先頭
        writer.writeLabel(labelLoop);

        // 'while'

        // '('
        tokenizer.advance();

        // expression
        tokenizer.advance();
        compileExpression();

        // ')'

        // 条件が偽ならループ終了
        writer.writeArithmetic(VMWriter.Command.NOT);
        writer.writeIf(labelEnd);

        // '{'
        tokenizer.advance();

        // statements
        tokenizer.advance();
        compileStatements();

        // '}'

        // ループの先頭へ戻る
        writer.writeGoto(labelLoop);

        writer.writeLabel(labelEnd);
        tokenizer.advance();
    }

    /**
     * do文をコンパイル
     * 'do' subroutineCall ';'
     */
    public void compileDo() {
        // 'do'

        // subroutineCall
        tokenizer.advance();
        compileSubroutineCall();

        // 戻り値を破棄
        writer.writePop(VMWriter.Segment.TEMP, 0);

        // ';'
        tokenizer.advance();
    }

    /**
     * return文をコンパイル
     * 'return' expression? ';'
     */
    public void compileReturn() {
        // 'return'

        // expression?
        tokenizer.advance();
        if (!isSymbol(';')) {
            compileExpression();
        } else {
            // void関数の場合、0を返す
            writer.writePush(VMWriter.Segment.CONSTANT, 0);
        }

        writer.writeReturn();

        // ';'
        tokenizer.advance();
    }

    /**
     * 式をコンパイル
     * term (op term)*
     */
    public void compileExpression() {
        // term
        compileTerm();

        // (op term)*
        while (isOp()) {
            char op = tokenizer.symbol();
            tokenizer.advance();
            compileTerm();

            // 演算子に対応するVMコードを出力
            switch (op) {
                case '+': writer.writeArithmetic(VMWriter.Command.ADD); break;
                case '-': writer.writeArithmetic(VMWriter.Command.SUB); break;
                case '*': writer.writeCall("Math.multiply", 2); break;
                case '/': writer.writeCall("Math.divide", 2); break;
                case '&': writer.writeArithmetic(VMWriter.Command.AND); break;
                case '|': writer.writeArithmetic(VMWriter.Command.OR); break;
                case '<': writer.writeArithmetic(VMWriter.Command.LT); break;
                case '>': writer.writeArithmetic(VMWriter.Command.GT); break;
                case '=': writer.writeArithmetic(VMWriter.Command.EQ); break;
            }
        }
    }

    /**
     * 項をコンパイル
     */
    public void compileTerm() {
        if (tokenizer.tokenType() == JackTokenizer.TokenType.INT_CONST) {
            // integerConstant
            writer.writePush(VMWriter.Segment.CONSTANT, tokenizer.intVal());
            tokenizer.advance();
        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.STRING_CONST) {
            // stringConstant
            String str = tokenizer.stringVal();
            // 文字列オブジェクトを作成
            writer.writePush(VMWriter.Segment.CONSTANT, str.length());
            writer.writeCall("String.new", 1);
            // 各文字を追加
            for (int i = 0; i < str.length(); i++) {
                writer.writePush(VMWriter.Segment.CONSTANT, (int) str.charAt(i));
                writer.writeCall("String.appendChar", 2);
            }
            tokenizer.advance();
        } else if (isKeywordConstant()) {
            // keywordConstant: true | false | null | this
            JackTokenizer.Keyword kw = tokenizer.keyword();
            switch (kw) {
                case TRUE:
                    writer.writePush(VMWriter.Segment.CONSTANT, 0);
                    writer.writeArithmetic(VMWriter.Command.NOT);
                    break;
                case FALSE:
                case NULL:
                    writer.writePush(VMWriter.Segment.CONSTANT, 0);
                    break;
                case THIS:
                    writer.writePush(VMWriter.Segment.POINTER, 0);
                    break;
            }
            tokenizer.advance();
        } else if (isSymbol('(')) {
            // '(' expression ')'
            tokenizer.advance();
            compileExpression();
            // ')'
            tokenizer.advance();
        } else if (isUnaryOp()) {
            // unaryOp term
            char op = tokenizer.symbol();
            tokenizer.advance();
            compileTerm();
            if (op == '-') {
                writer.writeArithmetic(VMWriter.Command.NEG);
            } else { // '~'
                writer.writeArithmetic(VMWriter.Command.NOT);
            }
        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.IDENTIFIER) {
            // varName | varName '[' expression ']' | subroutineCall
            String next = tokenizer.peekNext();
            if ("[".equals(next)) {
                // varName '[' expression ']' - 配列アクセス
                String varName = tokenizer.identifier();
                SymbolTable.Kind kind = symbolTable.kindOf(varName);
                int index = symbolTable.indexOf(varName);
                String segment = SymbolTable.kindToSegment(kind);

                // 配列のベースアドレスをpush
                writer.writePush(segment, index);

                tokenizer.advance(); // '['
                tokenizer.advance();
                compileExpression();
                // ']'

                // インデックスを加算
                writer.writeArithmetic(VMWriter.Command.ADD);

                // thatを設定して値を取得
                writer.writePop(VMWriter.Segment.POINTER, 1);
                writer.writePush(VMWriter.Segment.THAT, 0);

                tokenizer.advance();
            } else if ("(".equals(next) || ".".equals(next)) {
                // subroutineCall
                compileSubroutineCall();
            } else {
                // varName - 変数参照
                String varName = tokenizer.identifier();
                SymbolTable.Kind kind = symbolTable.kindOf(varName);
                int index = symbolTable.indexOf(varName);
                String segment = SymbolTable.kindToSegment(kind);
                writer.writePush(segment, index);
                tokenizer.advance();
            }
        }
    }

    /**
     * サブルーチン呼び出しをコンパイル
     * subroutineName '(' expressionList ')' |
     * (className|varName) '.' subroutineName '(' expressionList ')'
     */
    private void compileSubroutineCall() {
        String firstName = tokenizer.identifier();
        String subroutineName;
        int nArgs = 0;

        tokenizer.advance();
        if (isSymbol('.')) {
            // className.subroutineName() または varName.methodName()
            tokenizer.advance();
            subroutineName = tokenizer.identifier();

            // firstNameが変数ならメソッド呼び出し
            if (symbolTable.contains(firstName)) {
                // メソッド呼び出し: obj.method()
                // objをthisとしてpush
                SymbolTable.Kind kind = symbolTable.kindOf(firstName);
                int index = symbolTable.indexOf(firstName);
                String segment = SymbolTable.kindToSegment(kind);
                writer.writePush(segment, index);
                nArgs = 1;

                // クラス名を取得
                subroutineName = symbolTable.typeOf(firstName) + "." + subroutineName;
            } else {
                // 関数/コンストラクタ呼び出し: ClassName.function()
                subroutineName = firstName + "." + subroutineName;
            }

            tokenizer.advance();
        } else {
            // 同じクラス内のメソッド呼び出し: methodName()
            // thisをpush
            writer.writePush(VMWriter.Segment.POINTER, 0);
            nArgs = 1;
            subroutineName = className + "." + firstName;
        }

        // '('
        tokenizer.advance();

        // expressionList
        nArgs += compileExpressionList();

        // ')'

        // 関数呼び出し
        writer.writeCall(subroutineName, nArgs);

        tokenizer.advance();
    }

    /**
     * 式のリストをコンパイル
     * (expression (',' expression)*)?
     * @return 式の数
     */
    public int compileExpressionList() {
        int count = 0;

        if (!isSymbol(')')) {
            compileExpression();
            count++;

            while (isSymbol(',')) {
                tokenizer.advance();
                compileExpression();
                count++;
            }
        }

        return count;
    }

    // ===== ヘルパーメソッド =====

    /**
     * 現在のトークンから型名を取得
     */
    private String getCurrentType() {
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
            return tokenizer.getCurrentToken();
        } else {
            return tokenizer.identifier();
        }
    }

    private boolean isClassVarDec() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
               (tokenizer.keyword() == JackTokenizer.Keyword.STATIC ||
                tokenizer.keyword() == JackTokenizer.Keyword.FIELD);
    }

    private boolean isSubroutineDec() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
               (tokenizer.keyword() == JackTokenizer.Keyword.CONSTRUCTOR ||
                tokenizer.keyword() == JackTokenizer.Keyword.FUNCTION ||
                tokenizer.keyword() == JackTokenizer.Keyword.METHOD);
    }

    private boolean isVarDec() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
               tokenizer.keyword() == JackTokenizer.Keyword.VAR;
    }

    private boolean isStatement() {
        if (tokenizer.tokenType() != JackTokenizer.TokenType.KEYWORD) {
            return false;
        }
        JackTokenizer.Keyword kw = tokenizer.keyword();
        return kw == JackTokenizer.Keyword.LET ||
               kw == JackTokenizer.Keyword.IF ||
               kw == JackTokenizer.Keyword.WHILE ||
               kw == JackTokenizer.Keyword.DO ||
               kw == JackTokenizer.Keyword.RETURN;
    }

    private boolean isKeyword(String keyword) {
        return tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
               tokenizer.getCurrentToken().equals(keyword);
    }

    private boolean isKeywordConstant() {
        if (tokenizer.tokenType() != JackTokenizer.TokenType.KEYWORD) {
            return false;
        }
        JackTokenizer.Keyword kw = tokenizer.keyword();
        return kw == JackTokenizer.Keyword.TRUE ||
               kw == JackTokenizer.Keyword.FALSE ||
               kw == JackTokenizer.Keyword.NULL ||
               kw == JackTokenizer.Keyword.THIS;
    }

    private boolean isSymbol(char symbol) {
        return tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL &&
               tokenizer.symbol() == symbol;
    }

    private boolean isOp() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL &&
               OPS.indexOf(tokenizer.symbol()) >= 0;
    }

    private boolean isUnaryOp() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL &&
               UNARY_OPS.indexOf(tokenizer.symbol()) >= 0;
    }
}

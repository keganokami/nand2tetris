import java.io.*;

/**
 * CompilationEngine - Jack言語の構文解析エンジン
 *
 * 第10章の中核モジュール。再帰下降構文解析を用いて、
 * JackTokenizerが生成したトークン列を構文木に変換します。
 *
 * Jack言語の文法（BNF形式の簡略版）:
 *
 * class:          'class' className '{' classVarDec* subroutineDec* '}'
 * classVarDec:    ('static'|'field') type varName (',' varName)* ';'
 * type:           'int' | 'char' | 'boolean' | className
 * subroutineDec:  ('constructor'|'function'|'method') ('void'|type)
 *                 subroutineName '(' parameterList ')' subroutineBody
 * parameterList:  ((type varName) (',' type varName)*)?
 * subroutineBody: '{' varDec* statements '}'
 * varDec:         'var' type varName (',' varName)* ';'
 * statements:     statement*
 * statement:      letStatement | ifStatement | whileStatement | doStatement | returnStatement
 *
 * 式の文法:
 * expression:     term (op term)*
 * term:           integerConstant | stringConstant | keywordConstant |
 *                 varName | varName '[' expression ']' | subroutineCall |
 *                 '(' expression ')' | unaryOp term
 * subroutineCall: subroutineName '(' expressionList ')' |
 *                 (className|varName) '.' subroutineName '(' expressionList ')'
 * expressionList: (expression (',' expression)*)?
 * op:             '+' | '-' | '*' | '/' | '&' | '|' | '<' | '>' | '='
 * unaryOp:        '-' | '~'
 * keywordConstant: 'true' | 'false' | 'null' | 'this'
 */
public class CompilationEngine {

    private JackTokenizer tokenizer;
    private PrintWriter writer;
    private int indentLevel;

    // 演算子のセット
    private static final String OPS = "+-*/&|<>=";
    private static final String UNARY_OPS = "-~";

    /**
     * コンストラクタ - トークナイザと出力ストリームを初期化
     * @param tokenizer JackTokenizerインスタンス
     * @param outputFile 出力XMLファイル
     */
    public CompilationEngine(JackTokenizer tokenizer, File outputFile) throws IOException {
        this.tokenizer = tokenizer;
        this.writer = new PrintWriter(new FileWriter(outputFile));
        this.indentLevel = 0;
    }

    /**
     * コンパイルを開始（クラス全体をコンパイル）
     * 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() {
        writeOpenTag("class");

        // 'class'
        tokenizer.advance();
        writeKeyword();

        // className
        tokenizer.advance();
        writeIdentifier();

        // '{'
        tokenizer.advance();
        writeSymbol();

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
        writeSymbol();

        writeCloseTag("class");
        writer.close();
    }

    /**
     * クラス変数宣言をコンパイル
     * ('static'|'field') type varName (',' varName)* ';'
     */
    public void compileClassVarDec() {
        writeOpenTag("classVarDec");

        // 'static' | 'field'
        writeKeyword();

        // type
        tokenizer.advance();
        writeType();

        // varName
        tokenizer.advance();
        writeIdentifier();

        // (',' varName)*
        tokenizer.advance();
        while (isSymbol(',')) {
            writeSymbol();
            tokenizer.advance();
            writeIdentifier();
            tokenizer.advance();
        }

        // ';'
        writeSymbol();

        writeCloseTag("classVarDec");
        tokenizer.advance();
    }

    /**
     * サブルーチン（メソッド/関数/コンストラクタ）をコンパイル
     * ('constructor'|'function'|'method') ('void'|type)
     * subroutineName '(' parameterList ')' subroutineBody
     */
    public void compileSubroutine() {
        writeOpenTag("subroutineDec");

        // 'constructor' | 'function' | 'method'
        writeKeyword();

        // 'void' | type
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
            tokenizer.keyword() == JackTokenizer.Keyword.VOID) {
            writeKeyword();
        } else {
            writeType();
        }

        // subroutineName
        tokenizer.advance();
        writeIdentifier();

        // '('
        tokenizer.advance();
        writeSymbol();

        // parameterList
        tokenizer.advance();
        compileParameterList();

        // ')'
        writeSymbol();

        // subroutineBody
        tokenizer.advance();
        compileSubroutineBody();

        writeCloseTag("subroutineDec");
        tokenizer.advance();
    }

    /**
     * パラメータリストをコンパイル（括弧は含まない）
     * ((type varName) (',' type varName)*)?
     */
    public void compileParameterList() {
        writeOpenTag("parameterList");

        // パラメータがある場合
        if (!isSymbol(')')) {
            // type
            writeType();

            // varName
            tokenizer.advance();
            writeIdentifier();

            // (',' type varName)*
            tokenizer.advance();
            while (isSymbol(',')) {
                writeSymbol();

                // type
                tokenizer.advance();
                writeType();

                // varName
                tokenizer.advance();
                writeIdentifier();

                tokenizer.advance();
            }
        }

        writeCloseTag("parameterList");
    }

    /**
     * サブルーチンの本体をコンパイル
     * '{' varDec* statements '}'
     */
    public void compileSubroutineBody() {
        writeOpenTag("subroutineBody");

        // '{'
        writeSymbol();

        // varDec*
        tokenizer.advance();
        while (isVarDec()) {
            compileVarDec();
        }

        // statements
        compileStatements();

        // '}'
        writeSymbol();

        writeCloseTag("subroutineBody");
    }

    /**
     * ローカル変数宣言をコンパイル
     * 'var' type varName (',' varName)* ';'
     */
    public void compileVarDec() {
        writeOpenTag("varDec");

        // 'var'
        writeKeyword();

        // type
        tokenizer.advance();
        writeType();

        // varName
        tokenizer.advance();
        writeIdentifier();

        // (',' varName)*
        tokenizer.advance();
        while (isSymbol(',')) {
            writeSymbol();
            tokenizer.advance();
            writeIdentifier();
            tokenizer.advance();
        }

        // ';'
        writeSymbol();

        writeCloseTag("varDec");
        tokenizer.advance();
    }

    /**
     * 文の列をコンパイル
     * statement*
     */
    public void compileStatements() {
        writeOpenTag("statements");

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

        writeCloseTag("statements");
    }

    /**
     * let文をコンパイル
     * 'let' varName ('[' expression ']')? '=' expression ';'
     */
    public void compileLet() {
        writeOpenTag("letStatement");

        // 'let'
        writeKeyword();

        // varName
        tokenizer.advance();
        writeIdentifier();

        // ('[' expression ']')?
        tokenizer.advance();
        if (isSymbol('[')) {
            writeSymbol();
            tokenizer.advance();
            compileExpression();
            // ']'
            writeSymbol();
            tokenizer.advance();
        }

        // '='
        writeSymbol();

        // expression
        tokenizer.advance();
        compileExpression();

        // ';'
        writeSymbol();

        writeCloseTag("letStatement");
        tokenizer.advance();
    }

    /**
     * if文をコンパイル
     * 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     */
    public void compileIf() {
        writeOpenTag("ifStatement");

        // 'if'
        writeKeyword();

        // '('
        tokenizer.advance();
        writeSymbol();

        // expression
        tokenizer.advance();
        compileExpression();

        // ')'
        writeSymbol();

        // '{'
        tokenizer.advance();
        writeSymbol();

        // statements
        tokenizer.advance();
        compileStatements();

        // '}'
        writeSymbol();

        // ('else' '{' statements '}')?
        tokenizer.advance();
        if (isKeyword("else")) {
            writeKeyword();

            // '{'
            tokenizer.advance();
            writeSymbol();

            // statements
            tokenizer.advance();
            compileStatements();

            // '}'
            writeSymbol();

            tokenizer.advance();
        }

        writeCloseTag("ifStatement");
    }

    /**
     * while文をコンパイル
     * 'while' '(' expression ')' '{' statements '}'
     */
    public void compileWhile() {
        writeOpenTag("whileStatement");

        // 'while'
        writeKeyword();

        // '('
        tokenizer.advance();
        writeSymbol();

        // expression
        tokenizer.advance();
        compileExpression();

        // ')'
        writeSymbol();

        // '{'
        tokenizer.advance();
        writeSymbol();

        // statements
        tokenizer.advance();
        compileStatements();

        // '}'
        writeSymbol();

        writeCloseTag("whileStatement");
        tokenizer.advance();
    }

    /**
     * do文をコンパイル
     * 'do' subroutineCall ';'
     */
    public void compileDo() {
        writeOpenTag("doStatement");

        // 'do'
        writeKeyword();

        // subroutineCall
        tokenizer.advance();
        compileSubroutineCall();

        // ';'
        writeSymbol();

        writeCloseTag("doStatement");
        tokenizer.advance();
    }

    /**
     * return文をコンパイル
     * 'return' expression? ';'
     */
    public void compileReturn() {
        writeOpenTag("returnStatement");

        // 'return'
        writeKeyword();

        // expression?
        tokenizer.advance();
        if (!isSymbol(';')) {
            compileExpression();
        }

        // ';'
        writeSymbol();

        writeCloseTag("returnStatement");
        tokenizer.advance();
    }

    /**
     * 式をコンパイル
     * term (op term)*
     */
    public void compileExpression() {
        writeOpenTag("expression");

        // term
        compileTerm();

        // (op term)*
        while (isOp()) {
            writeSymbol();
            tokenizer.advance();
            compileTerm();
        }

        writeCloseTag("expression");
    }

    /**
     * 項をコンパイル
     * integerConstant | stringConstant | keywordConstant |
     * varName | varName '[' expression ']' | subroutineCall |
     * '(' expression ')' | unaryOp term
     */
    public void compileTerm() {
        writeOpenTag("term");

        if (tokenizer.tokenType() == JackTokenizer.TokenType.INT_CONST) {
            // integerConstant
            writeIntConstant();
            tokenizer.advance();
        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.STRING_CONST) {
            // stringConstant
            writeStringConstant();
            tokenizer.advance();
        } else if (isKeywordConstant()) {
            // keywordConstant: true | false | null | this
            writeKeyword();
            tokenizer.advance();
        } else if (isSymbol('(')) {
            // '(' expression ')'
            writeSymbol();
            tokenizer.advance();
            compileExpression();
            writeSymbol(); // ')'
            tokenizer.advance();
        } else if (isUnaryOp()) {
            // unaryOp term
            writeSymbol();
            tokenizer.advance();
            compileTerm();
        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.IDENTIFIER) {
            // varName | varName '[' expression ']' | subroutineCall
            // 次のトークンを見て判断
            String next = tokenizer.peekNext();
            if ("[".equals(next)) {
                // varName '[' expression ']'
                writeIdentifier();
                tokenizer.advance();
                writeSymbol(); // '['
                tokenizer.advance();
                compileExpression();
                writeSymbol(); // ']'
                tokenizer.advance();
            } else if ("(".equals(next) || ".".equals(next)) {
                // subroutineCall
                compileSubroutineCall();
            } else {
                // varName
                writeIdentifier();
                tokenizer.advance();
            }
        }

        writeCloseTag("term");
    }

    /**
     * サブルーチン呼び出しをコンパイル
     * subroutineName '(' expressionList ')' |
     * (className|varName) '.' subroutineName '(' expressionList ')'
     */
    private void compileSubroutineCall() {
        // subroutineName or className/varName
        writeIdentifier();

        tokenizer.advance();
        if (isSymbol('.')) {
            // (className|varName) '.' subroutineName
            writeSymbol();
            tokenizer.advance();
            writeIdentifier();
            tokenizer.advance();
        }

        // '('
        writeSymbol();

        // expressionList
        tokenizer.advance();
        compileExpressionList();

        // ')'
        writeSymbol();

        tokenizer.advance();
    }

    /**
     * 式のリストをコンパイル（括弧は含まない）
     * (expression (',' expression)*)?
     */
    public int compileExpressionList() {
        writeOpenTag("expressionList");
        int count = 0;

        // 式がある場合
        if (!isSymbol(')')) {
            compileExpression();
            count++;

            // (',' expression)*
            while (isSymbol(',')) {
                writeSymbol();
                tokenizer.advance();
                compileExpression();
                count++;
            }
        }

        writeCloseTag("expressionList");
        return count;
    }

    // ===== ヘルパーメソッド =====

    /**
     * 現在のトークンがクラス変数宣言の開始か
     */
    private boolean isClassVarDec() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
               (tokenizer.keyword() == JackTokenizer.Keyword.STATIC ||
                tokenizer.keyword() == JackTokenizer.Keyword.FIELD);
    }

    /**
     * 現在のトークンがサブルーチン宣言の開始か
     */
    private boolean isSubroutineDec() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
               (tokenizer.keyword() == JackTokenizer.Keyword.CONSTRUCTOR ||
                tokenizer.keyword() == JackTokenizer.Keyword.FUNCTION ||
                tokenizer.keyword() == JackTokenizer.Keyword.METHOD);
    }

    /**
     * 現在のトークンがローカル変数宣言の開始か
     */
    private boolean isVarDec() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
               tokenizer.keyword() == JackTokenizer.Keyword.VAR;
    }

    /**
     * 現在のトークンが文の開始か
     */
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

    /**
     * 現在のトークンが指定されたキーワードか
     */
    private boolean isKeyword(String keyword) {
        return tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD &&
               tokenizer.getCurrentToken().equals(keyword);
    }

    /**
     * 現在のトークンがキーワード定数か（true, false, null, this）
     */
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

    /**
     * 現在のトークンが指定された記号か
     */
    private boolean isSymbol(char symbol) {
        return tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL &&
               tokenizer.symbol() == symbol;
    }

    /**
     * 現在のトークンが演算子か
     */
    private boolean isOp() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL &&
               OPS.indexOf(tokenizer.symbol()) >= 0;
    }

    /**
     * 現在のトークンが単項演算子か
     */
    private boolean isUnaryOp() {
        return tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL &&
               UNARY_OPS.indexOf(tokenizer.symbol()) >= 0;
    }

    // ===== XML出力メソッド =====

    /**
     * 開始タグを出力
     */
    private void writeOpenTag(String tag) {
        writeIndent();
        writer.println("<" + tag + ">");
        indentLevel++;
    }

    /**
     * 終了タグを出力
     */
    private void writeCloseTag(String tag) {
        indentLevel--;
        writeIndent();
        writer.println("</" + tag + ">");
    }

    /**
     * インデントを出力
     */
    private void writeIndent() {
        for (int i = 0; i < indentLevel; i++) {
            writer.print("  ");
        }
    }

    /**
     * キーワードをXML出力
     */
    private void writeKeyword() {
        writeIndent();
        writer.println("<keyword> " + tokenizer.getCurrentToken() + " </keyword>");
    }

    /**
     * 記号をXML出力（特殊文字はエスケープ）
     */
    private void writeSymbol() {
        writeIndent();
        String symbol = escapeXML(String.valueOf(tokenizer.symbol()));
        writer.println("<symbol> " + symbol + " </symbol>");
    }

    /**
     * 識別子をXML出力
     */
    private void writeIdentifier() {
        writeIndent();
        writer.println("<identifier> " + tokenizer.identifier() + " </identifier>");
    }

    /**
     * 整数定数をXML出力
     */
    private void writeIntConstant() {
        writeIndent();
        writer.println("<integerConstant> " + tokenizer.intVal() + " </integerConstant>");
    }

    /**
     * 文字列定数をXML出力
     */
    private void writeStringConstant() {
        writeIndent();
        writer.println("<stringConstant> " + tokenizer.stringVal() + " </stringConstant>");
    }

    /**
     * 型をXML出力（キーワードまたは識別子）
     */
    private void writeType() {
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
            writeKeyword();
        } else {
            writeIdentifier();
        }
    }

    /**
     * XMLで特殊な意味を持つ文字をエスケープ
     */
    private String escapeXML(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * JackTokenizer - Jack言語の字句解析器
 *
 * 第10章の中核となるモジュール。Jackソースコードを読み込み、
 * トークン（字句）の列に分解します。
 *
 * Jack言語のトークンは以下の5種類:
 * 1. KEYWORD    - 予約語（class, method, function, etc.）
 * 2. SYMBOL     - 記号（{ } ( ) [ ] . , ; + - * / & | < > = ~）
 * 3. INT_CONST  - 整数定数（0〜32767）
 * 4. STRING_CONST - 文字列定数（"..."で囲まれた文字列）
 * 5. IDENTIFIER - 識別子（変数名、クラス名、関数名など）
 */
public class JackTokenizer {

    // トークンの種類を表す列挙型
    public enum TokenType {
        KEYWORD,        // 予約語
        SYMBOL,         // 記号
        INT_CONST,      // 整数定数
        STRING_CONST,   // 文字列定数
        IDENTIFIER      // 識別子
    }

    // Jack言語の予約語を表す列挙型
    public enum Keyword {
        CLASS, METHOD, FUNCTION, CONSTRUCTOR,
        INT, BOOLEAN, CHAR, VOID,
        VAR, STATIC, FIELD,
        LET, DO, IF, ELSE, WHILE, RETURN,
        TRUE, FALSE, NULL, THIS
    }

    // 予約語のセット（高速な検索用）
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "class", "constructor", "function", "method", "field", "static",
        "var", "int", "char", "boolean", "void", "true", "false", "null",
        "this", "let", "do", "if", "else", "while", "return"
    ));

    // 記号のセット
    private static final Set<Character> SYMBOLS = new HashSet<>(Arrays.asList(
        '{', '}', '(', ')', '[', ']', '.', ',', ';',
        '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'
    ));

    // ソースコード全体（コメント除去後）
    private String source;

    // 現在のトークンリストと位置
    private List<String> tokens;
    private int currentIndex;

    // 現在のトークン情報
    private String currentToken;
    private TokenType currentType;

    /**
     * コンストラクタ - ファイルを読み込み、トークン化の準備
     * @param inputFile 入力Jackファイル
     */
    public JackTokenizer(File inputFile) throws IOException {
        // ファイル全体を読み込む
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        // コメントを除去
        source = removeComments(sb.toString());

        // トークンに分割
        tokens = tokenize(source);
        currentIndex = -1;
        currentToken = null;
        currentType = null;
    }

    /**
     * コメントを除去する
     * - 行コメント: // から行末まで
     * - ブロックコメント: /* から *\/ まで
     * - APIドキュメント: /** から *\/ まで
     */
    private String removeComments(String input) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        boolean inString = false;

        while (i < input.length()) {
            // 文字列リテラル内はコメント処理しない
            if (input.charAt(i) == '"' && (i == 0 || input.charAt(i-1) != '\\')) {
                inString = !inString;
                result.append(input.charAt(i));
                i++;
                continue;
            }

            if (!inString && i + 1 < input.length()) {
                // 行コメント //
                if (input.charAt(i) == '/' && input.charAt(i + 1) == '/') {
                    // 行末まで読み飛ばす
                    while (i < input.length() && input.charAt(i) != '\n') {
                        i++;
                    }
                    continue;
                }
                // ブロックコメント /* ... */
                if (input.charAt(i) == '/' && input.charAt(i + 1) == '*') {
                    i += 2;
                    // */ が見つかるまで読み飛ばす
                    while (i + 1 < input.length() &&
                           !(input.charAt(i) == '*' && input.charAt(i + 1) == '/')) {
                        i++;
                    }
                    i += 2; // */ をスキップ
                    continue;
                }
            }

            result.append(input.charAt(i));
            i++;
        }

        return result.toString();
    }

    /**
     * ソースコードをトークンのリストに分割する
     */
    private List<String> tokenize(String source) {
        List<String> result = new ArrayList<>();
        int i = 0;

        while (i < source.length()) {
            char c = source.charAt(i);

            // 空白文字をスキップ
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // 記号
            if (SYMBOLS.contains(c)) {
                result.add(String.valueOf(c));
                i++;
                continue;
            }

            // 整数定数
            if (Character.isDigit(c)) {
                StringBuilder num = new StringBuilder();
                while (i < source.length() && Character.isDigit(source.charAt(i))) {
                    num.append(source.charAt(i));
                    i++;
                }
                result.add(num.toString());
                continue;
            }

            // 文字列定数
            if (c == '"') {
                StringBuilder str = new StringBuilder();
                str.append('"');
                i++; // 開始の " をスキップ
                while (i < source.length() && source.charAt(i) != '"') {
                    str.append(source.charAt(i));
                    i++;
                }
                str.append('"');
                i++; // 終了の " をスキップ
                result.add(str.toString());
                continue;
            }

            // 識別子または予約語
            if (Character.isLetter(c) || c == '_') {
                StringBuilder word = new StringBuilder();
                while (i < source.length() &&
                       (Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '_')) {
                    word.append(source.charAt(i));
                    i++;
                }
                result.add(word.toString());
                continue;
            }

            // 不明な文字（エラー）
            i++;
        }

        return result;
    }

    /**
     * 次のトークンがあるか確認
     * @return トークンが残っていればtrue
     */
    public boolean hasMoreTokens() {
        return currentIndex + 1 < tokens.size();
    }

    /**
     * 次のトークンに進む
     * hasMoreTokens()がtrueの場合のみ呼び出し可能
     */
    public void advance() {
        if (!hasMoreTokens()) {
            throw new IllegalStateException("No more tokens");
        }

        currentIndex++;
        currentToken = tokens.get(currentIndex);

        // トークンの種類を判定
        if (KEYWORDS.contains(currentToken)) {
            currentType = TokenType.KEYWORD;
        } else if (currentToken.length() == 1 && SYMBOLS.contains(currentToken.charAt(0))) {
            currentType = TokenType.SYMBOL;
        } else if (currentToken.startsWith("\"")) {
            currentType = TokenType.STRING_CONST;
        } else if (Character.isDigit(currentToken.charAt(0))) {
            currentType = TokenType.INT_CONST;
        } else {
            currentType = TokenType.IDENTIFIER;
        }
    }

    /**
     * 現在のトークンの種類を返す
     */
    public TokenType tokenType() {
        return currentType;
    }

    /**
     * 現在のトークンが予約語の場合、その種類を返す
     * tokenType()がKEYWORDの場合のみ呼び出し可能
     */
    public Keyword keyword() {
        if (currentType != TokenType.KEYWORD) {
            throw new IllegalStateException("Current token is not a keyword");
        }
        return Keyword.valueOf(currentToken.toUpperCase());
    }

    /**
     * 現在のトークンが記号の場合、その文字を返す
     * tokenType()がSYMBOLの場合のみ呼び出し可能
     */
    public char symbol() {
        if (currentType != TokenType.SYMBOL) {
            throw new IllegalStateException("Current token is not a symbol");
        }
        return currentToken.charAt(0);
    }

    /**
     * 現在のトークンが識別子の場合、その文字列を返す
     * tokenType()がIDENTIFIERの場合のみ呼び出し可能
     */
    public String identifier() {
        if (currentType != TokenType.IDENTIFIER) {
            throw new IllegalStateException("Current token is not an identifier");
        }
        return currentToken;
    }

    /**
     * 現在のトークンが整数定数の場合、その値を返す
     * tokenType()がINT_CONSTの場合のみ呼び出し可能
     */
    public int intVal() {
        if (currentType != TokenType.INT_CONST) {
            throw new IllegalStateException("Current token is not an integer constant");
        }
        return Integer.parseInt(currentToken);
    }

    /**
     * 現在のトークンが文字列定数の場合、その値を返す（引用符なし）
     * tokenType()がSTRING_CONSTの場合のみ呼び出し可能
     */
    public String stringVal() {
        if (currentType != TokenType.STRING_CONST) {
            throw new IllegalStateException("Current token is not a string constant");
        }
        // 前後の引用符を除去
        return currentToken.substring(1, currentToken.length() - 1);
    }

    /**
     * 現在のトークンをそのまま返す（デバッグ用）
     */
    public String getCurrentToken() {
        return currentToken;
    }

    /**
     * 先読み：次のトークンを見る（位置は変えない）
     */
    public String peekNext() {
        if (currentIndex + 1 < tokens.size()) {
            return tokens.get(currentIndex + 1);
        }
        return null;
    }

    /**
     * 先読み：2つ先のトークンを見る（位置は変えない）
     */
    public String peekNext2() {
        if (currentIndex + 2 < tokens.size()) {
            return tokens.get(currentIndex + 2);
        }
        return null;
    }

    /**
     * トークン化されたリストをXML形式で出力（デバッグ・テスト用）
     */
    public String toXML() {
        StringBuilder xml = new StringBuilder();
        xml.append("<tokens>\n");

        // 現在の位置を保存
        int savedIndex = currentIndex;
        currentIndex = -1;

        while (hasMoreTokens()) {
            advance();
            switch (currentType) {
                case KEYWORD:
                    xml.append("<keyword> ").append(currentToken).append(" </keyword>\n");
                    break;
                case SYMBOL:
                    String symbolXml = escapeXML(currentToken);
                    xml.append("<symbol> ").append(symbolXml).append(" </symbol>\n");
                    break;
                case INT_CONST:
                    xml.append("<integerConstant> ").append(currentToken).append(" </integerConstant>\n");
                    break;
                case STRING_CONST:
                    xml.append("<stringConstant> ").append(stringVal()).append(" </stringConstant>\n");
                    break;
                case IDENTIFIER:
                    xml.append("<identifier> ").append(currentToken).append(" </identifier>\n");
                    break;
            }
        }

        xml.append("</tokens>\n");

        // 位置を復元
        currentIndex = savedIndex;
        if (savedIndex >= 0) {
            currentToken = tokens.get(savedIndex);
        }

        return xml.toString();
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

    /**
     * トークナイザをリセット（最初から再度トークン化）
     */
    public void reset() {
        currentIndex = -1;
        currentToken = null;
        currentType = null;
    }
}

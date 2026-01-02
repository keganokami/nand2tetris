import java.io.*;

/**
 * VMWriter - VMコード出力モジュール
 *
 * 第11章のモジュール。VM言語のコマンドを.vmファイルに出力します。
 * CompilationEngineがVMWriterを使ってVMコードを生成します。
 *
 * VMセグメント:
 * - constant: 定数（push only）
 * - argument: 関数の引数
 * - local: ローカル変数
 * - static: 静的変数
 * - this: オブジェクトのフィールド
 * - that: 配列要素
 * - pointer: this/thatベースアドレス
 * - temp: 一時的な値（8ワード）
 *
 * VM算術コマンド:
 * - add, sub, neg: 算術演算
 * - eq, gt, lt: 比較演算
 * - and, or, not: 論理演算
 */
public class VMWriter {

    /**
     * VMセグメントを表す列挙型
     */
    public enum Segment {
        CONSTANT("constant"),
        ARGUMENT("argument"),
        LOCAL("local"),
        STATIC("static"),
        THIS("this"),
        THAT("that"),
        POINTER("pointer"),
        TEMP("temp");

        private final String name;

        Segment(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * 文字列からSegmentを取得
         */
        public static Segment fromString(String s) {
            for (Segment seg : values()) {
                if (seg.name.equals(s)) {
                    return seg;
                }
            }
            throw new IllegalArgumentException("Unknown segment: " + s);
        }
    }

    /**
     * VM算術コマンドを表す列挙型
     */
    public enum Command {
        ADD("add"),
        SUB("sub"),
        NEG("neg"),
        EQ("eq"),
        GT("gt"),
        LT("lt"),
        AND("and"),
        OR("or"),
        NOT("not");

        private final String name;

        Command(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private PrintWriter writer;

    /**
     * コンストラクタ - 出力ファイルを開く
     * @param outputFile 出力.vmファイル
     */
    public VMWriter(File outputFile) throws IOException {
        this.writer = new PrintWriter(new FileWriter(outputFile));
    }

    /**
     * pushコマンドを出力
     * @param segment セグメント
     * @param index インデックス
     */
    public void writePush(Segment segment, int index) {
        writer.println("push " + segment + " " + index);
    }

    /**
     * pushコマンドを出力（セグメント名を文字列で指定）
     * @param segment セグメント名
     * @param index インデックス
     */
    public void writePush(String segment, int index) {
        writer.println("push " + segment + " " + index);
    }

    /**
     * popコマンドを出力
     * @param segment セグメント
     * @param index インデックス
     */
    public void writePop(Segment segment, int index) {
        writer.println("pop " + segment + " " + index);
    }

    /**
     * popコマンドを出力（セグメント名を文字列で指定）
     * @param segment セグメント名
     * @param index インデックス
     */
    public void writePop(String segment, int index) {
        writer.println("pop " + segment + " " + index);
    }

    /**
     * 算術コマンドを出力
     * @param command 算術コマンド
     */
    public void writeArithmetic(Command command) {
        writer.println(command);
    }

    /**
     * 算術コマンドを出力（文字列で指定）
     * @param command コマンド名
     */
    public void writeArithmetic(String command) {
        writer.println(command);
    }

    /**
     * labelコマンドを出力
     * @param label ラベル名
     */
    public void writeLabel(String label) {
        writer.println("label " + label);
    }

    /**
     * gotoコマンドを出力
     * @param label ジャンプ先ラベル
     */
    public void writeGoto(String label) {
        writer.println("goto " + label);
    }

    /**
     * if-gotoコマンドを出力
     * @param label ジャンプ先ラベル
     */
    public void writeIf(String label) {
        writer.println("if-goto " + label);
    }

    /**
     * callコマンドを出力
     * @param name 関数名
     * @param nArgs 引数の数
     */
    public void writeCall(String name, int nArgs) {
        writer.println("call " + name + " " + nArgs);
    }

    /**
     * functionコマンドを出力
     * @param name 関数名
     * @param nLocals ローカル変数の数
     */
    public void writeFunction(String name, int nLocals) {
        writer.println("function " + name + " " + nLocals);
    }

    /**
     * returnコマンドを出力
     */
    public void writeReturn() {
        writer.println("return");
    }

    /**
     * 出力を閉じる
     */
    public void close() {
        writer.close();
    }

    /**
     * コメントを出力（デバッグ用）
     * @param comment コメント文字列
     */
    public void writeComment(String comment) {
        writer.println("// " + comment);
    }
}

import java.io.*;

/**
 * CodeWriter - VMコマンドをHackアセンブリコードに変換するクラス
 *
 * 役割：
 * - VMコマンドを受け取り、対応するHackアセンブリコードを生成
 * - 生成したアセンブリコードを.asmファイルに書き込む
 *
 * Hackコンピュータのメモリマップ：
 * - SP (Stack Pointer): RAM[0] - スタックの次の空き位置を指す
 * - LCL (Local): RAM[1] - localセグメントのベースアドレス
 * - ARG (Argument): RAM[2] - argumentセグメントのベースアドレス
 * - THIS: RAM[3] - thisセグメントのベースアドレス
 * - THAT: RAM[4] - thatセグメントのベースアドレス
 * - Temp: RAM[5-12] - tempセグメント（8つの固定位置）
 * - Static: RAM[16-255] - staticセグメント
 * - Stack: RAM[256-2047] - スタック領域
 */
public class CodeWriter {
    private PrintWriter writer;
    private String currentFileName;
    private String currentFunctionName; // 現在の関数名（ラベルのスコープ用）
    private int labelCounter; // 一意なラベルを生成するためのカウンタ
    private int returnCounter; // return用の一意なラベルカウンタ

    /**
     * コンストラクタ
     * @param file 出力する.asmファイル
     */
    public CodeWriter(File file) throws IOException {
        writer = new PrintWriter(new FileWriter(file));
        labelCounter = 0;
        returnCounter = 0;
        currentFunctionName = ""; // 初期状態は関数外
    }

    /**
     * 現在翻訳中の.vmファイル名を設定
     * staticセグメントのシンボル生成に使用
     * @param fileName ファイル名（拡張子なし）
     */
    public void setFileName(String fileName) {
        this.currentFileName = fileName;
    }

    /**
     * 算術/論理コマンドをアセンブリコードに変換
     *
     * スタック操作の基本：
     * - スタックはRAM[256]から始まる
     * - SPはスタックの次の空き位置を指す
     * - pushは値をスタックに積み、SPをインクリメント
     * - popはSPをデクリメントし、その位置から値を取り出す
     *
     * @param command 算術コマンド（add, sub, neg, eq, gt, lt, and, or, not）
     */
    public void writeArithmetic(String command) {
        writer.println("// " + command);

        switch (command) {
            case "add":
                // スタックトップの2つの値を取り出して加算
                // y = pop(), x = pop(), push(x + y)
                writeBinaryOperation("D+M");
                break;

            case "sub":
                // スタックトップの2つの値を取り出して減算
                // y = pop(), x = pop(), push(x - y)
                writeBinaryOperation("M-D");
                break;

            case "neg":
                // スタックトップの値を負数に
                // x = pop(), push(-x)
                writeUnaryOperation("-M");
                break;

            case "eq":
                // 等しいかチェック（x == y なら -1、そうでなければ 0）
                writeComparisonOperation("JEQ");
                break;

            case "gt":
                // より大きいかチェック（x > y なら -1、そうでなければ 0）
                writeComparisonOperation("JGT");
                break;

            case "lt":
                // より小さいかチェック（x < y なら -1、そうでなければ 0）
                writeComparisonOperation("JLT");
                break;

            case "and":
                // ビット単位のAND
                // y = pop(), x = pop(), push(x & y)
                writeBinaryOperation("D&M");
                break;

            case "or":
                // ビット単位のOR
                // y = pop(), x = pop(), push(x | y)
                writeBinaryOperation("D|M");
                break;

            case "not":
                // ビット単位のNOT
                // x = pop(), push(!x)
                writeUnaryOperation("!M");
                break;

            default:
                throw new IllegalArgumentException("Unknown arithmetic command: " + command);
        }
    }

    /**
     * 二項演算のアセンブリコードを生成
     * スタックから2つの値を取り出し、演算を実行し、結果をスタックに積む
     * @param operation 演算子（例："D+M", "M-D", "D&M"）
     */
    private void writeBinaryOperation(String operation) {
        writer.println("@SP");
        writer.println("AM=M-1");  // SP--して、その位置に移動（2つ目の値）
        writer.println("D=M");     // D = 2つ目の値
        writer.println("A=A-1");   // 1つ前の位置に移動（1つ目の値）
        writer.println("M=" + operation);  // 演算結果をスタックに保存
    }

    /**
     * 単項演算のアセンブリコードを生成
     * スタックトップの値に演算を適用
     * @param operation 演算子（例："-M", "!M"）
     */
    private void writeUnaryOperation(String operation) {
        writer.println("@SP");
        writer.println("A=M-1");   // スタックトップの位置に移動
        writer.println("M=" + operation);  // 演算結果をスタックに保存
    }

    /**
     * 比較演算のアセンブリコードを生成
     * スタックから2つの値を取り出し、比較し、結果（-1 or 0）をスタックに積む
     * @param jumpType ジャンプタイプ（JEQ, JGT, JLT）
     */
    private void writeComparisonOperation(String jumpType) {
        String trueLabel = "TRUE_" + labelCounter;
        String endLabel = "END_" + labelCounter;
        labelCounter++;

        writer.println("@SP");
        writer.println("AM=M-1");  // SP--して、2つ目の値の位置に移動
        writer.println("D=M");     // D = 2つ目の値
        writer.println("A=A-1");   // 1つ前の位置に移動（1つ目の値）
        writer.println("D=M-D");   // D = 1つ目の値 - 2つ目の値
        writer.println("@" + trueLabel);
        writer.println("D;" + jumpType);  // 条件が真ならジャンプ

        // 偽の場合：0をスタックに積む
        writer.println("@SP");
        writer.println("A=M-1");
        writer.println("M=0");
        writer.println("@" + endLabel);
        writer.println("0;JMP");

        // 真の場合：-1をスタックに積む
        writer.println("(" + trueLabel + ")");
        writer.println("@SP");
        writer.println("A=M-1");
        writer.println("M=-1");

        writer.println("(" + endLabel + ")");
    }

    /**
     * push/popコマンドをアセンブリコードに変換
     *
     * メモリセグメント：
     * - constant: 定数（0-32767）
     * - local: ローカル変数（LCLベース）
     * - argument: 関数の引数（ARGベース）
     * - this: オブジェクトのフィールド（THISベース）
     * - that: 配列の要素（THATベース）
     * - temp: 一時変数（RAM[5-12]）
     * - pointer: thisとthatのポインタ（RAM[3-4]）
     * - static: 静的変数（ファイル名.インデックス）
     *
     * @param command C_PUSH または C_POP
     * @param segment メモリセグメント
     * @param index インデックス
     */
    public void writePushPop(Parser.CommandType command, String segment, int index) {
        writer.println("// " + (command == Parser.CommandType.C_PUSH ? "push" : "pop")
                       + " " + segment + " " + index);

        if (command == Parser.CommandType.C_PUSH) {
            writePush(segment, index);
        } else {
            writePop(segment, index);
        }
    }

    /**
     * pushコマンドのアセンブリコードを生成
     * 指定されたセグメントの値をスタックに積む
     */
    private void writePush(String segment, int index) {
        switch (segment) {
            case "constant":
                // 定数をスタックに積む
                writer.println("@" + index);
                writer.println("D=A");     // D = 定数値
                break;

            case "local":
                writePushFromSegment("LCL", index);
                break;

            case "argument":
                writePushFromSegment("ARG", index);
                break;

            case "this":
                writePushFromSegment("THIS", index);
                break;

            case "that":
                writePushFromSegment("THAT", index);
                break;

            case "temp":
                // tempセグメント（RAM[5-12]）
                writer.println("@" + (5 + index));
                writer.println("D=M");
                break;

            case "pointer":
                // pointer 0 = THIS (RAM[3]), pointer 1 = THAT (RAM[4])
                writer.println("@" + (3 + index));
                writer.println("D=M");
                break;

            case "static":
                // 静的変数（ファイル名.インデックス）
                writer.println("@" + currentFileName + "." + index);
                writer.println("D=M");
                break;

            default:
                throw new IllegalArgumentException("Unknown segment: " + segment);
        }

        // Dレジスタの値をスタックに積む
        writer.println("@SP");
        writer.println("A=M");
        writer.println("M=D");     // *SP = D
        writer.println("@SP");
        writer.println("M=M+1");   // SP++
    }

    /**
     * ポインタベースのセグメントからpushする共通処理
     */
    private void writePushFromSegment(String segmentPointer, int index) {
        writer.println("@" + segmentPointer);
        writer.println("D=M");     // D = セグメントのベースアドレス
        writer.println("@" + index);
        writer.println("A=D+A");   // A = ベースアドレス + インデックス
        writer.println("D=M");     // D = *(ベースアドレス + インデックス)
    }

    /**
     * popコマンドのアセンブリコードを生成
     * スタックから値を取り出して指定されたセグメントに格納
     */
    private void writePop(String segment, int index) {
        switch (segment) {
            case "local":
                writePopToSegment("LCL", index);
                break;

            case "argument":
                writePopToSegment("ARG", index);
                break;

            case "this":
                writePopToSegment("THIS", index);
                break;

            case "that":
                writePopToSegment("THAT", index);
                break;

            case "temp":
                // tempセグメント（RAM[5-12]）
                writer.println("@SP");
                writer.println("AM=M-1");  // SP--して、その位置に移動
                writer.println("D=M");     // D = スタックトップの値
                writer.println("@" + (5 + index));
                writer.println("M=D");
                break;

            case "pointer":
                // pointer 0 = THIS (RAM[3]), pointer 1 = THAT (RAM[4])
                writer.println("@SP");
                writer.println("AM=M-1");
                writer.println("D=M");
                writer.println("@" + (3 + index));
                writer.println("M=D");
                break;

            case "static":
                // 静的変数（ファイル名.インデックス）
                writer.println("@SP");
                writer.println("AM=M-1");
                writer.println("D=M");
                writer.println("@" + currentFileName + "." + index);
                writer.println("M=D");
                break;

            default:
                throw new IllegalArgumentException("Unknown segment: " + segment);
        }
    }

    /**
     * ポインタベースのセグメントにpopする共通処理
     */
    private void writePopToSegment(String segmentPointer, int index) {
        // まず目的のアドレスを計算してR13に保存
        writer.println("@" + segmentPointer);
        writer.println("D=M");     // D = セグメントのベースアドレス
        writer.println("@" + index);
        writer.println("D=D+A");   // D = ベースアドレス + インデックス
        writer.println("@R13");
        writer.println("M=D");     // R13 = 目的のアドレス

        // スタックから値をpop
        writer.println("@SP");
        writer.println("AM=M-1");  // SP--して、その位置に移動
        writer.println("D=M");     // D = スタックトップの値

        // R13に保存したアドレスに値を格納
        writer.println("@R13");
        writer.println("A=M");
        writer.println("M=D");
    }

    // ========================================
    // プログラムフロー制御（プロジェクト8）
    // ========================================

    /**
     * labelコマンドをアセンブリコードに変換
     *
     * VMのラベルは関数スコープを持つため、
     * 「関数名$ラベル名」の形式で一意なアセンブリラベルを生成
     *
     * @param label ラベル名
     */
    public void writeLabel(String label) {
        writer.println("// label " + label);
        writer.println("(" + currentFunctionName + "$" + label + ")");
    }

    /**
     * gotoコマンドをアセンブリコードに変換
     *
     * 無条件ジャンプ：指定されたラベルに無条件でジャンプ
     *
     * @param label ジャンプ先のラベル名
     */
    public void writeGoto(String label) {
        writer.println("// goto " + label);
        writer.println("@" + currentFunctionName + "$" + label);
        writer.println("0;JMP");
    }

    /**
     * if-gotoコマンドをアセンブリコードに変換
     *
     * 条件付きジャンプ：スタックトップの値をpopし、
     * その値が0でなければ（true）指定されたラベルにジャンプ
     *
     * スタック操作：
     * 1. SPをデクリメント
     * 2. スタックトップの値を取得
     * 3. 値が0でなければジャンプ
     *
     * @param label ジャンプ先のラベル名
     */
    public void writeIf(String label) {
        writer.println("// if-goto " + label);
        writer.println("@SP");
        writer.println("AM=M-1");    // SP--して、その位置に移動
        writer.println("D=M");       // D = スタックトップの値
        writer.println("@" + currentFunctionName + "$" + label);
        writer.println("D;JNE");     // D != 0 ならジャンプ
    }

    // ========================================
    // 関数呼び出し（プロジェクト8）
    // ========================================

    /**
     * functionコマンドをアセンブリコードに変換
     *
     * 関数の開始点を定義し、ローカル変数領域を初期化
     *
     * 処理：
     * 1. 関数のエントリポイント（ラベル）を宣言
     * 2. nLocals個のローカル変数を0で初期化（スタックにpush）
     *
     * @param functionName 関数名
     * @param nLocals ローカル変数の数
     */
    public void writeFunction(String functionName, int nLocals) {
        writer.println("// function " + functionName + " " + nLocals);
        currentFunctionName = functionName;  // 現在の関数名を更新
        writer.println("(" + functionName + ")");

        // ローカル変数を0で初期化
        for (int i = 0; i < nLocals; i++) {
            writer.println("@SP");
            writer.println("A=M");
            writer.println("M=0");      // *SP = 0
            writer.println("@SP");
            writer.println("M=M+1");    // SP++
        }
    }

    /**
     * callコマンドをアセンブリコードに変換
     *
     * 関数呼び出しの処理：
     * 1. return-addressをスタックにpush
     * 2. 呼び出し元のLCLをスタックにpush
     * 3. 呼び出し元のARGをスタックにpush
     * 4. 呼び出し元のTHISをスタックにpush
     * 5. 呼び出し元のTHATをスタックにpush
     * 6. ARG = SP - 5 - nArgs（引数の先頭位置を設定）
     * 7. LCL = SP（ローカルセグメントの開始位置を設定）
     * 8. 呼び出し先関数にジャンプ
     * 9. return-addressラベルを宣言（関数から戻ってきた時の位置）
     *
     * @param functionName 呼び出す関数名
     * @param nArgs 引数の数
     */
    public void writeCall(String functionName, int nArgs) {
        String returnLabel = "RETURN_" + returnCounter;
        returnCounter++;

        writer.println("// call " + functionName + " " + nArgs);

        // 1. return-addressをpush
        writer.println("@" + returnLabel);
        writer.println("D=A");
        pushD();

        // 2. LCLをpush
        writer.println("@LCL");
        writer.println("D=M");
        pushD();

        // 3. ARGをpush
        writer.println("@ARG");
        writer.println("D=M");
        pushD();

        // 4. THISをpush
        writer.println("@THIS");
        writer.println("D=M");
        pushD();

        // 5. THATをpush
        writer.println("@THAT");
        writer.println("D=M");
        pushD();

        // 6. ARG = SP - 5 - nArgs
        writer.println("@SP");
        writer.println("D=M");
        writer.println("@" + (5 + nArgs));
        writer.println("D=D-A");
        writer.println("@ARG");
        writer.println("M=D");

        // 7. LCL = SP
        writer.println("@SP");
        writer.println("D=M");
        writer.println("@LCL");
        writer.println("M=D");

        // 8. 関数にジャンプ
        writer.println("@" + functionName);
        writer.println("0;JMP");

        // 9. return-addressラベル
        writer.println("(" + returnLabel + ")");
    }

    /**
     * returnコマンドをアセンブリコードに変換
     *
     * 関数からの復帰処理：
     * 1. endFrame = LCL（LCLの値を一時保存）
     * 2. retAddr = *(endFrame - 5)（戻りアドレスを取得）
     * 3. *ARG = pop()（戻り値を引数0の位置に格納）
     * 4. SP = ARG + 1（SPを戻り値の次の位置に設定）
     * 5. THAT = *(endFrame - 1)（呼び出し元のTHATを復元）
     * 6. THIS = *(endFrame - 2)（呼び出し元のTHISを復元）
     * 7. ARG = *(endFrame - 3)（呼び出し元のARGを復元）
     * 8. LCL = *(endFrame - 4)（呼び出し元のLCLを復元）
     * 9. 戻りアドレスにジャンプ
     */
    public void writeReturn() {
        writer.println("// return");

        // 1. endFrame (R13) = LCL
        writer.println("@LCL");
        writer.println("D=M");
        writer.println("@R13");     // R13 = endFrame
        writer.println("M=D");

        // 2. retAddr (R14) = *(endFrame - 5)
        writer.println("@5");
        writer.println("A=D-A");    // A = endFrame - 5
        writer.println("D=M");
        writer.println("@R14");     // R14 = retAddr
        writer.println("M=D");

        // 3. *ARG = pop()（戻り値を設定）
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("@ARG");
        writer.println("A=M");
        writer.println("M=D");

        // 4. SP = ARG + 1
        writer.println("@ARG");
        writer.println("D=M+1");
        writer.println("@SP");
        writer.println("M=D");

        // 5. THAT = *(endFrame - 1)
        writer.println("@R13");
        writer.println("AM=M-1");   // endFrame--して、その位置に移動
        writer.println("D=M");
        writer.println("@THAT");
        writer.println("M=D");

        // 6. THIS = *(endFrame - 2)
        writer.println("@R13");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("@THIS");
        writer.println("M=D");

        // 7. ARG = *(endFrame - 3)
        writer.println("@R13");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("@ARG");
        writer.println("M=D");

        // 8. LCL = *(endFrame - 4)
        writer.println("@R13");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("@LCL");
        writer.println("M=D");

        // 9. 戻りアドレスにジャンプ
        writer.println("@R14");
        writer.println("A=M");
        writer.println("0;JMP");
    }

    /**
     * Dレジスタの値をスタックにpushするヘルパーメソッド
     */
    private void pushD() {
        writer.println("@SP");
        writer.println("A=M");
        writer.println("M=D");
        writer.println("@SP");
        writer.println("M=M+1");
    }

    // ========================================
    // ブートストラップコード（プロジェクト8）
    // ========================================

    /**
     * ブートストラップコードを出力
     *
     * VMプログラムの起動時に実行されるコード：
     * 1. SP = 256（スタックポインタの初期化）
     * 2. call Sys.init（Sys.init関数を呼び出し）
     *
     * 注意：これはプログラム全体の最初に1回だけ出力する
     */
    public void writeBootstrap() {
        writer.println("// Bootstrap code");

        // SP = 256
        writer.println("@256");
        writer.println("D=A");
        writer.println("@SP");
        writer.println("M=D");

        // call Sys.init
        writeCall("Sys.init", 0);
    }

    /**
     * プログラムの最後に無限ループを追加
     * CPUが暴走しないようにするための終了処理
     */
    public void writeInfiniteLoop() {
        writer.println("// 無限ループ（プログラム終了）");
        writer.println("(END)");
        writer.println("@END");
        writer.println("0;JMP");
    }

    /**
     * ライターを閉じる
     */
    public void close() {
        writer.close();
    }
}

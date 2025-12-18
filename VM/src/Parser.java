import java.io.*;
import java.util.*;

/**
 * Parser - VMコマンドを解析するクラス
 *
 * 役割：
 * - .vmファイルを1行ずつ読み込む
 * - コメントと空白を除去
 * - 各VMコマンドを解析してコマンドタイプと引数を抽出
 */
public class Parser {
    private BufferedReader reader;
    private String currentCommand;
    private String nextCommand;

    // VMコマンドのタイプ
    public enum CommandType {
        C_ARITHMETIC,  // 算術/論理演算コマンド
        C_PUSH,        // pushコマンド
        C_POP,         // popコマンド
        C_LABEL,       // labelコマンド (プロジェクト8で使用)
        C_GOTO,        // gotoコマンド (プロジェクト8で使用)
        C_IF,          // if-gotoコマンド (プロジェクト8で使用)
        C_FUNCTION,    // functionコマンド (プロジェクト8で使用)
        C_RETURN,      // returnコマンド (プロジェクト8で使用)
        C_CALL         // callコマンド (プロジェクト8で使用)
    }

    /**
     * コンストラクタ
     * @param file 読み込む.vmファイル
     */
    public Parser(File file) throws IOException {
        reader = new BufferedReader(new FileReader(file));
        currentCommand = null;
        nextCommand = null;
        readNextCommand(); // 最初のコマンドをnextCommandに読み込む
    }

    /**
     * まだコマンドが残っているか確認
     * @return まだコマンドがあればtrue
     */
    public boolean hasMoreCommands() {
        return nextCommand != null;
    }

    /**
     * 次のコマンドを読み込んで現在のコマンドにする
     * hasMoreCommands()がtrueの時のみ呼び出すこと
     */
    public void advance() throws IOException {
        if (!hasMoreCommands()) {
            throw new IllegalStateException("No more commands to read");
        }
        currentCommand = nextCommand;
        readNextCommand();
    }

    /**
     * ファイルから次の有効なコマンドを読み込む
     * コメントと空行をスキップ
     */
    private void readNextCommand() throws IOException {
        nextCommand = null;

        String line;
        while ((line = reader.readLine()) != null) {
            // コメントを除去
            int commentIndex = line.indexOf("//");
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex);
            }

            // 空白を除去
            line = line.trim();

            // 空行でなければ次のコマンドとして保存
            if (!line.isEmpty()) {
                nextCommand = line;
                break;
            }
        }
    }

    /**
     * 現在のコマンドのタイプを返す
     * @return コマンドタイプ
     */
    public CommandType commandType() {
        if (currentCommand == null) {
            throw new IllegalStateException("No current command");
        }

        String[] parts = currentCommand.split("\\s+");
        String command = parts[0];

        switch (command) {
            case "push":
                return CommandType.C_PUSH;
            case "pop":
                return CommandType.C_POP;
            case "label":
                return CommandType.C_LABEL;
            case "goto":
                return CommandType.C_GOTO;
            case "if-goto":
                return CommandType.C_IF;
            case "function":
                return CommandType.C_FUNCTION;
            case "call":
                return CommandType.C_CALL;
            case "return":
                return CommandType.C_RETURN;
            default:
                // add, sub, neg, eq, gt, lt, and, or, not
                return CommandType.C_ARITHMETIC;
        }
    }

    /**
     * 現在のコマンドの第1引数を返す
     * C_ARITHMETICの場合、コマンド自体（add, subなど）を返す
     * @return 第1引数
     */
    public String arg1() {
        CommandType type = commandType();

        if (type == CommandType.C_RETURN) {
            throw new IllegalStateException("arg1() should not be called for C_RETURN");
        }

        if (type == CommandType.C_ARITHMETIC) {
            // 算術コマンドの場合、コマンド自体を返す
            return currentCommand.split("\\s+")[0];
        } else {
            // それ以外の場合、第1引数を返す
            String[] parts = currentCommand.split("\\s+");
            return parts.length > 1 ? parts[1] : "";
        }
    }

    /**
     * 現在のコマンドの第2引数を返す
     * C_PUSH, C_POP, C_FUNCTION, C_CALLの場合のみ呼び出すこと
     * @return 第2引数（整数）
     */
    public int arg2() {
        CommandType type = commandType();

        if (type != CommandType.C_PUSH &&
            type != CommandType.C_POP &&
            type != CommandType.C_FUNCTION &&
            type != CommandType.C_CALL) {
            throw new IllegalStateException("arg2() called for invalid command type");
        }

        String[] parts = currentCommand.split("\\s+");
        return Integer.parseInt(parts[2]);
    }

    /**
     * リーダーを閉じる
     */
    public void close() throws IOException {
        reader.close();
    }
}

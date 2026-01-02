import java.io.*;
import java.util.*;

/**
 * JackCompiler - Jack言語コンパイラ メインクラス
 *
 * 第11章のエントリーポイント。Jackファイル（またはディレクトリ）を受け取り、
 * 各ファイルに対してコンパイルを行い、VMコードを生成します。
 *
 * コンパイルの流れ:
 * 1. JackTokenizer: ソースコードをトークンに分解
 * 2. CompilationEngineVM: トークンを解析しながらVMコードを生成
 *    - SymbolTable: 変数を管理
 *    - VMWriter: VMコードを出力
 *
 * 使用方法:
 *   java JackCompiler <input>
 *
 *   <input>は以下のいずれか:
 *   - 単一の.jackファイルへのパス
 *   - .jackファイルを含むディレクトリへのパス
 *
 * 出力:
 *   各.jackファイルに対して:
 *   - Xxx.vm: コンパイル結果（VMコード）
 */
public class JackCompiler {

    /**
     * メインエントリーポイント
     */
    public static void main(String[] args) {
        // 引数チェック
        if (args.length != 1) {
            System.out.println("使用方法: java JackCompiler <source>");
            System.out.println("  <source>: .jackファイルまたはディレクトリ");
            System.exit(1);
        }

        File input = new File(args[0]);
        if (!input.exists()) {
            System.err.println("エラー: ファイルまたはディレクトリが見つかりません: " + args[0]);
            System.exit(1);
        }

        try {
            // 処理対象の.jackファイルを収集
            List<File> jackFiles = new ArrayList<>();

            if (input.isFile()) {
                if (!input.getName().endsWith(".jack")) {
                    System.err.println("エラー: .jackファイルを指定してください");
                    System.exit(1);
                }
                jackFiles.add(input);
            } else if (input.isDirectory()) {
                File[] files = input.listFiles((dir, name) -> name.endsWith(".jack"));
                if (files == null || files.length == 0) {
                    System.err.println("エラー: ディレクトリ内に.jackファイルがありません");
                    System.exit(1);
                }
                jackFiles.addAll(Arrays.asList(files));
                // ファイル名でソート
                jackFiles.sort(Comparator.comparing(File::getName));
            }

            System.out.println("Jack Compiler");
            System.out.println("=============");

            // 各ファイルをコンパイル
            for (File jackFile : jackFiles) {
                System.out.println("\nコンパイル中: " + jackFile.getName());
                compileFile(jackFile);
            }

            System.out.println("\n完了: " + jackFiles.size() + " ファイルをコンパイルしました");

        } catch (Exception e) {
            System.err.println("エラー: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 単一の.jackファイルをコンパイル
     * @param jackFile 入力Jackファイル
     */
    private static void compileFile(File jackFile) throws IOException {
        String baseName = jackFile.getName().replace(".jack", "");
        File parentDir = jackFile.getParentFile();

        // 出力ファイル
        File vmFile = new File(parentDir, baseName + ".vm");

        // トークナイザを作成
        JackTokenizer tokenizer = new JackTokenizer(jackFile);

        // コンパイル
        CompilationEngineVM engine = new CompilationEngineVM(tokenizer, vmFile);
        engine.compileClass();

        System.out.println("  → " + vmFile.getName() + " を生成");
    }

    /**
     * ヘルプメッセージを表示
     */
    private static void printHelp() {
        System.out.println("JackCompiler - Jack言語コンパイラ");
        System.out.println("");
        System.out.println("使用方法:");
        System.out.println("  java JackCompiler <source>");
        System.out.println("");
        System.out.println("引数:");
        System.out.println("  <source>  .jackファイルまたは.jackファイルを含むディレクトリ");
        System.out.println("");
        System.out.println("出力:");
        System.out.println("  各.jackファイルに対して:");
        System.out.println("    Xxx.vm  - VMコード");
        System.out.println("");
        System.out.println("例:");
        System.out.println("  java JackCompiler Square/Main.jack");
        System.out.println("  java JackCompiler Square/");
    }
}

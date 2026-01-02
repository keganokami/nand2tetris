import java.io.*;
import java.util.*;

/**
 * JackAnalyzer - Jack言語の構文解析器メインクラス
 *
 * 第10章のエントリーポイント。Jackファイル（またはディレクトリ）を受け取り、
 * 各ファイルに対してトークン化と構文解析を行います。
 *
 * 機能:
 * 1. 単一ファイル: Xxx.jack → Xxx.xml（構文解析結果）, XxxT.xml（トークン列）
 * 2. ディレクトリ: ディレクトリ内の全.jackファイルを処理
 *
 * 使用方法:
 *   java JackAnalyzer <input>
 *
 *   <input>は以下のいずれか:
 *   - 単一の.jackファイルへのパス
 *   - .jackファイルを含むディレクトリへのパス
 *
 * 出力:
 *   各.jackファイルに対して:
 *   - XxxT.xml: トークナイザの出力（デバッグ用）
 *   - Xxx.xml: 構文解析器の出力（構文木）
 */
public class JackAnalyzer {

    /**
     * メインエントリーポイント
     */
    public static void main(String[] args) {
        // 引数チェック
        if (args.length != 1) {
            System.out.println("使用方法: java JackAnalyzer <source>");
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
                // ファイル名でソート（一貫した処理順序のため）
                jackFiles.sort(Comparator.comparing(File::getName));
            }

            // 各ファイルを処理
            for (File jackFile : jackFiles) {
                System.out.println("処理中: " + jackFile.getName());
                processFile(jackFile);
            }

            System.out.println("\n完了: " + jackFiles.size() + " ファイルを処理しました");

        } catch (Exception e) {
            System.err.println("エラー: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 単一の.jackファイルを処理
     * @param jackFile 入力Jackファイル
     */
    private static void processFile(File jackFile) throws IOException {
        String baseName = jackFile.getName().replace(".jack", "");
        File parentDir = jackFile.getParentFile();

        // 出力ファイル名を決定
        File tokenXmlFile = new File(parentDir, baseName + "T.xml");
        File parseXmlFile = new File(parentDir, baseName + ".xml");

        // === Phase 1: トークン化 ===
        System.out.println("  トークン化中...");
        JackTokenizer tokenizer = new JackTokenizer(jackFile);

        // トークンをXMLファイルに出力（デバッグ・テスト用）
        try (PrintWriter tokenWriter = new PrintWriter(new FileWriter(tokenXmlFile))) {
            tokenWriter.print(tokenizer.toXML());
        }
        System.out.println("  → " + tokenXmlFile.getName() + " を生成");

        // === Phase 2: 構文解析 ===
        System.out.println("  構文解析中...");
        tokenizer.reset(); // トークナイザをリセット

        CompilationEngine engine = new CompilationEngine(tokenizer, parseXmlFile);
        engine.compileClass();

        System.out.println("  → " + parseXmlFile.getName() + " を生成");
    }

    /**
     * ヘルプメッセージを表示
     */
    private static void printHelp() {
        System.out.println("JackAnalyzer - Jack言語構文解析器");
        System.out.println("");
        System.out.println("使用方法:");
        System.out.println("  java JackAnalyzer <source>");
        System.out.println("");
        System.out.println("引数:");
        System.out.println("  <source>  .jackファイルまたは.jackファイルを含むディレクトリ");
        System.out.println("");
        System.out.println("出力:");
        System.out.println("  各.jackファイルに対して以下を生成:");
        System.out.println("    XxxT.xml  - トークン列（字句解析結果）");
        System.out.println("    Xxx.xml   - 構文木（構文解析結果）");
        System.out.println("");
        System.out.println("例:");
        System.out.println("  java JackAnalyzer Square/Main.jack");
        System.out.println("  java JackAnalyzer Square/");
    }
}

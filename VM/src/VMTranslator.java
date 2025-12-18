import java.io.*;

/**
 * VMTranslator - VMコードをHackアセンブリコードに変換するメインクラス
 *
 * 使用方法：
 *   java VMTranslator source.vm
 *   または
 *   java VMTranslator directory
 *
 * 動作：
 * 1. コマンドライン引数から.vmファイルまたはディレクトリを受け取る
 * 2. Parserで.vmファイルを1行ずつ解析
 * 3. CodeWriterで各VMコマンドをアセンブリコードに変換
 * 4. 生成したアセンブリコードを.asmファイルに出力
 *
 * Nand2Tetris プロジェクト7 & 8：
 * - プロジェクト7：スタック演算とメモリセグメントアクセス
 * - プロジェクト8：プログラムフローと関数呼び出し
 */
public class VMTranslator {

    public static void main(String[] args) {
        // コマンドライン引数のチェック
        if (args.length != 1) {
            System.err.println("使用方法: java VMTranslator source.vm");
            System.err.println("または: java VMTranslator directory");
            System.exit(1);
        }

        String sourcePath = args[0];
        File source = new File(sourcePath);

        // 入力ファイルの存在確認
        if (!source.exists()) {
            System.err.println("エラー: ファイルまたはディレクトリが見つかりません: " + sourcePath);
            System.exit(1);
        }

        try {
            if (source.isFile() && sourcePath.endsWith(".vm")) {
                // 単一の.vmファイルを処理
                translateFile(source);
            } else if (source.isDirectory()) {
                // ディレクトリ内の全.vmファイルを処理
                translateDirectory(source);
            } else {
                System.err.println("エラー: .vmファイルまたはディレクトリを指定してください");
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("エラー: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 単一の.vmファイルを.asmファイルに変換
     * 例：SimpleAdd.vm → SimpleAdd.asm
     *
     * @param vmFile 変換する.vmファイル
     */
    private static void translateFile(File vmFile) throws IOException {
        // 出力ファイル名を生成（.vm → .asm）
        String vmPath = vmFile.getAbsolutePath();
        String asmPath = vmPath.substring(0, vmPath.length() - 3) + ".asm";
        File asmFile = new File(asmPath);

        System.out.println("変換中: " + vmFile.getName() + " → " + asmFile.getName());

        // ParserとCodeWriterを初期化
        Parser parser = new Parser(vmFile);
        CodeWriter codeWriter = new CodeWriter(asmFile);

        // ファイル名を設定（staticセグメント用）
        String fileName = vmFile.getName();
        fileName = fileName.substring(0, fileName.length() - 3); // .vmを除去
        codeWriter.setFileName(fileName);

        // 各VMコマンドを解析してアセンブリコードに変換
        while (parser.hasMoreCommands()) {
            parser.advance();
            Parser.CommandType commandType = parser.commandType();

            switch (commandType) {
                case C_ARITHMETIC:
                    // 算術/論理コマンド
                    codeWriter.writeArithmetic(parser.arg1());
                    break;

                case C_PUSH:
                case C_POP:
                    // push/popコマンド
                    codeWriter.writePushPop(commandType, parser.arg1(), parser.arg2());
                    break;

                case C_LABEL:
                    // labelコマンド
                    codeWriter.writeLabel(parser.arg1());
                    break;

                case C_GOTO:
                    // gotoコマンド
                    codeWriter.writeGoto(parser.arg1());
                    break;

                case C_IF:
                    // if-gotoコマンド
                    codeWriter.writeIf(parser.arg1());
                    break;

                case C_FUNCTION:
                    // functionコマンド
                    codeWriter.writeFunction(parser.arg1(), parser.arg2());
                    break;

                case C_CALL:
                    // callコマンド
                    codeWriter.writeCall(parser.arg1(), parser.arg2());
                    break;

                case C_RETURN:
                    // returnコマンド
                    codeWriter.writeReturn();
                    break;

                default:
                    throw new IllegalArgumentException("不明なコマンドタイプ: " + commandType);
            }
        }

        // リソースをクローズ
        parser.close();
        codeWriter.writeInfiniteLoop();  // プログラム終了時の無限ループを追加
        codeWriter.close();

        System.out.println("変換完了: " + asmFile.getAbsolutePath());
    }

    /**
     * ディレクトリ内の全.vmファイルを1つの.asmファイルに変換
     * 例：MyProgram/ディレクトリ → MyProgram.asm
     *
     * @param directory .vmファイルを含むディレクトリ
     */
    private static void translateDirectory(File directory) throws IOException {
        // ディレクトリ内の.vmファイルを検索
        File[] vmFiles = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".vm");
            }
        });

        if (vmFiles == null || vmFiles.length == 0) {
            System.err.println("エラー: ディレクトリ内に.vmファイルが見つかりません");
            System.exit(1);
        }

        // 出力ファイル名を生成（ディレクトリ名.asm）
        String asmPath = directory.getAbsolutePath() + "/" + directory.getName() + ".asm";
        File asmFile = new File(asmPath);

        System.out.println("変換中: " + directory.getName() + "/ → " + asmFile.getName());

        CodeWriter codeWriter = new CodeWriter(asmFile);

        // Sys.vmが存在する場合はブートストラップコードを出力
        boolean hasSysFile = false;
        for (File vmFile : vmFiles) {
            if (vmFile.getName().equals("Sys.vm")) {
                hasSysFile = true;
                break;
            }
        }
        if (hasSysFile) {
            System.out.println("  ブートストラップコードを出力");
            codeWriter.writeBootstrap();
        }

        // 各.vmファイルを処理
        for (File vmFile : vmFiles) {
            System.out.println("  処理中: " + vmFile.getName());

            Parser parser = new Parser(vmFile);

            // ファイル名を設定（staticセグメント用）
            String fileName = vmFile.getName();
            fileName = fileName.substring(0, fileName.length() - 3); // .vmを除去
            codeWriter.setFileName(fileName);

            // 各VMコマンドを解析してアセンブリコードに変換
            while (parser.hasMoreCommands()) {
                parser.advance();
                Parser.CommandType commandType = parser.commandType();

                switch (commandType) {
                    case C_ARITHMETIC:
                        codeWriter.writeArithmetic(parser.arg1());
                        break;

                    case C_PUSH:
                    case C_POP:
                        codeWriter.writePushPop(commandType, parser.arg1(), parser.arg2());
                        break;

                    case C_LABEL:
                        codeWriter.writeLabel(parser.arg1());
                        break;

                    case C_GOTO:
                        codeWriter.writeGoto(parser.arg1());
                        break;

                    case C_IF:
                        codeWriter.writeIf(parser.arg1());
                        break;

                    case C_FUNCTION:
                        codeWriter.writeFunction(parser.arg1(), parser.arg2());
                        break;

                    case C_CALL:
                        codeWriter.writeCall(parser.arg1(), parser.arg2());
                        break;

                    case C_RETURN:
                        codeWriter.writeReturn();
                        break;

                    default:
                        throw new IllegalArgumentException("不明なコマンドタイプ: " + commandType);
                }
            }

            parser.close();
        }

        codeWriter.writeInfiniteLoop();  // プログラム終了時の無限ループを追加
        codeWriter.close();

        System.out.println("変換完了: " + asmFile.getAbsolutePath());
    }
}

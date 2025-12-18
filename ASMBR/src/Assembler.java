import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Assembler {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("使用法: java Assembler <ファイル名.asm>");
            return;
        }

        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            System.out.println("エラー: 入力ファイルが見つかりません。 " + args[0]);
            return;
        }

        String outputFileName = inputFile.getAbsolutePath().replace(".asm", ".hack");
        File outputFile = new File(outputFileName);

        SymbolTable symbolTable = new SymbolTable();
        
        // --- 第1パス: ラベルシンボルをシンボルテーブルに追加 ---
        firstPass(inputFile, symbolTable);

        // --- 第2パス: アセンブル実行 ---
        secondPass(inputFile, outputFile, symbolTable);

        System.out.println("アセンブルが完了しました。出力ファイル: " + outputFileName);
    }

    /**
     * 第1パス: (XXX) 形式のラベルシンボルを探し、
     * そのアドレスをシンボルテーブルに追加します。
     */
    private static void firstPass(File inputFile, SymbolTable symbolTable) {
        Parser parser = new Parser(inputFile);
        int romAddress = 0;
        while (parser.hasMoreLines()) {
            parser.advance();
            Parser.InstructionType type = parser.instructionType();
            if (type == Parser.InstructionType.L_INSTRUCTION) {
                symbolTable.addEntry(parser.symbol(), romAddress);
            } else if (type == Parser.InstructionType.A_INSTRUCTION || type == Parser.InstructionType.C_INSTRUCTION) {
                romAddress++;
            }
        }
    }

    /**
     * 第2パス: ファイルを再度読み込み、各命令をバイナリに変換して
     * 出力ファイルに書き込みます。
     */
    private static void secondPass(File inputFile, File outputFile, SymbolTable symbolTable) {
        Parser parser = new Parser(inputFile);
        Code code = new Code();
        int nextRamAddress = 16; // 変数用RAMアドレスは16から始まる

        try (FileWriter writer = new FileWriter(outputFile)) {
            while (parser.hasMoreLines()) {
                parser.advance();
                Parser.InstructionType type = parser.instructionType();
                String binaryInstruction = null;

                if (type == Parser.InstructionType.A_INSTRUCTION) {
                    String symbol = parser.symbol();
                    int address;
                    if (isNumeric(symbol)) {
                        address = Integer.parseInt(symbol);
                    } else {
                        if (!symbolTable.contains(symbol)) {
                            // 新しい変数シンボルなら、RAMアドレスを割り当ててテーブルに追加
                            symbolTable.addEntry(symbol, nextRamAddress);
                            nextRamAddress++;
                        }
                        address = symbolTable.getAddress(symbol);
                    }
                    binaryInstruction = String.format("%16s", Integer.toBinaryString(address)).replace(' ', '0');

                } else if (type == Parser.InstructionType.C_INSTRUCTION) {
                    String comp = code.comp(parser.comp());
                    String dest = code.dest(parser.dest());
                    String jump = code.jump(parser.jump());
                    binaryInstruction = "111" + comp + dest + jump;
                }

                if (binaryInstruction != null) {
                    writer.write(binaryInstruction + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
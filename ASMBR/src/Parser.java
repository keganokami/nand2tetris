

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
    
    public enum InstructionType {
        A_INSTRUCTION, C_INSTRUCTION, L_INSTRUCTION
    }

    private BufferedReader reader;
    private String currentLine;

    public Parser(File inputFile) {
        try {
            this.reader = new BufferedReader(new FileReader(inputFile));
            this.currentLine = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasMoreLines() {
        try {
            return reader.ready();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void advance() {
        try {
            while (reader.ready()) {
                String line = reader.readLine().trim();
                // コメントを除去
                int commentIndex = line.indexOf("//");
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex).trim();
                }
                if (!line.isEmpty()) {
                    currentLine = line;
                    return;
                }
            }
            currentLine = null; // ファイルの終端
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InstructionType instructionType() {
        if (currentLine.startsWith("@")) {
            return InstructionType.A_INSTRUCTION;
        } else if (currentLine.startsWith("(") && currentLine.endsWith(")")) {
            return InstructionType.L_INSTRUCTION;
        } else {
            return InstructionType.C_INSTRUCTION;
        }
    }

    public String symbol() {
        if (instructionType() == InstructionType.A_INSTRUCTION) {
            return currentLine.substring(1);
        } else if (instructionType() == InstructionType.L_INSTRUCTION) {
            return currentLine.substring(1, currentLine.length() - 1);
        }
        return null;
    }

    public String dest() {
        if (instructionType() == InstructionType.C_INSTRUCTION) {
            if (currentLine.contains("=")) {
                return currentLine.split("=")[0];
            }
        }
        return "null";
    }

    public String comp() {
        if (instructionType() == InstructionType.C_INSTRUCTION) {
            String temp = currentLine;
            if (temp.contains("=")) {
                temp = temp.split("=")[1];
            }
            if (temp.contains(";")) {
                temp = temp.split(";")[0];
            }
            return temp;
        }
        return "null";
    }

    public String jump() {
        if (instructionType() == InstructionType.C_INSTRUCTION) {
            if (currentLine.contains(";")) {
                return currentLine.split(";")[1];
            }
        }
        return "null";
    }
}
import java.util.*;

/**
 * SymbolTable - シンボルテーブル（変数の情報を管理）
 *
 * 第11章の中核モジュール。Jack言語の変数（クラスレベル・サブルーチンレベル）を管理し、
 * 各変数の名前、型、種類（kind）、インデックスを記録します。
 *
 * スコープ:
 * - クラススコープ: static変数、field変数
 * - サブルーチンスコープ: argument変数、local変数
 *
 * 変数の種類（Kind）:
 * - STATIC: クラスレベルの静的変数 → VMセグメント: static
 * - FIELD: クラスレベルのフィールド → VMセグメント: this
 * - ARG: サブルーチンの引数 → VMセグメント: argument
 * - VAR: サブルーチンのローカル変数 → VMセグメント: local
 *
 * 例:
 *   class Point {
 *       field int x, y;        // FIELD 0, FIELD 1
 *       static int count;      // STATIC 0
 *
 *       method int distance(Point other) {  // ARG 0 = this, ARG 1 = other
 *           var int dx, dy;    // VAR 0, VAR 1
 *           ...
 *       }
 *   }
 */
public class SymbolTable {

    /**
     * 変数の種類を表す列挙型
     */
    public enum Kind {
        STATIC,  // クラスレベルの静的変数
        FIELD,   // クラスレベルのフィールド
        ARG,     // サブルーチンの引数
        VAR,     // サブルーチンのローカル変数
        NONE     // 未定義（エラー用）
    }

    /**
     * シンボル（変数）の情報を保持する内部クラス
     */
    private static class Symbol {
        String type;   // 変数の型（int, char, boolean, またはクラス名）
        Kind kind;     // 変数の種類
        int index;     // 同じ種類内でのインデックス

        Symbol(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    // クラススコープのシンボルテーブル（static, field）
    private Map<String, Symbol> classScope;

    // サブルーチンスコープのシンボルテーブル（arg, var）
    private Map<String, Symbol> subroutineScope;

    // 各種類の変数カウンタ
    private int staticCount;
    private int fieldCount;
    private int argCount;
    private int varCount;

    /**
     * コンストラクタ - 空のシンボルテーブルを作成
     */
    public SymbolTable() {
        classScope = new HashMap<>();
        subroutineScope = new HashMap<>();
        staticCount = 0;
        fieldCount = 0;
        argCount = 0;
        varCount = 0;
    }

    /**
     * 新しいサブルーチンスコープを開始する
     * サブルーチンスコープをリセットし、argとvarのカウンタを0に戻す
     */
    public void startSubroutine() {
        subroutineScope.clear();
        argCount = 0;
        varCount = 0;
    }

    /**
     * 新しい変数を定義する
     * @param name 変数名
     * @param type 型（int, char, boolean, またはクラス名）
     * @param kind 種類（STATIC, FIELD, ARG, VAR）
     */
    public void define(String name, String type, Kind kind) {
        Symbol symbol;

        switch (kind) {
            case STATIC:
                symbol = new Symbol(type, kind, staticCount++);
                classScope.put(name, symbol);
                break;
            case FIELD:
                symbol = new Symbol(type, kind, fieldCount++);
                classScope.put(name, symbol);
                break;
            case ARG:
                symbol = new Symbol(type, kind, argCount++);
                subroutineScope.put(name, symbol);
                break;
            case VAR:
                symbol = new Symbol(type, kind, varCount++);
                subroutineScope.put(name, symbol);
                break;
            default:
                throw new IllegalArgumentException("Unknown kind: " + kind);
        }
    }

    /**
     * 指定された種類の変数の数を返す
     * @param kind 種類
     * @return その種類の変数の数
     */
    public int varCount(Kind kind) {
        switch (kind) {
            case STATIC: return staticCount;
            case FIELD:  return fieldCount;
            case ARG:    return argCount;
            case VAR:    return varCount;
            default:     return 0;
        }
    }

    /**
     * 指定された名前の変数の種類を返す
     * @param name 変数名
     * @return 種類（見つからない場合はNONE）
     */
    public Kind kindOf(String name) {
        Symbol symbol = lookup(name);
        return (symbol != null) ? symbol.kind : Kind.NONE;
    }

    /**
     * 指定された名前の変数の型を返す
     * @param name 変数名
     * @return 型（見つからない場合はnull）
     */
    public String typeOf(String name) {
        Symbol symbol = lookup(name);
        return (symbol != null) ? symbol.type : null;
    }

    /**
     * 指定された名前の変数のインデックスを返す
     * @param name 変数名
     * @return インデックス（見つからない場合は-1）
     */
    public int indexOf(String name) {
        Symbol symbol = lookup(name);
        return (symbol != null) ? symbol.index : -1;
    }

    /**
     * 変数を検索する（サブルーチンスコープ → クラススコープの順）
     * @param name 変数名
     * @return シンボル（見つからない場合はnull）
     */
    private Symbol lookup(String name) {
        // まずサブルーチンスコープを検索
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name);
        }
        // 次にクラススコープを検索
        if (classScope.containsKey(name)) {
            return classScope.get(name);
        }
        return null;
    }

    /**
     * 指定された名前の変数が存在するか確認
     * @param name 変数名
     * @return 存在すればtrue
     */
    public boolean contains(String name) {
        return lookup(name) != null;
    }

    /**
     * シンボルテーブルの内容を文字列で返す（デバッグ用）
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Symbol Table ===\n");

        sb.append("Class Scope:\n");
        for (Map.Entry<String, Symbol> entry : classScope.entrySet()) {
            Symbol s = entry.getValue();
            sb.append(String.format("  %s: %s %s #%d\n",
                entry.getKey(), s.type, s.kind, s.index));
        }

        sb.append("Subroutine Scope:\n");
        for (Map.Entry<String, Symbol> entry : subroutineScope.entrySet()) {
            Symbol s = entry.getValue();
            sb.append(String.format("  %s: %s %s #%d\n",
                entry.getKey(), s.type, s.kind, s.index));
        }

        return sb.toString();
    }

    /**
     * Kindに対応するVMセグメント名を返す
     * @param kind 変数の種類
     * @return VMセグメント名
     */
    public static String kindToSegment(Kind kind) {
        switch (kind) {
            case STATIC: return "static";
            case FIELD:  return "this";
            case ARG:    return "argument";
            case VAR:    return "local";
            default:     return null;
        }
    }
}

package tech.inudev.profundus.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.map.MinecraftFont;
import tech.inudev.profundus.Profundus;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ヘルプの表示するためのクラス。
 * ヘルプを追加する場合は、enum HelpTypeに追加していく。
 *
 * @author toru-toruto
 */
public class HelpUtil {
    private static final String HELP_DIR = "help";
    private static final String BOOK_SHAPE_DIR = "book_shape";

    /**
     * それぞれのヘルプの情報を管理する列挙型。
     * ヘルプを追加する場合は、ここに追加していく。
     */
    public enum HelpType {
        // 例：
        // HelpTypeName("file_name.txt", "Book Title"),
        // NewHelp("new_help.txt", "New Help");
        /**
         * サンプル用ヘルプ
         */
        Sample("sample.txt", "Sample");

        private final String fileName;
        private final String title;

        HelpType(String fileName, String title) {
            this.fileName = fileName;
            this.title = title;
        }
    }

    /**
     * 初期化処理。
     * ヘルプのテキストを本に表示するために整形し、別のtxtファイルとして保存する。
     * ヘルプ表示時はこのtxtファイルを読み込む。
     */
    public static void initializeHelp() {
        for (HelpType helpType : HelpType.values()) {
            Profundus.getInstance().saveResource(HELP_DIR + "/" + helpType.fileName, true);

            // txtファイルからヘルプを読み込み
            List<String> helpLines;
            try {
                String dirPath = "$data/$help"
                        .replace("$data", Profundus.getInstance().getDataFolder().getPath())
                        .replace("$help", HELP_DIR);
                Path path = Paths.get(dirPath, helpType.fileName);
                helpLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 本に合わせて整形
            List<String> bookLines = HelpUtil.getBookLines(helpLines);

            // 整形したものを別のtxtファイルへ保存
            try {
                Path dirPath = Paths.get("$data/$help/$bookShape"
                        .replace("$data", Profundus.getInstance().getDataFolder().getPath())
                        .replace("$help", HELP_DIR)
                        .replace("$bookShape", BOOK_SHAPE_DIR));
                if (!Files.exists(dirPath)) {
                    Files.createDirectory(dirPath);
                }
                Path filePath = Paths.get(dirPath.toString(), "_" + helpType.fileName);
                Files.write(
                        filePath,
                        bookLines,
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * ヘルプを開く。
     * 初期化処理で生成された整形済みtxtファイルを読み込み、本に表示する。
     *
     * @param playerUUID ヘルプを開く対象のプレイヤー
     * @param helpType   開くヘルプの種類
     */
    public static void openHelp(UUID playerUUID, HelpType helpType) {
        if (playerUUID == null || helpType == null) {
            throw new IllegalArgumentException();
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            throw new IllegalArgumentException("オンラインでないか存在しないプレイヤーです。");
        }

        ItemStack writtenBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = ((BookMeta) writtenBook.getItemMeta())
                .author(Component.text("Master"))
                .title(Component.text(helpType.title));

        // 初期化時に生成した、整形済みtxtファイルの読み込み
        List<String> bookLines;
        try {
            String dirPath = "$data/$help/$bookShape"
                    .replace("$data", Profundus.getInstance().getDataFolder().getPath())
                    .replace("$help", HELP_DIR)
                    .replace("$bookShape", BOOK_SHAPE_DIR);
            Path path = Paths.get(dirPath, "_" + helpType.fileName);
            bookLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ページ分割
        List<Component> pageList = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        for (int i = 0; i < bookLines.size(); i++) {
            page.append(bookLines.get(i)).append("\n");
            if (i != 0 && (i % 13 == 0 || i == bookLines.size() - 1)) {
                pageList.add(Component.text(page.toString()));
                page = new StringBuilder();
            }
        }
        bookMeta.addPages(pageList.toArray(new Component[0]));
        writtenBook.setItemMeta(bookMeta);

        player.openBook(writtenBook);
    }

    /**
     * ヘルプの本での表示を確認するためのテストメソッド。
     * 改行やページの区切りを確認する用。
     *
     * @param helpType 開くヘルプの種類
     */
    public static void testHelpText(HelpType helpType) {
        if (helpType == null) {
            throw new IllegalArgumentException();
        }

        // txtファイルの読み込み
        List<String> bookLines;
        try {
            String dirPath = "src/main/resources/$help"
                    .replace("$help", HELP_DIR);
            Path path = Paths.get(dirPath, helpType.fileName);
            bookLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bookLines = getBookLines(bookLines);

        // ページング
        StringBuilder page = new StringBuilder();
        for (int i = 0; i < bookLines.size(); i++) {
            page.append(bookLines.get(i)).append("\n");
            if (i != 0 && (i % 13 == 0 || i == bookLines.size() - 1)) {
                System.out.println(page);
                System.out.println("＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊");
                page = new StringBuilder();
            }
        }
    }

    /**
     * 与えられたテキストを本に合わせて整形し、行のリストとして返す。
     * 連続する（半角スペースで区切られていない）文字列が行に入りきらない場合、文字列をまとめて次行に折り返す。
     * カラーコードや装飾コードは、ボールドを除いて使用可能。（ボールドは文字の幅が変化するため現状は不可(2022/06/16)）
     * リソースパックなどによってフォントが変更された場合、おそらく破綻してしまう。(2022/06/16)
     *
     * @param helpLines テキストファイルに書かれたヘルプの行ごとのリスト
     * @return 本のサイズに合わせて整形されたテキストの行ごとのリスト
     */
    private static List<String> getBookLines(List<String> helpLines) {
        if (helpLines == null) {
            throw new IllegalArgumentException();
        }
        List<String> resultLines = new ArrayList<>();
        // それぞれの行についてループ
        for (String paragraph : helpLines) {
            if (paragraph.equals("")) {
                // 空行の場合
                resultLines.add("");
            } else {
                resultLines.addAll(buildLines(paragraph));
            }
        }
        return resultLines;
    }

    private static List<String> buildLines(String paragraph) {
        if (paragraph == null) {
            throw new IllegalArgumentException();
        }
        final List<String> newLines = new ArrayList<>();

        StringBuilder newLineBuilder = new StringBuilder();
        int newLineWidth = 0;

        // 半角スペースで分割した各文字列をループ（文字列をまとめて折り返す目的）
        for (String word : paragraph.split(" ")) {
            if (word.equals("")) {
                // 文頭や連続半角スペースの場合
                final MinecraftFont font = new MinecraftFont();
                final int maxLineWidth = font.getWidth("LLLLLLLLLLLLLLLLLLL");
                if (newLineWidth + font.getWidth(" ") > maxLineWidth) {
                    newLines.add(newLineBuilder.toString());
                    newLineBuilder = new StringBuilder();
                    newLineWidth = 0;
                } else {
                    newLineBuilder.append(" ");
                    newLineWidth += font.getWidth(" ");
                }
            } else {
                // 文字列の場合
                JoinWordResult result =
                        joinWord(newLineBuilder.toString(), newLineWidth, word);
                newLines.addAll(result.newLines);
                newLineBuilder = new StringBuilder(result.newLineStr);
                newLineWidth = result.newLineWidth;
            }
        }
        if (!newLineBuilder.toString().equals("")) {
            newLines.add(newLineBuilder.toString());
        }
        return newLines;
    }

    private record JoinWordResult(List<String> newLines, String newLineStr, int newLineWidth) {
        private JoinWordResult {
            if (newLines == null || newLineStr == null || newLineWidth < 0) {
                throw new IllegalArgumentException();
            }
        }
    }

    // 文字列の場合
    private static JoinWordResult joinWord(String lineStr, int lineWidth, String word) {
        if (lineStr == null || lineWidth < 0 || word == null) {
            throw new IllegalArgumentException();
        }
        List<String> newLines = new ArrayList<>();
        StringBuilder newLineStr = new StringBuilder(lineStr);

        // 文字列を文字単位に分割しループ
        String[] letters = word.split("");
        int id = 0;
        while (id < letters.length) {
            if (isSectionLetter(letters[id])) {
                // 「§」の場合
                DecorationResult result
                        = joinDecorationLetters(newLineStr.toString(), letters, id);
                newLineStr = new StringBuilder(result.newLineStr);
                id = result.id;
            } else {
                // 通常の文字の場合
                JoinLettersResult result = joinLetters(newLineStr.toString(), lineWidth, letters, id);
                if (result.newLines.size() > 0) {
                    newLines.addAll(result.newLines);
                }
                newLineStr = new StringBuilder(result.newLineStr);
                id = result.id;
                lineWidth = result.newLineWidth;
            }
        }
        return new JoinWordResult(newLines, newLineStr.toString(), lineWidth);
    }


    private record DecorationResult(String newLineStr, int id) {
        private DecorationResult {
            if (newLineStr == null || id < 0) {
                throw new IllegalArgumentException();
            }
        }
    }

    private static DecorationResult joinDecorationLetters(String lineStr, String[] letters, int id) {
        if (lineStr == null || letters == null || id < 0) {
            throw new IllegalArgumentException();
        }
        StringBuilder newLineStr = new StringBuilder(lineStr);
        if (id < letters.length - 1 && isDecorationLetter(letters[id + 1])) {
            if (!letters[id + 1].matches("[lL]")) {
                newLineStr.append(letters[id]).append(letters[id + 1]);
            }
            id += 2;
        } else {
            id++;
        }
        return new DecorationResult(newLineStr.toString(), id);
    }


    private record JoinLettersResult(List<String> newLines, String newLineStr, int id, int newLineWidth) {
        private JoinLettersResult {
            if (newLines == null || newLineStr == null || id < 0 || newLineWidth < 0) {
                throw new IllegalArgumentException();
            }
        }
    }

    // 通常の文字の場合
    private static JoinLettersResult joinLetters(String lineStr, int lineWidth, String[] letters, int id) {
        if (lineStr == null || lineWidth < 0 || letters == null || id < 0) {
            throw new IllegalArgumentException();
        }

        final MinecraftFont font = new MinecraftFont();
        final int maxLineWidth = font.getWidth("LLLLLLLLLLLLLLLLLLL");
        final int letterMargin = 1;

        // 完成した行のリスト
        List<String> newLines = new ArrayList<>();
        // 処理中の行の文字列
        StringBuilder newLineStr = new StringBuilder(lineStr);
        // 連続する文字列
        StringBuilder newWord = new StringBuilder();

        while (id < letters.length) {
            if (isSectionLetter(letters[id])) {
                // §の場合の処理
                DecorationResult result
                        = joinDecorationLetters(newWord.toString(), letters, id);
                newWord = new StringBuilder(result.newLineStr);
                id = result.id;
            } else {
                newWord.append(letters[id]);
                id++;
            }
        }

        // 追加される文字列が行をはみ出す場合、文字列ごと折り返す
        final int newWidth = (newLineStr.toString().equals("") ? 0 : letterMargin)
                + getWidth(newWord.toString(), font);
        if (lineWidth + newWidth > maxLineWidth) {
            if (lineWidth > 0) {
                newLines.add(newLineStr.toString());
                // 次の行へ
                lineWidth = 0;
                newLineStr = new StringBuilder();
            }
            if (newWidth > maxLineWidth) {
                // ひとつの連続する文字列で何行も埋めてしまう場合
                StringBuilder sb = new StringBuilder();
                String[] wordLetters = newWord.toString().split("");
                for (String wordLetter : wordLetters) {
                    if (getWidth(sb + wordLetter, font) > maxLineWidth) {
                        newLines.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    sb.append(wordLetter);
                }
                newWord = new StringBuilder(sb.toString());
            }
        }

        // 行へ追加
        lineWidth += newLineStr.toString().equals("")
                ? getWidth(newWord.toString(), font)
                : letterMargin + getWidth(newWord.toString(), font);
        newLineStr.append(newWord);

        // 行のラストにスペースの余地があるならスペースを入れる
        final int endSpaceWidth = letterMargin + font.getWidth(" ");
        if (lineWidth + endSpaceWidth <= maxLineWidth) {
            lineWidth += endSpaceWidth;
            newLineStr.append(" ");
        }
        id++;
        return new JoinLettersResult(newLines, newLineStr.toString(), id, lineWidth);
    }

    private static int getWidth(String str, MinecraftFont font) {
        if (str == null || font == null) {
            throw new IllegalArgumentException();
        }
        final int zenkakuWidth = 8; // 全角文字の幅基準
        final int letterMargin = 1;
        int result = 0;

        String[] letters = getRawNewWord(str).split("");
        for (int i = 0; i < letters.length; i++) {
            final int letterWidth = (font.isValid(letters[i]))
                    ? font.getWidth(letters[i])
                    : zenkakuWidth;
            result += (i == 0)
                    ? letterWidth
                    : letterMargin + letterWidth;
        }
        return result;
    }

    private static String getRawNewWord(String newWord) {
        if (newWord == null) {
            throw new IllegalArgumentException();
        }
        String dupRegx = "[" + ChatColor.COLOR_CHAR + "]+";
        String decoRegx = "(?i)" + ChatColor.COLOR_CHAR + "[0-9A-FK-ORX]";
        String secRegx = "" + ChatColor.COLOR_CHAR;
        return newWord
                .replaceAll(dupRegx, "" + ChatColor.COLOR_CHAR)
                .replaceAll(decoRegx, "")
                .replaceAll(secRegx, "");
    }

    private static boolean isSectionLetter(String letter) {
        if (letter == null) {
            throw new IllegalArgumentException();
        }
        return letter.equals(String.valueOf(ChatColor.COLOR_CHAR));
    }

    private static boolean isDecorationLetter(String letter) {
        if (letter == null) {
            throw new IllegalArgumentException();
        }
        String regex = "(?i)[0-9A-FK-ORX]";
        return letter.matches(regex);
    }
}

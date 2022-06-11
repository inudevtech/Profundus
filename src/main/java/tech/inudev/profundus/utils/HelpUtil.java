package tech.inudev.profundus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.map.MinecraftFont;
import tech.inudev.profundus.Profundus;

import java.awt.print.Book;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelpUtil {
    public static void openHelp(UUID playerUUID, int helpId) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            ItemStack writtenBook = new ItemStack(Material.WRITTEN_BOOK);
            if (helpId == 0) {
                BookMeta bookMeta = ((BookMeta) writtenBook.getItemMeta())
                        .author(Component.text("Master"))
                        .title(Component.text("Help"));

//                InputStream stream = Profundus.getInstance().getResource("help/test.txt");
//                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
//                Stream<String> lines = new BufferedReader(reader).lines();
//                String str = lines.collect(Collectors.joining("\n"));

                String str = """
                        これから説明しますのは、なんといってもowesomeでgreatなsomething。

                         まず第一にclass 1において、この機構は軸のroleを果たしている。一見すると取るに足らないcrude shapeをしているが、実際のところ、これがthe best shapeであることは間違いない。

                        ごま豆乳鍋のprecious valueは、この舌触りにある。通常の鍋は、どちらかと言えば澄ましたうまみ出しによる鍋であるが、ごま豆乳鍋では豆乳の滑らかさに加え、ごまのほのかなざらつきによって、深みのある食感体験を演出しているのである。""";
                String afterStr = "";
                for (String s : HelpUtil.getLines(str)) {
                    afterStr += s + "\n";
//                    Profundus.getInstance().getLogger().info(s);
                }
                Profundus.getInstance().getLogger().info(afterStr);

                bookMeta.addPages(Component.text(afterStr));
                writtenBook.setItemMeta(bookMeta);
            }
            player.openBook(writtenBook);
        }
    }

    public static List<String> getLines(String text) {
        final MinecraftFont font = new MinecraftFont();
        final int maxLineWidth = font.getWidth("LLLLLLLLLLLLLLLLLLL");

        List<String> lineList = new ArrayList<>();
        text.lines().forEach((String section) -> {
            if (section.equals("")) {
                lineList.add("(BREAK)");

            } else {
                String[] wordList = ChatColor.stripColor(section).split(" ");
                String line = ""; // 行の文字列を保存
                int width = 0;
                for (String word : wordList) {
                    if (word.equals("")) {
                        line += " ";
                        continue;
                    }
                    String[] charList = word.split("");
                    int i = 0;
                    while (i < charList.length) {
                        if (font.isValid(charList[i])) {
                            String mcFontWord = charList[i];
                            // MinecraftFontに含まれる文字が続く限りiを進行
                            while (i < word.length() - 1 && font.isValid(charList[i + 1])) {
                                mcFontWord += charList[i + 1];
                                i++;
                            }
                            // 主に英単語の幅が最大を超えてしまう場合、英単語ごと折り返す
                            if (width + font.getWidth(mcFontWord) > maxLineWidth) {
                                // todo:ひと単語で一行埋めてしまう場合の特別処理が必要
                                lineList.add(line);
                                // 次の行
                                width = 0;
                                line = "";
                            }
                            width += line.equals("")
                                    ? font.getWidth(mcFontWord)
                                    : font.getWidth(mcFontWord) + 1;
                            line += mcFontWord;
                            i++;
                            if (width + 1 + font.getWidth(" ") <= maxLineWidth && i == word.length()) {
                                line += " ";
                                width += font.getWidth(" ");
                            }
                        } else {
                            final int margin = 1;
                            final int charWidth = 8; // 全角文字の幅基準
//                            // 日本語の場合
//                            if (isJapanese(charList[i])) {
//                                final int margin = 1;
//                                final int jpCharWidth = 8;
//                                String jpWord = "";
//                                // 日本語文字が続く限りiを進行
//                                while(i < word.length() - 1 && isJapanese(charList[i + 1])) {
//                                    int currentWidth = (jpCharWidth * jpWord.length())
//                                            + (margin * (jpWord.length() - 1));
//
//                                    // 日本語が続くが文字列の幅が最大を超えてしまう場合
//                                    if (currentWidth + margin + jpCharWidth > maxLineWidth) {
//                                        line += jpWord;
//                                        lineList.add(line);
//                                        line = "";
//                                        jpWord = "";
//                                    }
//                                    jpWord += charList[i + 1];
//                                    i++;
//                                }
                            if (width + margin + charWidth > maxLineWidth) {
                                lineList.add(line);
                                // 次の行
                                width = 0;
                                line = "";
                            }
                            width += (line.equals(""))
                                    ? charWidth
                                    : margin + charWidth;
                            line += charList[i];
                            i++;
                        }
                    }
                }
                lineList.add(line);
            }
        });
        return lineList;
    }

//    private static boolean isJapanese(String str) {
//        String regex = "[　一-龠ぁ-んァ-ヶ]+";
//        return str.matches(regex);
//    }

}

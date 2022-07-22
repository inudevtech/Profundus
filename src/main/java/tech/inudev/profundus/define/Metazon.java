package tech.inudev.profundus.define;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tech.inudev.profundus.utils.DatabaseUtil;
import tech.inudev.profundus.utils.HelpUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Metazonを実行するクラス
 *
 * @author toru-toruto
 */
public class Metazon {
    private static final String METAZON_TITLE = "Metazon";
    private static final int EMERALD_X = 5;
    private static final int EMERALD_Y = 1;
    private static final int GOODS_X = 5;
    private static final int GOODS_Y = 3;
    private int sellPrice = 1;

    // region top

    /**
     * Metazonを起動する。
     *
     * @param player 起動したプレイヤー
     */
    public void open(Player player) {
        this.sellPrice = 1;
        Gui gui = new Gui(METAZON_TITLE);
        gui.setMenuItems(generateTopMenu(player));
        gui.open(player, false);
    }

    private List<Gui.PosMenuItem> generateTopMenu(Player player) {
        List<Gui.PosMenuItem> result = new ArrayList<>();

        // 購入するボタン
        MenuItem paper = new MenuItem(
                Component.text("購入する"),
                null,
                (menuItem, _player) -> openBuyMode(_player),
                new ItemStack(Material.GREEN_WOOL),
                Gui.isBedrock(player),
                false);
        result.add(new Gui.PosMenuItem(paper, 4, 1));

        // 販売するボタン
        MenuItem sellMode = new MenuItem(
                Component.text("販売する"),
                null,
                (menuItem, _player) -> openSellMode(_player, null),
                new ItemStack(Material.ORANGE_WOOL),
                Gui.isBedrock(player),
                false);
        result.add(new Gui.PosMenuItem(sellMode, 6, 1));

        int[][] filledArray = {{4, 6}};
        result.addAll(generateDisuses(player, filledArray));
        return result;
    }
    // endregion

    // region buyMode
    private void openBuyMode(Player player) {

    }
    // endregion

    // region sellMode
    private void openSellMode(Player player, ItemStack sellItem) {
        Gui gui = new Gui(METAZON_TITLE + " 販売する");
        gui.setMenuItems(generateSellMenu(gui, player, sellItem));
        gui.open(player, true);
    }

    private List<Gui.PosMenuItem> generateSellMenu(Gui gui, Player player, ItemStack sellItem) {
        List<Gui.PosMenuItem> result = new ArrayList<>();

        // 金額変動ボタン
        for (int i = 0; i < 3; i++) {
            int x = 5;
            result.add(generatePriceChanger(gui, (int) Math.pow(10, i), x + (i + 1)));
            result.add(generatePriceChanger(gui, (int) -Math.pow(10, i), x - (i + 1)));
        }

        // 金額表示
        MenuItem emerald = new MenuItem(
                Component.text("金額"),
                List.of(Component.text(this.sellPrice + "円")),
                null,
                new ItemStack(Material.EMERALD));
        result.add(new Gui.PosMenuItem(emerald, EMERALD_X, EMERALD_Y));

        // 販売するボタン
        MenuItem paper = new MenuItem(
                Component.text("販売する"),
                sellItem != null ? null : List.of(Component.text("下に売りたいアイテムをセットしてください")),
                (menuItem, _player) -> onSellPaperClick(gui, _player),
                new ItemStack(Material.PAPER),
                null,
                sellItem != null,
                false,
                true);
        result.add(new Gui.PosMenuItem(paper, EMERALD_X, EMERALD_Y + 1));

        // 販売アイテムセット用の空欄
        MenuItem itemBox = MenuItem.generateDraggable(
                (menuItem, _player) -> onItemBoxClick(gui, menuItem),
                sellItem);
        result.add(new Gui.PosMenuItem(itemBox, EMERALD_X, EMERALD_Y + 2));

        // 戻るボタン
        MenuItem backPage = new MenuItem(
                Component.text("前に戻る"),
                null,
                (menuItem, _player) -> open(_player),
                new ItemStack(Material.PAPER),
                Gui.isBedrock(player),
                false);
        result.add(new Gui.PosMenuItem(backPage, 1, 3));

        // ヘルプボタン
        MenuItem help = new MenuItem(
                Component.text("ヘルプ"),
                List.of(Component.text("Metazonのつかいかた")),
                (menuItem, _player) -> HelpUtil.openHelp(_player.getUniqueId(), HelpUtil.HelpType.Sample),
                new ItemStack(Material.WRITTEN_BOOK),
                null,
                true,
                true,
                false);
        result.add(new Gui.PosMenuItem(help, 9, EMERALD_Y + 2));

        int[][] filledArray = {{2, 3, 4, 5, 6, 7, 8}, {5}, {1, 5, 9}};
        result.addAll(generateDisuses(player, filledArray));
        return result;
    }

    private Gui.PosMenuItem generatePriceChanger(Gui gui, int value, int x) {
        BiConsumer<MenuItem, Player> onClick = (menuItem, player) -> {
            this.sellPrice += (int) menuItem.getCustomData();
            if (this.sellPrice < 1) {
                this.sellPrice = 1;
            }
            gui.setItemLore(EMERALD_X, EMERALD_Y, List.of(Component.text(this.sellPrice + "円")));
        };
        MenuItem newItem = new MenuItem(
                Component.text(Math.abs(value) + (value > 0 ? "円増やす" : "円減らす")),
                null,
                onClick,
                new ItemStack(value > 0 ? Material.BLUE_WOOL : Material.RED_WOOL),
                value,
                false);
        return new Gui.PosMenuItem(newItem, x, 1);
    }

    private void onSellPaperClick(Gui gui, Player player) {
        ItemStack sellItem = gui.cloneItemStack(GOODS_X, GOODS_Y);
        if (sellItem == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 1);
            return;
        }
        openSellConfirm(player, sellItem);
    }

    private void onItemBoxClick(Gui gui, MenuItem menuItem) {
        if (menuItem.getIcon() != null) {
            gui.setItemLore(EMERALD_X, EMERALD_Y + 1, null);
            gui.setItemShiny(EMERALD_X, EMERALD_Y + 1, true);
        } else {
            gui.setItemLore(EMERALD_X, EMERALD_Y + 1, List.of(Component.text("下に売りたいアイテムをセットしてください")));
            gui.setItemShiny(EMERALD_X, EMERALD_Y + 1, false);
        }
    }
    // endregion

    // region sellConfirm
    private void openSellConfirm(Player player, ItemStack sellItem) {
        Gui gui = new Gui(METAZON_TITLE + " 販売確認");
        gui.setMenuItems(generateSellConfirmMenu(gui, player, sellItem));
        gui.open(player, true);
    }

    private List<Gui.PosMenuItem> generateSellConfirmMenu(Gui gui, Player player, ItemStack sellItem) {
        List<Gui.PosMenuItem> result = new ArrayList<>();

        // 確認説明
        MenuItem paper = new MenuItem(
                Component.text("確認"),
                List.of(Component.text("こちらのアイテムを販売します。"),
                        Component.text("左：戻る 右：確定")),
                null,
                new ItemStack(Material.PAPER));
        result.add(new Gui.PosMenuItem(paper, 3, 1));

        // 販売金額表示
        MenuItem emerald = new MenuItem(
                Component.text("金額"),
                List.of(Component.text(this.sellPrice + "円")),
                null,
                new ItemStack(Material.EMERALD));
        result.add(new Gui.PosMenuItem(emerald, 5, 1));

        // 販売アイテム表示
        MenuItem goods = new MenuItem(
                null,
                null,
                sellItem,
                false,
                false,
                true);
        result.add(new Gui.PosMenuItem(goods, 5, 2));

        // 戻るボタン
        MenuItem backPage = new MenuItem(
                Component.text("前に戻る"),
                null,
                (menuItem, _player) -> openSellMode(_player, sellItem),
                new ItemStack(Material.PAPER),
                false,
                true);
        result.add(new Gui.PosMenuItem(backPage, 4, 3));

        // 確定ボタン
        MenuItem confirm = new MenuItem(
                Component.text("販売を確定").color(TextColor.color(0x55FF55)),
                null,
                (menuItem, _player) -> onConfirmPaperClick(sellItem, _player),
                new ItemStack(Material.PAPER),
                null,
                true,
                true,
                true);
        result.add(new Gui.PosMenuItem(confirm, 6, 3));

        int[][] filledArray = {{3, 5}, {5}, {4, 6}};
        result.addAll(generateDisuses(player, filledArray));

        return result;
    }

    private void onConfirmPaperClick(ItemStack sellItem, Player player) {
        DatabaseUtil.createGoodsRecord(sellItem, this.sellPrice, player.getUniqueId().toString());
        player.sendMessage(Component.text("Metazon : アイテムの販売登録が完了しました。"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 1);
    }
    // endregion

    // 不使用スロットを埋める
    private List<Gui.PosMenuItem> generateDisuses(Player player, int[][] filledArray) {
        List<Gui.PosMenuItem> result = new ArrayList<>();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < filledArray.length; y++) {
                int finalX = x;
                if (Arrays.stream(filledArray[y]).filter(v -> v == finalX + 1).findFirst().isEmpty()) {
                    result.add(new Gui.PosMenuItem(
                            MenuItem.generateDisuse(player), x + 1, y + 1));
                }
            }
        }
        return result;
    }
}

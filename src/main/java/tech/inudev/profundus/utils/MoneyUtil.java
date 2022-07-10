package tech.inudev.profundus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tech.inudev.profundus.Profundus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * お金の処理に役立つ処理がまとまっているUtilクラス
 *
 * @author tererun
 */
public class MoneyUtil {

    private static NamespacedKey cashItemKey;
    private static ItemStack base1CashItem;
    private static ItemStack base10CashItem;
    private static ItemStack base100CashItem;
    private static ItemStack base1000CashItem;
    private static ItemStack base10000CashItem;

    static {
        cashItemKey = new NamespacedKey(Profundus.getInstance(), "cash");
        base1CashItem = createBaseCashItem(1);
        base10CashItem = createBaseCashItem(10);
        base100CashItem = createBaseCashItem(100);
        base1000CashItem = createBaseCashItem(1000);
        base10000CashItem = createBaseCashItem(10000);
    }

    /**
     * お金を現金に変換
     * @param amount 変換する金額
     * @return 変換された現金
     */
    public static List<ItemStack> convertToCashItems(int amount) {
        List<ItemStack> cashStacks = new ArrayList<>();
        int moneyAmount = amount;
        int moneyDigit = (int) Math.log(moneyAmount);
        if (moneyDigit > 5) {
            moneyDigit = 5;
        }
        for (int i = moneyDigit; i >= 0; i--) {
            double moneyUnit = Math.pow(10, i);
            ItemStack cashItem = getCashItem((int) moneyUnit, (int) (moneyAmount / moneyUnit));
            cashStacks.add(cashItem);
            moneyAmount %= (int) Math.pow(10, i);
        }
        return cashStacks;
    }

    /**
     * プレイヤーに現金を付与
     * @param player 現金を与えるプレイヤー
     * @param amount 与える金額
     */
    public static void giveCashItems(Player player, int amount) {
        InventoryUtil.addItems(player.getInventory(), convertToCashItems(amount));
    }

    /**
     * お金の単位から現金を取得
     * @param moneyUnit 現金の単位
     * @param itemAmount アイテムの個数
     * @return 現金
     */
    private static ItemStack getCashItem(int moneyUnit, int itemAmount) {
        return switch (moneyUnit) {
            case 1 -> get1CashItem(itemAmount);
            case 10 -> get10CashItem(itemAmount);
            case 100 -> get100CashItem(itemAmount);
            case 1000 -> get1000CashItem(itemAmount);
            case 10000 -> get10000CashItem(itemAmount);
            default -> null;
        };
    }

    /**
     * 1円アイテムを取得
     * @param amount アイテムの個数
     * @return 1円アイテム
     */
    public static ItemStack get1CashItem(int amount) {
        return base1CashItem.asQuantity(amount);
    }

    /**
     * 10円アイテムを取得
     * @param amount アイテムの個数
     * @return 10円アイテム
     */
    public static ItemStack get10CashItem(int amount) {
        return base10CashItem.asQuantity(amount);
    }

    /**
     * 100円アイテムを取得
     * @param amount アイテムの個数
     * @return 100円アイテム
     */
    public static ItemStack get100CashItem(int amount) {
        return base100CashItem.asQuantity(amount);
    }

    /**
     * 1000円アイテムを取得
     * @param amount アイテムの個数
     * @return 1000円アイテム
     */
    public static ItemStack get1000CashItem(int amount) {
        return base1000CashItem.asQuantity(amount);
    }

    /**
     * 10000円アイテムを取得
     * @param amount アイテムの個数
     * @return 10000円アイテム
     */
    public static ItemStack get10000CashItem(int amount) {
        return base10000CashItem.asQuantity(amount);
    }
    
    /**
     * 現金のベースを1個作成
     * @param moneyUnit 現金の単位
     * @return 現金1個
     */
    private static ItemStack createBaseCashItem(int moneyUnit) {
        return createBaseCashItem(moneyUnit, 1);
    }

    /**
     * 現金のベースを作成
     * @param moneyUnit 現金の単位
     * @param itemAmount アイテムの個数
     * @return 現金
     */
    private static ItemStack createBaseCashItem(int moneyUnit, int itemAmount) {
        ItemStack cashItem = new ItemStack(Material.EMERALD, itemAmount);
        ItemMeta cashMeta = cashItem.getItemMeta();
        cashMeta.displayName(Component.text(moneyUnit + " 円").decoration(TextDecoration.ITALIC, false));
        cashMeta.lore(Collections.singletonList(Component.text("ゲーム内通貨").color(NamedTextColor.GREEN)));
        cashMeta.getPersistentDataContainer().set(cashItemKey, PersistentDataType.INTEGER, moneyUnit);
        cashItem.setItemMeta(cashMeta);
        return cashItem;
    }

}

package tech.inudev.profundus.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Inventoryを操作する際に使うであろうメソッドたちをまとめたUtilクラス
 *
 * @author tererun
 */
public class InventoryUtil {

    /**
     * インベントリに複数のアイテムを追加します
     * @param inventory 対象のインベントリ
     * @param itemStacks 追加するアイテム
     */
    public static void addItems(Inventory inventory, List<ItemStack> itemStacks) {
        ItemStack[] arrayStack = new ItemStack[itemStacks.size()];
        itemStacks.toArray(arrayStack);
        inventory.addItem(arrayStack);
    }

}

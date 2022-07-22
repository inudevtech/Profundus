package tech.inudev.profundus.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * アイテムに関する関数(群)
 *
 * @author toru-toruto
 */

public class ItemUtil {

    /**
     * 入らなかったアイテムは吐き出すようにしてある、進化系addItem。
     * インベントリにItemStackを入れる。
     *
     * @param item 入れるアイテム
     * @param inv  入れるインベントリ
     * @param p    吐き出すプレイヤー
     */
    public static void addItem(ItemStack item, Inventory inv, Player p) {
        addItem(item, inv, p.getLocation());
    }

    /**
     * 入らなかったアイテムは吐き出すようにしてある、進化系addItem。
     * インベントリにItemStackを入れる。
     *
     * @param item     入れるアイテム
     * @param inv      入れるインベントリ
     * @param location 吐き出す場所
     */
    public static void addItem(ItemStack item, Inventory inv, Location location) {
        final Map<Integer, ItemStack> map = inv.addItem(item);
        map.values().forEach(e -> location.getWorld().dropItemNaturally(location, e));
    }
}
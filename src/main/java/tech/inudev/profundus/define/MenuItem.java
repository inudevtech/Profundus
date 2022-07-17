package tech.inudev.profundus.define;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * GUIを構成するアイテムを表すクラス
 *
 * @author kumitatepazuru
 */
public class MenuItem {
    @Getter
    private final BiConsumer<MenuItem, Player> onClick;
    @Getter
    private final ItemStack icon;
    @Getter
    private final Object customData;
    @Getter
    private final boolean shiny;
    @Getter
    private final boolean close;
    @Getter
    private final Component title;

    @Getter
    private final List<Component> lore;

    @Getter
    private final boolean draggable;

    /**
     * メニューのアイテム。
     *
     * @param title      アイテムのタイトル
     * @param onClick    クリック時のイベント
     * @param close      クリック時にGUIを閉じるか
     * @param icon       アイテムのブロック
     * @param customData Itemにつける任意のデータ
     * @param shiny      ブロックをキラキラさせるか
     * @param lore       アイテムの説明
     * @param draggable  アイテムをドラッグできるか(統合版のFromAPIでは動作しない)
     */
    public MenuItem(Component title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon, Object customData, boolean shiny, boolean close, boolean draggable) {
        this.onClick = onClick;
        this.icon = icon;
        this.customData = customData;
        this.shiny = shiny;
        this.close = close;
        this.title = title;
        this.lore = lore;
        this.draggable = draggable;

        if (shiny) {
            icon.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
        }

        ItemMeta meta = icon.getItemMeta();
        if (title != null) {
            meta.displayName(title);
        }

        if (lore != null) {
            meta.lore(lore);
        }
        icon.setItemMeta(meta);
    }

    /**
     * メニューのアイテム。
     *
     * @param title      アイテムのタイトル
     * @param lore       アイテムの説明
     * @param onClick    クリック時のイベント
     * @param icon       アイテムのブロック
     * @param customData Itemにつける任意のデータ
     * @param shiny      ブロックをキラキラさせるか
     */
    public MenuItem(String title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon, Object customData, boolean shiny) {
        this(Component.text(title), lore, onClick, icon, customData, shiny, true, false);
    }

    /**
     * メニューのアイテム。
     *
     * @param title      アイテムのタイトル
     * @param lore       アイテムの説明
     * @param onClick    クリック時のイベント
     * @param icon       アイテムのブロック
     * @param customData Itemにつける任意のデータ
     */
    public MenuItem(String title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon, Object customData) {
        this(title, lore, onClick, icon, customData, false);
    }

    /**
     * メニューのアイテム。
     *
     * @param title   アイテムのタイトル
     * @param lore    アイテムの説明
     * @param onClick クリック時のイベント
     * @param icon    アイテムのブロック
     */
    public MenuItem(String title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon) {
        this(title, lore, onClick, icon, null);
    }

    /**
     * メニューのアイテム。
     *
     * @param title   アイテムのタイトル
     * @param onClick クリック時のイベント
     */
    public MenuItem(String title, BiConsumer<MenuItem, Player> onClick) {
        this(title, null, onClick, new ItemStack(Material.STONE));
    }

    /**
     * メニューのアイテム。
     *
     * @param title     アイテムのタイトル
     * @param onClick   クリック時のイベント
     * @param icon      アイテムのブロック
     * @param shiny     ブロックをキラキラさせるか
     * @param close     クリック時にGUIを閉じるか
     * @param draggable アイテムをドラッグできるか(統合版のFromAPIでは動作しない)
     */
    public MenuItem(String title, BiConsumer<MenuItem, Player> onClick, ItemStack icon, boolean shiny, boolean close, boolean draggable) {
        this(Component.text(title), null, onClick, icon, null, shiny, close, draggable);
    }

    /**
     * メニューのアイテム。
     *
     * @param title アイテムのタイトル
     */
    public MenuItem(String title) {
        this(title, null, null, new ItemStack(Material.STONE));
    }

    /**
     * メニューのアイテム。
     *
     * @param title   アイテムのタイトル
     * @param lore    アイテムの説明
     * @param onClick クリック時のイベント
     */
    public MenuItem(String title, List<Component> lore, BiConsumer<MenuItem, Player> onClick) {
        this(title, lore, onClick, new ItemStack(Material.STONE));
    }

    /**
     * メニューのアイテム。
     *
     * @param title     アイテムのタイトル
     * @param onClick   クリック時のイベント
     * @param icon      アイテムのブロック
     * @param draggable アイテムをドラッグできるか(統合版のFromAPIでは動作しない)
     * @param close     クリック時にGUIを閉じるか
     */
    public MenuItem(String title, BiConsumer<MenuItem, Player> onClick, ItemStack icon, boolean close, boolean draggable) {
        this(Component.text(title), null, onClick, icon, null, false, close, draggable);
    }
}

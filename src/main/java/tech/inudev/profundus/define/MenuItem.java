package tech.inudev.profundus.define;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
    private final Component title;
    @Getter
    private List<Component> lore;

    /**
     * アイテムの説明をセットする。
     *
     * @param lore アイテムの新しい説明
     */
    public void setLore(List<Component> lore) {
        this.lore = lore;
        if (!isDraggable() && icon != null) {
            ItemMeta meta = icon.getItemMeta();
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
    }

    @Getter
    private final BiConsumer<MenuItem, Player> onClick;
    @Getter
    private ItemStack icon;

    /**
     * アイテムのブロックをセットする。
     *
     * @param icon アイテムの新しいブロック
     */
    public void setIcon(ItemStack icon) {
        this.icon = icon;

        if (this.icon == null) {
            if (draggable) {
                return;
            } else {
                throw new IllegalArgumentException("draggableがfalseの場合、iconをnullにはできません");
            }
        }
        if (draggable) {
            return;
        }


        ItemMeta meta = this.icon.getItemMeta();
        if (title != null) {
            meta.displayName(title);
        }

        if (lore != null) {
            meta.lore(lore.size() > 0 ? lore : null);
        }

        if (shiny) {
            meta.addEnchant(Enchantment.DURABILITY, 1, false);
        }
        this.icon.setItemMeta(meta);
    }

    @Getter
    private final Object customData;
    @Getter
    private boolean shiny;

    /**
     * アイテムのキラキラをセットする。
     *
     * @param shiny アイテムをキラキラさせるか
     */
    public void setShiny(boolean shiny) {
        this.shiny = shiny;
        if (!isDraggable() && icon != null) {
            ItemMeta meta = icon.getItemMeta();
            if (shiny) {
                if (!meta.hasEnchant(Enchantment.DURABILITY)) {
                    meta.addEnchant(Enchantment.DURABILITY, 1, false);
                }
            } else {
                meta.removeEnchant(Enchantment.DURABILITY);
            }
            icon.setItemMeta(meta);
        }
    }

    @Getter
    private final boolean close;

    @Getter
    private final boolean draggable;

    @Getter
    private final boolean returnOnClose;

    @Getter
    private final boolean cancelReturn;

    /**
     * メニューのアイテム。
     *
     * @param title         アイテムのタイトル
     * @param lore          アイテムの説明
     * @param onClick       クリック時のイベント
     * @param icon          アイテムのブロック
     * @param customData    Itemにつける任意のデータ
     * @param shiny         ブロックをキラキラさせるか
     * @param close         クリック時にGUIを閉じるか
     * @param draggable     アイテムをドラッグできるか(統合版のFromAPIでは動作しない)
     * @param returnOnClose GUIが閉じられたときにアイテムを返却するか
     * @param cancelReturn  クリック時にアイテム返却をキャンセルするか
     */
    public MenuItem(Component title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon, Object customData, boolean shiny, boolean close, boolean draggable, boolean returnOnClose, boolean cancelReturn) {
        this.title = title;
        this.lore = lore;
        this.onClick = onClick;
        this.customData = customData;
        this.shiny = shiny;
        this.close = close;
        this.draggable = draggable;
        this.returnOnClose = returnOnClose;
        this.cancelReturn = cancelReturn;
        setIcon(icon);
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
    public MenuItem(Component title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon, Object customData, boolean shiny) {
        this(title, lore, onClick, icon, customData, shiny, false, false, false, false);
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
    public MenuItem(Component title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon, Object customData) {
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
    public MenuItem(Component title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon) {
        this(title, lore, onClick, icon, null);
    }

    /**
     * メニューのアイテム。
     *
     * @param title   アイテムのタイトル
     * @param lore    アイテムの説明
     * @param onClick クリック時のイベント
     */
    public MenuItem(Component title, List<Component> lore, BiConsumer<MenuItem, Player> onClick) {
        this(title, lore, onClick, new ItemStack(Material.STONE));
    }

    /**
     * メニューのアイテム。
     *
     * @param title   アイテムのタイトル
     * @param onClick クリック時のイベント
     */
    public MenuItem(Component title, BiConsumer<MenuItem, Player> onClick) {
        this(title, null, onClick);
    }

    /**
     * メニューのアイテム。
     *
     * @param title アイテムのタイトル
     */
    public MenuItem(Component title) {
        this(title, null);
    }

    /**
     * メニューのアイテム。
     *
     * @param title         アイテムのタイトル
     * @param onClick       クリック時のイベント
     * @param icon          アイテムのブロック
     * @param close         クリック時にGUIを閉じるか
     * @param draggable     アイテムをドラッグできるか(統合版のFromAPIでは動作しない)
     * @param returnOnClose GUIが閉じられたときにアイテムを返却するか
     */
    public MenuItem(Component title, BiConsumer<MenuItem, Player> onClick, ItemStack icon, boolean close, boolean draggable, boolean returnOnClose) {
        this(title, null, onClick, icon, null, false, close, draggable, returnOnClose, false);
    }

    /**
     * メニューのアイテム。
     *
     * @param title        アイテムのタイトル
     * @param lore         アイテムの説明
     * @param onClick      クリック時のイベント
     * @param icon         アイテムのブロック
     * @param customData   Itemにつける任意のデータ
     * @param shiny        ブロックをキラキラさせるか
     * @param close        クリック時にGUIを閉じるか
     * @param cancelReturn クリック時にアイテム返却をキャンセルするか
     */
    public MenuItem(Component title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon, Object customData, boolean shiny, boolean close, boolean cancelReturn) {
        this(title, lore, onClick, icon, customData, shiny, close, false, false, cancelReturn);
    }

    /**
     * メニューのアイテム。
     *
     * @param title        アイテムのタイトル
     * @param lore         アイテムの説明
     * @param onClick      クリック時のイベント
     * @param icon         アイテムのブロック
     * @param close        クリック時にGUIを閉じるか
     * @param cancelReturn クリック時にアイテム返却をキャンセルするか
     */
    public MenuItem(Component title, List<Component> lore, BiConsumer<MenuItem, Player> onClick, ItemStack icon, boolean close, boolean cancelReturn) {
        this(title, lore, onClick, icon, null, false, close, cancelReturn);
    }


    /**
     * 不使用スロットを埋めるアイテムを生成する。
     *
     * @param player アイテムを表示するGuiを起動したプレイヤー
     * @return 生成された不使用スロットを埋めるアイテム
     */
    public static MenuItem generateDisuse(Player player) {
        ItemStack icon = new ItemStack(Gui.isBedrock(player) ? Material.IRON_BARS : Material.GRAY_STAINED_GLASS_PANE);
        return new MenuItem(Component.text(""), null, null, icon, null, false, false, false, false, false);
    }

    /**
     * draggableなアイテムを生成する。
     *
     * @param onClick クリック時のイベント
     * @param icon    アイテムのブロック
     * @return 生成されたdraggableなアイテム
     */
    public static MenuItem generateDraggable(BiConsumer<MenuItem, Player> onClick, ItemStack icon) {
        return new MenuItem(null, null, onClick, icon, null, false, false, true, true, false);
    }
}

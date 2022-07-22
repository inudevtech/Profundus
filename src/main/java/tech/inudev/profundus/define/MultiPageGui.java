package tech.inudev.profundus.define;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.inudev.profundus.Profundus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * いくらでもMenuItemを追加できるGUI。
 * 場所の指定はできず、順番に並んでいく。
 *
 * @author kumitatepazuru
 */
public class MultiPageGui extends Gui {

    private final List<MenuItem> menuItems = new ArrayList<>();

    @Getter
    @Setter
    private MenuItem centerItem = new MenuItem(Component.text("閉じる"), null, null, new ItemStack(Material.BARRIER));

    @Getter
    private int page = 1;

    /**
     * コンストラクタ
     *
     * @param title GUIのタイトル
     */
    public MultiPageGui(String title) {
        super(title);
    }

    /**
     * GUIのアイテムを追加する
     *
     * @param menuItem 追加するアイテム
     */
    public void addMenuItems(MenuItem... menuItem) {
        menuItems.addAll(Arrays.stream(menuItem).toList());
    }

    /**
     * GUIのアイテムを追加する
     *
     * @param menuItem 追加するアイテム
     */
    public void addMenuItems(Collection<MenuItem> menuItem) {
        menuItems.addAll(menuItem);
    }

    /**
     * MultiPageGuiではGui#addItemが使用できないため、
     * 無効なメソッドでOverride。
     *
     * @param menuItem 使用されない
     * @param x        使用されない
     * @param y        使用されない
     * @deprecated このメソッドは無効です。
     */
    @Override
    public void addItem(MenuItem menuItem, int x, int y) {
        Profundus.getInstance().getLogger().info("MultiPageGuiではaddItemは無効です");
    }

    @Override
    public void open(Player p) {
        if (Gui.isBedrock(p)) {
            Gui gui = new Gui(title);
            gui.setMenuItems(menuItems.stream().map(n -> new Gui.PosMenuItem(n, 0, 0)).collect(Collectors.toList()));
            gui.open(p);
        } else {
            inventory = Bukkit.createInventory(null, 27, Component.text(title));
            update(p);
            Bukkit.getPluginManager().registerEvents(this, Profundus.getInstance());
            p.openInventory(inventory);
        }
    }

    private void update(Player player) {
        inventory.clear();
        final ItemStack back_button = new ItemStack(Material.RED_WOOL);
        ItemMeta itemMeta = back_button.getItemMeta();
        itemMeta.displayName(Component.text("前ページ"));
        back_button.setItemMeta(itemMeta);

        final ItemStack up_button = new ItemStack(Material.GREEN_WOOL);
        itemMeta = up_button.getItemMeta();
        itemMeta.displayName(Component.text("次ページ"));
        up_button.setItemMeta(itemMeta);

        final ItemStack centerButton = centerItem.getIcon();

        inventory.setItem(3, back_button);
        inventory.setItem(4, centerButton);
        inventory.setItem(5, up_button);
        for (int i = 0; i < 9; i++) {
            if (i != 3 && i != 4 && i != 5) {
                inventory.setItem(i, MenuItem.generateDisuse(player).getIcon());
            }
        }

        int count = 0;
        for (MenuItem i : menuItems) {
            if ((page - 1) * 18 - 1 < count && count < page * 18) {
                inventory.setItem(count + 9 - (page - 1) * 18, i.getIcon());
            }
            count++;
        }
        for (int i = count; i < page * 18; i++) {
            inventory.setItem(i + 9 - (page - 1) * 18, MenuItem.generateDisuse(player).getIcon());
        }
    }

    @EventHandler
    @Override
    public void onInventoryClick(InventoryClickEvent e) {
        final Inventory inv = e.getClickedInventory();
        final int id = e.getRawSlot();
        if (inv == null) {
            return;
        }

        if (inv.equals(inventory)) {
            e.setCancelled(true);

            switch (id) {
                case 3 -> {
                    if (page != 1) {
                        page--;
                        update((Player) e.getWhoClicked());
                    }
                }
                case 5 -> {
                    if (page < Math.floor(menuItems.size() / 18f) + 1) {
                        page++;
                        update((Player) e.getWhoClicked());
                    }
                }
                case 4 -> {
                    if (centerItem.getOnClick() != null)
                        centerItem.getOnClick().accept(centerItem, (Player) e.getWhoClicked());
                    if (centerItem.isClose()) {
                        inventory.close();
                    }
                }
            }
            if (id > 8 && id < 27 && e.getCurrentItem() != null) {
                final MenuItem clickedMenuItem = menuItems.get((page - 1) * 18 + id - 9);
                if (clickedMenuItem.getOnClick() != null)
                    clickedMenuItem.getOnClick().accept(clickedMenuItem, (Player) e.getWhoClicked());
                if (clickedMenuItem.isClose()) {
                    inventory.close();
                }
            }
        }
    }
}

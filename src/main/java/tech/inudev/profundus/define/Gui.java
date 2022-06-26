package tech.inudev.profundus.define;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import tech.inudev.profundus.Profundus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * GUIを簡単に作れるようになるクラス。
 *
 * @author kumitatepazuru
 */
public class Gui implements Listener {
    /**
     * 内部で使用するMenuItemに座標データをつけたもの。
     * @param menuItem MenuItem
     * @param x x座標
     * @param y y座標
     */
    record PosMenuItem(MenuItem menuItem, int x, int y) {
    }

    @Setter
    private List<PosMenuItem> menuItems = new ArrayList<>();

    /**
     * GUIのタイトル
     */
    @Getter
    @Setter
    protected String title;

    /**
     * GUIに使うインベントリ
     */
    protected Inventory inventory;

    /**
     * コンストラクタ
     *
     * @param title GUIのタイトル
     */
    public Gui(String title) {
        this.title = title;
    }

    /**
     * GUIにアイテムを追加する。
     *
     * @param menuItem 追加するアイテム
     * @param x        アイテムを設置するX座標。左が0。Java版のみに適応する。
     * @param y        アイテムを設置するY座標。上が0。Java版のみに適応する。
     */
    public void addItem(MenuItem menuItem, int x, int y) {
        menuItems.add(new PosMenuItem(menuItem, x, y));
    }

    public List<PosMenuItem> cloneMenuItems() {
        return new ArrayList<>(menuItems);
    }

    /**
     * GUIを開く。
     *
     * @param player GUIを開くプレイヤー
     */
    public void open(Player player) {
        if (isBedrock(player)) {
            openBedrockImpl(player);
        } else {
            openJavaImpl(player);
        }
    }

    /**
     * GUIを開く。
     *
     * @param player GUIを開くプレイヤー
     * @param forceInventoryGui 統合版でもインベントリGUIを使用するか
     */
    public void open(Player player, boolean forceInventoryGui) {
        if (isBedrock(player) && !forceInventoryGui) {
            openBedrockImpl(player);
        } else {
            openJavaImpl(player);
        }
    }

    /**
     * Playerが統合版か確認する関数
     *
     * @param player プレイヤー
     * @return 統合版ならtrue
     */
    public static boolean isBedrock(Player player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }

    private void openBedrockImpl(Player player) {
        final SimpleForm.Builder builder = SimpleForm.builder().title(title);

        for (PosMenuItem posItem : menuItems) {
            MenuItem item = posItem.menuItem();
            List<String> buttonText = new ArrayList<>();
            if (item.getIcon() != null) {
                String text = item.getIcon().getItemMeta().getDisplayName();
                if (item.isShiny()) {
                    text = text + "§a";
                }
                buttonText.add(text);
                if (item.getIcon().lore() != null) {
                    buttonText.addAll(Objects.requireNonNull(item.getIcon().getLore()));
                }
            }
            if (item.isClose()) {
                builder.button(String.join("\n", buttonText));
            } else {
                builder.content(String.join("\n", buttonText));
            }
        }

        builder.responseHandler((form, data) -> {
            final SimpleFormResponse res = form.parseResponse(data);
            if (!res.isCorrect()) {
                return;
            }

            final List<MenuItem> item = menuItems.stream().map(PosMenuItem::menuItem).toList();
            final int id = Math.toIntExact(res.getClickedButtonId() + item.stream().filter(value -> !value.isClose()).count());
            final BiConsumer<MenuItem, Player> callback = item.get(id).getOnClick();
            if (callback != null) {
                callback.accept(item.get(id), player);
            }
        });

        final FloodgatePlayer fPlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
        fPlayer.sendForm(builder);
    }

    private void openJavaImpl(Player player) {
        inventory = Bukkit.createInventory(null, menuItems.stream().map(PosMenuItem::y).mapToInt(Integer::intValue).max().orElseThrow() * 9, Component.text(title));
        for (PosMenuItem menuItem : menuItems) {
            inventory.setItem(menuItem.x() - 1 + (menuItem.y() - 1) * 9, menuItem.menuItem().getIcon());
        }
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().registerEvents(this, Profundus.getInstance());
        player.openInventory(inventory);
    }

    /**
     * GUIを閉じたときにGCをするリスナー
     *
     * @param e イベント
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(inventory)) {
            // GC
            HandlerList.unregisterAll(this);
        }
    }

    /**
     * GUIをクリックしたときにアイテムの処理をするリスナー
     *
     * @param e イベント
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getClickedInventory();
        if (inv == null) {
            return;
        }
        if (inv.equals(inventory)) {
            e.setCancelled(true);
            // Handle click
            for (PosMenuItem menuItem : menuItems) {
                if (e.getSlot() == menuItem.x() - 1 + (menuItem.y() - 1) * 9) {
                    if (menuItem.menuItem().isDraggable()) {
                        e.setCancelled(false);
                        // 移動後のアイテムを取得するため1tick実行遅延
                        Bukkit.getScheduler().runTaskLater(Profundus.getInstance(), () -> {
                            menuItems.forEach(v -> {
                                if (v.menuItem().isDraggable()) {
                                    int id = v.x() - 1 + (v.y() - 1) * 9;
                                    v.menuItem().setIcon(inventory.getItem(id));
                                }
                            });
                            if (menuItem.menuItem().getOnClick() != null) {
                                menuItem.menuItem().getOnClick().accept(menuItem.menuItem(), (Player) e.getWhoClicked());
                            }
                            if (menuItem.menuItem().isClose()) {
                                inventory.close();
                            }
                        }, 1);
                    } else {
                        if (menuItem.menuItem().getOnClick() != null) {
                            menuItem.menuItem().getOnClick().accept(menuItem.menuItem(), (Player) e.getWhoClicked());
                        }
                        if (menuItem.menuItem().isClose()) {
                            inventory.close();
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDragEvent(InventoryDragEvent e) {

        Profundus.getInstance().getLogger().info(e.getEventName());

        for (int key : e.getNewItems().keySet()) {
            Profundus.getInstance().getLogger().info(
                    key + ":" + e.getNewItems().get(key).getType().name());
        }

//        if (e.getDestination().equals(inventory)) {
//            Profundus.getInstance().getLogger().info("dest");
//        } else if (e.getSource().equals(inventory)) {
//            Profundus.getInstance().getLogger().info("src");
//        }
    }
}

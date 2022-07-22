package tech.inudev.profundus.define;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
import tech.inudev.profundus.utils.ItemUtil;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * GUIを簡単に作れるようになるクラス。
 *
 * @author kumitatepazuru
 */
public class Gui implements Listener {
    /**
     * 内部で使用するMenuItemに座標データをつけたもの。
     *
     * @param menuItem MenuItem
     * @param x        x座標
     * @param y        y座標
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

    @Setter
    private boolean itemReturn = true;

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
     * @param player            GUIを開くプレイヤー
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
        Map<Integer, Integer> bedrockIdMap = new HashMap<>();

        for (int i = 0; i < menuItems.size(); i++) {
            MenuItem item = menuItems.get(i).menuItem();
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
                bedrockIdMap.put(bedrockIdMap.size(), i);
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
            final int id = Math.toIntExact(bedrockIdMap.get(res.getClickedButtonId()));
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

        Bukkit.getPluginManager().registerEvents(this, Profundus.getInstance());
        player.openInventory(inventory);
    }

    /**
     * (x,y)にあるアイテムの説明をセットする。
     *
     * @param x    アイテムのx座標
     * @param y    アイテムのy座標
     * @param lore アイテムの新しい説明
     */
    public void setItemLore(int x, int y, List<Component> lore) {
        for (PosMenuItem menuItem : menuItems) {
            if (menuItem.x() == x && menuItem.y() == y) {
                menuItem.menuItem().setLore(lore);
                inventory.setItem(x - 1 + (y - 1) * 9, menuItem.menuItem().getIcon());
            }
        }
    }

    /**
     * (x,y)にあるアイテムのキラキラをセットする。
     *
     * @param x     アイテムのx座標
     * @param y     アイテムのy座標
     * @param shiny アイテムをキラキラさせるか
     */
    public void setItemShiny(int x, int y, boolean shiny) {
        for (PosMenuItem menuItem : menuItems) {
            if (menuItem.x() == x && menuItem.y() == y) {
                menuItem.menuItem().setShiny(shiny);
                inventory.setItem(x - 1 + (y - 1) * 9, menuItem.menuItem().getIcon());
            }
        }
    }

    /**
     * (x,y)にあるアイテムのItemStackのクローンを取得する。
     * むやみに元のItemStackを変更させないため。
     *
     * @param x アイテムのx座標
     * @param y アイテムのy座標
     * @return (x, y)にあるアイテムのItemStackのクローン
     */
    public ItemStack cloneItemStack(int x, int y) {
        PosMenuItem pos = menuItems.stream().filter(v -> v.x() == x && v.y() == y).findFirst().orElse(null);
        return pos != null ? pos.menuItem().getIcon().clone() : null;
    }

    /**
     * GUIを閉じたときに、アイテムの返還、GCをするリスナー
     *
     * @param e イベント
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(inventory)) {
            // アイテムの返還
            if (itemReturn) {
                menuItems.stream().filter(v -> v.menuItem().isReturnOnClose()).toList()
                        .forEach(v -> {
                            if (v.menuItem().getIcon() != null) {
                                ItemUtil.addItem(v.menuItem().getIcon(), e.getPlayer().getInventory(), (Player) e.getPlayer());
                            }
                        });
            }
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

        // 手持ちのインベントリに対するクリックの場合、draggableなアイテムの更新処理のみ行う
        if (inv != null && !inv.equals(inventory) && e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT) {
            Bukkit.getScheduler().runTaskLater(Profundus.getInstance(), () -> {
                menuItems.forEach(v -> {
                    if (v.menuItem().isDraggable()) {
                        int id = v.x() - 1 + (v.y() - 1) * 9;
                        v.menuItem().setIcon(inventory.getItem(id));
                        if (v.menuItem().getOnClick() != null) {
                            v.menuItem().getOnClick().accept(v.menuItem(), (Player) e.getWhoClicked());
                        }
                    }
                });
            }, 1);
            return;
        }

        if (inv == null || !inv.equals(inventory)) {
            return;
        }
        e.setCancelled(true);
        // Handle click
        for (PosMenuItem menuItem : menuItems) {
            if (e.getSlot() != menuItem.x() - 1 + (menuItem.y() - 1) * 9) {
                continue;
            }
            if (menuItem.menuItem().isCancelReturn()) {
                setItemReturn(false);
            }
            if (menuItem.menuItem().isClose()) {
                inventory.close();
            }
            if (menuItem.menuItem().isDraggable()) {
                e.setCancelled(false);
                // 移動後のアイテムを取得するため1tick実行遅延
                Bukkit.getScheduler().runTaskLater(Profundus.getInstance(), () -> {
                    menuItems.forEach(v -> {
                        if (v.menuItem().isDraggable()) {
                            int id = v.x() - 1 + (v.y() - 1) * 9;
                            v.menuItem().setIcon(inventory.getItem(id));
                            if (v.menuItem().getOnClick() != null) {
                                v.menuItem().getOnClick().accept(v.menuItem(), (Player) e.getWhoClicked());
                            }
                        }
                    });
                }, 1);
            } else {
                if (e.getClick() == ClickType.DOUBLE_CLICK) {
                    return;
                }
                if (menuItem.menuItem().getOnClick() != null) {
                    Player p = (Player) e.getWhoClicked();
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);

                    if (isBedrock((Player) e.getWhoClicked()) && menuItem.menuItem().isClose()) {
                        // 統合版では、Java版インベントリを閉じてから統合版GUIを開くまでに2tickが必要となる
                        Bukkit.getScheduler().runTaskLater(Profundus.getInstance(), () -> {
                            menuItem.menuItem().getOnClick().accept(menuItem.menuItem(), (Player) e.getWhoClicked());
                        }, 2);
                    } else {
                        menuItem.menuItem().getOnClick().accept(menuItem.menuItem(), (Player) e.getWhoClicked());
                    }
                }
            }
        }
    }

    /**
     * GUIをドラッグしたときにアイテムの処理をするリスナー
     *
     * @param e イベント
     */
    @EventHandler
    public void onInventoryDragEvent(InventoryDragEvent e) {
        Inventory inv = e.getInventory();
        if (!inv.equals(inventory)) {
            return;
        }
        for (PosMenuItem menuItem : menuItems) {
            if (!menuItem.menuItem().isDraggable()) {
                continue;
            }
            int id = menuItem.x() - 1 + (menuItem.y() - 1) * 9;
            if (e.getNewItems().containsKey(id)) {
                ItemStack item = e.getNewItems().get(id);
                menuItem.menuItem().setIcon(item);
                if (menuItem.menuItem().getOnClick() != null) {
                    menuItem.menuItem().getOnClick().accept(menuItem.menuItem(), (Player) e.getWhoClicked());
                }
            }
        }
    }
}

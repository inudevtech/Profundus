package tech.inudev.profundus.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;

import tech.inudev.profundus.utils.*;
/**
 * Login関係listener実装クラス
 * @author kidocchy
 *
 */
public class LoginEvent implements Listener {
		/**
		 * PlayerJoin時呼び出し
		 * Userの最終ログインをアップデート
		 * @param e サーバーから渡される。
		 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoinEvent(PlayerJoinEvent e) {
		User.getByPlayer(e.getPlayer(), true).updateLastLogin();

	}

}

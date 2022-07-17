package tech.inudev.profundus.scheduler;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import tech.inudev.profundus.utils.StairSittingUtil;

/**
 * 階段ブロックからプレイヤーが立ち上がった後、再度座れるようになるまで
 * 一定時間のクールダウンを発生させるためのスケジューラ
 *
 * @author toru-toruto
 */
public class SittingCooldownCancellation extends BukkitRunnable {
    private final Player player;

    /**
     * コンストラクタ
     *
     * @param player 一定時間後にクールダウンが解除されるプレイヤー
     */
    public SittingCooldownCancellation(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        if (player == null) {
            return;
        }
        StairSittingUtil.cancelCoolDown(player);
    }
}

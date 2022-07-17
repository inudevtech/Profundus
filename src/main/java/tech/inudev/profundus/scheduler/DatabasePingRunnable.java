package tech.inudev.profundus.scheduler;

import org.bukkit.scheduler.BukkitRunnable;
import tech.inudev.profundus.utils.DatabaseUtil;

/**
 * DatabaseのConnectionが切断されないように定期的にPingを送るためのクラス
 *
 * @author tererun
 */
public class DatabasePingRunnable extends BukkitRunnable {

    @Override
    public void run() {
        DatabaseUtil.ping();
    }

}

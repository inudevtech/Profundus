package tech.inudev.profundus.listener;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.entity.EntityDismountEvent;
import tech.inudev.profundus.utils.StairSittingUtil;

/**
 * 階段ブロックへ座る、階段ブロックから立ち上がる処理を行うためのリスナー
 * 主に以下の場合の処理を行う
 * - プレイヤーが階段ブロックに座ろうとする場合
 * - 階段に座ったプレイヤーが自分の意志で立ち上がる場合
 * - 階段に座ったプレイヤーがサーバーから退出する場合
 * - 階段に座ったプレイヤーがデスした場合
 * - 階段ブロックが破壊された場合
 * - サーバーが停止された場合
 * - 何らかの理由で座席用エンティティが消滅した場合。
 *
 * @author toru-toruto
 */
public class StairSittingListener implements Listener {
    /**
     * プレイヤーがクリックによって他の何かに影響を与えるときに呼び出される。
     * 着席可能なブロックへ、座る処理を実行する。
     *
     * @param e PlayerInteractEventのデータ
     */
    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent e) {
        Block stair = e.getClickedBlock();
        if (!StairSittingUtil.isAbleToSit(stair, e.getAction(), e.getPlayer())) {
            return;
        }
        e.setCancelled(true);

        StairSittingUtil.sitDown(stair, e.getPlayer());
    }

    // 何らかの理由で降りたときのリスナー

    /**
     * エンティティが乗っているエンティティから降りるときに呼び出される。
     * 着席可能なブロックから、立ち上がる処理を実行する。
     * <p>
     * 何らかの出来事の結果として乗り物から降りる場合、他のイベントと共に発火される場合があるが、
     * こちらのイベントはタイミングとして後で発火されるので、
     * 処理内容が全く同一である場合を除いて、他のそれぞれのイベントで処理は実行される。
     *
     * @param e EntityDismountEventのデータ
     */
    @EventHandler
    public void onEntityDismount(EntityDismountEvent e) {
        if (!StairSittingUtil.getSeatEntityList().contains(e.getDismounted())) {
            return;
        }
        e.setCancelled(true);

        StairSittingUtil.standUp(e.getDismounted(), (Player) e.getEntity());
    }

    /**
     * プレイヤーがサーバーから退出する直前に呼び出される。
     * 着席可能なブロックから、立ち上がる処理を実行する。
     * <p>
     * プレイヤー退出時にはEntityDismountEventも実行されるが、
     * そちらのイベントではプレイヤーのテレポートが許されていないので、
     * 退出時の処理はこちらのイベントで実行する
     * （おそらくEntityDismountEventはプレイヤー退出後に発火されるため）。
     * <p>
     * また、EntityDismountEventよりも、こちらのイベントの方が先に実行される。
     *
     * @param e PlayerQuitEventのデータ
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Entity seatEntity = e.getPlayer().getVehicle();
        if (seatEntity == null || !StairSittingUtil.getSeatEntityList().contains(seatEntity)) {
            return;
        }
        StairSittingUtil.standUp(seatEntity, e.getPlayer());
    }

    /**
     * プレイヤーがデスしたときに呼び出される。
     * 着席可能なブロックから、外的要因によって立たされる場合の処理を実行する。
     * <p>
     * プレイヤーデス時にはEntityDismountEventも実行されるが、
     * デスしたプレイヤーがテレポートされるのは不自然なので、
     * デス時の処理はこちらのイベントで実行する。
     * <p>
     * EntityDismountEventよりも、こちらのイベントの方が先に実行される。
     *
     * @param e PlayerDeathEventのデータ
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Entity seatEntity = e.getPlayer().getVehicle();
        if (seatEntity == null || !StairSittingUtil.getSeatEntityList().contains(seatEntity)) {
            return;
        }
        StairSittingUtil.standUpInAccident(seatEntity, e.getPlayer());
    }


    // 階段ブロックの破壊に関するリスナー

    /**
     * ブロックが破壊されたときに呼び出される。
     * プレイヤーが座っているブロックが破壊されたとき、立ち上がる場合の処理を実行する。
     *
     * @param e BlockBreakEventのデータ
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Entity brokenSeatEntity = StairSittingUtil.findSeatEntity(e.getBlock());
        if (brokenSeatEntity == null) {
            return;
        }
        Player player = (Player) brokenSeatEntity.getPassengers()
                .stream().findFirst().orElse(null);
        if (player == null) {
            return;
        }
        StairSittingUtil.standUp(brokenSeatEntity, player);
    }

    /**
     * ブロックがサーバーによって破棄されるときに呼び出される。
     * プレイヤーが座っているブロックが破棄されたとき、立ち上がる場合の処理を実行する。
     *
     * @param e BlockDestroyEventのデータ
     */
    @EventHandler
    public void onBlockDestroy(BlockDestroyEvent e) {
        Entity brokenSeatEntity = StairSittingUtil.findSeatEntity(e.getBlock());
        if (brokenSeatEntity == null) {
            return;
        }
        Player player = (Player) brokenSeatEntity.getPassengers()
                .stream().findFirst().orElse(null);
        if (player == null) {
            return;
        }
        StairSittingUtil.standUp(brokenSeatEntity, player);
    }

    /**
     * ブロックが自然に消滅するときに呼び出される。
     * プレイヤーが座っているブロックが自然消滅したとき、立ち上がる処理を実行する。
     *
     * @param e BlockFadeEventのデータ
     */
    @EventHandler
    public void onBlockFadeEvent(BlockFadeEvent e) {
        Entity brokenSeatEntity = StairSittingUtil.findSeatEntity(e.getBlock());
        if (brokenSeatEntity == null) {
            return;
        }
        Player player = (Player) brokenSeatEntity.getPassengers()
                .stream().findFirst().orElse(null);
        if (player == null) {
            return;
        }
        StairSittingUtil.standUp(brokenSeatEntity, player);
    }

    /**
     * ブロックが燃え尽きるときに呼び出される
     * プレイヤーが座っているブロックが燃え尽きたとき、立ち上がる処理を実行する。
     *
     * @param e BlockBurnEventのデータ
     */
    @EventHandler
    public void onBlockBurn(BlockBurnEvent e) {
        Entity brokenSeatEntity = StairSittingUtil.findSeatEntity(e.getBlock());
        if (brokenSeatEntity == null) {
            return;
        }
        Player player = (Player) brokenSeatEntity.getPassengers()
                .stream().findFirst().orElse(null);
        if (player == null) {
            return;
        }
        StairSittingUtil.standUp(brokenSeatEntity, player);
    }

    /**
     * エンティティが爆発したときに呼び出される。
     * プレイヤーが座っているブロックが爆発に巻き込まれ消滅したとき、外的要因によって立たされる場合の処理を実行する。
     *
     * @param e EntityExplodeEventのデータ
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            if (!StairSittingUtil.isTargetStair(block)) {
                continue;
            }
            Entity brokenSeatEntity = StairSittingUtil.findSeatEntity(block);
            if (brokenSeatEntity == null) {
                return;
            }
            Player player = (Player) brokenSeatEntity.getPassengers()
                    .stream().findFirst().orElse(null);
            if (player == null) {
                return;
            }
            StairSittingUtil.standUpInAccident(brokenSeatEntity, player);
        }
    }

    /**
     * ブロックが爆発したときに呼び出される。
     * プレイヤーが座っているブロックが爆発に巻き込まれ消滅したとき、外的要因によって立たされる場合の処理を実行する。
     *
     * @param e BlockExplodeEventのデータ
     */
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        for (Block block : e.blockList()) {
            if (!StairSittingUtil.isTargetStair(block)) {
                continue;
            }
            Entity brokenSeatEntity = StairSittingUtil.findSeatEntity(block);
            if (brokenSeatEntity == null) {
                return;
            }
            Player player = (Player) brokenSeatEntity.getPassengers()
                    .stream().findFirst().orElse(null);
            if (player == null) {
                return;
            }
            StairSittingUtil.standUpInAccident(brokenSeatEntity, player);
        }
    }
}

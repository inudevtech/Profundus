package tech.inudev.profundus.utils;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import tech.inudev.profundus.Profundus;
import tech.inudev.profundus.scheduler.SittingCooldownCancellation;
import tech.inudev.profundus.scheduler.StairSeatElimination;

import java.util.*;

/**
 * 階段への座る・立ち上がる処理を管理するクラス
 *
 * @author toru-toruto
 */
public class StairSittingUtil {
    // 座席エンティティの管理用。
    @Getter
    private static final List<Entity> seatEntityList = new ArrayList<>();
    // 階段ブロックのメタデータ削除用。座席用エンティティが消去される時に階段ブロックのメタデータを削除する。
    private static final HashMap<UUID, Block> entityToBlockMap = new HashMap<>();
    // 座席用エンティティ永続化防止のためのタスクのキャンセル用。座席エンティティが消去される時にキャンセルする。
    private static final HashMap<UUID, BukkitTask> eliminationTaskMap = new HashMap<>();

    private static final String SEAT_METADATA_KEY = "SEAT_ENTITY_UUID";
    private static final String COOLDOWN_METADATA_KEY = "SITTING_COOLDOWN";

    private static final Vector STAND_UP_OFFSET = new Vector(0, 1.0, 0);
    private static final long ELIMINATION_DELAY = 20 * 3600 * 2;
    private static final long COOLDOWN_TIME = 10;

    /**
     * 指定されたブロックに座れるかどうかのチェック
     *
     * @param block  座る対象のブロック
     * @param action ブロックに対するプレイヤーのアクション
     * @param player 座ろうとするプレイヤー
     * @return 階段ブロックに座れるならばtrue
     */
    public static boolean isAbleToSit(Block block, Action action, Player player) {
        // 階段ブロックでなければ座れない
        if (block == null) {
            return false;
        }
        // 右クリックでなければ座れない
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        // しゃがんでいたら座れない
        if (player == null || player.isSneaking()) {
            return false;
        }
        // 指定の階段ブロックでなければ座れない
        if (!isTargetStair(block)) {
            return false;
        }
        // 階段ブロックが逆さならば座れない
        Bisected.Half dir = ((Stairs) block.getBlockData()).getHalf();
        if (dir == Bisected.Half.TOP) {
            return false;
        }
        // 上が開いてなければ座れない
        Location upLoc = block.getLocation().clone().add(0, 1.0, 0);
        if (upLoc.getBlock().getType() != Material.AIR) {
            return false;
        }
        // だれかが先に座っていたら座れない
        if (findSeatEntity(block) != null) {
            return false;
        }
        // クールダウン中なら座れない
        return !isCoolDown(player);
    }

    /**
     * 指定されたブロックが設定された階段ブロックであるかをチェックする。
     *
     * @param block チェック対象のブロック
     * @return 階段ブロックであればtrue。そうでなければfalseを返す。
     */
    public static boolean isTargetStair(Block block) {
        if (block == null) {
            throw new IllegalArgumentException();
        }
        return Profundus.getInstance().getStairsHandler()
                .getStairList().contains(block.getType().toString());
    }

    /**
     * 指定されたブロックに設定された座席用エンティティ参照用メタデータをもとに、
     * 座席用エンティティのリストから対応する要素を取得する。
     *
     * @param block ブロック
     * @return メタデータから参照したエンティティを返す。参照に失敗すればnullを返す。
     */
    public static Entity findSeatEntity(Block block) {
        if (block == null) {
            throw new IllegalArgumentException();
        }
        String seatEntityUUID = "";
        for (MetadataValue v : block.getMetadata(SEAT_METADATA_KEY)) {
            if (v.getOwningPlugin() == null) {
                continue;
            }
            if (v.getOwningPlugin().getName().equals(Profundus.getInstance().getName())) {
                seatEntityUUID = v.asString();
                break;
            }
        }
        if (seatEntityUUID.isEmpty()) {
            return null;
        }

        String finalSeatEntityUUID = seatEntityUUID;
        Optional<Entity> entityOpt = seatEntityList.stream()
                .filter(ent -> ent.getUniqueId().toString().equalsIgnoreCase(finalSeatEntityUUID))
                .findFirst();
        return entityOpt.orElse(null);
    }

    private static boolean isCoolDown(Player player) {
        if (player == null) {
            throw new IllegalArgumentException();
        }
        for (MetadataValue v : player.getMetadata(COOLDOWN_METADATA_KEY)) {
            if (v.getOwningPlugin() == null) {
                continue;
            }
            if (v.getOwningPlugin().getName().equals(Profundus.getInstance().getName())) {
                return v.asBoolean();
            }
        }
        return false;
    }

    /**
     * 座る処理
     *
     * @param stair  座る対象の階段ブロック
     * @param player 座るプレイヤー
     */
    public static void sitDown(Block stair, Player player) {
        if (stair == null || player == null) {
            throw new IllegalArgumentException();
        }
        LivingEntity seatEntity = createSeatEntity(player, getSeatLocation(stair));
        seatEntity.addPassenger(player);

        // 参照用データの保存S
        seatEntityList.add(seatEntity);
        entityToBlockMap.put(seatEntity.getUniqueId(), stair);
        stair.setMetadata(SEAT_METADATA_KEY, new FixedMetadataValue(
                Profundus.getInstance(),
                seatEntity.getUniqueId().toString()));

        // 座席Entity永続化防止のタスクを設定
        BukkitTask task = new StairSeatElimination(seatEntity).runTaskLater(
                Profundus.getInstance(),
                ELIMINATION_DELAY);
        eliminationTaskMap.put(seatEntity.getUniqueId(), task);
    }

    private static Location getSeatLocation(Block stair) {
        if (stair == null) {
            throw new IllegalArgumentException();
        }
        // 階段ブロックの中心位置へ（BATの場合、すこしだけ下へ）
        Vector offsetToCenter = new Vector(0.5, -0.4, 0.5);
        // 階段ブロック中心から少しだけ前方へ
        double forwardOffset = 0.2;

        Location seatLoc = stair.getLocation().clone();
        // 階段ブロックは椅子の背もたれ方向が前方
        Vector seatDir = ((Stairs) stair.getBlockData()).getFacing().getDirection().clone();
        seatDir = new Vector(-seatDir.getX(), 0, -seatDir.getZ());

        seatLoc.add(
                offsetToCenter.getX() + forwardOffset * seatDir.getX(),
                offsetToCenter.getY() + forwardOffset * seatDir.getY(),
                offsetToCenter.getZ() + forwardOffset * seatDir.getZ());
        seatLoc.setDirection(seatDir);
        return seatLoc;
    }

    private static LivingEntity createSeatEntity(Player player, Location seatLoc) {
        if (player == null || seatLoc == null) {
            throw new IllegalArgumentException();
        }
        Bat bat = (Bat) player.getWorld().spawnEntity(seatLoc, EntityType.BAT);
        Objects.requireNonNull(bat.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(1);
        bat.setSilent(true);
        bat.setInvisible(true);
        bat.setPersistent(true);
        bat.setInvulnerable(true);
        bat.setAwake(true);
        bat.setAI(false);
        return bat;
    }

    /**
     * 通常の立ち上がる処理
     *
     * @param seatEntity 座席用エンティティ
     * @param player     立ち上がるプレイヤー
     */
    public static void standUp(Entity seatEntity, Player player) {
        if (seatEntity == null || player == null) {
            throw new IllegalArgumentException();
        }
        removeSeat(seatEntity);
        player.teleport(player.getLocation().add(STAND_UP_OFFSET));
        setCoolDown(player);
    }

    /**
     * アクシデントでまともに立ち上がれない場合の立ち上がる処理
     *
     * @param seatEntity 座席用エンティティ
     * @param player     立ち上がるプレイヤー
     */
    public static void standUpInAccident(Entity seatEntity, Player player) {
        if (seatEntity == null || player == null) {
            throw new IllegalArgumentException();
        }
        removeSeat(seatEntity);
        setCoolDown(player);
    }

    private static void removeSeat(Entity seatEntity) {
        if (seatEntity == null) {
            throw new IllegalArgumentException();
        }
        Block stair = entityToBlockMap.get(seatEntity.getUniqueId());
        if (stair == null) {
            throw new IllegalArgumentException();
        }
        stair.removeMetadata(SEAT_METADATA_KEY, Profundus.getInstance());
        entityToBlockMap.remove(seatEntity.getUniqueId());
        seatEntityList.remove(seatEntity);
        seatEntity.remove();

        eliminationTaskMap.get(seatEntity.getUniqueId()).cancel();
        eliminationTaskMap.remove(seatEntity.getUniqueId());
    }

    private static void setCoolDown(Player player) {
        if (player == null) {
            throw new IllegalArgumentException();
        }
        player.setMetadata(COOLDOWN_METADATA_KEY, new FixedMetadataValue(
                Profundus.getInstance(),
                true));

        new SittingCooldownCancellation(player).runTaskLater(
                Profundus.getInstance(),
                COOLDOWN_TIME);
    }

    /**
     * 階段ブロックからプレイヤーが立ち上がった後、再度座れるようになるまでのクールダウンの終了
     *
     * @param player クールダウン中のプレイヤー
     */
    public static void cancelCoolDown(Player player) {
        if (player == null) {
            throw new IllegalArgumentException();
        }
        player.removeMetadata(
                COOLDOWN_METADATA_KEY,
                Profundus.getInstance());
    }

    /**
     * リストに保存される座席用Entityをすべてremoveする。サーバー停止時に使用。
     */
    public static void removeSeatsOnServerDisable() {
        for (Entity entity : new ArrayList<>(seatEntityList)) {
            entityToBlockMap.get(entity.getUniqueId()).removeMetadata(
                    SEAT_METADATA_KEY, Profundus.getInstance());
            Player player = (Player) entity.getPassengers().stream().findFirst().orElse(null);
            if (player != null) {
                standUp(entity, player);
            }
        }
    }
}

package tech.inudev.metaverseplugin.define;

import lombok.Data;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tech.inudev.metaverseplugin.utils.DatabaseUtil;

import java.util.UUID;

/**
 * お金の取引を処理するためのクラス
 *
 * @author toru-toruto
 */
public class Money {
    @Getter
    private int amount;
    private UUID playerUUID;
    private String bankName;
    private final boolean isBankMoney;

    /**
     * プレイヤーの所持金を使用する場合のコンストラクタ
     *
     * @param playerUUID プレイヤーのUUID
     */
    public Money(UUID playerUUID) {
        Integer amount = DatabaseUtil.loadMoneyAmount(playerUUID.toString());
        if (amount == null) {
            throw new IllegalArgumentException("引数に対応するデータが存在しません。");
        }
        this.amount = amount;
        this.playerUUID = playerUUID;
        this.isBankMoney = false;
    }

    /**
     * プレイヤーの口座上のお金を使用する場合のコンストラクタ
     *
     * @param bankName 口座の名前
     */
    public Money(String bankName) {
        if (isUUID(bankName)) {
            throw new IllegalArgumentException("UUID形式の文字列は引数に指定できません。");
        }

        Integer amount = DatabaseUtil.loadMoneyAmount(bankName);
        if (amount == null) {
            throw new IllegalArgumentException("引数に対応するデータが存在しません。");
        }
        this.amount = amount;
        this.bankName = bankName;
        this.isBankMoney = true;
    }

    /**
     * 金額への加算
     *
     * @param value 加算する金額
     */
    public void add(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("負の値は引数に指定できません。");
        }
        this.amount += value;
    }

    /**
     * 金額への減算
     * 減算するお金が足りない場合、プレイヤーへ通知する
     *
     * @param value 減算する金額
     * @return 正常に処理できたかどうか
     */
    public boolean remove(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("負の値は引数に指定できません。");
        }
        if (this.amount >= value) {
            this.amount -= value;
            return true;
        } else {
            if (this.isBankMoney || playerUUID == null) {
                return false;
            }
            // 所持金による取引の場合、プレイヤーへお金不足を通知
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                player.sendMessage(Component.text(
                        "取引するためのお金が足りません"));
            }
            return false;
        }
    }

    /**
     * 取引後の金額をDatabaseへ反映する
     *
     * @return 正常終了したかどうか
     */
    public boolean push() {
        if (this.isBankMoney) {
            if (this.bankName.isEmpty()) {
                return false;
            }
            DatabaseUtil.updateMoneyAmount(this.bankName, this.amount);
        } else {
            if (this.playerUUID == null) {
                return false;
            }
            DatabaseUtil.updateMoneyAmount(this.playerUUID.toString(), this.amount);
        }
        return true;
    }

    /**
     * 所持金データを作成する
     *
     * @param playerUUID プレイヤーのUUID
     */
    public static void createPlayerWallet(UUID playerUUID) {
        if (playerWalletExists(playerUUID)) {
            throw new IllegalArgumentException("プレイヤーの所持金データが既に存在しています。");
        }
        DatabaseUtil.createMoneyRecord(playerUUID.toString());
    }

    /**
     * 口座を開設する
     *
     * @param bankName 口座の名前
     */
    public static void createBankAccount(String bankName) {
        if (bankAccountExists(bankName)) {
            throw new IllegalArgumentException("同名の口座が既に存在しています。");
        }
        DatabaseUtil.createMoneyRecord(bankName);
    }

    /**
     * データベース上に所持金データが存在するかを判定する。
     *
     * @param playerUUID プレイヤーのUUID
     * @return データベース上に所持金データが存在していればtrue、そうでなければfalseを返す。
     */
    public static boolean playerWalletExists(UUID playerUUID) {
        return DatabaseUtil.loadMoneyAmount(playerUUID.toString()) != null;
    }

    /**
     * データベース上に口座データが存在するかを判定する。
     *
     * @param bankName 口座名
     * @return データベース上に口座データが存在していればtrue、そうでなければfalseを返す。
     */
    public static boolean bankAccountExists(String bankName) {
        if (isUUID(bankName)) {
            throw new IllegalArgumentException("UUID形式の文字列は引数に指定できません。");
        }
        return DatabaseUtil.loadMoneyAmount(bankName) != null;
    }

    /**
     * 指定された文字列がUUIDの形式であるか判定する
     *
     * @param name 文字列
     * @return UUIDの形式であればtrue、そうでなければfalseを返す。
     */
    public static boolean isUUID(String name) {
        String regex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
        return name.toLowerCase().matches(regex);
    }
}

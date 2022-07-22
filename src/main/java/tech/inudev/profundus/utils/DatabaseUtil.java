package tech.inudev.profundus.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import tech.inudev.profundus.Profundus;
import tech.inudev.profundus.config.ConfigHandler;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Databaseを管理するためのクラス
 * 絶対にConfigHandler初期化後に使用すること
 *
 * @author tererun
 */

public class DatabaseUtil {

    private static Connection connection;

    private static final String databaseUrl;

    static {
        ConfigHandler configHandler = Profundus.getInstance().getConfigHandler();
        if (configHandler.getDatabaseType().equals("mysql")) {
            databaseUrl = "jdbc:mysql://$address/$name?useUnicode=true&characterEncoding=utf8&autoReconnect=true&maxReconnects=10&useSSL=false"
                    .replace("$address", configHandler.getDatabaseAddress())
                    .replace("$name", configHandler.getDatabaseName());
        } else if (configHandler.getDatabaseType().equals("sqlite")) {
            databaseUrl = "jdbc:sqlite:$path".replace("$path", Profundus.getInstance().getDataFolder().getPath() + "/database.db");
        } else {
            throw new IllegalArgumentException("Invalid database type");
        }
    }

    /**
     * Databaseに接続する
     */
    public static void connect() {
        ConfigHandler configHandler = Profundus.getInstance().getConfigHandler();
        try {
            connection = DriverManager.getConnection(databaseUrl, configHandler.getDatabaseUsername(), configHandler.getDatabasePassword());
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Databaseから切断する
     */
    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * DatabaseにPingを送信する
     * Connectionが切断されないように送信する用
     */
    public static void ping() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    VALUES('ping', current_timestamp)
                    """);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private static void createMoneyTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS 'money' (
                            'name' VARCHAR(36) NOT NULL,
                            'amount' INT NOT NULL,
                            PRIMARY KEY ('name'))
                        """);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            try {
                connection.rollback();
                Profundus.getInstance().getLogger().info("rollback");
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    /**
     * Databaseに新たに金額データを登録する。
     * 処理が完了した場合は、トランザクションをコミットする。
     * 一方で、処理中にエラーが発生した場合は、トランザクションをロールバックする。
     *
     * @param name 金額データの名前
     */
    public static void createMoneyRecord(String name) {
        try {
            createMoneyTable();

            if (loadMoneyAmount(name) != null) {
                throw new SQLException();
            }

            PreparedStatement preparedStatement = connection.prepareStatement("""
                    INSERT INTO money (name, amount) VALUES (?, 0)
                    """);
            preparedStatement.setString(1, name);
            preparedStatement.execute();
            preparedStatement.close();

            commitTransaction();
        } catch (SQLException e) {
            try {
                connection.rollback();
                Profundus.getInstance().getLogger().info("rollback");
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    /**
     * Databaseのnameに対応する金額データを取得する。
     * 処理中にエラーが発生した場合は、トランザクションをロールバックする。
     *
     * @param name Databaseの検索に使用する金額データの名前
     * @return 金額。Database上にデータが存在しなければnullを返す
     */
    public static Integer loadMoneyAmount(String name) {
        try {
            createMoneyTable();

            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT * FROM money WHERE name=?
                    """);
            preparedStatement.setString(1, name);
            ResultSet resultSet = preparedStatement.executeQuery();

            Integer result = resultSet.next() ? resultSet.getInt("amount") : null;
            preparedStatement.close();
            return result;
        } catch (SQLException e) {
            try {
                connection.rollback();
                Profundus.getInstance().getLogger().info("rollback");
                return null;
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    /**
     * 送金処理のトランザクションを実行する。
     * 処理が完了した場合は、トランザクションをコミットする。
     * 一方で、処理中にエラーが発生した場合は、トランザクションはロールバックする。
     *
     * @param selfName      自分の金額データの名前
     * @param selfAmount    自分の金額データの金額
     * @param partnerName   相手の金額データの名前
     * @param partnerAmount 相手の金額データの金額
     */
    public static void remitTransaction(
            String selfName,
            int selfAmount,
            String partnerName,
            int partnerAmount) {
        updateMoneyAmount(selfName, selfAmount);
        updateMoneyAmount(partnerName, partnerAmount);
        commitTransaction();
    }

    private static void updateMoneyAmount(String bankName, int amount) {
        try {
            createMoneyTable();

            if (loadMoneyAmount(bankName) == null) {
                throw new SQLException();
            }

            PreparedStatement preparedStatement = connection.prepareStatement("""
                    UPDATE money SET amount=? WHERE name=?
                    """);
            preparedStatement.setInt(1, amount);
            preparedStatement.setString(2, bankName);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            try {
                connection.rollback();
                Profundus.getInstance().getLogger().info("rollback");
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    private static void createGoodsTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS 'goods' (
                            'id' INT AUTO_INCREMENT,
                            'item' VARBINARY(511) NOT NULL,
                            'price' INT NOT NULL,
                            'seller' VARCHAR(36) NOT NULL,
                            PRIMARY KEY ('id'))
                            """);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            try {
                connection.rollback();
                Profundus.getInstance().getLogger().info("rollback");
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    /**
     * Metazonの商品を新たに登録する。
     *
     * @param item   商品アイテム
     * @param price  商品の価格
     * @param seller 出品者の名前（プレイヤーであればUUIDの文字列)
     */
    public static void createGoodsRecord(ItemStack item, int price, String seller) {
        if (item == null) {
            throw new IllegalArgumentException();
        }
        try {
            createGoodsTable();

            PreparedStatement preparedStatement = connection.prepareStatement("""
                    INSERT INTO goods (item, price, seller)
                    VALUES (?, ?, ?)
                    """);
            preparedStatement.setBytes(1, item.serializeAsBytes());
            preparedStatement.setInt(2, price);
            preparedStatement.setString(3, seller);
            preparedStatement.execute();
            preparedStatement.close();

            commitTransaction();
        } catch (SQLException e) {
            try {
                connection.rollback();
                Profundus.getInstance().getLogger().info("rollback");
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    /**
     * Metazonの商品データ（商品アイテム、価格、出品者）をまとめたもの。
     *
     * @param item   商品アイテム
     * @param price  価格
     * @param seller 出品者
     */
    public record GoodsData(ItemStack item, int price, String seller) {

    }

    /**
     * Metazonの商品データのリストを取得する。
     *
     * @return 商品データのリスト
     */
    public static List<GoodsData> loadGoodsList() {
        try {
            createGoodsTable();

            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT * FROM goods
                    """);
            ResultSet resultSet = preparedStatement.executeQuery();

            List<GoodsData> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new GoodsData(
                        ItemStack.deserializeBytes(resultSet.getBytes("item")),
                        resultSet.getInt("price"),
                        resultSet.getString("seller")));
            }
            preparedStatement.close();
            return result;
        } catch (SQLException e) {
            try {
                connection.rollback();
                Profundus.getInstance().getLogger().info("rollback");
                return null;
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    private static void commitTransaction() {
        try {
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
                Profundus.getInstance().getLogger().info("rollback");
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }
}

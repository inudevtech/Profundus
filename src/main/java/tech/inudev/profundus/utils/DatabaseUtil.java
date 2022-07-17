package tech.inudev.profundus.utils;

import tech.inudev.profundus.Profundus;
import tech.inudev.profundus.config.ConfigHandler;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

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
        if(configHandler.getDatabaseType().equals("mysql")) {
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
            Profundus.getInstance().getLogger().log(Level.SEVERE, "Couldn't connect DB");
            throw new RuntimeException(e);
        }
    }

    /**
     * Databaseから切断する
     */
    public static void disconnect() {
        try {
            connection.close();
            connection = null;
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

    /**
     * Databaseに新たに金額データを登録する。
     * 処理が完了した場合は、トランザクションをコミットする。
     * 一方で、処理中にエラーが発生した場合は、トランザクションをロールバックする。
     *
     * @param name 金額データの名前
     */
    public static void createMoneyRecord(String name) {
        try {
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

            ResultSet resultSet = select(Table.MONEY,"name='"+name+"'");
            Integer result = resultSet.next() ? resultSet.getInt("amount") : null;
            resultSet.close();
            return result;
        } catch (SQLException e) {
            try {
                connection.rollback();
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
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }
/**
 * 確実にconnectedなインスタンスを取得する。
 * @return
 */
    static Connection getConnection(){
    	try {
			if (connection == null || connection.isClosed()) {connect();}
    	} catch(SQLException e) {
    		Profundus.getInstance().getLogger().log(Level.WARNING,"getConnection:" + e);
    	}
    	return connection;
    }
/**
 * SQLデータベースで扱うテーブル一覧
 * @author kidocchy
 *
 */
    public enum Table{
    	/**
    	 * SQLテーブル名:USER
    	 */
	USER,	//ユーザー
	/**
	 * SQLテーブル名:ITEM
	 */
	ITEM,	//アイテム
	/**
	 * SQLテーブル名:PFID
	 */
	PFID,	//Profundus ID
	/**
	 * SQLテーブル名:MONEY
	 */
	MONEY;
    }

    /**
     * テーブル作成。
     * 初回ぐらいしか呼ばないと思うのでまとめました。
     * @param tableName Table enum
     * @param dropIfExists true=>存在しても再作成(初期化) 
     * @return success?
     * TODO リリース時には第二引数dropIfExistsは削除。
     */
    public static Boolean createTable(Table tableName, Boolean dropIfExists) {
    	StringBuilder sql = new StringBuilder();
    	sql.append("CREATE TABLE IF NOT EXISTS " + tableName.name() +" (");

    	switch(tableName) {
    	case USER:
    		sql.append("""
    				seqID INTEGER PRIMARY KEY AUTOINCREMENT,
    				mostSignificantPFID BIGINT NOT NULL,
    				leastSignificantPFID BIGINT NOT NULL,
    				screenName VARCHAR NOT NULL,
    				mostSignificantUUID BIGINT NOT NULL,
    				leastSignificantUUID BIGINT NOT NULL,
    				createdAt TIMESTAMP,
    				lastLogin TIMESTAMP,
    				language1 VARCHAR,
    				language2 VARCHAR,
    				note TEXT
    				""");
    		break;
    	case PFID:
    		sql.append("""
    				seqID INTEGER PRIMARY KEY AUTOINCREMENT,
    				mostSignificantPFID BIGINT NOT NULL,
    				leastSignificantPFID BIGINT NOT NULL,
    				type VARCHAR NOT NULL,
    				createdAt TIMESTAMP NOT NULL
    				""");
    		break;
    	case ITEM:
    		sql.append("""
    				seqID INTEGER PRIMARY KEY AUTOINCREMENT,
    				mostSignificantOwnerPFID BIGINT NOT NULL,
    				leastSignificantOwnerPFID BIGINT NOT NULL,
    				mostSignificantPFID BIGINT NOT NULL,
    				leastSignificantPFID BIGINT NOT NULL,
     				mostSignificantUUID BIGINT NOT NULL,
    				leastSignificantUUID BIGINT NOT NULL,
       				note VARCHAR,
       				type VARCHAR,
    				createdAt TIMESTAMP NOT NULL
    				""");
    		break;
    	case MONEY:
    		sql.append("""
                    'name' VARCHAR(36) NOT NULL,
                    'amount' INT NOT NULL,
                    PRIMARY KEY ('name')
    				""");
    		break;

    	}
    	sql.append(");");
    	
    	Connection con = getConnection();
	    try {
	    	//TODO リリース時にはこのIFブロック丸々削除
	    	if(dropIfExists) {
		    	Statement st = con.createStatement();
		    	st.executeUpdate("DROP TABLE IF EXISTS " + tableName.name());
		    	con.commit();
		        Profundus.getInstance().getLogger().log(Level.INFO,"DropTable  : " + tableName.name());
		    	st.close();
	    	}

	    	Statement st = con.createStatement();
	        st.executeUpdate(sql.toString());
	        con.commit();
	        st.close();
			Profundus.getInstance().getLogger().log(Level.INFO, "Table created/exists : " + tableName.name() );
	        return true;
	    } catch (SQLException e) {
	    	Profundus.getInstance().getLogger().log(Level.WARNING,e.toString());
	        try {
	            con.rollback();
	        } catch (SQLException e2) {
                throw new RuntimeException(e2);
	        }
	        return false;
	    }
    }
    /**
     * PFID関連　INSERT
     * @param pfid
     * @param type
     * @return success?
     */
    static Boolean insertPFIDEntry(UUID pfid, Table type) {
    	StringBuilder sql = new StringBuilder();
    	sql.append("INSERT INTO " + Table.PFID.name());
    	sql.append("""
    			(
    			mostSignificantPFID,
    			leastSignificantPFID,
    			type,
    			createdAt
    			) VALUES(?, ?, ?, ?)
    			""");
    	Connection con = getConnection();

	    try {

	    	PreparedStatement preparedStatement = con.prepareStatement(sql.toString());

	    	preparedStatement.setLong(1, pfid.getMostSignificantBits());
	    	preparedStatement.setLong(2, pfid.getLeastSignificantBits());
	    	preparedStatement.setString(3, type.name());
	    	preparedStatement.setTimestamp(4, Timestamp.from(Instant.now()) );
	        int res = preparedStatement.executeUpdate();
	        con.commit();
	        preparedStatement.close();
	        return true;
	    } catch (SQLException e) {
	    	Profundus.getInstance().getLogger().log(Level.WARNING,"insertPFIDEntry: " + e);

	        try {
	            con.rollback();
	        } catch (SQLException e2) {
                throw new RuntimeException(e2);
	        }
	        return false;
      }
    }
    
    /**
     * User関連　INSERT
     * @param user
     * @return success?
     */
    static Boolean insertUserEntry(User user) {
    	StringBuilder sql = new StringBuilder();
    	sql.append("INSERT INTO " + Table.USER.name());
    	sql.append(" (mostSignificantPFID, leastSignificantPFID, screenName, mostSignificantUUID, leastSignificantUUID, createdAt, lastLogin) VALUES (?,?,?,?,?,?,?)");
    	Connection con = getConnection();
    	try {
	    	PreparedStatement preparedStatement = con.prepareStatement(sql.toString());
	    	preparedStatement.setLong(1, user.pfid.getMostSignificantBits());
	    	preparedStatement.setLong(2, user.pfid.getLeastSignificantBits());
	    	preparedStatement.setString(3, user.screenName);
	    	preparedStatement.setLong(4, user.uuid.getMostSignificantBits());
	    	preparedStatement.setLong(5, user.uuid.getLeastSignificantBits());
	    	preparedStatement.setTimestamp(6, Timestamp.from(user.createdAt));
	    	preparedStatement.setTimestamp(7, Timestamp.from(user.getLastLogin()));
	    	int r = preparedStatement.executeUpdate();
	        con.commit();
	    	preparedStatement.close();
	    	Profundus.getInstance().getLogger().log(Level.INFO,r +" rows inserted to USER");
	    	return true;
    	}catch(SQLException e) {
	    	Profundus.getInstance().getLogger().log(Level.WARNING,"insertUserEntry:" + e);

	        try {
	            con.rollback();
	        } catch (SQLException e2) {
                throw new RuntimeException(e2);
	        }
	        return false;
    	}
    }
    
    /**
     * 汎用SELECT文
     * @param tableName
     * @param query
     * @return success?
     */
    static ResultSet select(Table tableName, String query){
    	StringBuilder sql = new StringBuilder();
    	sql.append("SELECT * FROM ");
    	sql.append(tableName.name());
    	if(query!=null) {sql.append(" WHERE " + query);}
       	Connection con = getConnection();
	    try {
	    	PreparedStatement preparedStatement = con.prepareStatement(sql.toString());
	        ResultSet rs = preparedStatement.executeQuery();
	        return rs;
	    } catch (SQLException e) {
	    	Profundus.getInstance().getLogger().log(Level.WARNING,e.toString());
	        return null;
	    }
    }

    /**
     * PFIDテーブルのみ検索高速化のために，専用のpreparedStatementを準備。
     */
    private static PreparedStatement psPFID;
    private static PreparedStatement preparePsPFID() {
   		try {
   			if(psPFID==null || psPFID.isClosed()) {
   		       	Connection con = getConnection();
       			psPFID = con.prepareStatement("""
       					SELECT PFID.type, USER.screenName FROM PFID
       					LEFT JOIN USER
       					ON(
	       					PFID.mostSignificantPFID = USER.mostSignificantPFID
	       					AND
	       					PFID.leastSignificantPFID = USER.leastSignificantPFID
       					)
       					WHERE
       					PFID.mostSignificantPFID = ?
       					AND
       					PFID.leastSignificantPFID = ?
       					""");
   			}
   		} catch (SQLException e) {
   			Profundus.getInstance().getLogger().log(Level.WARNING,"prepare-psPFID:" + e);
   		}
   		return psPFID;
    }
    
    /**
     * UUID / PFIDでtableNameテーブルを検索
     * @param tableName Table Enum
     * @param idName UUID or PFID
     * @param uuid
     * @return java.sql.ResultSet
     */
    static java.sql.ResultSet selectUUID(Table tableName, String idName, UUID uuid){
	    try {
	    	PreparedStatement preparedStatement;
	    	if(tableName == Table.PFID) {
	    		preparedStatement = preparePsPFID();
	    	}else {
	           	Connection con = getConnection();

	        	StringBuilder sql = new StringBuilder();
	        	sql.append("SELECT * FROM ");
	        	sql.append(tableName.name());
	        	sql.append(" WHERE mostSignificant"+idName+" = ?");
	        	sql.append(" AND leastSignificant"+idName+" = ?");
		    	preparedStatement = con.prepareStatement(sql.toString());
	    	}
	    	preparedStatement.setLong(1, uuid.getMostSignificantBits());
	    	preparedStatement.setLong(2, uuid.getLeastSignificantBits());
	    	
	        ResultSet rs = preparedStatement.executeQuery();
	        return rs;
	    } catch (SQLException e) {
	    	Profundus.getInstance().getLogger().log(Level.WARNING,"selectUUID: " + e);
	        return null;
	    }
    }

    /**
     * USER関連　UPDATE
     * @param user
     * @return
     */
    static Boolean updateUserEntry(User user) {
      StringBuilder sql = new StringBuilder();
      sql.append("UPDATE " + Table.USER.name());
      sql.append("""
		
		SET
		screenName = ?,
		lastLogin = ?,
		language1 = ?,
		language2 = ?
		WHERE
		mostSignificantPFID = ?
		AND
		leastSignificantPFID = ?
		""");
	Connection con = getConnection();
	try {
		PreparedStatement preparedStatement = con.prepareStatement(sql.toString());
		preparedStatement.setString(1,user.screenName);
		preparedStatement.setTimestamp(2, Timestamp.from(user.getLastLogin()));
		preparedStatement.setString(3, user.mainLanguage);
		preparedStatement.setString(4, user.subLanguage);
		preparedStatement.setLong(5, user.pfid.getMostSignificantBits());
		preparedStatement.setLong(6, user.pfid.getLeastSignificantBits());
		if(preparedStatement.executeUpdate() == 1) {
			con.commit();
			preparedStatement.close();
			return true;
		}else {
			con.rollback();
			preparedStatement.close();
			return false;
		}
	}catch(SQLException e) {
    	Profundus.getInstance().getLogger().log(Level.WARNING,"updateUserEntry:" + e);

		try {
		    con.rollback();
		} catch (SQLException e2) {
	    	Profundus.getInstance().getLogger().log(Level.WARNING,"updateUserEntry:" + e2);
		}
		return false;
	}
	}
    
    /**
     * PFIDでデータ削除 DELETE
     * @param tableName
     * @param pfid
     * @return
     */
    static Boolean deleteByPFID(Table tableName, UUID pfid) {
    	//PFIDエントリーを削除
    	if(tableName != Table.PFID) {deleteByPFID(Table.PFID,pfid);}
    	
    	Connection con = getConnection();
    	try {
    		PreparedStatement preparedStatement = con.prepareStatement("DELETE FROM " + tableName.name() + " WHERE mostSignificantPFID = ? AND leastSignificantPFID = ?;");
    		preparedStatement.setLong(1, pfid.getMostSignificantBits());
    		preparedStatement.setLong(2, pfid.getLeastSignificantBits());
    		con.commit();
    		preparedStatement.close();
    		return true;
    	}catch(SQLException e) {
	    	Profundus.getInstance().getLogger().log(Level.WARNING,e.toString());

    		try {
    		    con.rollback();
    		} catch (SQLException e2) {
                throw new RuntimeException(e2);
    		}
    		return false;
    	}
    }
}

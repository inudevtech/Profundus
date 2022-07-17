package tech.inudev.profundus.utils;

import org.bukkit.entity.*;

import lombok.Getter;
import tech.inudev.profundus.Profundus;
import tech.inudev.profundus.utils.DatabaseUtil.Table;

import java.util.*;
import java.util.logging.Level;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;

/**
 * プラグイン内ユーザー管理用クラス
 * プラグイン依存のアクションはこのインスタンスから呼び出せるようにする想定。
 * ＞最下部TODO参照。
 * @author kidocchy
 *
 */
public class User extends PFAgent{
	private static Map<UUID,User> storedList = new HashMap<UUID,User>();
	
	UUID uuid;
	String mainLanguage;
	String subLanguage;
	
	@Getter
	private Instant lastLogin;
	@Getter
	private String source;
	@Getter
	private Player player;

	
	private User(Player p) {
		super(Table.USER,p.getName()); //getNewPFID
		uuid = p.getUniqueId();
		createdAt = java.time.Instant.now();
		lastLogin = java.time.Instant.now();
		mainLanguage = "";
		subLanguage = "";
	}
	private User() {};
	
	/**
	 * Playerインスタンスから，UUIDでProfundusユーザーエントリーを検索。
	 * 高速化のために，StoredListを内部で管理している。
	 * 第二引数にtrueを指定すると，エントリーがない場合新規作成を行う。
	 * ログイン時実行はtrue,その他情報検索時などはfalseで呼び出す想定。
	 * @see getByPlayer(Player, Booelan)
	 * @param p BUKKIT API Player
	 * @return User Profundus User クラスインスタンス
	 */
	public static User getByPlayer(Player p) {
		return getByPlayer(p, false);
	}
	
	/**
	 * Playerインスタンスから，UUIDでProfundusユーザーエントリーを検索。
	 * 高速化のために，StoredListを内部で管理している。
	 * 第二引数にtrueを指定すると，エントリーがない場合新規作成を行う。
	 * ログイン時実行はtrue,その他情報検索時などはfalseで呼び出す想定。
	 * @see getByPlayer(Player, Booelan)
	 * @param p BUKKIT API Player
	 * @param createIfNotExist エントリーがなければ新規作成するか
	 * @return User Profundus User クラスインスタンス
	 */
	public static User getByPlayer(Player p,Boolean createIfNotExist) {
		UUID queryUUID = p.getUniqueId();
		User u = getByUUID(queryUUID);
		if(u == null) {
			if(!createIfNotExist) {return null;}
			u = new User(p);
			u.source = "CreateNew";
			u.addToDB();
			u.addToStoredList();
		}
		u.player = p;
		return u;
	}
	
	/**
	 * これで呼び出すと，Playerインスタンスをラップしない。
	 * @see getByPlayer(Player)
	 * @param queryUUID
	 * @return User
	 */
	static User getByUUID(UUID queryUUID) {
		User u;
		//まず高速化のためにstoredListを検索
		if(storedList.containsKey(queryUUID)) {
			u = storedList.get(queryUUID);
			u.source = "StoredList";
		//なければ，データベースを検索,storedListに追加。
		}else if(isExistOnDB(queryUUID)) {
			u = fetchDB(queryUUID,"UUID");
			u.source = "Database";
			u.addToStoredList();

		//それでもなければ，新規作成，データベース,storedListに追加。
		}else {
			return null;
		}
		return u;
	}
	/**
	 * PFIDで検索。UUIDに変換して，getByUUIDに渡す。
	 * @param pfid PFID
	 * @return Userクラス
	 */
	public static User getByPFID(UUID pfid) {
		User tmp = fetchDB(pfid,"PFID");
		return getByUUID(tmp.uuid);
	}
	
	/**
	 * UserのUUIDが存在するかDB上で検索
	 * @param q UUID
	 * @return exist?
	 */
	private static Boolean isExistOnDB(UUID q) {
		try {
			ResultSet rs = DatabaseUtil.selectUUID(Table.USER, "UUID", q);
			if(rs == null) {return false;}
			return rs.next();
		}catch(SQLException e) {
			Profundus.getInstance().getLogger().log(Level.WARNING,e.toString());
		}
		return false;
	}
	
	/**
	 * DBからUserを検索，Userインスタンスに格納。
	 * 外部からは，getByPlayerで呼び出す。
	 * @see getByPlayer(Player)
	 * @param q UUID
	 * @param ID UUID or PFID
	 * @return User class instance
	 */
	private static User fetchDB(UUID q, String ID) {
		try {
			ResultSet rs = DatabaseUtil.selectUUID(Table.USER, ID, q);
			rs.next();
			User ret = new User();
			ret.uuid = new UUID(rs.getLong("mostSignificantUUID"),rs.getLong("leastSignificantUUID"));
			ret.pfid = new UUID(rs.getLong("mostSignificantPFID"),rs.getLong("leastSignificantPFID"));
			ret.screenName = rs.getString("screenName");
			ret.createdAt = rs.getTimestamp("createdAt").toInstant();
			ret.lastLogin = rs.getTimestamp("lastLogin").toInstant();
			ret.mainLanguage = rs.getString("language1");
			ret.subLanguage = rs.getString("language2");
			ret.type = Table.USER;
			return ret;
		}catch(SQLException e) {
			Profundus.getInstance().getLogger().log(Level.WARNING,"fetch:" + e);
		}
		return null;
	}
	

	protected void addToDB() {
		DatabaseUtil.insertUserEntry(this);
	}
	

	protected void updateDB() {
		DatabaseUtil.updateUserEntry(this);
	}
	

	protected void removeFromDB() {
		DatabaseUtil.deleteByPFID(Table.USER, pfid);
	}
	
	/**
	 * 検索高速化のためのstoredListに登録。
	 * storedListは，サーバーリスタート/プラグインreloadでクリアされる。
	 */
	private void addToStoredList() {
		storedList.put(uuid, this);
	}
	
	/**
	 * 検索高速化のためのstoredListから削除。ログインしていないユーザーとか。
	 */
	void removeFromStoredList() {
		storedList.remove(uuid);
	}
	
	/**
	 * lastLoginを更新。ログイン時に呼ばれるべき。
	 */
	public void updateLastLogin() {
		lastLogin = java.time.Instant.now();
		updateDB();
	}
	
	/**
	 * ユーザープロフィール「mainLanguage」を更新。出番があるかは不明。
	 * @param lang 第１言語
	 */
	public void setMainLanguage(String lang) {
		mainLanguage = lang;
		updateDB();
	}
	
	/**
	 * ユーザープロフィール「subLanguage」を更新。出番があるかは不明。
	 * @param lang 第２言語
	 */
	public void setSubLanguage(String lang) {
		subLanguage = lang;
		updateDB();
	}

	@Override
	public void sendMessage(String str, Boolean sendOnLogin) {
		if(player != null) {
			player.sendMessage(str);
		} else {
			// TODO messageStore to send on Login
			Profundus.getInstance().getLogger().log(Level.INFO,"Player is null");
		}
		
	}
	
	// TODO //
	/*
	 * 他に登録したい項目・プロフィールとかはないですかね。
	 * public Boolean joinGroup(Group); グループに参加
	 * public Boolean isMemberOf(Group); グループのメンバーであるかを判定
	 * public Group[] getGroups(); 所属するグループを取得
	 * public String getRoleFor(Group); グループ内での役割情報を取得。使い道不明。
	 * public Boolean canEditChunk(x, z); チャンクの編集権を判定
	 * public Boolean canEditChunk(location); 同上。
	*/
	
}

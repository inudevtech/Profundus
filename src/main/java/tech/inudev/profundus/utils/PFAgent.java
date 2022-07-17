package tech.inudev.profundus.utils;

import tech.inudev.profundus.utils.DatabaseUtil.Table;

/**
 * PFGROUPとUSERのスーパークラス。
 * どちらも，土地などの所有者になれる。
 * @author kidocchy
 *
 */
public abstract class PFAgent extends PFID{

	String screenName;//表示名
	/**
	 * コンストラクタ
	 * @param type Table
	 * @param name 表示名
	 */
	public PFAgent(Table type,String name) {
		super(type);
		screenName = name;
	}
	
	/**
	 * からのコンストラクタ
	 */
	public PFAgent() {}
	/**
	 * チャットメッセージ送信。グループ宛に送ると，メンバー全員に送信する想定。
	 * 
	 * @param str 本文
	 * @param sendOnLogin 記憶してログイン時に送れるようにしたいが，未実装。
	 */
	public abstract void sendMessage(String str, Boolean sendOnLogin);

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gameElements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 盤面を管理するクラス HashMapで状態を取得可能で, テキストによる出力も可能
 *
 * -1:誰もおいていない 0:プレイヤー0の設置 1:プレイヤー1の設置 とする。
 *
 * ワーカー配置のメソッドを持つ
 *
 * @author ktajima
 */
public class Board implements Cloneable {

    public static final String[] PLACE_NAMES = {"1-1", "2-1", "2-2", "3-1", "3-2", "3-3", "3-4", "4-1", "4-2", "4-3", "4-4", "4-5", "5-1", "5-2", "5-3", "5-4", "5-5", "6-1", "6-2", "6-3"};
    public static final int PLAYER_COUNT = 2;

    /**
     * 現在の盤面におかれているコマを表すマップ（Hard Workerにより複数置かれることも考慮
     */
    public HashMap<String, ArrayList<String>> boardState;

    /**
     * 基本的なコンストラクタ
     */
    public Board() {
        init();
    }

    /**
     * ボードの初期化メソッド
     */
    private void init() {
        //ボードの初期化
        this.clear();
    }

    /**
     * ボード上におかれたコマを消す（設備の利用状況は変わらない）
     */
    private void clear() {
        this.boardState = new HashMap<String, ArrayList<String>>();
        for (String key : PLACE_NAMES) {
            this.boardState.put(key, new ArrayList<String>());
        }
    }

    /**
     * *
     * 季節の変わり目などにボード上におかれたワーカーをすべて除去する
     */
    public void returnAllWorkers() {
        this.clear();
    }

    /**
     * ピース設置可能かの判定1
     *
     * @param place 第1引数:設置場所 第3引数:コマンドのオプションはここでは判別しない 設置済みかどうかのみを判定する
     * @return 戻り値は設置可能かどうかのブール値
     */
    public boolean checkActionIsEmpty(String place) {
        if (!this.boardState.containsKey(place)) {
            //設置場所が不正
            return false;
        }
        //1-1はいくつでもピースを受け入れ可能
        if (place.equals("1-1")) {
            return true;
        }

        //その場所に既にコマがおかれているかを確認
        if (!this.boardState.get(place).isEmpty()) {
            return false;
        }

        //設置誓約により置けない場合
        if (place.equals("2-1")) {
            if (!this.boardState.get("2-2").isEmpty()) {
                return false;
            }
        }

        //2-2は設置誓約あり
        if (place.equals("2-2")) {
            if (!this.boardState.get("2-1").isEmpty()) {
                return false;
            }
        }

        //設置誓約により置けない場合
        if (place.equals("3-3")) {
            if (!this.boardState.get("3-4").isEmpty()) {
                return false;
            }
        }
        if (place.equals("3-4")) {
            if (!this.boardState.get("3-3").isEmpty()) {
                return false;
            }
        }

        //設置誓約により置けない場合
        if (place.equals("4-1")) {
            if (!this.boardState.get("4-2").isEmpty()) {
                return false;
            }
        }
        if (place.equals("4-2")) {
            if (!this.boardState.get("4-1").isEmpty()) {
                return false;
            }
        }

        //設置誓約により置けない場合
        if (place.equals("5-2")) {
            if (!this.boardState.get("5-3").isEmpty()) {
                return false;
            }
        }
        if (place.equals("5-3")) {
            if (!this.boardState.get("5-2").isEmpty()) {
                return false;
            }
        }

        //以上の条件に引っかからなければOK
        return true;
    }

    /**
     * ピース設置可能かの判定2
     *
     * @param player 第1引数:プレイヤー番号0または1
     * @param place 第2引数:設置場所
     * @param option 第3引数:コマンドのオプション
     * @return 戻り値は設置可能かどうかのブール値
     */
    public boolean checkOptionIsRight(int player, String place, String option) {
        //プレイヤー番号の確認
        if (player < 0 || player > 1) {
            //プレイヤー番号が不正
            return false;
        }

        //オプションの正しさの確認
        if (place.equals("2-1")) {
            if (option.equals("F") || option.equals("G")) {
                return true;
            } else {
                return false;
            }
        }
        if (place.equals("3-4")) {
            if (option.equals("F") || option.equals("G")) {
                return true;
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * コマを設置するメソッド
     *
     * @param player 第1引数:プレイヤー番号0または1
     * @param place 第2引数:設置場所
     * @param worker 第3引数:ワーカーの種類
     * @param option 第4引数:設置時のオプション
     * @return 設置ができたらtrue
     */
    public boolean putWorker(int player, String place, String worker, String option) {
        String pawnName = worker + player;

        //場所が開いているかの確認（努力家のみ実施不要）
        if (!worker.equals("H")) {
            if (!this.checkActionIsEmpty(place)) {
                return false;
            }
        }
        //オプション関係が正しいかを確認
        if (!this.checkOptionIsRight(player, place, option)) {
            return false;
        }

        ArrayList list = this.boardState.get(place);
        list.add(pawnName);
        this.boardState.put(place, list);

        return true;
    }
    
    /**
     * コマを設置するメソッド（ゲーム複製用）
     *
     * @param player 第1引数:プレイヤー番号0または1
     * @param place 第2引数:設置場所
     * @param worker 第3引数:ワーカーの種類
     * @return 設置ができたらtrue
     */
    public boolean putWorker(int player, String place, String worker) {
        String pawnName = worker + player;

        //場所が開いているかの確認（努力家のみ実施不要）
        if (!worker.equals("H")) {
            if (!this.checkActionIsEmpty(place)) {
                return false;
            }
        }

        ArrayList list = this.boardState.get(place);
        list.add(pawnName);
        this.boardState.put(place, list);

        return true;
    }
    
    

    /**
     * ゼミにおかれたコマの一覧を取得
     * @return ゼミに置かれたコマ一覧
     */
    public ArrayList<String> getSeminorWorkers() {
        return this.boardState.get("1-1");
    }

    /**
     * ボード上におかれたコマを得るためのマップを取得する,これを使うと1-1もまとめて手に入る
     * @return ボード状態のHashMap
     */
    public HashMap<String, ArrayList<String>> getWorkersOnBoard() {
        return this.boardState;
    }

    /**
     * 現在のボードの状況を表示する（CUI用）
     */
    public void printCurrentBoard() {
        for (String key : PLACE_NAMES) {
            System.out.print(key);
            System.out.print(":");
            System.out.println(this.boardState.get(key));
        }
    }

    /**
     * Boardオブジェクトを複製（ディープコピー）します。<br>
     * このメソッドを経由しないとシャローコピー＝ポインタの複製になるため注意してください。<br>
     * コピー元が意図しないタイミングで書き換わる可能性があります。
     * @return 複製したBoardオブジェクト
     */
    @Override
    public Board clone() {
        Board cloned = null;

        try {
            cloned = (Board) super.clone();
            HashMap<String, ArrayList<String>> copyedState = new HashMap<>();
            for (String key : this.boardState.keySet()) {
                ArrayList<String> list = new ArrayList<>();
                list.addAll(this.boardState.get(key));
                copyedState.put(key, list);
            }
            cloned.boardState = copyedState;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Board.class.getName()).log(Level.SEVERE, null, ex);
        }

        return cloned;
    }

}

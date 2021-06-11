/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai;

import gameElements.Game;
import gameElements.GameResources;
import gui.ClientGUI;
import gui.MessageRecevable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import network.ServerConnecter;

/**
 * サンプルのAI<br>
 *
 * ただただひたすらゼミに置き続けます。
 *
 * @author niwatakumi
 */
public class SampleAI extends LaboAI {

    // 自プレイヤーの名前
    protected String myName;
    // 自プレイヤーの番号
    protected int myNumber;
    // 相手プレイヤーの番号
    protected int enemyNumber;
    
    protected char[] selectedWorker = {'E', 'L'};

    // サーバーとのコネクタ
    protected ServerConnecter connecter;
    // GUI
    protected ClientGUI gui;

    // 自分が何手打ったかを数える
    protected int handNum = 0;

    // 初期化が完了したか
    protected boolean isInitEnded = false;
    // 210 CONFPRMしたときに返ってくるメッセージを記録する
    protected ArrayList<String> confirmMessages = new ArrayList<>();

    /**
     * コンストラクタ＝初期化関数
     *
     * @param game
     */
    public SampleAI(Game game) {
        super(game);
        // 初期化時に設定する変数はここ
        this.myName = "SampleAI";
    }

    public String getMyName() {
        return this.myName;
    }

    /**
     * AIとサーバを接続するメソッド
     *
     * @param connecter
     */
    @Override
    public void setConnecter(ServerConnecter connecter) {
        // サーバに接続する
        this.connecter = connecter;
        this.connecter.addMessageRecever(this);
    }

    /**
     * GUIとAIを接続するメソッド
     *
     * @param mr
     */
    @Override
    public void setOutputInterface(MessageRecevable mr) {
        this.gui = (ClientGUI) mr;
    }

    /**
     * クライアント側のログにテキストを表示（緑）
     *
     * @param text
     */
    @Override
    public void addMessage(String text) {
        //属性情報を作成
        SimpleAttributeSet attribute = new SimpleAttributeSet();
        //属性情報の文字色に緑を設定
        attribute.addAttribute(StyleConstants.Foreground, Color.GREEN);

        // クライアント側のログに緑で表示
        this.gui.addMessage(text, attribute);
    }

    /**
     * サーバーにメッセージを送る
     *
     * @param sendText 送るメッセージ
     */
    public void sendMessage(String sendText) {
        //属性情報を作成
        SimpleAttributeSet attribute = new SimpleAttributeSet();
        //属性情報の文字色に赤を設定
        attribute.addAttribute(StyleConstants.Foreground, Color.RED);

        //サーバーへ送信
        if (this.connecter.canWrite()) {
            this.connecter.sendMessage(sendText);
            this.gui.addMessage("[send]" + sendText + "\n", attribute);
        } else {
            this.gui.addMessage("(送信失敗)" + sendText + "\n", attribute);
        }
    }

    /**
     * メッセージを受信した時のメソッド
     *
     * @param text
     */
    @Override
    public void reciveMessage(String text) {
        // 受信メッセージの最初の番号を確認する
        String messageNum = text.substring(0, 3);
        switch (messageNum) {
            case "100":
                // サーバーが応答した時
                this.helloServer();
                break;
            case "102":
                // サーバーからプレイヤー番号が返ってきた時
                this.checkNumber(text);
                break;
            case "103":
                // サーバーからキャラクターをセレクトするように言われたとき
                this.sendCharacters();
                break;
            case "204":
                // 盤面状態を確認
                this.sendMessage("210 CONFPRM");
                this.confirmMessages = new ArrayList<>();
                break;
            case "206":
                // 相手が打った時の処理
                break;
            case "207":
                // 季節が変わったらしい時は自分の仮想ゲームでも更新する
                this.changeSeason();
                break;
            case "211":
            case "212":
            case "213":
            case "214":
            case "215":
            case "216":
                // 210 CONFPRMしたときのメッセージ，記録する
                this.confirmMessages.add(text);
                break;
            case "202":
                // マルチラインの終了＝リソース情報取得終了
                // 仮想ゲームに反映する
//                System.out.println(this.confirmMessages);
                this.gameBoard = new Game(confirmMessages, this.myNumber);
                this.think();
                break;
        }
    }

    /**
     * サーバーが応答した時のメソッド
     */
    protected void helloServer() {
        // 名前を送る
        this.sendMessage("101 NAME " + this.myName);
    }

    /**
     * サーバーからプレイヤー番号が送られてきた時の処理
     *
     * @param text サーバーからのメッセージ
     */
    protected void checkNumber(String text) {
        // 番号を確認する
        this.myNumber = Integer.parseInt(text.substring(13));
        if (this.myNumber == 0) {
            this.enemyNumber = 1;
        } else {
            this.enemyNumber = 0;
        }
    }

    /**
     * サーバーからキャラクターを送るように言われたときのメソッド
     */
    protected void sendCharacters() {
        // キャラクターを送る
        String message = "104 CHARACTER ";
        message += this.selectedWorker[0];
        message += " ";
        message += this.selectedWorker[1];
        this.sendMessage(message);
    }

    /**
     * コマを置くメソッド<br>
     * 引数の詳細は以下のリンクを確認してください。 <br>
     * https://lms.gifu-nct.ac.jp/pluginfile.php/68446/mod_resource/content/0/5E%E5%AE%9F%E9%A8%93%E3%83%97%E3%83%AD%E3%83%88%E3%82%B3%E3%83%AB%E8%AA%AC%E6%98%8E200410.pdf
     *
     * @param worker コマの種類
     * @param place 場所
     * @param option 場所に応じたオプション
     */
    private void putWorker(String worker, String place, String option) {
        if (this.gameBoard.canPutWorker(this.myNumber, place, worker, option)) {
            this.sendMessage("205 PLAY " + this.myNumber + " " + worker + " " + place + " " + option);
            this.gameBoard.play(this.myNumber, place, worker, option);
        } else {
            System.err.println("Put Error!!");
        }
    }

    /**
     * 仮想ゲームの季節を更新する
     */
    protected void changeSeason() {
        this.gameBoard.changeNewSeason();
        this.seasonChanged();
    }

    /**
     * 季節が変わった時に呼び出される関数
     */
    protected void seasonChanged() {
        // 手数をリセット
        this.handNum = 0;

        // 季節を取得
        Game currentGameBoard = this.gameBoard.clone();
        String season = currentGameBoard.getSeason();

        // もし"06"ならメッセージを表示
        if (season.equals("06")) {
            this.addMessage("=====折り返し！=====");
        }
    }

    /**
     * 手を考えて打つ処理<br>
     * 基本的にはここをいじって思考させるといいと思います。
     */
    protected void think() {
        // 現在のゲームの状態はthis.gameBoradに入っています。
        // 上の方の処理で自動的にサーバーと同期する…はず
        // 色々試しに打ったりすることを考えると，cloneで複製しておくことをおすすめします．
        Game currentGameBoard = this.gameBoard.clone();

        // 試しに季節を取得。
        String season = currentGameBoard.getSeason();
        // GUIにログを書く場合はthis.addMessage(text)を利用
        // System.out.println()では表示されません。
        this.addMessage("現在の季節は" + season);

        // お金などのリソースはGameResourcesの中にあります。
        GameResources myResources = currentGameBoard.getResourcesOf(this.myNumber);
        GameResources enemyResources = currentGameBoard.getResourcesOf(this.enemyNumber);

        // 自分のリソースを取得→表示
        int myMoney = myResources.getCurrentMoney();
        int myFlask = myResources.getCurrentResrchPoint(0);
        int myGear = myResources.getCurrentResrchPoint(1);
        this.addMessage(myMoney + "円");
        this.addMessage(myFlask + "フラスコ");
        this.addMessage(myGear + "ギア");

        // 両者のスコアを取得
        int myScore = myResources.getTotalScore();
        int enemyScore = enemyResources.getTotalScore();
        // 比較してみる
        if (myScore > enemyScore) {
            this.addMessage((myScore - enemyScore) + "点勝っています");
        } else if (myScore == enemyScore) {
            this.addMessage("引き分けています");
        } else {
            this.addMessage((enemyScore - myScore) + "点負けています");
        }
        
        /* 
        ここに何らかの処理を入れるといいと思います。
        今回はゼミに置くだけ
         */
        Seo_AI ai_ab = new Seo_AI(currentGameBoard, this.myNumber);
        ai_ab.search_ab(-1);    // 深さをマイナス指定するとシーズン終わりまで探索する
        String[] move = ai_ab.getBestMove();
        this.addMessage("最善手： "+move[0]+" "+move[1]+" "+move[2]);
        this.putWorker(move[0], move[1], move[2]);

    }

}

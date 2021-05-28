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
import java.util.List;
import java.util.Scanner;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import network.ServerConnecter;


public class AI extends LaboAI {

    // 自プレイヤーの名前
    protected String myName;
    // 自プレイヤーの番号
    protected int myNumber;
    // 相手プレイヤーの番号
    protected int enemyNumber;
    // キャラクター情報
    protected String C1, C2;
    
    // 盤面のアドレス一覧
    protected String[] actionID = {
        "1-1","2-1","2-2","3-1","3-2","3-3","3-4","4-1","4-2","4-3","4-4","4-5",
        "5-1","5-2","5-3","5-4","5-5","6-1","6-2","6-3"
    };
    
    // 打てる場所一覧用
    protected String[][] actionIDcheck = {
        {
            "1-1","1-1","1-1","2-1","2-1","2-2","3-1","3-2","3-3","3-4","3-4",
            "4-1","4-2","4-3","4-4","4-5",
            "5-1","5-2","5-3","5-4","5-5","6-1","6-2","6-2","6-2","6-3"
        }, {
            "F","G","M","F","G","","","","","F","G",
            "","","","","",
            "","","","","","","FF","FG","GG",""
        }
    };
    
    // このプログラムでプレイ可能なキャラクターの組み合わせ
    protected String[][] canCharacter = {
        {"E", "L"}, {"R", "M"}, {"G", "E"}, {"G", "R"}
    };
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
    public AI(Game game) {
        super(game);
        // 初期化時に設定する変数はここ
        this.myName = "AI_DeguchiLabo";
        // キャラクター選択(指定)
        this.C1 = "G";
        this.C2 = "R";
        /* 
        キャラクターをキーボードから入力する場合
        */
        Scanner scanner = new Scanner(System.in);
        System.out.println("キャラクターを入力");
        this.C1 = scanner.next();
        this.C2 = scanner.next();
        boolean exe = false;
        for(String[] chara : canCharacter){
            if(this.C1.equals(chara[0]) && this.C2.equals(chara[1])) exe = true;
            else if(this.C1.equals(chara[1]) && this.C2.equals(chara[0])) exe = true;
            if(exe) break;
        }
        if(!exe){
            System.err.println("Character Error!!");
            System.exit(0);
        }
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
        message += C1;
        message += " ";
        message += C2;
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
    * 手をシミュレートする
    * (季節はまたげないので注意)
    * @param nowGame 現在の盤面
    * @param player プレイヤー番号
    * @param worker 駒の種類
    * @param place 場所
    * @param option オプション
    * @return 打てる場合は打った後のゲーム盤面，打てない場合はnull
    */
    private Game simulateGame(Game nowGame, int player, String worker, String place, String option) {
        // ゲームを複製する（複製しないと大元のゲームデータが変更されてしまう）
        Game clonedGame = nowGame.clone();

        // 指定された手をうつことができるかをチェック
        if (clonedGame.canPutWorker(player, place, worker, option)) {
            // 打てるなら実際に打ってみたゲームデータを返却
            clonedGame.play(player, place, worker, option);
            return clonedGame;
        } else {
            // 打てないならnullを返す
            return null;
        }
    }
    
    // クラス
    class Hand {
        Game simulateBoard;
        int myID, enemyID;
        
        // コンストラクタ
        void Hand(Game board) {
            this.simulateBoard = board;
            this.myID = myNumber;
            this.enemyID = enemyID;
        }
        
        boolean canPut(String worker, String place, String option){
            return this.simulateBoard.canPutWorker(this.myID, place, worker, option);
        }
        Game simulate(String worker, String place, String option){
            return simulateGame(this.simulateBoard, this.myID, worker, place, option);
        }
        Game simulate(int playerID, String worker, String place, String option){
            return simulateGame(this.simulateBoard, playerID, worker, place, option);
        }
        int simulateResource(Game board, String resource, int ID) {
            GameResources boardResource = board.getResourcesOf(ID);
            switch(resource){
                case "F":
                    return boardResource.getCurrentResrchPoint(0);
                case "G":
                    return boardResource.getCurrentResrchPoint(1);
                case "M":
                    return boardResource.getCurrentMoney();
                default:
                    return -1;
            }
        }
        int simulateMyResource(Game board, String resource) {
            return simulateResource(board, resource, this.myID);
        }
        int simulateEnemyResource(Game board, String resource) {
            return simulateResource(board, resource, this.enemyID);
        }
        int simulatePoint(Game board, int ID) {
            GameResources boardResource = board.getResourcesOf(ID);
            return boardResource.getTotalScore();
        }
        int simulateMyPoint(Game board) {
            return simulatePoint(board, this.myID);
        }
        int simulateEnemyPoint(Game board) {
            return simulatePoint(board, this.enemyID);
        }
        int simulateAchievement(Game board, int ID) {
            GameResources boardResource = board.getResourcesOf(ID);
            return boardResource.getAchievementCount();
        }
        int simulateMyAchievement(Game board) {
            return simulateAchievement(board, this.myID);
        }
        int simulateEnemyAchievement(Game board) {
            return simulateAchievement(board, this.enemyID);
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
         */
        
        // エンジニア and リーダー
        if(C1.equals("E") && C2.equals("L") || C1.equals("L") && C2.equals("E"))
            EandL(currentGameBoard, myResources, enemyResources, season);
        // リサーチャー and マネージャー
        else if(C1.equals("R") && C2.equals("M") || C1.equals("M") && C2.equals("R"))
            RandM(currentGameBoard, myResources, enemyResources, season);
        // 天才科学者 and エンジニア
        else if(C1.equals("G") && C2.equals("E") || C1.equals("E") && C2.equals("G"))
            GandE(currentGameBoard, myResources, enemyResources, season);
        // 天才科学者 and リサーチャー
        else if(C1.equals("G") && C2.equals("R") || C1.equals("R") && C2.equals("G"))
            GandR(currentGameBoard, myResources, enemyResources, season);
        else {
            System.err.println("Character Error!!");
            System.exit(1);
        }
        
        switch (season) {
            case "01":
            case "02":
            case "03":
            case "04":
            case "05":
                // 前半戦
                
            case "06":
            case "07":
            case "08":
            case "09":
            case "10":
                // 後半戦
                break;
        }

        // 手数をカウント
        this.handNum++;

        
        
    }
    
    /**
     * エンジニアとリーダーのAIプラン
     * @param currentGameBoard 複製したゲームボード
     */
    public void EandL(Game currentGameBoard, GameResources myResources, GameResources enemyResources, String season){
        // 両者のリソースを取得    
        int myMoney = myResources.getCurrentMoney();
        int myFlask = myResources.getCurrentResrchPoint(0);
        int myGear = myResources.getCurrentResrchPoint(1);
        int enemyMoney = enemyResources.getCurrentMoney();
        int enemyFlask = enemyResources.getCurrentResrchPoint(0);
        int enemyGear = enemyResources.getCurrentResrchPoint(1);
        // 両者のスコアを取得
        int myScore = myResources.getTotalScore();
        int enemyScore = enemyResources.getTotalScore();
        // 両者のアチーブメントを取得
        int myAchievement = myResources.getAchievementCount();
        int enemyAchievement = enemyResources.getAchievementCount();
        // 自分が先手かどうかを取得
        boolean startPlayer = myResources.isStartPlayer();
        
        // 自分が何の駒を打てるかを取得
        boolean canE = currentGameBoard.canPutWorker(this.myNumber, "1-1", "E", "M");
        boolean canL = currentGameBoard.canPutWorker(this.myNumber, "1-1", "L", "M");
        boolean canS = currentGameBoard.canPutWorker(this.myNumber, "1-1", "S", "M");
        boolean canA = currentGameBoard.canPutWorker(this.myNumber, "1-1", "A", "M");
        
        /** 
         * 盤面の打てる場所一覧を取得 
         * @param canPlace[i][j]
         * i=0 : engineer
         * i=1 : leader
         * i=2 : student
         * i=3 : Additional worker
         */
        Game[][] canPlace = new Game[4][actionIDcheck[0].length];
        //Arrays.fill(canPlace, false);
        String C;
        
        for(int i=0;i<4;i++){
            if(!canE && i==0) continue;
            if(!canL && i==1) continue;
            if(!canS && i==2) continue;
            if(!canA && i==3) continue;
            
            if(i==0) C="E";
            else if(i==1) C="L";
            else if(i==2) C="S";
            else C="A";
            int j=0;
            for(String ID : actionIDcheck[0]){
                canPlace[i][j] = simulateGame(currentGameBoard, this.myNumber, C, ID, actionIDcheck[1][j]);
                /*
                if(canPlace[i][j]==null) this.addMessage(C + ": " + ID + " " + actionIDcheck[1][j] + " -> ×");
                else this.addMessage(C + ": " + ID + " " + actionIDcheck[1][j] + " -> ○");
                */
                j++;
            }
        }
        List<String> placeList = Arrays.asList(actionIDcheck[0]);
        String optionList[] = actionIDcheck[1];
        
        String[][] engineerHand = {{"3-2",""},{"2-2",""},{"3-4","G"},{"3-3",""},{"6-1",""},{"1-1","G"}};
        String[][] leaderHand = {{"4-2",""},{"4-3",""},{"5-3",""},{"6-1",""},{"2-2",""},{"3-2",""},{"3-4","G"},{"3-3",""},{"5-3",""},{"6-2","GG"},{"1-1","M"}};
        String[][] leaderHandSeason10 = {{"5-3",""},{"4-3",""},{"4-2",""},{"2-2",""},{"6-1",""},{"3-2",""},{"3-4","G"},{"3-3",""},{"6-2","GG"},{"1-1","M"}};
        
        // ローカルクラス
        class Hand {
            // 前半 (season1-5)
            void first(){
                boolean last = false; // 2手目かどうかのフラグ
                if(handNum == 1) last = true;
                if(Board("4-2", "L") != null) {
                    putWorker("L", "4-2", "");
                } else if(myGear>13 && Board("4-5", "E") != null) {
                    putWorker("E", "4-5", "");
                } else if(myGear<6 && canE) {
                    engineerPut(last);
                } else if(!startPlayer && Board("6-1", "L") != null){
                    putWorker("L", "6-1", "");
                } else if(Board("2-2", "L") != null && Board("2-2", "L").getResourcesOf(myNumber).getCurrentMoney() >= 15) {
                    putWorker("L", "2-2", "");
                } else if(canL) {
                    learderPut("L", last);
                } else {
                    engineerPut(last);
                }
            }
            
            // 後半 (season6-9)
            void second(){
                boolean last = false; // 3手目かどうかのフラグ
                if(handNum == 2) last = true;
                
                String chara; // キャラクター
                if(canL) chara="L";
                else if(canS) chara="S";
                else chara="A";
                
                if(Board("4-2", chara) != null) {
                    putWorker(chara, "4-2", "");
                } else if(myGear>13 && Board("4-5", "E") != null) {
                    putWorker("E", "4-5", "");
                } else if(myGear<6 && canE) {
                    engineerPut(last);
                } else if(!startPlayer && Board("6-1", chara) != null){
                    putWorker(chara, "6-1", "");
                } else if(Board("2-2", chara) != null && Board("2-2", chara).getResourcesOf(myNumber).getCurrentMoney() >= 15) {
                    putWorker(chara, "2-2", "");
                } else if((canL || canS) && myAchievement >= 15) {
                    learderPutFinal(chara, last);
                } else if(canL || canS) {
                    learderPut(chara, last);
                } else {
                    engineerPut(last);
                }
            }
            
            // season10
            void finalSeason(){
                boolean last = false; // 3手目かどうかのフラグ
                if(handNum == 2) last = true;
                
                String chara; // キャラクター
                if(canL) chara="L";
                else if(canS) chara="S";
                else chara="A";
                
                addMessage("season 10!!!");
                
                if(myGear<6 && canE) {
                    engineerPut(last);
                }else if(Board("2-2", chara) != null && Board("2-2", chara).getResourcesOf(myNumber).getCurrentMoney() >= 15) {
                    putWorker(chara, "2-2", "");
                } else if(canL || canS || canA) {
                    learderPutFinal(chara, last);
                } else {
                    engineerPut(last);
                }
            }
            
            void additional(){
                if(!startPlayer && Board("6-1", "A") != null){
                    putWorker("A", "6-1", "");
                } else {
                    learderPut("A", false);
                }
            }
            
            void engineerPut(boolean last){
                int i=0;
                do{
                    String place = engineerHand[i][0];
                    String option = engineerHand[i][1];
                    Game game = Board(place, option, "E");
                    if(game != null){
                        if(game.getResourcesOf(myNumber).getCurrentResrchPoint(1) < 1 //getCurrentResrchPoint(1) : ギアを取得
                                || (last && game.getResourcesOf(myNumber).getCurrentMoney() < 1)) { //getCurrentMoney() : 所持金を取得
                            i++;
                            continue;
                        }
                        putWorker("E", place, option);
                        break;
                    }
                    i++;
                }while(true);
            }
            void learderPut(String worker, boolean last){
                int i=0;
                do{
                    String place = leaderHand[i][0];
                    String option = leaderHand[i][1];
                    Game game = Board(place, option, worker);
                    if(game != null){
                        // リーダー追加駒が置けなくなる場合はスキップ
                        if(last && game.getResourcesOf(myNumber).getCurrentMoney() < 1){
                            i++;
                            continue;
                        }
                        putWorker(worker, place, option);
                        break;
                    }//yeaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaah!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    i++;
                }while(true);
            }
            void learderPutFinal(String worker, boolean last){
                int i=0;
                do{
                    String place = leaderHandSeason10[i][0];
                    String option = leaderHandSeason10[i][1];
                    Game game = Board(place, option, worker);
                    if(game != null){
                        // リーダー追加駒が置けなくなる場合はスキップ
                        if(last && game.getResourcesOf(myNumber).getCurrentMoney() < 1){
                            i++;
                            continue;
                        }
                        putWorker(worker, place, option);
                        break;
                    }
                    i++;
                }while(true);
            }
            
            /**
             * @param place アクションID
             * @return placeとoptionに一致する盤面を返す
             */
            Game Board(String place, String chara){
                return canPlace[ID_convert(chara)][placeList.indexOf(place)];
            }
            /**
             * @param place アクションID
             * @param option "", "G" など
             * @return placeとoptionに一致する盤面を返す
             */
            Game Board(String place, String option, String chara){
                int index = placeList.indexOf(place);
                while(!option.equals(optionList[index])) index++;
                return canPlace[ID_convert(chara)][index];
            }
            
            /**
             * @retuen E:0, L:1, S:2, A:3
             */
            int ID_convert(String chara){
                switch(chara){
                    case "E":
                        return 0;
                    case "L":
                        return 1;
                    case "S":
                        return 2;
                    case "A":
                        return 3;
                    default:
                        return -1;
                }
            }
        }
        
        Hand hand = new Hand();
        
        switch(season){
            case "01":
            case "02":
            case "03":
            case "04":
            case "05":
                switch(this.handNum){
                    case 0:
                    case 1:
                        hand.first();
                        break;
                    case 2:
                        hand.additional();
                        break;
                }
                break;
            case "06":
            case "07":
            case "08":
            case "09":
                switch(this.handNum){
                    case 0:
                    case 1:
                    case 2:
                        hand.second();
                        break;
                    case 3:
                        hand.additional();
                        break;
                }
                break;
            case "10":
                hand.finalSeason();
                break;
        }
    }
    
    /**
     * リサーチャーとマネージャーのAIプラン
     * @param currentGameBoard 複製したゲームボード
     */
    public void RandM(Game currentGameBoard, GameResources myResources, GameResources enemyResources, String season){
        // 両者のリソースを取得    
        int myMoney = myResources.getCurrentMoney();
        int myFlask = myResources.getCurrentResrchPoint(0);
        int myGear = myResources.getCurrentResrchPoint(1);
        int enemyMoney = enemyResources.getCurrentMoney();
        int enemyFlask = enemyResources.getCurrentResrchPoint(0);
        int enemyGear = enemyResources.getCurrentResrchPoint(1);
        // 両者のスコアを取得
        int myScore = myResources.getTotalScore();
        int enemyScore = enemyResources.getTotalScore();
        // 両者のアチーブメントを取得
        int myAchievement = myResources.getAchievementCount();
        int enemyAchievement = enemyResources.getAchievementCount();
        // 自分が先手かどうかを取得
        boolean startPlayer = myResources.isStartPlayer();
        
        // 自分が何の駒を打てるかを取得
        boolean canR = currentGameBoard.canPutWorker(this.myNumber, "1-1", "R", "M");
        boolean canM = currentGameBoard.canPutWorker(this.myNumber, "1-1", "M", "M");
        boolean canS = currentGameBoard.canPutWorker(this.myNumber, "1-1", "S", "M");
        
        /** 
         * 盤面の打てる場所一覧を取得 
         * @param canPlace[i][j]
         * i=0 : researcher
         * i=1 : manager
         * i=2 : student
         */
        Game[][] canPlace = new Game[3][actionIDcheck[0].length];
        String C;
        
        for(int i=0;i<3;i++){
            if(!canR && i==0) continue;
            if(!canM && i==1) continue;
            if(!canS && i==2) continue;
            
            if(i==0) C="R";
            else if(i==1) C="M";
            else C="S";
            int j=0;
            for(String ID : actionIDcheck[0]){
                canPlace[i][j] = simulateGame(currentGameBoard, this.myNumber, C, ID, actionIDcheck[1][j]);
                /*
                if(canPlace[i][j]==null) this.addMessage(C + ": " + ID + " " + actionIDcheck[1][j] + " -> ×");
                else this.addMessage(C + ": " + ID + " " + actionIDcheck[1][j] + " -> ○");
                */
                j++;
            }
        }
        List<String> placeList = Arrays.asList(actionIDcheck[0]);
        String optionList[] = actionIDcheck[1];
        
        String[][] researcherHand = {{"3-1",""},{"2-1","F"},{"1-1","F"}};
        String[][] managerHand = {{"4-4",""},{"4-1",""},{"6-1",""},{"1-1","F"}};
        String[][] managerHandSecond = {{"5-2",""},{"4-4",""},{"4-1",""},{"6-1",""},{"2-1","F"},{"1-1","F"}};
        String[][] studentHand = {{"2-1","F"},{"1-1","F"}};
        
        // ローカルクラス
        class Hand {
            // 前半 (season1-5)
            void first(){
                int targetAchi = Integer.parseInt(season); //目標アチーブメント
                int targetMoney = 5 - Integer.parseInt(season); //目標マネー
                System.out.println("Achievement: "+targetAchi+", Money: "+targetMoney);
                
                //シミュレーション
                Game game = null;
                Game enemyGame = currentGameBoard;
                int i=0;
                String place, option;
                String place0=null, option0=null, place1=null, option1=null;
                switch(handNum){
                    case 0:
                        if(!canR) break;
                        do{
                            place0 = researcherHand[i][0];
                            option0 = researcherHand[i][1];
                            game = simulateGame(currentGameBoard, myNumber, "R", place0, option0);
                            i++;
                        }while(game == null);
                        addMessage("forecast: R "+place0+" "+option0);
                        if(game.canPutWorker(enemyNumber, "4-1", game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), ""))
                            enemyGame = simulateGame(game, enemyNumber, game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), "4-1", "");
                        else
                            enemyGame = simulateGame(game, enemyNumber, game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), "1-1", "M");
                    case 1:
                        if(!canM) break;
                        i=0;
                        do{
                            place1 = managerHand[i][0];
                            option1 = managerHand[i][1];
                            game = simulateGame(enemyGame, myNumber, "M", place1, option1);
                            i++;
                        }while(game == null);
                        addMessage("forecast: M "+place1+" "+option1);
                        break;
                }
                // シミュレーション結果を分析
                int myMoneySimulate = 0;
                int myAchievementSimulate = 0;
                if(game == null) {
                    addMessage("forecast: ×");
                } else {
                    GameResources myResourcesSimulate = game.getResourcesOf(myNumber);
                    myMoneySimulate = myResourcesSimulate.getCurrentMoney();
                    myAchievementSimulate = myResourcesSimulate.getAchievementCount();
                    addMessage("forecast: Money: "+myMoneySimulate+", Achievement: "+myAchievementSimulate);
                }
                if(myAchievementSimulate >= targetAchi && myMoneySimulate >= targetMoney){
                    addMessage("順調です!");
                    if(handNum == 0) putWorker("R", place0, option0);
                    else putWorker("M", place1, option1);
                } else if (handNum==0) {
                    if(Board("M","4-1") != null) putWorker("M", "4-1", "");
                    else if(Board("M", "4-4") != null) putWorker("M", "4-4", "");
                    else {
                        i = 0;
                        do{
                            place = researcherHand[i][0];
                            option = researcherHand[i][1];
                            i++;
                        }while(Board("R", place, option) == null);
                        putWorker("R", place, option);
                    }
                } else if(canR) {
                    putWorker("R", "1-1", "F");
                    addMessage("Oh, No!");
                } else {
                    if(Board("M","4-1") != null) putWorker("M", "4-1", "");
                    else if(Board("M", "4-4") != null) putWorker("M", "4-4", "");
                    else if(Board("M", "6-1") != null) putWorker("M", "6-1", "");
                    else putWorker("M", "1-1", "F");
                }
                
            }
            
            // 後半 (season6-9)
            void second(){
                int targetAchi = Integer.parseInt(season); //目標アチーブメント
                int targetMoney = 10 - 2 * (Integer.parseInt(season)-5); //目標マネー
                System.out.println("Achievement: "+targetAchi+", Money: "+targetMoney);
                
                //シミュレーション
                Game game = null;
                Game enemyGame = currentGameBoard.clone();
                int i=0;
                String chara; // キャラクター
                String place, option;
                String place0=null, option0=null, worker0=null;
                String place1=null, option1=null, worker1=null;
                String place2=null, option2=null;
                boolean canRs = canR, canMs = canM, canSs = canS;
                boolean enemyResearcher = false; // 相手のリサーチャーの動き
                for(String cha : currentGameBoard.getResourcesOf(enemyNumber).getUnusedWorkers()){
                    if(cha.equals("R")) enemyResearcher = true;
                }
                switch(handNum){
                    case 0:
                        if(enemyResearcher){
                            worker0 = "R";
                            do{
                                place0 = researcherHand[i][0];
                                option0 = researcherHand[i][1];
                                game = simulateGame(currentGameBoard, myNumber, "R", place0, option0);
                                i++;
                            }while(game == null);
                        } else {
                            if(currentGameBoard.canPutWorker(myNumber, "2-1", "S", "F")){
                                worker0 = "S";
                                place0 = "2-1";
                                option0 = "F";
                                game = simulateGame(currentGameBoard, myNumber, "S", place0, option0);
                            } else {
                                worker0 = "R";
                                do{
                                    place0 = researcherHand[i][0];
                                    option0 = researcherHand[i][1];
                                    game = simulateGame(currentGameBoard, myNumber, "R", place0, option0);
                                    i++;
                                }while(game == null);
                            }
                        }
                        addMessage("forecast: "+worker0+" "+place0+" "+option0);
                        switch(worker0){
                            case "R": canRs=false; break;
                            case "M": canMs=false; break;
                            case "S": canSs=false; break;
                        }
                        if(game.canPutWorker(enemyNumber, "4-1", game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), ""))
                            enemyGame = simulateGame(game, enemyNumber, game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), "4-1", "");
                        else if(game.canPutWorker(enemyNumber, "3-1", game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), ""))
                            enemyGame = simulateGame(game, enemyNumber, game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), "3-1", "");
                        else
                            enemyGame = simulateGame(game, enemyNumber, game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), "1-1", "M");
                    case 1:
                        if(canMs) chara="M";
                        else chara="S";
                        i=0;
                        if(canRs){
                            worker1 = "R";
                            do{
                                place1 = researcherHand[i][0];
                                option1 = researcherHand[i][1];
                                game = simulateGame(enemyGame, myNumber, "R", place1, option1);
                                i++;
                            }while(game == null);
                        } else {
                            worker1 = chara;
                            do{
                                place1 = studentHand[i][0];
                                option1 = studentHand[i][1];
                                game = simulateGame(enemyGame, myNumber, chara, place1, option1);
                                i++;
                            }while(game == null);
                        }
                        addMessage("forecast: "+worker1+" "+place1+" "+option1);
                        switch(worker1){
                            case "R": canRs=false; break;
                            case "M": canMs=false; break;
                            case "S": canSs=false; break;
                        }
                        if(game.canPutWorker(enemyNumber, "4-1", game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), ""))
                            enemyGame = simulateGame(game, enemyNumber, game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), "4-1", "");
                        else if(game.canPutWorker(enemyNumber, "3-1", game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), ""))
                            enemyGame = simulateGame(game, enemyNumber, game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), "3-1", "");
                        else
                            enemyGame = simulateGame(game, enemyNumber, game.getResourcesOf(enemyNumber).getUnusedWorkers().get(0), "1-1", "M");
                    case 2:
                        if(!canMs && !canSs) break;
                        if(enemyGame == null){
                            game = null;
                            System.out.println("enemyGame is null !!");
                            break;
                        }
                        if(canMs) chara="M";
                        else chara="S";
                        i=0;
                        do{
                            place2 = managerHandSecond[i][0];
                            option2 = managerHandSecond[i][1];
                            System.out.println("place: "+place2+", option: "+option2+", i: "+i);
                            game = simulateGame(enemyGame, myNumber, chara, place2, option2);
                            i++;
                        }while(game == null);
                        addMessage("forecast: M "+place2+" "+option2);
                        break;
                }
                // シミュレーション結果を分析
                int myMoneySimulate = 0;
                int myAchievementSimulate = 0;
                if(canM) chara="M";
                else chara="S";
                if(game == null) addMessage("forecast: ×");
                else{
                    GameResources myResourcesSimulate = game.getResourcesOf(myNumber);
                    myMoneySimulate = myResourcesSimulate.getCurrentMoney();
                    myAchievementSimulate = myResourcesSimulate.getAchievementCount();
                    addMessage("forecast: Money: "+myMoneySimulate+", Achievement: "+myAchievementSimulate);
                }
                if(myAchievementSimulate >= targetAchi && myMoneySimulate >= targetMoney){
                    addMessage("順調です!");
                    if(handNum == 0) putWorker(worker0, place0, option0);
                    else if(handNum==1) putWorker(worker1, place1, option1);
                    else putWorker("M", place2, option2);
                } else if((canM || canS) && (handNum < 2) && (myAchievement < targetAchi)) {
                    if(Board(chara,"4-1") != null) putWorker(chara, "4-1", "");
                    else if(Board(chara, "4-4") != null) putWorker(chara, "4-4", "");
                    else if(Board(chara, "5-2") != null) putWorker(chara, "5-2", "");
                    else if(enemyResearcher) {
                        i=0;
                        do{
                            place = researcherHand[i][0];
                            option = researcherHand[i][1];
                            i++;
                        } while(Board("R", place, option) == null);
                        putWorker("R", place, option); 
                    } else {
                        i=0;
                        do{
                            place = studentHand[i][0];
                            option = studentHand[i][1];
                            i++;
                        } while(Board(chara, place, option) == null);
                        putWorker(chara, place, option); 
                    }
                } else if(!enemyResearcher && currentGameBoard.canPutWorker(myNumber, "2-1", chara, "F") && handNum<2) {
                    putWorker(chara, "2-1", "F"); 
                } else if(canR) {
                    i=0;
                    do{
                        place = researcherHand[i][0];
                        option = researcherHand[i][1];
                        i++;
                    } while(Board("R", place, option) == null);
                    putWorker("R", place, option); 
                } else if (myAchievement >= targetAchi){
                    i=0;
                    do{
                        place = studentHand[i][0];
                        option = studentHand[i][1];
                        i++;
                    } while(Board(chara, place, option) == null);
                    putWorker(chara, place, option); 
                } else {
                    i=0;
                    do{
                        place = managerHandSecond[i][0];
                        option = managerHandSecond[i][1];
                        i++;
                    }while(Board(chara, place, option) == null);
                    putWorker(chara, place, option); 
                }
                
            }
            
            // season 5
            void firstFinal(){
                putWorker("R", "3-1", "");
                putWorker("R", "2-1", "F");
                putWorker("R", "1-1", "F");
                putWorker("M", "4-4", "");
                putWorker("M", "4-1", "");
                putWorker("M", "6-1", "");
                putWorker("M", "1-1", "F");
            }
            // season10
            void finalSeason(){
                String chara;
                if(canM) chara="M";
                else chara="S";
                addMessage("season 10!!!");
                putWorker(chara, "2-1", "F");
                putWorker("R", "3-1", "");
                putWorker("R", "2-1", "F");
                putWorker("R", "1-1", "F");
                putWorker(chara, "5-2", "");
                putWorker(chara, "4-4", "");
                putWorker(chara, "4-1", "");
                putWorker(chara, "6-1", "");
                putWorker(chara, "2-1", "F");
                putWorker(chara, "1-1", "F");
            }
            
            /**
             * @param place アクションID
             * @return placeとoptionに一致する盤面を返す
             */
            Game Board(String chara, String place){
                return canPlace[ID_convert(chara)][placeList.indexOf(place)];
            }
            /**
             * @param place アクションID
             * @param option "", "G" など
             * @return placeとoptionに一致する盤面を返す
             */
            Game Board(String chara, String place, String option){
                int index = placeList.indexOf(place);
                while(!option.equals(optionList[index])) index++;
                return canPlace[ID_convert(chara)][index];
            }
            
            /**
             * @retuen R:0, M:1, S:2
             */
            int ID_convert(String chara){
                switch(chara){
                    case "R":
                        return 0;
                    case "M":
                        return 1;
                    case "S":
                        return 2;
                    default:
                        return -1;
                }
            }
        }
        
        Hand hand = new Hand();
        
        switch(season){
            case "01":
            case "02":
            case "03":
            case "04":
                hand.first();
                break;
            case "05":
                hand.firstFinal();
                break;
            case "06":
            case "07":
            case "08":
            case "09":
                hand.second();
                break;
            case "10":
                hand.finalSeason();
                break;
        }
        
    }
    
    
    /**
     * エンジニアとリーダーのAIプラン（シミュレーション用）
     * @param currentGameBoard 複製したゲームボード
     */
    public void GandE(Game currentGameBoard, GameResources myResources, GameResources enemyResources, String season){
        // 両者のリソースを取得    
        int myMoney = myResources.getCurrentMoney();
        int myFlask = myResources.getCurrentResrchPoint(0);
        int myGear = myResources.getCurrentResrchPoint(1);
        int enemyMoney = enemyResources.getCurrentMoney();
        int enemyFlask = enemyResources.getCurrentResrchPoint(0);
        int enemyGear = enemyResources.getCurrentResrchPoint(1);
        // 両者のスコアを取得
        int myScore = myResources.getTotalScore();
        int enemyScore = enemyResources.getTotalScore();
        // 両者の最終スコアを取得
        int myFinalScore = currentGameBoard.getFinalScore()[this.myNumber];
        int enemyFinalScore = currentGameBoard.getFinalScore()[this.enemyNumber];
        // 両者のアチーブメントを取得
        int myAchievement = myResources.getAchievementCount();
        int enemyAchievement = enemyResources.getAchievementCount();
        // 自分が先手かどうかを取得
        boolean startPlayer = myResources.isStartPlayer();
        // 未使用の駒を取得
        ArrayList<String> myworkers = myResources.getUnusedWorkers();
        ArrayList<String> enemyworkers = enemyResources.getUnusedWorkers();
        // 自分が何の駒を打てるかを取得
        boolean canG = currentGameBoard.canPutWorker(this.myNumber, "1-1", "G", "M");
        boolean canE = currentGameBoard.canPutWorker(this.myNumber, "1-1", "E", "M");
        boolean canS = currentGameBoard.canPutWorker(this.myNumber, "1-1", "S", "M");
        
        List<String> placeList = Arrays.asList(actionIDcheck[0]);
        String optionList[] = actionIDcheck[1];
        
        //String[] bestHand = {null, null, null};
        
        // ローカルクラス
        class Hand {
            String[][] bestHand = new String[4][3];
            String[][] hand;
            double bestPoints[] = {0,0,0,0};
            boolean finalHandFlag;
            long startTime;

            // コンストラクタ
            Hand() {
                this.hand = new String[][]{{null,null,null}, {null,null,null}, {null,null,null}};
            }
            
            void ResetBestPoints(){
                for(int k=0;k<4;k++){
                    bestPoints[k] = -1000;
                }
            }
            String[] getBestHand(int index){
                return this.bestHand[index];
            }
            void setFinalHandFlag(boolean flag){
                this.finalHandFlag = flag;
            }
            void setStartTime(){
                this.startTime = System.currentTimeMillis();
            }
            long getTime(){
                long endTime = System.currentTimeMillis();
                return endTime - this.startTime;
            }
            void setHand(int index,String worker, String place, String option){
                this.hand[index-1][0] = worker;
                this.hand[index-1][1] = place;
                this.hand[index-1][2] = option;
            }
            String[] getHand(int index){
                return this.hand[index-1];
            }
            /*
            void setCounter(int count){
                this.i = count;
            }*/
            
            double evaluation(Game game, int playerNumber){
                double point=0; // 評価ポイント
                
                String season = game.getSeason();
                GameResources myResources = game.getResourcesOf(playerNumber);
                // 両者のリソースを取得    
                double myMoney = myResources.getCurrentMoney();
                double myFlask = myResources.getCurrentResrchPoint(0);
                double myGear = myResources.getCurrentResrchPoint(1);
                // 両者のスコアを取得
                double myScore = myResources.getTotalScore();
                // 両者の最終スコアを取得
                double myFinalScore = currentGameBoard.getFinalScore()[playerNumber];
                // 両者のアチーブメントを取得
                double myAchievement = myResources.getAchievementCount();
                // 自分が先手かどうかを取得
                boolean startPlayer = myResources.isStartPlayer();
                
                // 重みづけ
                if(playerNumber == myNumber){
                    // 自分
                    myMoney *= 0.5;
                    myFlask *= 2;
                    myGear *= 2;
                    myScore *= 10;
                    myFinalScore *= 2;
                    myAchievement *= 0.5;
                    if(startPlayer) point+=1;
                } else {
                    // 相手
                    myMoney *= 1;
                    myFlask *= 2;
                    myGear *= 2;
                    myScore *= 10;
                    myFinalScore *= 1;
                    myAchievement *= 1;
                    if(startPlayer) point+=1;
                }
                
                point += myMoney + myFlask + myGear + myScore + myFinalScore + myAchievement;
                
                return point;
            }
            
            /**
             * 
             */
            double simulate(Game originalGameBoard, int handCount, double points){
                //System.out.println("simulate");
                Game simulateBoard, simulateBoardE;
                double myPoints, enemyPoints, evaPoints = 0;
                for(String worker : myworkers){
                    int j=0;
                    for(String place : placeList){
                        String option = optionList[j];
                        if(!originalGameBoard.canPutWorker(myNumber, place, worker, option)){
                            j++;
                            continue;
                        }
                        if(handCount!=0) setHand(handCount, worker, place, option);
                        simulateBoard = simulateGame(originalGameBoard, myNumber, worker, place, option);
                        myPoints = evaluation(simulateBoard, myNumber);
                        
                        if(handCount == 0){
                            evaPoints = points + myPoints;
                            if(bestPoints[handCount] < evaPoints){
                                bestPoints[handCount] = evaPoints;
                                bestHand[handCount][0] = worker;
                                bestHand[handCount][1] = place;
                                bestHand[handCount][2] = option;
                                addMessage("[hand: "+handCount+"] points: "+String.format("%.1f", evaPoints)+":  "+worker+" "+place+" "+option);
                                System.out.println("[hand: "+handCount+"] j = "+j+", worker: "+worker+" "+place+" "+option);
                                for(int index=1;index<4;index++){
                                    bestHand[index] = this.getHand(index);
                                    if(bestHand[index][0] != null){
                                        addMessage("[hand: "+index+"] "+bestHand[index][0]+" "+bestHand[index][1]+" "+bestHand[index][2]);
                                        System.out.println("[hand: "+index+"] "+bestHand[index][0]+" "+bestHand[index][1]+" "+bestHand[index][2]);  
                                    }
                                }
                            }
                        } else {
                            
                            for(String workerE : enemyworkers){
                                int i=0;
                                for(String placeE : placeList){
                                    String optionE = optionList[i];
                                    if(!simulateBoard.canPutWorker(enemyNumber, placeE, workerE, optionE)){
                                        //System.out.println("[can't put] i ="+i);
                                        i++;
                                        continue;
                                    }
                                    System.out.println("handCount: "+handCount+", j = "+j+", worker: "+worker+", i = "+i+", enemy: "+workerE);
                                    evaPoints = points + myPoints;/*
                                    if(bestPoints[handCount] < evaPoints){
                                        bestPoints[handCount] = evaPoints;
                                        bestHand[handCount][0] = worker;
                                        bestHand[handCount][1] = place;
                                        bestHand[handCount][2] = option;
                                        addMessage("[hand: "+handCount+"] points: "+String.format("%.1f", evaPoints)+":  "+worker+" "+place+" "+option);
                                        System.out.println("[hand: "+handCount+"] j = "+j+", worker: "+worker+" "+place+" "+option);
                                    }*/
                                    simulateBoardE = simulateGame(simulateBoard, enemyNumber, workerE, placeE, optionE);
                                    enemyPoints = evaluation(simulateBoardE, enemyNumber);
                                    //System.out.println("enemyPoints: "+enemyPoints);
                                    evaPoints = points + myPoints - enemyPoints;
                                    evaPoints = simulate(simulateBoardE, handCount-1, evaPoints);
                                    i++;
                                }
                            }
                            
                            //handCount--;
                        }/*
                        System.out.println("handCount: "+(handCount)+", j = "+j+", worker: "+worker);
                        if(bestPoints[handCount] < evaPoints){
                            bestPoints[handCount] = evaPoints;
                            bestHand[handCount][0] = worker;
                            bestHand[handCount][1] = place;
                            bestHand[handCount][2] = option;
                            addMessage("hand: "+handCount+", points: "+String.format("%.1f", evaPoints)+":  "+worker+" "+place+" "+option);
                            System.out.println("j = "+j+", "+worker+" "+place+" "+option);
                        }*/
                        j++;
                    }
                }
                return evaPoints;
            }
            
        }
        
        
        Hand hand = new Hand();
        
        /*
        シミュレーション
        */
        int remainHand;
        int i=0;
        double points, bestPoints = 0;
        String[] bestHand = {null, null, null};
        //Game simulateBoard = currentGameBoard;
        
        if(Integer.parseInt(season) < 6) remainHand = 1 - this.handNum;
        else {
            remainHand = 2 - this.handNum;
            if(remainHand >= 2) remainHand = 1;
        }
        
        System.out.println("----------------------------------------------");
        System.out.println("season: "+season);
        System.out.println("remainHand: "+remainHand);
        System.out.print("enemyworkers: ");
        for(String worker : enemyworkers) System.out.print(worker+" ");
        System.out.print("\n----------------------------------------------\n");
        
        if(remainHand == 0) hand.setFinalHandFlag(true);
        else hand.setFinalHandFlag(false);
        hand.ResetBestPoints();
        hand.setStartTime();
        hand.simulate(currentGameBoard, remainHand, 0.0);
        bestHand = hand.getBestHand(remainHand);
        System.out.println("bestHand: "+bestHand[0]+" "+bestHand[1]+" "+bestHand[2]);
        System.out.println("time: "+String.format("%.2f",hand.getTime()/1000.0)+"s");
        
        /*
        for(String worker : myworkers){
            i=0;
            for(String place : actionIDcheck[0]){
                String option = actionIDcheck[1][i];
                simulateBoard = simulateGame(currentGameBoard, myNumber, worker, place, option);
                if(simulateBoard == null){
                    i++;
                    continue;
                }
                points = hand.evaluation(simulateBoard);
                if(bestPoints < points){
                    bestPoints = points;
                    bestHand[0] = worker;
                    bestHand[1] = place;
                    bestHand[2] = option;
                    addMessage("points: "+String.format("%.1f", points)+":  "+worker+" "+place+" "+option);
                }
                i++;
            }
        }
        */
        // 最善手をプレイ
        this.putWorker(bestHand[0], bestHand[1], bestHand[2]);
        
    }
    
    /**
     * 天才科学者とリサーチャーのAIプラン
     * @param currentGameBoard 複製したゲームボード
     */
    public void GandR(Game currentGameBoard, GameResources myResources, GameResources enemyResources, String season){
        
        // 両者のリソースを取得    
        int myMoney = myResources.getCurrentMoney();
        int myFlask = myResources.getCurrentResrchPoint(0);
        int myGear = myResources.getCurrentResrchPoint(1);
        int enemyMoney = enemyResources.getCurrentMoney();
        int enemyFlask = enemyResources.getCurrentResrchPoint(0);
        int enemyGear = enemyResources.getCurrentResrchPoint(1);
        // 両者のスコアを取得
        int myScore = myResources.getTotalScore();
        int enemyScore = enemyResources.getTotalScore();
        // 両者のアチーブメントを取得
        int myAchievement = myResources.getAchievementCount();
        int enemyAchievement = enemyResources.getAchievementCount();
        // 自分が先手かどうかを取得
        boolean startPlayer = myResources.isStartPlayer();
        
        // 自分が何の駒を打てるかを取得
        boolean canG = currentGameBoard.canPutWorker(this.myNumber, "1-1", "G", "M");
        boolean canR = currentGameBoard.canPutWorker(this.myNumber, "1-1", "R", "M");
        boolean canS = currentGameBoard.canPutWorker(this.myNumber, "1-1", "S", "M");
        
        /** 
         * 盤面の打てる場所一覧を取得 
         * @param canPlace[i][j]
         * i=0 : genius
         * i=1 : researcher
         * i=2 : student
         */
        Game[][] canPlace = new Game[3][actionIDcheck[0].length];
        String C;
        
        for(int i=0;i<3;i++){
            if(!canG && i==0) continue;
            if(!canR && i==1) continue;
            if(!canS && i==2) continue;
            
            if(i==0) C="G";
            else if(i==1) C="R";
            else C="S";
            int j=0;
            for(String ID : actionIDcheck[0]){
                canPlace[i][j] = simulateGame(currentGameBoard, this.myNumber, C, ID, actionIDcheck[1][j]);
                j++;
            }
        }
        List<String> placeList = Arrays.asList(actionIDcheck[0]);
        String optionList[] = actionIDcheck[1];
        
        // handオブジェクトを宣言
        Hand hand = new Hand();
        hand.Hand(currentGameBoard);
        
        // 打つ場所の配列
        String[][] researcherHand = {{"2-1","F"},{"1-1","F"}};
        String[][] geniusHand = {{"2-1","F"},{"1-1","F"}};
        String[][] studentHand = {{"2-1","F"},{"1-1","F"}};
        
        // プレイ
        switch (season) {
            // 前半戦
            case "01":
            case "02":
            case "03":
            case "04":
            case "05":
                if(hand.canPut("G", "5-5", "")) {
                    this.putWorker("G", "5-5", "");
                } else if(hand.canPut("R", "3-1", "") && myFlask < 8) {
                    this.putWorker("R", "3-1", "");
                } else if(hand.canPut("R", "2-1", "F") && myFlask < 12) {
                    this.putWorker("R", "2-1", "F");
                } else if(myMoney < 2 && (hand.canPut("G", "6-2", "FG") || hand.canPut("G", "6-2", "FF"))){
                    if(hand.canPut("G", "6-2", "FG") && myFlask%2 == 0) this.putWorker("G", "6-2", "FG");
                    else this.putWorker("G", "6-2", "FF");
                } else if(hand.canPut("G","2-1","F") && (myFlask<14 || myFlask>=14 && this.handNum==1 && hand.simulateMyResource(hand.simulate("G","2-1","F"),"M")>=3)) {
                    if(myFlask == 8 || myFlask >= 14) this.putWorker("G", "2-1", "G");
                    else this.putWorker("G", "2-1", "F");
                } else if(hand.canPut("G", "6-2", "FG") || hand.canPut("G", "6-2", "FF")){
                    if(hand.canPut("G", "6-2", "FG") && myFlask%2 == 0) this.putWorker("G", "6-2", "FG");
                    else this.putWorker("G", "6-2", "FF");
                } else if(canG) {
                    int i=0;
                    String place, option;
                    while(true) {
                        place = geniusHand[i][0];
                        option = geniusHand[i][1];
                        System.out.println("G: "+place+" "+option);
                        if(hand.canPut("G", place, option)) {
                            this.putWorker("G", place, option);
                            break;
                        }
                        i++;
                    }
                } else {
                    int i=0;
                    String place, option;
                    while(true) {
                        place = researcherHand[i][0];
                        option = researcherHand[i][1];
                        System.out.println("R: "+place+" "+option);
                        if(hand.canPut("R", place, option)) {
                            this.putWorker("R", place, option);
                            break;
                        }
                        i++;
                    }
                }
                
                break;
                
            // 後半戦
            case "06":
            case "07":
            case "08":
            case "09":
            case "10":
                if(canS) C="S";
                else C="G";
                
                if(hand.canPut("G", "5-5", "")) {
                    this.putWorker("G", "5-5", "");
                } else if(hand.canPut("R", "3-1", "") && myFlask < 8) {
                    this.putWorker("R", "3-1", "");
                } else if(hand.canPut("R", "2-1", "F") && myFlask < 12) {
                    this.putWorker("R", "2-1", "F");
                } else if(season.equals("10") && !canG && (hand.canPut("S","4-1","")||(hand.canPut("S","4-2",""))||(hand.canPut("S","4-4","")))) {
                    System.out.println("final season hand.");
                    if(hand.canPut("S","4-4","")) this.putWorker("S", "4-4", "");
                    else if(hand.canPut("S","4-2","")) this.putWorker("S", "4-2", "");
                    else this.putWorker("S", "4-1", "");
                } else if(myMoney < 2 && (hand.canPut(C, "6-2", "FG") || hand.canPut(C, "6-2", "FF"))){
                    if(hand.canPut(C, "6-2", "FG")&& myFlask % 2 == 0) this.putWorker(C, "6-2", "FG");
                    else this.putWorker(C, "6-2", "FF");
                } else if(hand.canPut(C,"2-1","F") && (myFlask<14 || myFlask>=14 && this.handNum==1 && hand.simulateMyResource(hand.simulate(C,"2-1","F"),"M")>=3)) {
                    if(myFlask == 8 || myFlask >= 14) this.putWorker(C, "2-1", "G");
                    else this.putWorker(C, "2-1", "F");
                } else if(hand.canPut(C, "6-2", "FG") || hand.canPut(C, "6-2", "FF")){
                    if(hand.canPut(C, "6-2", "FG") && myFlask%2 == 0) this.putWorker(C, "6-2", "FG");
                    else this.putWorker(C, "6-2", "FF");
                } else if(hand.canPut(C, "3-1", "") && (hand.simulateMyResource(hand.simulate(C,"3-1",""),"M")>=3)) {
                    this.putWorker(C, "3-1", "");
                } else if(canG || canS) {
                    int i=0;
                    String place, option;
                    while(true) {
                        place = studentHand[i][0];
                        option = studentHand[i][1];
                        if(hand.canPut(C, place, option)) {
                            this.putWorker(C, place, option);
                            break;
                        }
                        i++;
                    }
                } else {
                    int i=0;
                    String place, option;
                    while(true) {
                        place = researcherHand[i][0];
                        option = researcherHand[i][1];
                        if(hand.canPut("R", place, option)) {
                            this.putWorker("R", place, option);
                            break;
                        }
                        i++;
                    }
                }
                
                break;
        }
        
        
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai; 


import gameElements.Board;
import gameElements.Game;
import gameElements.GameResources;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ss04x
 */
public class Seo_AI {
    // オプション
    private String[] resource = {"", "M", "F", "G"}; 
    
    // 現在のボード状態
    protected Game currentGame;
    // 自分の番号
    protected int myNumber;
    // 相手の番号
    protected int enemyNumber;
    // 最善手
    protected String[] bestMove;
    // 評価関数の閾値{スコア, お金, フラスコ, ギア, スタートフラグ}
    protected double[][] W = {{0.3, 0.2, 0.0, 0.2, 0.5},    // 自分
                              {0.3, 0.2, 0.0, 0.2, 0.5}};   // 相手
    
    protected int count=0;
    
    public Seo_AI(Game currentGame, int myNumber){
        this.currentGame = currentGame;
        this.myNumber = myNumber;
        this.enemyNumber = (this.myNumber + 1) % 2;
        this.bestMove = new String[3];
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
        }
        else {
            // 打てないならnullを返す
            return null;
        }
    }
    
    public void setW(double[][] w){
        this.W = w;
    }
    
    public void updateGame(Game game){
        this.currentGame = game;
    }
    
    /**
     * ゲーム状態を評価する
     * ここの重みを変更することでAIの次の手が変わる
     */
    private double evaluate(Game game){
        // 例外処理
        if(game == null){
            return -1000.0;
        }
        
        // 各リソースへの重み[得点, お金, フラスコ, ギア, スタートフラグ]
        double myW[] = this.W[this.myNumber];
        //double enW[] = {3.0, 2.0, 2.0, 0.0, 3}; // GandR
        double enW[] = this.W[this.enemyNumber];
        
        
        // 自分と相手のリソースを取得
        GameResources myResources = game.getResourcesOf(this.myNumber);
        GameResources enemyResources = game.getResourcesOf(this.enemyNumber);
        int myScore = myResources.getTotalScore();
        int myMoney = myResources.getCurrentMoney();
        int myFlask = myResources.getCurrentResrchPoint(0);
        int myGear = myResources.getCurrentResrchPoint(1);
        int enemyScore = enemyResources.getTotalScore();
        int enemyMoney = enemyResources.getCurrentMoney();
        int enemyFlask = enemyResources.getCurrentResrchPoint(0);
        int enemyGear = enemyResources.getCurrentResrchPoint(1);
        int myAchcnt = myResources.getAchievementCount();
        int enemyAchcnt = enemyResources.getAchievementCount();
        boolean startFlag = myResources.isStartPlayer();
        
        // リソースから基礎点を計算
        double myValue = myScore*myW[0] + myMoney*myW[1] + myFlask*myW[2] + myGear*myW[3];
        double enemyValue = enemyScore*enW[0] + enemyMoney*enW[1] + enemyFlask*enW[2] + enemyGear*enW[3];
        
        
        // 基礎点に先手や発表回数などの重みを追加する
        myValue += myAchcnt * myW[0];
        enemyValue += enemyAchcnt * enW[0];
        if(startFlag){
            myValue += myW[4];
        } else {
            enemyValue += enW[4];
        }
        
        // 自分の評価から相手の評価を引いて評価値とする
        return (myValue - enemyValue);
    }
       
    
    /**
     * 一手先を読む
     */
    public void search(){
        // 次の状態
        Game nextGame;
        // 最善手
        String[] nextMove = new String[3];
        // 評価値
        double eva;
        // 最善手の評価
        double maxEva = -1000.0;
        // リソース情報
        GameResources myResources = this.currentGame.getResourcesOf(this.myNumber);
        
        // 使用可能なコマ
        for(String job: myResources.getUnusedWorkers()){
            // 各場所
            for(String place: Board.PLACE_NAMES){
                // オプション
                for(String option:resource){
                    // シミュレーションしてみる
                    nextGame = simulateGame(this.currentGame, this.myNumber, job, place, option);
                    if(nextGame != null){
                        
                        eva = evaluate(nextGame);
                        System.out.println(job + "  " + place + "  " + option + " = " + eva);
                        // 評価値が最大値を上回っていれば最善手を書き換える
                        if(maxEva < eva){
                            maxEva = eva;
                            nextMove[0] = job;
                            nextMove[1] = place;
                            nextMove[2] = option;
                        }
                        
                        // オプションなしでOKならほかのオプションは試さずに次へ行く
                        if("".equals(option)){
                            break;
                        }
                    }
                    
                }
            }
        }
        this.bestMove = nextMove;
    }
    
    
    
    /**
     * 打てる手を全て試す(αβ法) 
     */
    
    // すべての打てる手をリストにして返す
    protected List allMove(Game game, int number){
        // 動ける手
        List<String[]> allMove = new ArrayList<>();
        // リソース情報
        GameResources myResources = game.getResourcesOf(number);
        
        // 使用可能なコマ
        for(String job: myResources.getUnusedWorkers()){
            // 各場所
            for(String place: Board.PLACE_NAMES){
                // オプション
                for(String option:resource){
                    if(simulateGame(game, number, job, place, option) != null){
                        String[] move = {job, place, option};
                        allMove.add(move);
                        
                        // オプションがなくてよかったらほかのオプションは試さない
                        if("".equals(option)){
                                break;
                        }
                    }
                }
            }
        }
        /*for(String[] m: allMove){
            System.out.println(m[0] +" "+ m[1] +" "+ m[2]);
        }*/
        return allMove;
    }
    
    // αβ法の開始
    public void search_ab(int deep){
        //System.out.println("〈探索開始〉");
        Game game = this.currentGame.clone();
        // 次のゲーム状態
        Game nextGame;
        // 評価値
        double eva;
        double alpha = -1000;
        double beta = 1000;
        
        // 次に動ける手を求める
        List<String[]> moveList = allMove(game, this.myNumber);
        //System.out.println(moveList.size());
        
        for(String[] move: moveList){
            nextGame = simulateGame(game, this.myNumber, move[0], move[1], move[2]);
            eva = ab_MIN(nextGame, deep-1, alpha, beta);
            if(alpha < eva){
                //System.out.println("update bestMove");
                alpha = eva;
                this.bestMove = move;
            }
        }
        //System.out.println("探索した道： " + count);
    }
    
    // 相手の手番
    protected double ab_MIN(Game game, int deep, double alpha, double beta){
        // 最終場面なら評価値を返す
        // 駒がなくて相手は打てないが自分は打てる場合は、ab_MAXへ飛ばす
        if(deep == 0 || game.getGameState() == Game.STATE_SEASON_END){
            count++;
            return evaluate(game);
        }else if(!game.getResourcesOf(this.enemyNumber).hasWorker()){
            return ab_MAX(game, deep, alpha, beta);
        }
        
        // 次のゲーム状態
        Game nextGame;
        // 評価値
        double eva;
        
        // 相手の打てる手のリスト
        List<String[]> moveList = allMove(game, this.enemyNumber);
        
        for(String[] move: moveList){
            nextGame = simulateGame(game, this.enemyNumber, move[0], move[1], move[2]);
            eva = ab_MAX(nextGame, deep-1, alpha, beta);
            // より小さい評価値(=自分にとって悪い手)を探す
            if(beta > eva){
                beta = eva;
                // ほかの手の評価値がすでにこの手の最小評価値より大きかったら、探索を打ち切る
                if(alpha >= beta){
                    return alpha;
                }
            }
        } 
        return beta;
    }
    
    // 自分の手番
    protected double ab_MAX(Game game, int deep, double alpha, double beta){
        // 最終場面なら評価値を返す
        // 駒がなくて自分は打てないが相手は打てる場合は、ab_MINへ飛ばす
        if(deep == 0 || game.getGameState() == Game.STATE_SEASON_END){
            count++;
            return evaluate(game);
        }else if(!game.getResourcesOf(this.myNumber).hasWorker()){
            return ab_MIN(game, deep, alpha, beta);
        }
        
        // 次のゲーム状態
        Game nextGame;
        // 評価値
        double eva;
        
        // 相手の打てる手のリスト
        List<String[]> moveList = allMove(game, this.myNumber);
        
        for(String[] move: moveList){
            nextGame = simulateGame(game, this.myNumber, move[0], move[1], move[2]);
            eva = ab_MIN(nextGame, deep-1, alpha, beta);
            // より小さい評価値(=自分にとって悪い手)を探す
            if(alpha < eva){
                alpha = eva;
                // ほかの手の評価値がすでにこの手の最小評価値より大きかったら、探索を打ち切る
                if(alpha >= beta){
                    return beta;
                }
            }
        } 
        return alpha;
    }
    
    /**
     * 最善手の取得
     */
    public String[] getBestMove(){
        if(this.bestMove == null)
            return null;
        else
            return this.bestMove;
    }
    
    // シミュレーション用にゼミに置くだけの関数を作った
    public String[] zemi(){
        List<String[]> moveList = allMove(this.currentGame, this.myNumber);
        return moveList.get(0);
    }
    
}

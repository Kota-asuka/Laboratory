/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simpleclient;

import ai.*;
import gameElements.Game;
import java.util.Random;

/**
 *
 * @author koji
 */
public class SimpleClient {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // メインゲーム
        Game testGame;
        // 使うAI(不要であればいらない)
        Seo_AI ai1, ai2;
        // ループ回数
        int limit = 10;
        
        // シミュレーション
        int count = 0;
        while(count < limit){
            count++;
            System.out.println("≪ Game: " + count + " ≫");

            // ゲームのインスタンス化
            testGame = new Game();

            // 対戦するAIの設定(正直名前はどうでもいい)
            testGame.setPlayerName("Player1");
            testGame.setWorkers(0, "E", "L");
            testGame.setPlayerName("Player2");
            testGame.setWorkers(1, "E", "L");

            // ゲームプレイ
            while(testGame.getGameState() != Game.STATE_GAME_END){
                if(testGame.getGameState() == Game.STATE_SEASON_END){
                    testGame.changeNewSeason();
                }else if(testGame.getCurrentPlayer() == 0){
                    /*
                    ai1の処理を記述する
                    */
                    ai1 = new Seo_AI(testGame, 0);
                    ai1.search_ab(3);
                    String move[] = ai1.getBestMove();
                    
                    // メインゲームに反映させる
                    testGame.play(0, move[1], move[0], move[2]);

                }else{
                    /*
                    ai2の処理を記述する
                    */
                    ai2 = new Seo_AI(testGame, 1);
                    ai2.search_ab(3);
                    String move[] = ai2.getBestMove();
                    
                    // メインゲームに反映させる
                    testGame.play(1, move[1], move[0], move[2]);

                }
            }
            
            // スコア表示
            int score[] = testGame.getFinalScore();
            System.out.println("result ");
            System.out.println("ai1: " + score[0]);
            System.out.println("ai2: " + score[1]);
        }
    
    
    }
}

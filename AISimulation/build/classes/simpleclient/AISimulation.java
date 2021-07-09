/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simpleclient;

import ai.*;
import gameElements.Game;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author koji
 */
public class AISimulation {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Game testGame;      // メインゲーム
        Seo_AI ai1, ai2;    // 使うAI(不要であればいらない)
        int limit = 10000;     // ループ回数
        
        // 検証用に作ったやつ
        String path = "C:\\Users\\ss04x\\OneDrive - 独立行政法人 国立高等専門学校機構\\共有ファイル\\5E\\前期\\電気電子工学実験\\AISimulation\\simulation_result";
        String fname = path + "\\" + "ELvsEL.csv";  // ファイル名
        ArrayList<String> result = new ArrayList<>();   // CSVに書き込む用のデータ
        result.add("worker1, worker2, score, money, flask, gear, startflag, score1(startFlag=True), score2(startFlag=False)");
        Random random = new Random();
        
        // シミュレーション
        int count = 1000;  // スコアの重みが少しでもないと意味ないため
        while(count < limit){
            count++;
            System.out.println("≪ Game: " + (count-1000) + " ≫");

            // ゲームのインスタンス化
            testGame = new Game();
            
            // 検証用に作るランダムな値（ワーカー、評価関数の重み）
//            String[][] worker = {{Game.WORKER_ID[random.nextInt(9)], Game.WORKER_ID[random.nextInt(9)]},    // 自分
//                                 {Game.WORKER_ID[random.nextInt(9)], Game.WORKER_ID[random.nextInt(9)]}};   // 相手
            String[][] worker = {{"E", "L"},    // 自分
                                 {"E", "L"}};   // 相手
//            double[][] W = {{random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble()},      // 自分
//                            {random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble()}};     // 相手
            double[][] W = {{(count-1)/1000, ((count-1)%1000)/100, 0, ((count-1)%100)/10, ((count-1)%10)},   // フラスコは最初から0
                            {(count-1)/1000, ((count-1)%1000)/100, 0, ((count-1)%100)/10, ((count-1)%10)}};
            
            // 対戦するAIの設定
            ai1 = new Seo_AI(testGame, 0);
            testGame.setPlayerName("Player1");
            testGame.setWorkers(0, worker[0][0], worker[0][1]);
            ai1.setW(W);
            ai2 = new Seo_AI(testGame, 1);
            testGame.setPlayerName("Player2");
            testGame.setWorkers(1, worker[1][0], worker[1][1]);
            ai2.setW(W);

            // ゲームプレイ
            while(testGame.getGameState() != Game.STATE_GAME_END){
                if(testGame.getGameState() == Game.STATE_SEASON_END){
                    testGame.changeNewSeason();
                }else if(testGame.getCurrentPlayer() == 0){
                    /*
                    ai1の処理を記述する
                    SampleAIをそのまま使うのは無理（GUIとデータのやり取りをしてしまうため）
                    そのためプログラムの改変の必要があるかも
                    */
                    ai1.updateGame(testGame);
                    ai1.search_ab(3);
                    String move[] = ai1.getBestMove();
                    
                    // メインゲームに反映させる
                    testGame.play(0, move[1], move[0], move[2]);

                }else{
                    /*
                    ai2の処理を記述する
                    SampleAIをそのまま使うのは無理（GUIとデータのやり取りをしてしまうため）
                    そのためプログラムの改変の必要があるかも
                    */
                    ai2.updateGame(testGame);
                    ai2.search_ab(3);
                    String move[] = ai2.getBestMove();
//                    String move[] = ai2.zemi();
                    
                    // メインゲームに反映させる
                    testGame.play(1, move[1], move[0], move[2]);

                }
            }
            
            // スコア表示
            int score[] = testGame.getFinalScore();
            System.out.println("result ");
            System.out.println("ai1: " + score[0]);
            System.out.println("ai2: " + score[1]);
            // 結果を追加
            if(score[0] >= score[1]){
                // Wはdouble型の配列なのでString型の配列に変換する
                String W_str[] = new String[W[0].length];
                for (int i = 0; i < W[1].length; i++) W_str[i] = Double.toString(W[0][i]);
                // スコアも追加したいのでスコアをString型の配列に変換する
                String S_str[] = {Integer.toString(score[0]), Integer.toString(score[1])};
                // 結合して追加
                String[] data;
                data = concat(worker[0], W_str);
                data = concat(data, S_str);
                result.add(String.join(",", data));
                
            }else{
                String W_str[] = new String[W[1].length];
                for (int i = 0; i < W[1].length; i++) W_str[i] = Double.toString(W[1][i]);
                String S_str[] = {Integer.toString(score[0]), Integer.toString(score[1])};
                String[] data;
                data = concat(worker[1], W_str);
                data = concat(data, S_str);
                result.add(String.join(",", data));
            }
            
            // 途中で一度書き込んでリセット
            if((count%100) == 0){
                System.out.println("kakikomi");
                exportCsv(fname, result);
                result.clear();
                result.add(null);
            }
//            // 結果をCSVファイルに書き込む
//            exportCsv(fname, result);
//            result.clear();
//            result.add(null);
        }
    }
    
    /**
     * 配列を結合する
     * 
     * @param <T>       配列のデータ型。使用する際に気にする必要はない
     * @param array1    結合する配列その1
     * @param array2    結合する配列その2
     * @return          結合された配列を返す
     */
    public static <T> T[] concat(final T[] array1, final T... array2) {
        final Class<?> type1 = array1.getClass().getComponentType();
        final T[] joinedArray = (T[]) Array.newInstance(type1, array1.length + array2.length);
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }
    
    /**
     * ArrayList<String>型のデータをCSVファイルに書き込む
     * 既にファイルがある場合は追記する
     * 
     * @param fname ファイル名
     * @param list  書き込むArrayList<String>型のデータ
     */
    public static void exportCsv(String fname, ArrayList<String> list){
        try {
            ArrayList<String> list_c = new ArrayList<>(list);
            // 出力ファイルの作成
            File file = new File(fname);
            // もし既にファイルがあるなら属性部分をなくしておく
            if(file.exists()){
                list_c.remove(0);
            }
            FileWriter fw = new FileWriter(file, true);
            // PrintWriterクラスのオブジェクトを生成
            PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
 
            // データを書き込む
            for(String data: list_c){
                pw.println(data);
                
            }
 
            // ファイルを閉じる
            pw.close();
 
            // 出力確認用のメッセージ
            System.out.println("csvファイルを出力しました");
 
        // 出力に失敗したときの処理
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gameElements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ゲームの進行状況を管理するクラス 手を打てるプレイヤーや得点を管理する
 *
 * @author koji
 */
public class Game extends Observable implements Cloneable {

    // ゲーム進行状況を表す整数
    public static final int STATE_WAIT_PLAYER_CONNECTION = 0;
    public static final int STATE_WAIT_PLAYER_PLAY = 1;
    public static final int STATE_SEASON_END = 2;
    public static final int STATE_GAME_END = 4;

    // 季節を表す番号
    public static String[] SEASON_NAMES = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10"};

    // ワーカーのIDと名前
    public static final String[] WORKER_ID = {"G", "H", "E", "R", "L", "M", "S", "A", "U"};
    public static final String[] WORKER_NAME = {"天才科学者", "努力家", "エンジニア", "リサーチャー", "リーダー", "マネージャー", "学生", "リーダー補助コマ",
        "リーダー補助コマ利用不可"};

    // 最大思考時間
    public static long maxThinkingTime = 1000 * 60 * 5;

    private int CurrentPlayer;
    private Board gameBoard;
    private String[] PlayerName;
    private int gameState;
    private GameResources[] gameResource;
    private int currentSeason = 0;
    private int currentStartPlayer = 0;

    public Game() {
        this.init();
    }

    /**
     * 210 CONFPRMしたときのメッセージ群からゲームボードを再現
     *
     * @param confprmMessages "210 CONFPRM"で返ってきたメッセージの配列
     * @param currentPlayer 現在手番のプレイヤー番号．通常は自分の番号
     */
    public Game(ArrayList<String> confprmMessages, int currentPlayer) {
        // 初期化
        this.gameBoard = new Board();
        this.gameResource = new GameResources[2];
        this.gameResource[0] = new GameResources();
        this.gameResource[1] = new GameResources();
        this.PlayerName = new String[2];

        // どうにも取得できないので名前は適当に
        this.PlayerName[0] = "0";
        this.PlayerName[1] = "1";
        // ゲーム状態は強制的にプレイ待ち
        this.gameState = STATE_WAIT_PLAYER_PLAY;

        this.CurrentPlayer = currentPlayer;

        // メッセージを一つずつ見る
        for (String confprmMessage : confprmMessages) {
            // メッセージを空白で区切る
            String[] args = confprmMessage.split(" ");
            String messageNum = args[0];

            boolean isPlaced61 = false;
            // 種類に応じて処理
            switch (messageNum) {
                case "211":
                    // RESOURCES
                    int playerId = Integer.parseInt(args[2]);
                    // cidsは空文字なことがある
                    ArrayList<String> cids;
                    if (args[3].length() != 0) {
                        cids = new ArrayList<>(Arrays.asList(args[3].split("")));
                    }
                    else {
                        cids = new ArrayList<>();
                    }
                    int money = Integer.parseInt(args[4].substring(1));
                    int flask = Integer.parseInt(args[5].substring(1));
                    int gear = Integer.parseInt(args[6].substring(1));
                    // セット
                    this.gameResource[playerId].setMoney(money);
                    this.gameResource[playerId].setReserchPoint(flask, 0);
                    this.gameResource[playerId].setReserchPoint(gear, 1);
                    this.gameResource[playerId].setWorkerList(cids);
                    break;
                case "212":
                    // BOARD
                    String place = args[2];
                    String worker = args[3].substring(0, 1);
                    playerId = Integer.parseInt(args[3].substring(1, 2));
                    this.gameBoard.putWorker(playerId, place, worker);
                    this.gameResource[playerId].addUsedWorker(worker);
                    if (place.equals("6-1")) {
                        this.gameResource[playerId].setStartPlayer(true);
                        isPlaced61 = true;
                    }
                    break;
                case "213":
                    // SEASON
                    String season = args[2];
                    int seasonId = Arrays.asList(SEASON_NAMES).indexOf(season);
                    this.currentSeason = seasonId;
                    break;
                case "214":
                    // ACHIEVE
                    int p0achieve = Integer.parseInt(args[2]);
                    int p1achieve = Integer.parseInt(args[3]);
                    this.gameResource[0].setAchievementCount(p0achieve);
                    this.gameResource[1].setAchievementCount(p1achieve);
                    break;
                case "215":
                    // SCORE
                    int p0score = Integer.parseInt(args[2]);
                    int p1score = Integer.parseInt(args[3]);
                    this.gameResource[0].setScore(p0score);
                    this.gameResource[1].setScore(p1score);
                    break;
                case "216":
                    // STARTPLAYER
                    int startId = Integer.parseInt(args[2]);
                    this.currentStartPlayer = startId;
                    if (!isPlaced61) {
                        this.gameResource[startId].setStartPlayer(true);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * AIが考える用
     */
    public void startGame() {
        this.PlayerName[0] = "0";
        this.PlayerName[1] = "1";
        this.gameState = STATE_WAIT_PLAYER_PLAY;
    }

    /**
     * ゲームの状況を取得する
     */
    public int getGameState() {
        return this.gameState;
    }

    /**
     * 強制的にゲーム状態を変更する（AI用）
     *
     * @param gameState
     */
    public void setGameState(int gameState) {
        this.gameState = gameState;
    }

    public String[] getPlayerName() {
        return this.PlayerName;
    }

    public int[] getScore() {
        int[] score = new int[2];
        score[0] = this.gameResource[0].getTotalScore();
        score[1] = this.gameResource[1].getTotalScore();
        return score;
    }

    public int[] getFinalScore() {
        int[] scores = this.getScore();
        int[] achive = this.getAchievement();
        for (int p = 0; p < 2; p++) {
            if (achive[p] >= 15) {
                scores[p] += 50;
            } else if (achive[p] >= 10) {
                scores[p] += 30;
            } else if (achive[p] >= 5) {
                scores[p] += 15;
            }
        }
        return scores;
    }

    public int[] getAchievement() {
        int[] score = new int[2];
        score[0] = this.gameResource[0].getAchievementCount();
        score[1] = this.gameResource[1].getAchievementCount();
        return score;
    }

    public int getCurrentPlayer() {
        return this.CurrentPlayer;
    }

    // 以下はボードの状態を変更するメソッドのため、呼び出し時はObserverに必ず通知すること
    /**
     * ボードなどの初期化
     */
    private void init() {
        this.CurrentPlayer = 0;
        this.currentSeason = 0;

        this.gameBoard = new Board();

        this.gameResource = new GameResources[2];
        this.gameResource[0] = new GameResources();
        this.gameResource[0].setStartPlayer(true);
        this.gameResource[1] = new GameResources();

        this.PlayerName = new String[2];
        this.PlayerName[0] = null;
        this.PlayerName[1] = null;

        this.gameState = STATE_WAIT_PLAYER_CONNECTION;

        this.setChanged();
        this.notifyObservers();
    }

    /**
     * * 手が打てるか事前に検証するメソッド。実際にはPLAYとやることは変わらないが 呼び出し時の引数により実際にはおかない
     *
     * @param PlayerID 第１引数：プレイヤー番号0-1
     * @param place 第２引数：コマを置く場所
     * @param workerType 第３引数：workerの種類 PまたはS
     * @param option 第４引数：コマを置くさいのオプション
     * @return 設置可能ならtrueが帰る
     */
    public boolean canPutWorker(int PlayerID, String place, String workerType, String option) {
        return this.play(PlayerID, place, workerType, option, false);
    }

    /**
     * * 実際に手を打つメソッド
     *
     * @param player 第１引数：プレイヤー番号0-1
     * @param place 第２引数：コマを置く場所
     * @param typeOfWorker 第３引数：workerの種類 PまたはS
     * @param option 第４引数：コマを置くさいのオプション
     * @return 設置できたらtrueが帰る
     */
    public boolean play(int player, String place, String typeOfWorker, String option) {
        return this.play(player, place, typeOfWorker, option, true);
    }

    /**
     * * 実際に手を打つメソッドで最後の引数により打てるかの調査なのかが決まる
     *
     * @param player 第１引数：プレイヤー番号0-1
     * @param place 第２引数：コマを置く場所
     * @param typeOfWorker 第３引数：workerの種類 PまたはS
     * @param option 第４引数：コマを置くさいのオプション
     * @param putmode 第５引数：trueの場合は実際に手を打つ：ここで効果を分ける
     * @return 設置に成功できたらtrueが帰る
     */
    private boolean play(int player, String place, String typeOfWorker, String option, boolean putmode) {
        if (this.gameState != STATE_WAIT_PLAYER_PLAY) {
            return false;
        }
        if (this.CurrentPlayer != player) {
            return false;
        }
        if (typeOfWorker.equals("U")) {
            return false;
        }
        if (!this.gameResource[player].hasWorkerOf(typeOfWorker)) {
            return false;
        }
        //場所が開いているかの確認・開いていないけど努力家で置くなら１円払う
        if (typeOfWorker.equals("H")) {
            //努力家を使うときは所持金を確認
            if (!this.gameBoard.checkActionIsEmpty(place)) {
                if (this.gameResource[player].getCurrentMoney() < 1) {
                    return false;
                }
            }
        } else {
            if (!this.gameBoard.checkActionIsEmpty(place)) {
                return false;
            }
        }
        //リーダーの追加コマの場合は１円を払えるかチェック
        if (typeOfWorker.equals("A")) {
            //他のコマがすべて使い終わっているかチェック
            if (this.gameResource[player].getUnusedWorkers().size() > 1) {
                return false;
            }
            if (this.gameResource[player].getCurrentMoney() < 1) {
                return false;
            }
        }

        if (!this.gameBoard.checkOptionIsRight(player, place, option)) {
            return false;
        }

        //1-1はオプションに合わせて打つだけ
        if (place.equals("1-1")) {
            if (putmode) {
                this.gameBoard.putWorker(player, place, typeOfWorker, option);
                this.gameResource[player].putWorker(typeOfWorker);
                if (option.toUpperCase().equals("M")) {
                    this.gameResource[player].addMoney(1);
                } else if (option.toUpperCase().equals("F")) {
                    if (typeOfWorker.equals("R")) {
                        this.gameResource[player].addReserchPoint(1, 0);
                    }
                    this.gameResource[player].addReserchPoint(1, 0);
                } else if (option.toUpperCase().equals("G")) {
                    if (typeOfWorker.equals("E")) {
                        this.gameResource[player].addReserchPoint(3, 1);
                    }
                    this.gameResource[player].addReserchPoint(1, 1);
                }
                if (typeOfWorker.equals("A")) {
                    this.gameResource[player].addMoney(-1);
                }
                this.changePlayer();
                this.setChanged();
                this.notifyObservers();
            }
            return true;
        }
        //2-1と2-2もそのまま打てる
        if (place.startsWith("2-1")) {
            if (putmode) {
                if (typeOfWorker.equals("H")) {
                    if (!this.gameBoard.checkActionIsEmpty(place)) {
                        this.gameResource[player].addMoney(-1);
                    }
                }
                this.gameBoard.putWorker(player, place, typeOfWorker, option);
                if (typeOfWorker.equals("R")) {
                    this.gameResource[player].addReserchPoint(2, 0);
                } else {
                    this.gameResource[player].addReserchPoint(1, 0);
                }

                if (option.toUpperCase().equals("F")) {
                    if (typeOfWorker.equals("R")) {
                        this.gameResource[player].addReserchPoint(1, 0);
                    }
                    this.gameResource[player].addReserchPoint(1, 0);
                } else if (option.toUpperCase().equals("G")) {
                    if (typeOfWorker.equals("E")) {
                        this.gameResource[player].addReserchPoint(3, 1);
                    }
                    this.gameResource[player].addReserchPoint(1, 1);
                }
                if (typeOfWorker.equals("A")) {
                    this.gameResource[player].addMoney(-1);
                }
                this.gameResource[player].putWorker(typeOfWorker);
                this.changePlayer();
                this.setChanged();
                this.notifyObservers();
            }
            return true;
        }
        if (place.startsWith("2-2")) {
            if (putmode) {
                if (typeOfWorker.equals("H")) {
                    if (!this.gameBoard.checkActionIsEmpty(place)) {
                        this.gameResource[player].addMoney(-1);
                    }
                }
                this.gameBoard.putWorker(player, place, typeOfWorker, option);
                //現在の値を一度記録
                int baseValue = this.gameResource[player].getCurrentResrchPoint(1);
                this.gameResource[player].addReserchPoint(baseValue, 1);
                if (typeOfWorker.equals("E")) {
                    this.gameResource[player].addReserchPoint(3, 1);
                }
                if (typeOfWorker.equals("A")) {
                    this.gameResource[player].addMoney(-1);
                }
                this.gameResource[player].putWorker(typeOfWorker);
                this.changePlayer();
                this.setChanged();
                this.notifyObservers();
            }
            return true;
        }

        if (place.equals("3-1")) {
            int cost = 2;
            if (typeOfWorker.equals("M")) {
                cost--;
            } else if (typeOfWorker.equals("A")) {
                cost++;
            }
            if (typeOfWorker.equals("H")) {
                if (!this.gameBoard.checkActionIsEmpty(place)) {
//                    this.gameResource[player].addMoney(-1);
                    cost++;
                }
            }
            if (this.gameResource[player].getCurrentMoney() >= cost) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    if (typeOfWorker.equals("R")) {
                        this.gameResource[player].addReserchPoint(4, 0);
                    }
                    this.gameResource[player].addReserchPoint(4, 0);
                    this.gameResource[player].addMoney(-1 * cost);
                    this.gameResource[player].putWorker(typeOfWorker);

                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            } else {
                return false;
            }
        }
        if (place.equals("3-2")) {
            int cost = 2;
            if (typeOfWorker.equals("M")) {
                cost--;
            } else if (typeOfWorker.equals("A")) {
                cost++;
            } else if (typeOfWorker.equals("H")) {
                if (!this.gameBoard.checkActionIsEmpty(place)) {
//                    this.gameResource[player].addMoney(-1);
                    cost++;
                }
            }
            if (this.gameResource[player].getCurrentMoney() >= cost) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    if (typeOfWorker.equals("E")) {
                        this.gameResource[player].addReserchPoint(3, 1);
                    }
                    this.gameResource[player].addReserchPoint(4, 1);
                    this.gameResource[player].addMoney(-1 * cost);
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            } else {
                return false;
            }
        }

        if (place.equals("3-3")) {
            int cost = 3;
            if (typeOfWorker.equals("M")) {
                cost--;
            } else if (typeOfWorker.equals("A")) {
                cost++;
            } else if (typeOfWorker.equals("H")) {
                if (!this.gameBoard.checkActionIsEmpty(place)) {
//                    this.gameResource[player].addMoney(-1);
                    cost++;
                }
            }
            if (this.gameResource[player].getCurrentMoney() >= cost) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    //flask2
                    if (typeOfWorker.equals("R")) {
                        this.gameResource[player].addReserchPoint(2, 0);
                    }
                    this.gameResource[player].addReserchPoint(2, 0);
                    //gear3
                    if (typeOfWorker.equals("E")) {
                        this.gameResource[player].addReserchPoint(3, 1);
                    }
                    this.gameResource[player].addReserchPoint(3, 1);
                    this.gameResource[player].addMoney(-1 * cost);
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            } else {
                return false;
            }
        }

        if (place.equals("3-4")) {
            int cost = 4;
            if (typeOfWorker.equals("M")) {
                cost--;
            } else if (typeOfWorker.equals("A")) {
                cost++;
            } else if (typeOfWorker.equals("H")) {
                if (!this.gameBoard.checkActionIsEmpty(place)) {
//                    this.gameResource[player].addMoney(-1);
                    cost++;
                }
            }
            if (this.gameResource[player].getCurrentMoney() >= cost) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    if (option.toUpperCase().equals("F")) {
                        //flask4
                        if (typeOfWorker.equals("R")) {
                            this.gameResource[player].addReserchPoint(4, 0);
                        }
                        this.gameResource[player].addReserchPoint(4, 0);
                    } else if (option.toUpperCase().equals("G")) {
                        //gear3
                        if (typeOfWorker.equals("E")) {
                            this.gameResource[player].addReserchPoint(3, 1);
                        }
                        this.gameResource[player].addReserchPoint(5, 1);
                    }
                    this.gameResource[player].addMoney(-1 * cost);
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            } else {
                return false;
            }
        }

        if (place.equals("4-1")) {
            if (typeOfWorker.equals("G")) {
                if (this.gameResource[player].getCurrentMoney() < 1) {
                    return false;
                }
            }
            if (this.gameResource[player].getCurrentResrchPoint(0) >= 2) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    if (typeOfWorker.equals("G")) {
                        this.gameResource[player].addMoney(-1);
                        this.gameResource[player].addScorePoint(2);
                    }
                    this.gameResource[player].addScorePoint(2);
                    this.gameResource[player].addAchievement();
                    this.gameResource[player].addReserchPoint(-2, 0);
                    if (typeOfWorker.equals("A")) {
                        this.gameResource[player].addMoney(-1);
                    } else if (typeOfWorker.equals("H")) {
                        if (!this.gameBoard.checkActionIsEmpty(place)) {
                            this.gameResource[player].addMoney(-1);
                        }
                    }
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }
        if (place.equals("4-2")) {
            if (typeOfWorker.equals("G")) {
                if (this.gameResource[player].getCurrentMoney() < 1) {
                    return false;
                }
            }
            if (this.gameResource[player].getCurrentResrchPoint(1) >= 2) {
                if (putmode) {
                    if (typeOfWorker.equals("H")) {
                        if (!this.gameBoard.checkActionIsEmpty(place)) {
                            this.gameResource[player].addMoney(-1);
                        }
                    }
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    if (typeOfWorker.equals("G")) {
                        this.gameResource[player].addMoney(-1);
                        this.gameResource[player].addScorePoint(2);
                    }
                    this.gameResource[player].addScorePoint(2);
                    this.gameResource[player].addAchievement();
                    this.gameResource[player].addReserchPoint(-2, 1);
                    if (typeOfWorker.equals("A")) {
                        this.gameResource[player].addMoney(-1);
                    }
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }
        if (place.equals("4-3")) {
            if (typeOfWorker.equals("G")) {
                if (this.gameResource[player].getCurrentMoney() < 1) {
                    return false;
                }
            }
            if (this.gameResource[player].getCurrentResrchPoint(1) >= 4) {
                if (putmode) {
                    if (typeOfWorker.equals("H")) {
                        if (!this.gameBoard.checkActionIsEmpty(place)) {
                            this.gameResource[player].addMoney(-1);
                        }
                    }
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    if (typeOfWorker.equals("G")) {
                        this.gameResource[player].addMoney(-1);
                        this.gameResource[player].addScorePoint(2);
                    }
                    this.gameResource[player].addScorePoint(4);
                    this.gameResource[player].addAchievement();
                    this.gameResource[player].addReserchPoint(-4, 1);
                    if (typeOfWorker.equals("A")) {
                        this.gameResource[player].addMoney(-1);
                    }
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }
        if (place.equals("4-4")) {
            if (typeOfWorker.equals("G")) {
                if (this.gameResource[player].getCurrentMoney() < 1) {
                    return false;
                }
            }
            if (this.gameResource[player].getCurrentResrchPoint(0) >= 8) {
                if (putmode) {
                    if (typeOfWorker.equals("H")) {
                        if (!this.gameBoard.checkActionIsEmpty(place)) {
                            this.gameResource[player].addMoney(-1);
                        }
                    }
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    if (typeOfWorker.equals("G")) {
                        this.gameResource[player].addMoney(-1);
                        this.gameResource[player].addScorePoint(2);
                    }
                    this.gameResource[player].addScorePoint(8);
                    this.gameResource[player].addAchievement();
                    this.gameResource[player].addReserchPoint(-8, 0);
                    if (typeOfWorker.equals("A")) {
                        this.gameResource[player].addMoney(-1);
                    }
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }

        if (place.equals("4-5")) {
            if (this.gameResource[player].getCurrentResrchPoint(1) >= 10) {
                if (putmode) {
                    if (typeOfWorker.equals("H")) {
                        if (!this.gameBoard.checkActionIsEmpty(place)) {
                            this.gameResource[player].addMoney(-1);
                        }
                    }
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    this.gameResource[player].addMoney(10);
                    this.gameResource[player].addReserchPoint(-10, 1);
                    this.gameResource[player].addReserchPoint(2, 1);
                    if (typeOfWorker.equals("E")) {
                        this.gameResource[player].addReserchPoint(3, 1);
                    }
                    if (typeOfWorker.equals("A")) {
                        this.gameResource[player].addMoney(-1);
                    }
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.gameResource[player].addAchievement();
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }

        if (place.equals("5-1")) {
            int cost = 2;
            if (typeOfWorker.equals("G")) {
                cost++;
            } else if (typeOfWorker.equals("M")) {
                cost--;
            } else if (typeOfWorker.equals("A")) {
                cost++;
            } else if (typeOfWorker.equals("H")) {
                if (!this.gameBoard.checkActionIsEmpty(place)) {
                    cost++;
//                    this.gameResource[player].addMoney(-1);
                }
            }
            if (this.gameResource[player].getCurrentResrchPoint(0) >= 5
                    && this.gameResource[player].getCurrentResrchPoint(1) >= 3
                    && this.gameResource[player].getCurrentMoney() >= cost) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    this.gameResource[player].addReserchPoint(-5, 0);
                    this.gameResource[player].addReserchPoint(-3, 1);
                    this.gameResource[player].addMoney(-1 * cost);
                    if (typeOfWorker.equals("G")) {
                        this.gameResource[player].addScorePoint(2);
                    }
                    this.gameResource[player].addScorePoint(10);
                    this.gameResource[player].addAchievement();
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }

        if (place.equals("5-2")) {
            int cost = 2;
            if (typeOfWorker.equals("G")) {
                cost++;
            } else if (typeOfWorker.equals("M")) {
                cost--;
            } else if (typeOfWorker.equals("A")) {
                cost++;
            } else if (typeOfWorker.equals("H")) {
                if (!this.gameBoard.checkActionIsEmpty(place)) {
                    cost++;
//                    this.gameResource[player].addMoney(-1);
                }
            }
            if (this.gameResource[player].getCurrentResrchPoint(0) >= 10
                    && this.gameResource[player].getCurrentMoney() >= cost) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    this.gameResource[player].addReserchPoint(-10, 0);
                    this.gameResource[player].addMoney(-1 * cost);
                    if (typeOfWorker.equals("G")) {
                        this.gameResource[player].addScorePoint(2);
                    }
                    this.gameResource[player].addScorePoint(12);
                    this.gameResource[player].addAchievement();
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }

        if (place.equals("5-3")) {
            int cost = 2;
            if (typeOfWorker.equals("G")) {
                cost++;
            } else if (typeOfWorker.equals("M")) {
                cost--;
            } else if (typeOfWorker.equals("A")) {
                cost++;
            } else if (typeOfWorker.equals("H")) {
                if (!this.gameBoard.checkActionIsEmpty(place)) {
                    cost++;
//                    this.gameResource[player].addMoney(-1);
                }
            }
            if (this.gameResource[player].getCurrentResrchPoint(1) >= 10
                    && this.gameResource[player].getCurrentMoney() >= cost) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    this.gameResource[player].addReserchPoint(-10, 1);
                    this.gameResource[player].addMoney(-1 * cost);
                    if (typeOfWorker.equals("G")) {
                        this.gameResource[player].addScorePoint(2);
                    }
                    this.gameResource[player].addScorePoint(12);
                    this.gameResource[player].addAchievement();
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }

        if (place.equals("5-4")) {
            int cost = 2;
            if (typeOfWorker.equals("G")) {
                cost++;
            } else if (typeOfWorker.equals("M")) {
                cost--;
            } else if (typeOfWorker.equals("A")) {
                cost++;
            } else if (typeOfWorker.equals("H")) {
                if (!this.gameBoard.checkActionIsEmpty(place)) {
                    cost++;
//                    this.gameResource[player].addMoney(-1);
                }
            }
            if (this.gameResource[player].getCurrentResrchPoint(0) >= 7
                    && this.gameResource[player].getCurrentResrchPoint(1) >= 9
                    && this.gameResource[player].getCurrentMoney() >= cost) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    this.gameResource[player].addReserchPoint(-7, 0);
                    this.gameResource[player].addReserchPoint(-9, 1);
                    this.gameResource[player].addMoney(-1 * cost);
                    if (typeOfWorker.equals("G")) {
                        this.gameResource[player].addScorePoint(2);
                    }
                    this.gameResource[player].addScorePoint(18);
                    this.gameResource[player].addAchievement();
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }

        if (place.equals("5-5")) {
            int cost = 2;
            if (typeOfWorker.equals("G")) {
                cost++;
            } else if (typeOfWorker.equals("M")) {
                cost--;
            } else if (typeOfWorker.equals("A")) {
                cost++;
            } else if (typeOfWorker.equals("H")) {
                if (!this.gameBoard.checkActionIsEmpty(place)) {
                    cost++;
//                    this.gameResource[player].addMoney(-1);
                }
            }
            if (this.gameResource[player].getCurrentResrchPoint(0) >= 15
                    && this.gameResource[player].getCurrentMoney() >= cost) {
                if (putmode) {
                    this.gameBoard.putWorker(player, place, typeOfWorker, option);
                    this.gameResource[player].addReserchPoint(-15, 0);
                    this.gameResource[player].addMoney(-1 * cost);
                    if (typeOfWorker.equals("G")) {
                        this.gameResource[player].addScorePoint(2);
                    }
                    this.gameResource[player].addScorePoint(20);
                    this.gameResource[player].addAchievement();
                    this.gameResource[player].putWorker(typeOfWorker);
                    this.changePlayer();
                    this.setChanged();
                    this.notifyObservers();
                }
                return true;
            }
            return false;
        }

        if (place.equals("6-1")) {
            if (putmode) {
                if (typeOfWorker.equals("H")) {
                    if (!this.gameBoard.checkActionIsEmpty(place)) {
                        this.gameResource[player].addMoney(-1);
                    }
                }
                this.gameBoard.putWorker(player, place, typeOfWorker, option);
                this.gameResource[player].addMoney(3);
                if (typeOfWorker.equals("A")) {
                    this.gameResource[player].addMoney(-1);
                }
                this.gameResource[player].putWorker(typeOfWorker);
                this.gameResource[player].setStartPlayer(true);
                this.gameResource[(player + 1) % 2].setStartPlayer(false);
                this.changePlayer();
                this.setChanged();
                this.notifyObservers();
            }
            return true;
        }

        if (place.equals("6-2")) {
            if (option.toUpperCase().equals("FF")) {
                if (this.gameResource[player].getCurrentResrchPoint(0) < 2) {
                    return false;
                }
                if (putmode) {
                    this.gameResource[player].addReserchPoint(-2, 0);
                }
            } else if (option.toUpperCase().equals("FG")) {
                if (this.gameResource[player].getCurrentResrchPoint(0) < 1) {
                    return false;
                }
                if (this.gameResource[player].getCurrentResrchPoint(1) < 1) {
                    return false;
                }
                if (putmode) {
                    this.gameResource[player].addReserchPoint(-1, 0);
                    this.gameResource[player].addReserchPoint(-1, 1);
                }
            } else if (option.toUpperCase().equals("GG")) {
                if (this.gameResource[player].getCurrentResrchPoint(1) < 2) {
                    return false;
                }
                if (putmode) {
                    this.gameResource[player].addReserchPoint(-2, 1);
                }
            } else {
                return false;
            }
            if (putmode) {
                if (typeOfWorker.equals("H")) {
                    if (!this.gameBoard.checkActionIsEmpty(place)) {
                        this.gameResource[player].addMoney(-1);
                    }
                }
                this.gameBoard.putWorker(player, place, typeOfWorker, option);
                this.gameResource[player].addMoney(5);
                if (typeOfWorker.equals("A")) {
                    this.gameResource[player].addMoney(-1);
                }
                this.gameResource[player].putWorker(typeOfWorker);
                this.changePlayer();
                this.setChanged();
                this.notifyObservers();
            }
            return true;
        }

        if (place.equals("6-3")) {
            if (this.gameResource[player].getCurrentResrchPoint(0) < 5) {
                return false;
            }
            if (putmode) {
                if (typeOfWorker.equals("H")) {
                    if (!this.gameBoard.checkActionIsEmpty(place)) {
                        this.gameResource[player].addMoney(-1);
                    }
                }
                this.gameBoard.putWorker(player, place, typeOfWorker, option);
                this.gameResource[player].addReserchPoint(-5, 0);
                this.gameResource[player].addMoney(10);
                if (typeOfWorker.equals("A")) {
                    this.gameResource[player].addMoney(-1);
                }
                this.gameResource[player].putWorker(typeOfWorker);
                this.changePlayer();
                this.setChanged();
                this.notifyObservers();
            }
            return true;
        }

        return true;
    }

    /**
     * タイムアウトが発生した場合
     */
    public void pass(int playerID) {
        /* 強制的にゼミに打たれる？ */
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
        // Tools | Templates.
        // this.setChanged();
        // this.notifyObservers();
    }

    /**
     * 通常の手番切換え
     */
    public void changePlayer() {
        // this.timerThread.StopTimeCount(this.CurrentPlayer);

        if (this.gameResource[(this.CurrentPlayer + 1) % 2].hasWorker()) {
            // 相手に手がうつせる場合
            this.CurrentPlayer = (this.CurrentPlayer + 1) % 2;
            // this..StartTimeCount(this.CurrentPlayer);
            this.setChanged();
            this.notifyObservers();
        } else {
            // 相手がもう手が打てない場合
            if (this.gameResource[this.CurrentPlayer].hasWorker()) {
                // 自分がまだ打てるんであれば、そのまま自分の手番で継続
                // this.timerThread.StartTimeCount(this.CurrentPlayer);
                this.setChanged();
                this.notifyObservers();
            } else {
                // 互いに手が打てないのであれば、季節を進める
                this.CurrentPlayer = -1;
                this.setChanged();
                this.notifyObservers();
                this.gameState = STATE_SEASON_END;
                // 表示のために待つならココ

                // 次のシーズンの準備
                // this.changeNewSeason();
            }
        }
    }

    /**
     * ボードそのもののメソッドを呼び出すための取得
     */
    public Board getBoard() {
        return this.gameBoard;
    }

    public int setPlayerName(String name) {
        if (this.gameState == STATE_WAIT_PLAYER_CONNECTION) {
            if (this.PlayerName[0] == null) {
                this.setPlayerName(0, name);
                return 0;
            } else if (this.PlayerName[1] == null) {
                this.setPlayerName(1, name);
                return 1;
            }
        }
        return -1;
    }

    private void setPlayerName(int player, String name) {
        if (player >= 0 && player < 2) {
            this.PlayerName[player] = name;
        }
        // if(this.PlayerName[0] != null && this.PlayerName[1] != null){
        // this.gameState = STATE_WAIT_PLAYER_PLAY;
        // }
        this.setChanged();
        this.notifyObservers();
    }

    public void setWorkers(int player, String worker1, String worker2) {
        boolean c1 = false;
        boolean c2 = false;
        for (String type : WORKER_ID) {
            if (type.equals(worker1)) {
                c1 = true;
            }
            if (type.equals(worker2)) {
                c2 = true;
            }
        }
        if (c1 && c2) {
            this.gameResource[player].setWorkerType(worker1, worker2);
        }
        // 両プレイヤーのワーカーが決定したら状態を変える
        if (this.gameResource[0].hasWorker() && this.gameResource[1].hasWorker()) {
            this.gameState = STATE_WAIT_PLAYER_PLAY;
        }

        this.setChanged();
        this.notifyObservers();
    }

    public GameResources getgetResources(int playerID) {
        return this.gameResource[playerID];
    }

    public ArrayList<String> getWorkerNameOf(String place) {
        return this.gameBoard.getWorkersOnBoard().get(place);
    }

    public String getSeason() {
        return Game.SEASON_NAMES[this.currentSeason];
    }

    /**
     * * 季節の進行
     */
    public void changeNewSeason() {
        if (this.gameState == STATE_SEASON_END) {
            HashMap<String, ArrayList<String>> workers = this.getBoard().getWorkersOnBoard();

            // スタートプレイヤーの決定
            if (this.gameResource[0].isStartPlayer()) {
                this.currentStartPlayer = 0;
            } else if (this.gameResource[1].isStartPlayer()) {
                this.currentStartPlayer = 1;
            }
            this.CurrentPlayer = this.currentStartPlayer;

            // ボードのコマを全部戻す
            this.gameResource[0].returnAllWorkers();
            this.gameResource[1].returnAllWorkers();
            this.getBoard().returnAllWorkers();

            if (this.currentSeason == 9) {
                // 最後の季節の終了
                // this.currentSeason++;
                this.gameState = STATE_GAME_END;
            } else if (this.currentSeason == 4) {
                // 表彰のある季節なので表彰する
                int[] players = {0, 1};
                int addmoney = 0;
                for (int player : players) {
                    if (this.gameResource[player].getAchievementCount() >= 5) {
                        addmoney = 10;

                    }
                    this.gameResource[player].addMoney(addmoney);
                    this.gameResource[player].addNewStudent();
                }
                this.currentSeason++;
                this.gameState = STATE_WAIT_PLAYER_PLAY;
            } else {
                // それ以外は単に季節を進める
                this.currentSeason++;
                this.gameState = STATE_WAIT_PLAYER_PLAY;
            }
        }
        this.setChanged();
        this.notifyObservers();
    }

    public int getStartPlayer() {
        if (this.gameResource[0].isStartPlayer()) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * * CUI出力用 現在のボードの状態（どこに誰のコマがおいてあるか）を文字列で出力
     *
     * @return
     */
    public String getBoardInformation() {
        if (this.gameState == STATE_WAIT_PLAYER_CONNECTION) {
            return "プレイヤー接続待ち";
        }

        StringBuilder sbuf = new StringBuilder();
        sbuf.append("/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_//_/_/_/_/_/_/_/\n");
        sbuf.append("/_/_/_/_/_/_/_/  ボードの状態  /_/_/_/_/_/_/_/\n");
        sbuf.append("/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_//_/_/_/_/_/_/_/\n");
        sbuf.append("1-1 ゼミの配置状況\n");
        sbuf.append(this.gameBoard.getWorkersOnBoard().get("1-1") + "\n");
        sbuf.append("2 図書館の配置状況\n");
        sbuf.append("2-1:" + this.gameBoard.getWorkersOnBoard().get("2-1") + "\n");
        sbuf.append("2-2:" + this.gameBoard.getWorkersOnBoard().get("2-2") + "\n");
        sbuf.append("3 実験の配置状況\n");
        sbuf.append("3-1:" + this.gameBoard.getWorkersOnBoard().get("3-1") + "\n");
        sbuf.append("3-2:" + this.gameBoard.getWorkersOnBoard().get("3-2") + "\n");
        sbuf.append("3-3:" + this.gameBoard.getWorkersOnBoard().get("3-3") + "\n");
        sbuf.append("3-4:" + this.gameBoard.getWorkersOnBoard().get("3-3") + "\n");
        sbuf.append("4 発表の配置状況\n");
        sbuf.append("4-1:" + this.gameBoard.getWorkersOnBoard().get("4-1") + "\n");
        sbuf.append("4-2:" + this.gameBoard.getWorkersOnBoard().get("4-2") + "\n");
        sbuf.append("4-3:" + this.gameBoard.getWorkersOnBoard().get("4-3") + "\n");
        sbuf.append("4-4:" + this.gameBoard.getWorkersOnBoard().get("4-4") + "\n");
        sbuf.append("4-5:" + this.gameBoard.getWorkersOnBoard().get("4-5") + "\n");
        sbuf.append("5 論文の配置状況\n");
        sbuf.append("5-1:" + this.gameBoard.getWorkersOnBoard().get("5-1") + "\n");
        sbuf.append("5-2:" + this.gameBoard.getWorkersOnBoard().get("5-2") + "\n");
        sbuf.append("5-3:" + this.gameBoard.getWorkersOnBoard().get("5-3") + "\n");
        sbuf.append("5-4:" + this.gameBoard.getWorkersOnBoard().get("5-4") + "\n");
        sbuf.append("5-5:" + this.gameBoard.getWorkersOnBoard().get("5-5") + "\n");
        sbuf.append("6 研究報告の配置状況\n");
        sbuf.append("6-1:" + this.gameBoard.getWorkersOnBoard().get("6-1") + "\n");
        sbuf.append("6-2:" + this.gameBoard.getWorkersOnBoard().get("6-2") + "\n");
        sbuf.append("6-3:" + this.gameBoard.getWorkersOnBoard().get("6-3") + "\n");
        sbuf.append("----------------------------------------------\n");
        sbuf.append("時間経過と研究成果\n");
        sbuf.append("現在の季節：" + this.getSeason() + "\n");
        // sbuf.append("現在のトレンド："+this.getTrend()+"\n");
        sbuf.append("スコア：Player0=" + this.gameResource[0].getTotalScore() + ",Player1="
                + this.gameResource[1].getTotalScore() + "\n");
        sbuf.append("----------------------------------------------\n");
        if (this.CurrentPlayer == -1) {
            sbuf.append("現在季節を進めています\n");
        } else {
            sbuf.append(
                    "現在プレイ待ちのプレイヤー：Player" + this.CurrentPlayer + "(" + this.PlayerName[this.CurrentPlayer] + ")\n");
        }
        sbuf.append("スタートプレイヤー：Player" + this.currentStartPlayer + "\n");
        sbuf.append("/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_//_/_/_/_/_/_/_/\n");
        return sbuf.toString();
    }

    /**
     * * CUI出力用 現在のリソース状態（各プレイヤーが持つリソース）を文字列で出力
     *
     * @return
     */
    public String getResourceInformation() {
        if (this.gameState == STATE_WAIT_PLAYER_CONNECTION) {
            return "プレイヤー接続待ち";
        }

        StringBuilder sbuf = new StringBuilder();
        sbuf.append("/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_//_/_/_/_/_/_/_/\n");
        sbuf.append("/_/_/_/_/_/_/_/  プレイヤーの状態  /_/_/_/_/_/_/_/\n");
        sbuf.append("/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_//_/_/_/_/_/_/_/\n");
        sbuf.append(this.getResourceInformation(0));
        sbuf.append(this.getResourceInformation(1));
        sbuf.append("/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_//_/_/_/_/_/_/_/\n");
        return sbuf.toString();
    }

    private String getResourceInformation(int playerID) {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("Player" + playerID + "(" + this.PlayerName[playerID] + ")\n");
        sbuf.append("----------------------------------------------\n");
        sbuf.append("1 コマの利用可能状況\n");
        for (int i = 0; i < WORKER_ID.length; i++) {
            String type = WORKER_ID[i];
            if (this.gameResource[playerID].hasWorkerOf(type)) {
                sbuf.append(WORKER_NAME[i] + "\n");
            }
        }
        sbuf.append("2 資金と研究ポイントの状況\n");
        sbuf.append("お金:" + this.gameResource[playerID].getCurrentMoney() + "\n");
        sbuf.append("研究ポイントF:" + this.gameResource[playerID].getCurrentResrchPoint(0) + "\n");
        sbuf.append("研究ポイントG:" + this.gameResource[playerID].getCurrentResrchPoint(1) + "\n");
        sbuf.append("3 総合得点:" + this.gameResource[playerID].getTotalScore() + "\n");
        sbuf.append("----------------------------------------------\n");
        return sbuf.toString();
    }

    public void printMessage(String text) {
        this.setChanged();
        this.notifyObservers(text);
    }

    public GameResources getResourcesOf(int i) {
        if (i == 0 || i == 1) {
            return this.gameResource[i];
        }
        return null;
    }

    public String getPlayerNameOf(int i) {
        if (i == 0 || i == 1) {
            return this.PlayerName[i];
        }
        return "";
    }

    /**
     * Gameオブジェクトを複製（ディープコピー）します。<br>
     * このメソッドを経由しないとシャローコピー＝ポインタの複製になるため注意してください。<br>
     * コピー元が意図しないタイミングで書き換わる可能性があります。
     *
     * @return 複製したGameオブジェクト
     */
    @Override
    public Game clone() {
        Game cloned = null;

        try {
            cloned = (Game) super.clone();
            cloned.gameBoard = this.gameBoard.clone();
            cloned.gameResource = new GameResources[this.gameResource.length];
            for (int i = 0; i < this.gameResource.length; i++) {
                cloned.gameResource[i] = this.gameResource[i].clone();
            }
            cloned.PlayerName = new String[this.PlayerName.length];
            System.arraycopy(this.PlayerName, 0, cloned.PlayerName, 0, this.PlayerName.length);
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
        }

        return cloned;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameElements;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kosen
 */
public class GameResources implements Cloneable {

    //以下は個別に持つ値
    //所持金
    private int money;
    //研究成果
    private int[] reserchPoint;
    //得点
    private int score;
    //発表数
    private int achievementCount;
    //使用前のコマ
    private ArrayList<String> workerList;
    //使用済みのコマ
    private ArrayList<String> usedWorkers;
    //スタートプレーヤか否か
    private boolean startPlayerFlag;

    public GameResources() {
        this.money = 5;
        this.reserchPoint = new int[2];
        this.reserchPoint[0] = 0;
        this.reserchPoint[1] = 0;
        this.achievementCount = 0;
        this.score = 0;
        this.workerList = new ArrayList<String>();
        this.usedWorkers = new ArrayList<String>();
        this.startPlayerFlag = false;
    }

    public void setWorkerType(String typeofWorker1, String typeofWorker2) {
        this.workerList.add(typeofWorker1);
        this.workerList.add(typeofWorker2);
        if (typeofWorker1.equals(Game.WORKER_ID[4]) || typeofWorker2.equals(Game.WORKER_ID[4])) {
            this.workerList.add(Game.WORKER_ID[8]);
        }
    }

    public boolean hasWorkerOf(String typeOfWorker) {
        return this.workerList.contains(typeOfWorker);
    }

    public ArrayList<String> getUnusedWorkers() {
        return this.workerList;
    }

    public int getCurrentMoney() {
        return this.money;
    }

    public void addMoney(int i) {
        this.money += i;
    }

    public void putWorker(String typeOfWorker) {
        if (this.workerList.contains(typeOfWorker)) {
            this.workerList.remove(typeOfWorker);
            //残りワーカー数が１になったときにリーダーを雇用しているとAがつかえるようになる
            if (this.workerList.size() == 1) {
                if (this.workerList.get(0).equals(Game.WORKER_ID[8])) {
                    this.workerList.remove(0);
                    if (this.money > 0) {
                        this.workerList.add(Game.WORKER_ID[7]);
                    } else {
                        this.usedWorkers.add(Game.WORKER_ID[8]);
                    }
                }
            }
            if (typeOfWorker.equals(Game.WORKER_ID[7])) {
                this.usedWorkers.add(Game.WORKER_ID[8]);
            } else {
                this.usedWorkers.add(typeOfWorker);
            }
        }
    }

    public int getCurrentResrchPoint(int type) {
        return this.reserchPoint[type];
    }

    public void addReserchPoint(int point, int type) {
        this.reserchPoint[type] += point;
        if (this.reserchPoint[type] > 15) {
            this.reserchPoint[type] = 15;
        }
    }

    public void addAchievement() {
        this.achievementCount++;
        if (this.achievementCount > 15) {
            this.achievementCount = 15;
        }
        return;
    }

    public boolean hasWorker() {
        return !this.workerList.isEmpty();
    }

    public int getTotalScore() {
        return this.score;
    }

    public int getAchievementCount() {
        return this.achievementCount;
    }

    public boolean isStartPlayer() {
        return this.startPlayerFlag;
    }

    public void addScorePoint(int point) {
        this.score += point;
    }

    public void returnAllWorkers() {
        for (String w : this.usedWorkers) {
            this.workerList.add(w);
        }
        this.usedWorkers.clear();
    }

    public void setStartPlayer(boolean b) {
        this.startPlayerFlag = b;
    }

    //シーズン5の終わりに学生コマが増える
    public void addNewStudent() {
        this.workerList.add(Game.WORKER_ID[6]);
    }

    // edit by Takumi Niwa
    // 各種セッター
    public void setAchievementCount(int achievementCount) {
        this.achievementCount = achievementCount;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public void setReserchPoint(int point, int type) {
        this.reserchPoint[type] = point;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setUsedWorkers(ArrayList<String> usedWorkers) {
        this.usedWorkers = usedWorkers;
    }

    public void setWorkerList(ArrayList<String> workerList) {
        this.workerList = workerList;
    }

    public void addUsedWorker(String worker) {
        this.usedWorkers.add(worker);
    }

    /**
     * GameResourcesオブジェクトを複製（ディープコピー）します。<br>
     * このメソッドを経由しないとシャローコピー＝ポインタの複製になるため注意してください。<br>
     * コピー元が意図しないタイミングで書き換わる可能性があります。
     *
     * @return 複製したGameResourcesオブジェクト
     */
    @Override
    public GameResources clone() {
        GameResources cloned = null;
        try {
            cloned = (GameResources) super.clone();
            cloned.reserchPoint = new int[this.reserchPoint.length];
            for (int i = 0; i < reserchPoint.length; i++) {
                int r = this.reserchPoint[i];
                cloned.reserchPoint[i] = r;
            }
            // 所持ワーカーの複製
            ArrayList<String> clonedWorkerList = new ArrayList<>();
            clonedWorkerList.addAll(this.workerList);
            cloned.workerList = clonedWorkerList;
            // 使用済みワーカーの複製
            ArrayList<String> clonedUsedWorkers = new ArrayList<>();
            clonedUsedWorkers.addAll(this.usedWorkers);
            cloned.usedWorkers = clonedUsedWorkers;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(GameResources.class.getName()).log(Level.SEVERE, null, ex);
        }
        return cloned;
    }

}

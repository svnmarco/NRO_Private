package com.girlkun.models.boss.list_boss.android;

import com.girlkun.models.boss.Boss;
import com.girlkun.models.boss.BossID;
import com.girlkun.models.boss.BossesData;
import com.girlkun.models.map.ItemMap;
import com.girlkun.models.player.Player;
import com.girlkun.services.Service;
import com.girlkun.services.TaskService;
import com.girlkun.utils.Util;


public class Android13 extends Boss {

    public Android13() throws Exception {
        super(BossID.ANDROID_13, BossesData.ANDROID_13);
    }

    @Override
    public void reward(Player plKill) {
        int[] itemRan = new int[]{382, 383, 384};
        int itemId = itemRan[2];
        if (Util.isTrue(15, 100)) {
            ItemMap it = new ItemMap(this.zone, itemId, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, it);
        }
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public void doneChatS() {
        if (this.getParentBoss() == null) {
            return;
        }
        if (this.getParentBoss().getBossAppearTogether() == null
                || this.getParentBoss().getBossAppearTogether()[getParentBoss().getCurrentLevel()] == null) {
            return;
        }
        for (Boss boss : this.getParentBoss().getBossAppearTogether()[getParentBoss().getCurrentLevel()]) {
            if (boss.id == BossID.ANDROID_15 && !boss.isDie()) {
                boss.changeToTypePK();
                break;
            }
        }
        this.getParentBoss().changeToTypePK();
    }

    @Override
    public int injured(Player plAtt, int damage, boolean piercing, boolean isMobAttack) {
        if (damage >= this.nPoint.hp) {
            boolean flag = true;
            if (this.getParentBoss() != null) {
                if (this.getParentBoss().getBossAppearTogether() != null && getParentBoss().getBossAppearTogether()[this.getParentBoss().getCurrentLevel()] != null) {
                    for (Boss boss : this.getParentBoss().getBossAppearTogether()[this.getParentBoss().getCurrentLevel()]) {
                        if (boss.id == BossID.ANDROID_15 && !boss.isDie()) {
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag && !this.getParentBoss().isDie()) {
                    flag = false;
                }
            }
            if (!flag) {
                return 0;
            }
        }
        return super.injured(plAtt, damage, piercing, isMobAttack);
    }
}


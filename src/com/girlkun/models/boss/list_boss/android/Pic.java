package com.girlkun.models.boss.list_boss.android;

import com.girlkun.consts.ConstPlayer;
import com.girlkun.models.boss.Boss;
import com.girlkun.models.boss.BossData;
import com.girlkun.models.boss.BossID;
import com.girlkun.models.boss.BossManager;
import com.girlkun.models.boss.BossesData;
import com.girlkun.models.boss.SmallBoss;
import com.girlkun.models.map.ItemMap;
import com.girlkun.models.map.Zone;
import com.girlkun.models.player.Player;
import com.girlkun.services.EffectSkillService;
import com.girlkun.services.Service;
import com.girlkun.services.TaskService;
import com.girlkun.services.func.ChangeMapService;
import com.girlkun.utils.Util;

public class Pic extends SmallBoss {

    protected boolean isReady;
    private short x;
    private short y;

    public Pic() throws Exception {
        super(BossID.PIC, BossesData.PIC);
    }

    public Pic(Boss bigBoss, Zone zone, short x, short y, BossData data) throws Exception {
        super(BossID.PIC, bigBoss, data);
        this.isReady = false;
        this.zone = zone;
        this.x = x;
        this.y = y;
    }

    @Override
    public void joinMap() {
        if (this.bigBoss == null) {
            super.joinMap();
        } else {
            ChangeMapService.gI().changeMap(this, this.zone, x + Util.getOne(-1, 1) * 50, y);
        }
    }

    @Override
    public void reward(Player plKill) {
        int[] itemRan = new int[]{381, 382, 383, 384, 385};
        int itemId = itemRan[2];
        if (Util.isTrue(15, 100)) {
            ItemMap it = new ItemMap(this.zone, itemId, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, it);
        }
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public void doneChatE() {
        if (this.getParentBoss() == null || this.getParentBoss().getBossAppearTogether() == null
                || this.getParentBoss().getBossAppearTogether()[this.getParentBoss().getCurrentLevel()] == null) {
            this.changeToTypePK();
        } else {
            for (Boss boss : this.getParentBoss().getBossAppearTogether()[this.getParentBoss().getCurrentLevel()]) {
                if (boss.id == BossID.POC && !boss.isDie()) {
                    boss.changeToTypePK();
                    break;
                }
            }
        }
    }

    @Override
    public int injured(Player plAtt, int damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            if (damage >= this.nPoint.hp && this.bigBoss != null && !this.isReady) {
                this.changeToTypeNonPK();
                this.isReady = true;
                this.nPoint.hp = 1;
                this.effectSkill.removeSkillEffectWhenDie();
                if (((SuperAndroid17) this.bigBoss).isReady) {
                    ((SuperAndroid17) this.bigBoss).lastTimeFusion = System.currentTimeMillis();
                    ((SuperAndroid17) this.bigBoss).lastTimecanAttack = System.currentTimeMillis();
                }
                return 0;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return damage;
        } else {
            return 0;
        }
    }

    @Override
    public void active() {
        if (this.bigBoss == null) {
            if (this.typePk == ConstPlayer.NON_PK) {
                return;
            }
            this.attack();
        } else {
            if (this.bigBoss != null && this.bigBoss.typePk == ConstPlayer.PK_ALL && !this.isReady) {
                this.changeToTypePK();
            }
            this.attack();
        }
    }

    @Override
    public void wakeupAnotherBossWhenDisappear() {
        Boss boss = this.getParentBoss();
        if (boss != null && !boss.isDie()) {
            boss.changeToTypePK();
        }
    }

    @Override
    public void leaveMap() {
        if (this.bigBoss == null) {
            super.leaveMap();
        } else {
            synchronized (this) {
                BossManager.gI().removeBoss(this);
            }
            this.dispose();
        }
    }

}

/**
 * Vui lòng không sao chép mã nguồn này dưới mọi hình thức. Hãy tôn trọng tác
 * giả của mã nguồn này. Xin cảm ơn! - GirlBeo
 */

package ManaOnly;

import battlecode.common.*;

public class SymmetryRecognizer {
    boolean rotational = true;
    boolean vertical = true;
    boolean horizontal = true;
    MapComms mapComms;
    int widthm1, heightm1;

    public SymmetryRecognizer(MapComms mp) {
        mapComms = mp;
        widthm1 = Global.mapWidth-1;
        heightm1 = Global.mapHeight-1;
    }

    public void hqTest() {
        int enemyCount = mapComms.enemyHQCount;
        MapLocation[] enemyHqs = mapComms.enemyHQList;
        //Global.indicatorString += "enemyCount"+enemyCount;
        int allyCount = mapComms.alliedHQCount;
        MapLocation[] alliedHqs = mapComms.alliedHQList;
        boolean[][] explored = mapComms.explored;
        for (int i = allyCount; i-- > 0;) {
            MapLocation myMl = alliedHqs[i];
            boolean rot = false, ver = false, hor = false;
            MapLocation r = getRotational(myMl);
            MapLocation v = getVertical(myMl);
            MapLocation h = getHorizontal(myMl);
            //Global.rc.setIndicatorLine(myMl, r, 0, 255, 0);
            //Global.rc.setIndicatorLine(myMl, v, 0, 255, 0);
            //Global.rc.setIndicatorLine(myMl, h, 0, 255, 0);
            for (int j = enemyCount; j-- > 0;) {
                MapLocation enemyMl = enemyHqs[j];
                //Global.rc.setIndicatorDot(enemyMl, 0, 200, 0);
                if (enemyMl.equals(r))
                    rot = true;
                if (enemyMl.equals(v))
                    ver = true;
                if (enemyMl.equals(h))
                    hor = true;
            }
            if (explored[r.x][r.y] && !rot) {
                //Global.rc.setIndicatorLine(myMl, r, 255, 0, 0);
                rotational = false;
            }
            if (explored[v.x][v.y] && !ver) {
                //Global.rc.setIndicatorLine(myMl, v, 255, 0, 0);
                vertical = false;
            }
            if (explored[h.x][h.y] && !hor) {
                //Global.rc.setIndicatorLine(myMl, h, 255, 0, 0);
                horizontal = false;
            }
        }
    }

    public MapLocation[] possibleEnemyHqs() {
        //Global.indicatorString += "width" + Global.mapWidth;
        //Global.indicatorString += "HOR:" + horizontal;
        //Global.indicatorString += "VER:" + vertical;
        //Global.indicatorString += "ROT:" + rotational;
        int allyCount = mapComms.alliedHQCount;
        MapLocation[] alliedHqs = mapComms.alliedHQList;
        int siz = (horizontal ? 1 : 0) + (rotational ? 1 : 0) + (vertical ? 1 : 0);
        MapLocation[] ans = new MapLocation[allyCount*siz];
        int idx = 0;
        if (horizontal) {
            for (int i = allyCount; i-- > 0;){
                ans[idx++] = getHorizontal(alliedHqs[i]);
            }
        }
        if (vertical) {
            for (int i = allyCount; i-- > 0;){
                ans[idx++] = getVertical(alliedHqs[i]);
            }
        }
        if (rotational) {
            for (int i = allyCount; i-- > 0;){
                ans[idx++] = getRotational(alliedHqs[i]);
            }
        }
        return ans;
    }

    public MapLocation getRotational(MapLocation ml) {
        return new MapLocation(widthm1-ml.x, heightm1-ml.y);
    }

    public MapLocation getHorizontal(MapLocation ml) {
        return new MapLocation(ml.x, heightm1-ml.y);
    }

    public MapLocation getVertical(MapLocation ml) {
        return new MapLocation(widthm1-ml.x, ml.y);
    }
}

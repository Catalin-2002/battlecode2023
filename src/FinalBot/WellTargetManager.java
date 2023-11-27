package FinalBot;

import battlecode.common.*;

public class WellTargetManager {
    int timeLimit;
    MapLocation currentTarget = null;
    int[][] bannedTargets;
    boolean onlyMana;

    public WellTargetManager() {
        bannedTargets = new int[Global.mapWidth][Global.mapHeight];
        onlyMana = Global.rng.nextDouble() <= Parameters.MANA_PROPORTION;
    }

    public MapLocation getTarget() {
        if (currentTarget != null)
            recognizeBan();
        if (currentTarget == null)
            resetTarget();
        return currentTarget;
    }

    private void assignTarget(MapLocation ml) {
        currentTarget = ml;
        timeLimit = Global.rc.getRoundNum() + Parameters.TURNS_FOR_IMPOSSIBLE_WELL;
    }

    private void recognizeBan() {
        int dist = Global.myLocation.distanceSquaredTo(currentTarget);
        if (dist <= 2) {
            timeLimit = Global.rc.getRoundNum() + Parameters.TURNS_FOR_IMPOSSIBLE_WELL;
        }
        else if (dist <= 9) {
            timeLimit = Math.min(Global.rc.getRoundNum() + Parameters.TURNS_FOR_BUSY_WELL, timeLimit);
        }
        if (timeLimit <= Global.rc.getRoundNum()) {
            bannedTargets[currentTarget.x][currentTarget.y] = Global.rc.getRoundNum() + Parameters.TURNS_TO_ALLOW_WELL;
            currentTarget = null;
        }
    }

    private void resetTarget() {
        WellInfo[] wellInfos = Global.sensedWells;
        Global.indicatorString += "wells:" + wellInfos.length;
        for (int i = wellInfos.length; i-->0;) {
            if (onlyMana && wellInfos[i].getResourceType() != ResourceType.MANA)
                continue;
            if (!onlyMana && wellInfos[i].getResourceType() != ResourceType.ADAMANTIUM)
                continue;
            MapLocation ml = wellInfos[i].getMapLocation();
            if (bannedTargets[ml.x][ml.y] <= Global.rc.getRoundNum()) {
                assignTarget(ml);
                return;
            }
        }
        MapLocation[] adaCandidates = Robot.wellsComms.adaWells;
        MapLocation[] manaCandidates = Robot.wellsComms.manaWells;
        int hqCount = Carrier.mapComms.alliedHQCount;
        MapLocation[] hqs = Carrier.mapComms.alliedHQList;
        MapLocation best = null;
        int bestDist = 1000000000;
        if (onlyMana) {
            for (int i = Robot.wellsComms.manaCount; i-- > 0; ) {
                MapLocation ml = manaCandidates[i];
                if (bannedTargets[ml.x][ml.y] > Global.rc.getRoundNum())
                    continue;
                int dist = 1000000;
                for (int j = hqCount; j-- > 0; ) {
                    dist = Math.min(hqs[j].distanceSquaredTo(ml), dist);
                }
                if (bestDist > dist) {
                    bestDist = dist;
                    best = ml;
                }
            }
        }
        else {
            for (int i = Robot.wellsComms.adaCount; i-- > 0; ) {
                MapLocation ml = adaCandidates[i];
                if (bannedTargets[ml.x][ml.y] > Global.rc.getRoundNum())
                    continue;
                int dist = 1000000;
                for (int j = hqCount; j-- > 0; ) {
                    dist = Math.min(hqs[j].distanceSquaredTo(ml), dist);
                }
                if (bestDist > dist) {
                    bestDist = dist;
                    best = ml;
                }
            }
        }
        if (best != null) {
            assignTarget(best);
        }
    }
}

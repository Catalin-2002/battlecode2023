package BetterLaunchers;

import battlecode.common.*;


public class AttackTargetManager {

    SymmetryRecognizer symmetry;
    MapLocation target;
    int arrivalRound = 1000000;
    int bannedCount = 0;
    MapLocation[] banned = new MapLocation[100];

    public AttackTargetManager(SymmetryRecognizer symmetry) throws GameActionException{
        this.symmetry = symmetry;
        RobotInfo[] ris = Global.rc.senseNearbyRobots(20, Global.allies);
        for (int i = ris.length; i-->0;){
            if (ris[i].getType() == RobotType.HEADQUARTERS){
                target = symmetry.getRotational(ris[i].getLocation());
                break;
            }
        }
    }

    public void checkValidity() throws GameActionException{
        if (target == null)
            return;
        if (Global.rc.canSenseLocation(target)) {
            arrivalRound = Math.min(arrivalRound, Global.rc.getRoundNum());
            RobotInfo ri = Global.rc.senseRobotAtLocation(target);
            if (ri == null || ri.getType() != RobotType.HEADQUARTERS || ri.getTeam() != Global.enemies) {
                target = null;
                return;
            }
        }
        if (arrivalRound <= Global.rc.getRoundNum() + Parameters.TIME_UNTIL_DEFEATED) {
            banned[bannedCount++] = target;
            target = null;
        }
    }

    public void resetTarget() {
        symmetry.hqTest();
        arrivalRound = 10000000;
        MapLocation[] possibleEnemyHqs = symmetry.possibleEnemyHqs();
        int bestD = 1000000;
        for (int i = possibleEnemyHqs.length; i-- > 0;) {
            MapLocation ml = possibleEnemyHqs[i];
            boolean ban = false;
            for (int j = bannedCount; j-- > 0;) {
                if (banned[j].equals(ml)) {
                    ban = true;
                    break;
                }
            }
            if (ban)
                continue;
            int d = Global.myLocation.distanceSquaredTo(ml);
            if (bestD > d) {
                target = ml;
                bestD = d;
            }
        }
    }

    public MapLocation getTarget() throws GameActionException {
        if (target != null)
            checkValidity();
        if (target == null)
            resetTarget();
        if (target == null){
            bannedCount = 0;
        }
        if (target != null)
            Global.rc.setIndicatorLine(Global.myLocation, target, 0, 0, 0);
        return target;
    }
}

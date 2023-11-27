package FinalBot;

import battlecode.common.*;

public class CombatMicro {
    boolean[] canMove = new boolean[Direction.DIRECTION_ORDER.length];

    private void computeCanMove() {
        for (int i = canMove.length; i-->0;) {
            Direction dir = Direction.DIRECTION_ORDER[i];
            canMove[i] = true;
            if (!Global.rc.canMove(dir)) {
                canMove[i] = false;
            }
        }
        canMove[Direction.CENTER.getDirectionOrderNum()] = true;
    }

    public Direction doMicro() throws GameActionException{
        computeCanMove();
        RobotInfo[] robots = Global.sensedRobots;
        Team enemyTeam = Global.enemies;
        boolean enemies = false;
        int alliedCount = 1;
        int dangerCount = 0;
        for (int j = robots.length; j-- > 0;) {
            RobotInfo ri = robots[j];
            if (ri.getTeam() == enemyTeam) {
                if (ri.getType() == RobotType.LAUNCHER || ri.getType() == RobotType.DESTABILIZER){
                    dangerCount++;
                }
                enemies = true;
            }
            else {
                if (ri.getType() == RobotType.LAUNCHER){
                    alliedCount++;
                }
            }
        }
        if (!enemies)
            return null;
        boolean engage = (alliedCount > dangerCount);

        int[] canDealDamage = new int[canMove.length];
        int[] seenByDanger = new int[canMove.length];
        int[] inRangeOfDanger = new int[canMove.length];
        boolean[] campingHq = new boolean[canMove.length];
        int[] obstruction = new int[canMove.length];
        for (int i = canMove.length; i-- > 0;) {
            if (!canMove[i])
                continue;
            Direction dir = Direction.DIRECTION_ORDER[i];
            MapLocation loc = Global.myLocation.add(dir);
            boolean cloud = Global.rc.senseMapInfo(loc).hasCloud();
            for (int j = robots.length; j-- > 0;) {
                RobotInfo ri = robots[j];
                int dist = ri.getLocation().distanceSquaredTo(loc);
                if (ri.getTeam() == enemyTeam) { // Enemies
                    if (ri.getType() == RobotType.LAUNCHER || ri.getType() == RobotType.DESTABILIZER) { // Dangerous
                        if (dist <= 16)
                            canDealDamage[i] |= 3;
                        if ((dist <= 16 && !cloud) || dist <= 4){
                            inRangeOfDanger[i]++;
                        }
                        if ((dist <= 20 && !cloud) || dist <= 4){
                            seenByDanger[i]++;
                        }
                    }
                    else if (ri.getType() == RobotType.HEADQUARTERS) { // Enemy HQ
                        if (dist <= 9){
                            Global.indicatorString += "NO" + dir.toString();
                            canMove[i] = false;
                            break;
                        }
                        if (dist <= 16) {
                            campingHq[i] = true;
                        }
                    }
                    else { // Enemy defenseless
                        if (dist <= 16){
                            canDealDamage[i] |= 1;
                        }
                    }
                }
                else {
                    if (ri.getType() == RobotType.LAUNCHER) { // Fellow combatant
                        if (dist <= 4) {
                            obstruction[i]++;
                        }
                    }
                }
            }
        }

        double[] score = new double[canMove.length];
        if (engage && dangerCount > 0) {
            Global.indicatorString += "engageDanger";
            for (int i = canMove.length; i-- > 0;) {
                if (!canMove[i])
                    continue;
                score[i] = 1000;
                if (Global.rc.isActionReady())
                    score[i] += 10*canDealDamage[i];
                score[i] -= seenByDanger[i];
                score[i] -= inRangeOfDanger[i];
                score[i] -= obstruction[i];
            }
        }
        else if (engage) {
            Global.indicatorString += "engagePassive";
            for (int i = canMove.length; i-- > 0;) {
                if (!canMove[i])
                    continue;
                score[i] = 1000;
                score[i] += 4*canDealDamage[i];
                if (campingHq[i]) {
                    score[i]++;
                }
                score[i] -= 0.1*obstruction[i];
            }
        }
        else {
            Global.indicatorString += "retreatDanger";
            for (int i = canMove.length; i-- > 0;) {
                if (!canMove[i])
                    continue;
                score[i] = 1000;
                score[i] += canDealDamage[i];
                score[i] -= 10*seenByDanger[i];
                score[i] -= 5*inRangeOfDanger[i];
                score[i] -= obstruction[i];
            }
        }
        Direction best = Direction.CENTER;
        double bestScore = 0;
        for (int i = canMove.length; i-- > 0;) {
            //Global.indicatorString += "" + score[i] + ",";
            if (score[i] > bestScore) {
                bestScore = score[i];
                best = Direction.DIRECTION_ORDER[i];
            }
        }
        Global.indicatorString += best.toString();
        return best;
    }
}

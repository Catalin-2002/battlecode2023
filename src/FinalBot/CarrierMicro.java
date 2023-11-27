package FinalBot;

import battlecode.common.*;

public class CarrierMicro {
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

    public Direction doMicro() throws GameActionException {
        computeCanMove();
        RobotInfo[] robots = Global.rc.senseNearbyRobots(RobotType.CARRIER.visionRadiusSquared, Global.enemies);
        int dangerCount = 0;
        for (int j = robots.length; j-- > 0;) {
            RobotInfo ri = robots[j];
            if (ri.getType() == RobotType.LAUNCHER || ri.getType() == RobotType.DESTABILIZER){
                dangerCount++;
            }
        }
        if (dangerCount == 0)
            return null;

        int[] seenByDanger = new int[canMove.length];
        int[] inRangeOfDanger = new int[canMove.length];
        for (int i = canMove.length; i-- > 0;) {
            if (!canMove[i])
                continue;
            Direction dir = Direction.DIRECTION_ORDER[i];
            MapLocation loc = Global.myLocation.add(dir);
            for (int j = robots.length; j-- > 0;) {
                RobotInfo ri = robots[j];
                int dist = ri.getLocation().distanceSquaredTo(loc);
                if (ri.getType() == RobotType.LAUNCHER || ri.getType() == RobotType.DESTABILIZER) { // Dangerous
                    if (dist <= 16){
                        inRangeOfDanger[i]++;
                    }
                    if (dist <= 20) {
                        seenByDanger[i]++;
                    }
                }
                else if (ri.getType() == RobotType.HEADQUARTERS) { // Enemy HQ
                    if (dist <= 9){
                        canMove[i] = false;
                        break;
                    }
                }
            }
        }

        double[] score = new double[canMove.length];
        Global.indicatorString += "runFromDanger";
        for (int i = canMove.length; i-- > 0;) {
            if (!canMove[i])
                continue;
            score[i] = 1000;
            score[i] -= 10*seenByDanger[i];
            score[i] -= 5*inRangeOfDanger[i];
        }
        Direction best = Direction.CENTER;
        double bestScore = 0;
        for (int i = canMove.length; i-- > 0;) {
            Global.indicatorString += "" + score[i] + ",";
            if (score[i] > bestScore) {
                bestScore = score[i];
                best = Direction.DIRECTION_ORDER[i];
            }
        }
        return best;
    }
}

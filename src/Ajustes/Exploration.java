package Ajustes;

import battlecode.common.*;

public class Exploration {
    MapLocation explorationTarget;
    int timeLimit;
    MapLocation initialHq;

    public Exploration() throws GameActionException{
        RobotInfo[] ris = Global.rc.senseNearbyRobots(20, Global.allies);
        for (int i = ris.length; i-->0;){
            if (ris[i].getType() == RobotType.HEADQUARTERS){
                initialHq = ris[i].getLocation();
                break;
            }
        }
        ResetExplorationTarget();

    }

    public void ResetExplorationTarget() {
        if (Global.rc.getRoundNum() < 100 && initialHq != null) {
            do {
                explorationTarget = new MapLocation(Global.rng.nextInt(Global.rc.getMapWidth()), Global.rng.nextInt(Global.rc.getMapHeight()));
            } while(explorationTarget.distanceSquaredTo(initialHq) >= 100);
        }
        else
            explorationTarget = new MapLocation(Global.rng.nextInt(Global.rc.getMapWidth()), Global.rng.nextInt(Global.rc.getMapHeight()));
        timeLimit = Global.rc.getRoundNum() + Parameters.EXPLORATION_ROUND_LIMIT;
    }

    public MapLocation getTarget() {
        while (Global.rc.canSenseLocation(explorationTarget) || Global.rc.getRoundNum() > timeLimit) {
            ResetExplorationTarget();
        }
        return explorationTarget;
    }
}

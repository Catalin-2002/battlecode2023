package ManaOnly;

import battlecode.common.*;

public class Exploration {
    MapLocation explorationTarget;
    int timeLimit;

    public Exploration() {
        ResetExplorationTarget();
    }

    public void ResetExplorationTarget() {
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

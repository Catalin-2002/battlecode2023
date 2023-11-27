package BasicBot;
import battlecode.common.*;

public class Headquarter extends Robot {
    public static final MapLocation[] BUILDABLE_LOCATIONS = new MapLocation[28];
    @Override
    void Awake() throws GameActionException {
        super.Awake();
        MapLocation myLocation = rc.getLocation();
        for (int i = -2; i < 3; i++) {
            for (int j = -2; j < 3; j++) {
                BUILDABLE_LOCATIONS[5*i+j+12] = myLocation.translate(i, j);
            }
        }
        BUILDABLE_LOCATIONS[12] = myLocation.translate(3,0);
        BUILDABLE_LOCATIONS[25] = myLocation.translate(-3, 0);
        BUILDABLE_LOCATIONS[26] = myLocation.translate(0, 3);
        BUILDABLE_LOCATIONS[27] = myLocation.translate(0, -3);

        isFirstHQ = true; // TODO: register the HQ using comms & check that it actually is the first HQ
    }

    boolean builtCarrier = false, builtLauncher = false;

    @Override
    void Update() throws GameActionException {
        super.Update();
        builtCarrier = tryBuildRobot(RobotType.CARRIER);
        builtLauncher = tryBuildRobot(RobotType.LAUNCHER);
    }

    boolean tryBuildRobot(RobotType rt, MapLocation loc) throws GameActionException {
        if (rc.canBuildRobot(rt, loc)) {
            rc.buildRobot(rt, loc);
            return true;
        }
        return false;
    }

    boolean canBuildRobot(RobotType rt) {
        if (rc.isActionReady()) {
            for (ResourceType resType : RESOURCES) {
                if (rt.getBuildCost(resType) > rc.getResourceAmount(resType)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    boolean tryBuildRobot(RobotType rt) throws GameActionException{
        if (canBuildRobot(rt)) {
            for (int i = 27; i >= 0; i--) {
                MapLocation ml = BUILDABLE_LOCATIONS[i];
                if (tryBuildRobot(rt, ml)) {
                    return true;
                }
            }
        }
        return false;
    }
}

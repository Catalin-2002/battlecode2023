package BasicBot;
import battlecode.common.*;
import java.util.*;

public class Robot {
    public static final ResourceType[] RESOURCES = {ResourceType.ADAMANTIUM, ResourceType.MANA, ResourceType.ELIXIR};
    public static Random rng;
    public RobotController rc;
    Pathfinding pathfinding;
    Comms comms;
    // used in comm to update the tiles. If this is true, this unit MUST call comms.updateTiles() or the comm breaks.
    boolean isFirstHQ = false;

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    void Awake() throws GameActionException {
        rng = new Random(6147 + rc.getID());
        pathfinding = new Pathfinding(rc);
        comms = new Comms(rc);
    }

    void Update() throws GameActionException {

    }
    
    void run(RobotController rc) throws GameActionException {
        this.rc = rc;
        Awake();
        while(true) {
            Update();
            Clock.yield();
        }
    }

    MapLocation randomExploreTarget;
    int randomExploreStartRound = -10000;

    void randomExplore() throws GameActionException {
        while (randomExploreStartRound < rc.getRoundNum() - 150 || rc.getLocation().distanceSquaredTo(randomExploreTarget) <= 2){
            randomExploreStartRound = rc.getRoundNum();
            randomExploreTarget = new MapLocation(rng.nextInt(rc.getMapWidth()), rng.nextInt(rc.getMapHeight()));
        }
        while (pathfinding.Pathfind(randomExploreTarget)){}
    }
}

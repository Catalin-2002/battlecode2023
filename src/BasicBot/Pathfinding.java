package BasicBot;
import java.util.*;
import battlecode.common.*;


public class Pathfinding {
    RobotController rc;
    MapLocation location; //Current location

    boolean[] canMove = new boolean[Direction.DIRECTION_ORDER.length]; //Whether we can move in a direction, to avoid recomputation
    final Direction[] directions = Direction.DIRECTION_ORDER; //All directions

    final int BANSIZE = 20;
    int bannedIdx = 0;
    MapLocation[] banned = new MapLocation[BANSIZE]; //List of banned locations, to avoid getting stuck on currents

    int minDistToObj = 1000000; //Min distance to target during the travel

    MapLocation lastObj = new MapLocation(1,1);
    MapLocation lastObs = null; // Obstacle we are surrounding

    MapLocation lastLocation = new MapLocation(-10, -10); //Last location we were at

    boolean rotateRPath; // Surround obstacles to the right or to the left

    public Pathfinding(RobotController rc){
        this.rc = rc;
        rotateRPath = rc.getID() % 2 == 0; // half the robots will turn one way, and half will turn the other way, so that they don't get stuck as often
        location = rc.getLocation();
        Arrays.fill(banned, new MapLocation(-1, -1));
    }

    //Resets pathfinding, needed for switching targets and every so often to get unstuck.
    public void HardReset(MapLocation newObj) throws GameActionException {
        minDistToObj = 1000000;
        Arrays.fill(banned, new MapLocation(-1, -1));
        lastObs = null;
        lastObj = newObj;
        lastLocation = location;
    }

    // Moves with the goal of getting to the target. Returns whether it should be
    // called again.
    // Keep calling until it returns false for best results, it doesn't do that by
    // default because of possible emergencies.
    public boolean Pathfind(MapLocation target) throws GameActionException {
        location = rc.getLocation();
        if (!target.equals(lastObj) || !lastLocation.equals(location)) {
            HardReset(target);
        }
        if (!rc.isMovementReady()) {
            EndTurn();
            return false;
        }
        int dist = location.distanceSquaredTo(target);
        if (dist < minDistToObj) {
            minDistToObj = dist;
            lastObs = null;
        }
        fillCanMove();
        boolean hasMoved = false;
        if (lastObs == null) {
            if (!greedyMove(target)) {
                lastObs = location.add(location.directionTo(target));
            }
            else {
                hasMoved = true;
            }
        }
        if (lastObs != null) {
            hasMoved = surroundObstacle();
        }
        if (!rc.isMovementReady() || !hasMoved) {
            EndTurn();
            return false;
        }
        lastLocation = location;
        return true;
    }

    //To be called at the end of every turn when pathfinding, even if we couldnt move.
    private void EndTurn() throws GameActionException {
        location = rc.getLocation();
        Direction current = rc.senseMapInfo(location).getCurrentDirection();
        MapLocation loc = location.add(current);
        if (rc.isLocationOccupied(loc))
            current = Direction.CENTER;
        if (current != Direction.CENTER) {
            banned[(bannedIdx = (bannedIdx+1)%BANSIZE)] = location;
            if (lastObs != null)
                lastObs = location;
        }
        lastLocation = location.add(current);
    }

    //Computes the canMove array
    private void fillCanMove() throws GameActionException{
        //boolean lastMove = (rc.getMovementCooldownTurns() + rc.getType().movementCooldown) / GameConstants.COOLDOWN_LIMIT > 0;
        for (int i = canMove.length; i-- > 0;) {
            Direction dir = directions[i];
            if (rc.canMove(dir)) { //Check if we can possibly move
                canMove[i] = true;
                MapLocation loc = location.add(dir);
                for (int j = BANSIZE; j-->0;) { // Check if the location is banned
                    //rc.setIndicatorDot(banned[j], 0, 0, 255);
                    if (loc.equals(banned[j])){
                        canMove[i] = false;
                        break;
                    }
                }
                // Check if it is pointless (would be undone by current)
                /*
                if (lastMove && rc.senseMapInfo(loc).getCurrentDirection() == dir.opposite()) {
                    canMove[i] = false;
                }*/
            }
            else
                canMove[i] = false;
            //if (canMove[i])
                //rc.setIndicatorLine(location, location.add(dir), 0, 255, 0);
            //else
                //rc.setIndicatorLine(location, location.add(dir), 255, 0 , 0);
        }
    }

    //Tries to greedily get closer to the target, returs false if impossible
    private boolean greedyMove(MapLocation target) throws GameActionException{
        int bestDist = location.distanceSquaredTo(target);
        Direction bestDir = Direction.CENTER;
        for (int i = canMove.length; i-- > 0;) {
            if (canMove[i]) {
                Direction dir = directions[i];
                int dist = location.add(dir).distanceSquaredTo(target);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != Direction.CENTER) {
            rc.move(bestDir);
            location = rc.getLocation();
            return true;
        }
        return false;
    }

    // Moves around an obstacle, lastObs must be assigned
    private boolean surroundObstacle() throws GameActionException{
        Direction dir = location.directionTo(lastObs);
        //rc.setIndicatorDot(lastObs, 0, 0, 0);
        if (rotateRPath)
            dir = dir.rotateRight();
        else
            dir = dir.rotateLeft();
        for (int i = 7; i-- > 0;) {
            if (canMove[dir.getDirectionOrderNum()]) {
                //rc.setIndicatorDot(location.add(dir), 0, 255, 0);
                rc.move(dir);
                location = rc.getLocation();
                return true;
            }
            else {
                //rc.setIndicatorDot(location.add(dir), 255, 0, 0);
                lastObs = location.add(dir);
                if (!rc.onTheMap(lastObs)) {
                    rotateRPath = !rotateRPath;
                    HardReset(lastObj);
                    if (rotateRPath)
                        dir = dir.rotateRight();
                    else
                        dir = dir.rotateLeft();
                    i = 5;
                    continue;
                }
                if (rotateRPath)
                    dir = dir.rotateRight();
                else
                    dir = dir.rotateLeft();
            }
        }
        return false;
    }
}

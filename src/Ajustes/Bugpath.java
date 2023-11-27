package Ajustes;
import java.util.*;
import battlecode.common.*;


public class Bugpath {
    boolean[] canMove = new boolean[Direction.DIRECTION_ORDER.length]; //Whether we can move in a direction, to avoid recomputation
    final Direction[] directions = Direction.DIRECTION_ORDER; //All directions

    final int BANSIZE = Parameters.BUGPATH_BAN_SIZE;
    int bannedIdx = 0;
    MapLocation[] banned = new MapLocation[BANSIZE]; //List of banned locations, to avoid getting stuck on currents

    int minDistToObj = 1000000; //Min distance to target during the travel

    MapLocation lastObj = new MapLocation(1,1);
    MapLocation lastObs = null; // Obstacle we are surrounding

    MapLocation lastLocation = new MapLocation(-10, -10); //Last location we were at

    boolean rotateRPath; // Surround obstacles to the right or to the left

    int lastImprovement = 0;

    public Bugpath(){
        rotateRPath = Global.rc.getID() % 2 == 0; // half the robots will turn one way, and half will turn the other way, so that they don't get stuck as often
        Arrays.fill(banned, new MapLocation(-1, -1));
    }

    //Resets pathfinding, needed for switching targets and every so often to get unstuck.
    public void HardReset(MapLocation newObj) throws GameActionException {
        minDistToObj = 1000000;
        Arrays.fill(banned, new MapLocation(-1, -1));
        lastObs = null;
        lastObj = newObj;
        lastLocation = Global.myLocation;
        lastImprovement = Global.rc.getRoundNum();
        if (Global.rng.nextInt(10) <= 3)
            rotateRPath = !rotateRPath;
    }

    // Returns a direction with the goal of getting to the target.
    public Direction Pathfind(MapLocation target) throws GameActionException {
        //Global.rc.setIndicatorLine(Global.myLocation, target, 0, 0, 0);
        if (!target.equals(lastObj) || !lastLocation.equals(Global.myLocation)) {
            HardReset(target);
        }
        if (!Global.rc.isMovementReady()) {
            return null;
        }
        if (lastImprovement + Parameters.PATHFINDING_TIME_LIMIT < Global.rc.getRoundNum()) {
            HardReset(target);
        }
        int dist = Global.myLocation.distanceSquaredTo(target);
        if (dist < minDistToObj) {
            minDistToObj = dist;
            lastImprovement = Global.rc.getRoundNum();
            lastObs = null;
        }
        fillCanMove();
        Direction answer = null;
        if (lastObs == null) {
            answer = greedyMove(target);
            if (answer == null) {
                lastObs = Global.myLocation.add(Global.myLocation.directionTo(target));
            }
        }
        if (lastObs != null) {
            answer = surroundObstacle();
        }
        if (answer != null) {
            lastLocation = Global.myLocation.add(answer);
        }
        return answer;
    }

    //To be called at the end of every turn when pathfinding, even if we couldnt move.
    public void EndTurn() throws GameActionException {
        Direction current = Global.rc.senseMapInfo(Global.myLocation).getCurrentDirection();
        MapLocation loc = Global.myLocation.add(current);
        if (Global.rc.isLocationOccupied(loc))
            current = Direction.CENTER;
        if (current != Direction.CENTER) {
            banned[(bannedIdx = (bannedIdx+1)%BANSIZE)] = Global.myLocation;
            if (lastObs != null)
                lastObs = Global.myLocation;
        }
        lastLocation = Global.myLocation.add(current);
    }

    //Computes the canMove array
    private void fillCanMove() throws GameActionException{
        //boolean lastMove = (Global.rc.getMovementCooldownTurns() + Global.rc.getType().movementCooldown) / GameConstants.COOLDOWN_LIMIT > 0;
        for (int i = canMove.length; i-- > 0;) {
            Direction dir = directions[i];
            if (Global.rc.canMove(dir)) { //Check if we can possibly move
                canMove[i] = true;
            }
            else
                canMove[i] = false;
            //if (canMove[i])
            //Global.rc.setIndicatorLine(location, location.add(dir), 0, 255, 0);
            //else
            //Global.rc.setIndicatorLine(location, location.add(dir), 255, 0 , 0);
        }
        // Avoid banned locations
        for (int j = BANSIZE; j-->0;) {
            //Global.rc.setIndicatorDot(banned[j], 0, 0, 255);
            if (Global.myLocation.isAdjacentTo(banned[j])){
                canMove[Global.myLocation.directionTo(banned[j]).getDirectionOrderNum()] = false;
            }
        }
    }

    //Tries to greedily get closer to the target, returs false if impossible
    private Direction greedyMove(MapLocation target) throws GameActionException{
        int bestDist = Global.myLocation.distanceSquaredTo(target);
        Direction bestDir = null;
        for (int i = canMove.length; i-- > 0;) {
            if (canMove[i]) {
                Direction dir = directions[i];
                MapLocation ml = Global.myLocation.add(dir);
                Direction current = Global.rc.senseMapInfo(ml).getCurrentDirection();
                ml = ml.add(current);
                if (Global.rc.canSenseLocation(ml)) {
                    current = Global.rc.senseMapInfo(ml).getCurrentDirection();
                    ml = ml.add(current);
                }
                int dist = ml.distanceSquaredTo(target);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestDir = dir;
                }
            }
        }
        return bestDir;
    }

    // Moves around an obstacle, lastObs must be assigned
    private Direction surroundObstacle() throws GameActionException{
        Direction dir = Global.myLocation.directionTo(lastObs);
        //Global.rc.setIndicatorDot(lastObs, 0, 0, 0);
        if (rotateRPath)
            dir = dir.rotateRight();
        else
            dir = dir.rotateLeft();
        for (int i = 7; i-- > 0;) {
            if (canMove[dir.getDirectionOrderNum()]) {
                //Global.rc.setIndicatorDot(location.add(dir), 0, 255, 0);
                return dir;
            }
            else {
                //Global.rc.setIndicatorDot(location.add(dir), 255, 0, 0);
                lastObs = Global.myLocation.add(dir);
                if (!Global.rc.onTheMap(lastObs)) {
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
        return null;
    }
}


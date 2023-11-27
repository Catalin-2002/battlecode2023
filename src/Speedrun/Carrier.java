package Speedrun;

import battlecode.common.*;

public class Carrier extends Robot{
    static MapComms mapComms;
    Exploration exploration;
    Bugpath bugpath;
    WellTargetManager wellTargetManager;
    CarrierMicro micro;

    MapLocation lastWellTarget = null;
    boolean hasAnchor = false;
    boolean scared = false;
    MapLocation objectiveIsland = null;

    public void initRobot() throws GameActionException {
        super.initRobot();
        exploration = new Exploration();
        bugpath = new Bugpath();
        wellTargetManager = new WellTargetManager();
        mapComms = new MapComms();
        micro = new CarrierMicro();
    }

    public void initTurn() throws GameActionException {
        super.initTurn();

    }

    public void play() throws GameActionException {
        Direction microDirection = micro.doMicro();
        while (microDirection != null && microDirection != Direction.CENTER) {
            scared = true;
            tryMove(microDirection);
            microDirection = micro.doMicro();
        }
        if (hasAnchor) {
            goDepositAnchor();
        }
        else {
            if (Global.rc.getRoundNum() > Parameters.BUILD_ANCHORS_ROUND)
                attemptToTakeAnchor();
            int load = Global.rc.getResourceAmount(ResourceType.MANA) + Global.rc.getResourceAmount(ResourceType.ADAMANTIUM);
            if (load >= GameConstants.CARRIER_CAPACITY || scared) {
                goDeposit();
            } else {
                lastWellTarget = wellTargetManager.getTarget();
                if (lastWellTarget == null) {
                    explore();
                } else if (Global.myLocation.distanceSquaredTo(lastWellTarget) > 2) {
                    goToWell();
                } else {
                    collectResourcesAndJuggle();
                }
            }
        }
    }

    public void endTurn() throws GameActionException {
        bugpath.EndTurn();
        super.endTurn();
        mapComms.updateAll();
    }

    private void explore() throws GameActionException {
        Global.indicatorString += "explore";
        MapLocation exploreTarget = exploration.getTarget();
        while (Global.rc.isMovementReady() && Global.myLocation.distanceSquaredTo(exploreTarget) > 2) {
            Direction dir = bugpath.Pathfind(exploreTarget);
            if (dir == null)
                break;
            tryMove(dir);
        }
    }

    private void goToWell() throws GameActionException {
        Global.indicatorString += "goToWell";
        Global.rc.setIndicatorLine(Global.rc.getLocation(), lastWellTarget, 0, 255, 0);
        while (Global.rc.isMovementReady() && Global.myLocation.distanceSquaredTo(lastWellTarget) > 2) {
            Direction dir = bugpath.Pathfind(lastWellTarget);
            if (dir == null)
                break;
            tryMove(dir);
        }
    }

    private void collectResourcesAndJuggle() throws GameActionException {
        Global.indicatorString += "collect";
        while (Global.rc.canCollectResource(lastWellTarget, -1)){
            Global.rc.collectResource(lastWellTarget, -1);
        }
        for (int i = 5; i-->0;){
            Direction dir = Direction.DIRECTION_ORDER[Global.rng.nextInt(Direction.DIRECTION_ORDER.length)];
            if (Global.myLocation.add(dir).distanceSquaredTo(lastWellTarget) <= 2)
                tryMove(dir);
        }
    }

    private void goDeposit() throws GameActionException {
        Global.indicatorString += "goDeposit";
        MapLocation hq;
        if (lastWellTarget != null)
            hq = getClosestHq(lastWellTarget);
        else
            hq = getClosestHq(Global.myLocation);
        if (hq == null) {
            Global.indicatorString += "noHQ";
            return;
        }
        else {
            Global.indicatorString += hq.toString();
        }
        if (Global.rc.isMovementReady() && Global.myLocation.distanceSquaredTo(hq) > 2) {
            Direction dir = bugpath.Pathfind(hq);
            if (dir != null)
                tryMove(dir);
        }
        if (Global.myLocation.distanceSquaredTo(hq) <= 2){
            scared = false;
            deposit(hq);
        }
    }

    private MapLocation getClosestHq(MapLocation well) {
        MapLocation[] hqs = mapComms.alliedHQList;
        int i = mapComms.alliedHQCount;
        MapLocation best = null;
        int bestDist = 1000000;
        while (i-->0){
            MapLocation ml = hqs[i];
            int dist = well.distanceSquaredTo(ml);
            if (dist < bestDist) {
                best = ml;
                bestDist = dist;
            }
        }
        if (best != null)
            Global.rc.setIndicatorLine(best, well, 0, 0, 255);
        return best;
    }

    private void deposit(MapLocation ml) throws GameActionException{
        int mana = Global.rc.getResourceAmount(ResourceType.MANA);
        if (mana > 0 && Global.rc.canTransferResource(ml, ResourceType.MANA, mana)) {
            Global.rc.transferResource(ml, ResourceType.MANA, mana);
        }
        int ada = Global.rc.getResourceAmount(ResourceType.ADAMANTIUM);
        if (ada > 0 && Global.rc.canTransferResource(ml, ResourceType.ADAMANTIUM, ada)) {
            Global.rc.transferResource(ml, ResourceType.ADAMANTIUM, ada);
        }
        int elixir = Global.rc.getResourceAmount(ResourceType.ELIXIR);
        if (elixir > 0 && Global.rc.canTransferResource(ml, ResourceType.ELIXIR, elixir)) {
            Global.rc.transferResource(ml, ResourceType.ELIXIR, elixir);
        }
    }

    private void attemptToTakeAnchor() throws GameActionException {
        MapLocation hq = getClosestHq(Global.myLocation);
        if (hq == null)
            return;
        if (Global.rc.canTakeAnchor(hq, Anchor.STANDARD)) {
            Global.rc.takeAnchor(hq, Anchor.STANDARD);
            hasAnchor = true;
        }
    }

    private void resetObjectiveIsland() throws GameActionException {
        while (true) {
            int x = Global.rng.nextInt(Global.mapWidth);
            int y = Global.rng.nextInt(Global.mapHeight);
            if (mapComms.island[x][y]){
                objectiveIsland = new MapLocation(x, y);
                break;
            }
        }
    }

    private void goDepositAnchor() throws GameActionException {
        if (objectiveIsland == null)
            resetObjectiveIsland();
        if (Global.rc.canSenseLocation(objectiveIsland)) {
            int island = Global.rc.senseIsland(objectiveIsland);
            if (Global.rc.senseAnchor(island) != null)
                resetObjectiveIsland();
        }
        while (Global.rc.isMovementReady() && Global.myLocation.distanceSquaredTo(objectiveIsland) > 2) {
            Direction dir = bugpath.Pathfind(objectiveIsland);
            if (dir == null)
                break;
            tryMove(dir);
        }
        if (Global.rc.canPlaceAnchor()) {
            Global.rc.placeAnchor();
            hasAnchor = false;
        }
    }
}

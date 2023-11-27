package Speedrun;

import battlecode.common.*;

public class Launcher extends Robot{
    Exploration exploration;
    Bugpath bugpath;
    CombatMicro micro;

    public void initRobot() throws GameActionException {
        super.initRobot();
        exploration = new Exploration();
        bugpath = new Bugpath();
        micro = new CombatMicro();
    }

    public void initTurn() throws GameActionException {
        super.initTurn();

    }

    public void play() throws GameActionException {
        while (attack());
        Direction combatMove = micro.doMicro();
        if (combatMove != null && combatMove != Direction.CENTER) {
            tryMove(combatMove);
        }
        else {
            Direction dir = bugpath.Pathfind(exploration.getTarget());
            if (dir != null)
                tryMove(dir);
        }
        while (attack());
    }

    public void endTurn() throws GameActionException {
        super.endTurn();
    }

    private boolean attack() throws GameActionException {
        if (!Global.rc.isActionReady())
            return false;
        RobotInfo[] enemies = Global.rc.senseNearbyRobots(Global.rc.getType().actionRadiusSquared, Global.enemies);
        MapLocation best = null;
        int lowHp = 10000;
        for (int i = enemies.length; i-->0;) {
            RobotInfo ri = enemies[i];
            if (ri.getHealth() < lowHp && ri.getType() != RobotType.HEADQUARTERS) {
                lowHp = ri.getHealth();
                best = ri.getLocation();
            }
        }
        if (best != null) {
            Global.rc.attack(best);
            return true;
        }
        return false;
    }


}

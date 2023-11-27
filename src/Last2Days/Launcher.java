package Last2Days;

import battlecode.common.*;

public class Launcher extends Robot{
    Bugpath bugpath;
    CombatMicro micro;
    MapComms mapComms;
    SymmetryRecognizer symmetry;
    boolean inCombat;
    MapLocation target;

    public void initRobot() throws GameActionException {
        super.initRobot();
        bugpath = new Bugpath();
        micro = new CombatMicro();
        mapComms = new MapComms();
        symmetry = new SymmetryRecognizer(mapComms);
    }

    public void initTurn() throws GameActionException {
        super.initTurn();
        inCombat = false;

    }

    public void play() throws GameActionException {
        while (attack());
        Direction combatMove = micro.doMicro();
        if (combatMove != null) {
            inCombat = true;
            if (combatMove != Direction.CENTER)
                tryMove(combatMove);
        }
        else {
            if (target != null && Global.rc.canSenseLocation(target)) {
                RobotInfo ri = Global.rc.senseRobotAtLocation(target);
                if (ri == null || ri.getType() != RobotType.HEADQUARTERS || ri.getTeam() != Global.enemies)
                    target = null;
            }
            if (target == null)
                resetTarget();
            if (target != null) {
                Direction dir = bugpath.Pathfind(target);
                if (dir != null)
                    tryMove(dir);
            }
        }
        while (attack());
    }

    public void endTurn() throws GameActionException {
        super.endTurn();
        if (!inCombat)
            mapComms.updateAll();
        Global.rc.setIndicatorString(Global.indicatorString);
    }

    private void resetTarget() {
        symmetry.hqTest();
        MapLocation[] possibleEnemyHqs = symmetry.possibleEnemyHqs();
        if (possibleEnemyHqs.length > 0)
            target = possibleEnemyHqs[Global.rng.nextInt(possibleEnemyHqs.length)];
    }

    private boolean attack() throws GameActionException {
        if (!Global.rc.isActionReady())
            return false;
        RobotInfo[] enemies = Global.rc.senseNearbyRobots(Global.rc.getType().actionRadiusSquared, Global.enemies);
        MapLocation best = null;
        int lowHp = 100000;
        for (int i = enemies.length; i-->0;) {
            RobotInfo ri = enemies[i];
            int health = ri.getHealth();
            if (ri.getType() != RobotType.LAUNCHER && ri.getType() != RobotType.DESTABILIZER)
                health *= 10;
            if (health < lowHp && ri.getType() != RobotType.HEADQUARTERS) {
                lowHp = health;
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

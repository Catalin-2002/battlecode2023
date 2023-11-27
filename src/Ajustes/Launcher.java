package Ajustes;

import battlecode.common.*;

public class Launcher extends Robot{
    Bugpath bugpath;
    CombatMicro micro;
    MapComms mapComms;
    SymmetryRecognizer symmetry;
    boolean inCombat;
    AttackTargetManager targetManager;


    public void initRobot() throws GameActionException {
        super.initRobot();
        bugpath = new Bugpath();
        micro = new CombatMicro();
        mapComms = new MapComms();
        symmetry = new SymmetryRecognizer(mapComms);
        targetManager = new AttackTargetManager(symmetry);
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
            MapLocation target = targetManager.getTarget();
            if (target != null) {
                Direction dir = bugpath.Pathfind(target);
                if (dir != null)
                    tryMove(dir);
            }
        }
        while (attack());
    }

    public void endTurn() throws GameActionException {
        targetManager.checkValidity();
        super.endTurn();
        if (!inCombat)
            mapComms.updateAll();
    }

    private boolean attack() throws GameActionException {
        if (!Global.rc.isActionReady())
            return false;
        int radius = RobotType.LAUNCHER.actionRadiusSquared;
        RobotInfo[] enemies = Global.rc.senseNearbyRobots(radius, Global.enemies);
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
        if (Global.rc.senseMapInfo(Global.myLocation).hasCloud()) {
            RobotInfo[] all = Global.sensedRobots;
            for (int i = all.length; i-->0;) {
                RobotInfo ri = all[i];
                if (ri.getTeam() == Global.allies || ri.getLocation().distanceSquaredTo(Global.myLocation) > radius)
                    continue;
                int health = ri.getHealth();
                if (ri.getType() != RobotType.LAUNCHER && ri.getType() != RobotType.DESTABILIZER)
                    health *= 10;
                if (health < lowHp && ri.getType() != RobotType.HEADQUARTERS) {
                    lowHp = health;
                    best = ri.getLocation();
                }
            }
        }
        if (best != null) {
            Global.rc.attack(best);
            return true;
        }
        return false;
    }


}

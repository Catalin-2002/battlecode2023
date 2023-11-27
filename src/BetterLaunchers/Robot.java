package BetterLaunchers;

import battlecode.common.*;

import java.util.Random;

public abstract class Robot {
    static WellsComms wellsComms;

    public void run() throws GameActionException{
        initRobot();
        while (true) {
            int startingRound = Global.rc.getRoundNum();
            int bytecode1 = Clock.getBytecodeNum();
            initTurn();
            int bytecode2 = Clock.getBytecodeNum();
            Global.indicatorString += "InitTurn " + (bytecode2 - bytecode1);
            play();
            int bytecode3 = Clock.getBytecodeNum();
            Global.indicatorString += "Play " + (bytecode3 - bytecode2);
            endTurn();
            int bytecode4 = Clock.getBytecodeNum();
            Global.indicatorString += "EndTurn " + (bytecode4 - bytecode3);
            int endingRound = Global.rc.getRoundNum();
            if (startingRound < endingRound) {
                if (endingRound <= 5 + Global.startingRound) {
                    System.out.println("Starting out of bytecode at " + Global.myLocation.toString());
                }
                else
                    System.out.println("Playing out of bytecode at " + Global.myLocation.toString());
            }
            Clock.yield();
        }
    }

    public void initRobot() throws GameActionException {
        Global.rng = new Random(Global.rc.getID()^193561532);
        Global.mapWidth = Global.rc.getMapWidth();
        Global.mapHeight = Global.rc.getMapHeight();
        Global.allies = Global.rc.getTeam();
        Global.enemies = Global.allies.opponent();
        Global.spawningRound = Global.rc.getRoundNum();
        Global.myLocation = Global.rc.getLocation();
        wellsComms = new WellsComms();
    }

    public void initTurn() throws GameActionException {
        Global.myLocation = Global.rc.getLocation();
        Global.indicatorString = "";
        Global.startingRound = Global.rc.getRoundNum();
        Global.sensedWells = Global.rc.senseNearbyWells();
        Global.sensedRobots = Global.rc.senseNearbyRobots();
    }

    public abstract void play() throws GameActionException;

    public void endTurn() throws GameActionException {
        wellsComms.UpdateAll();
        Global.rc.setIndicatorString(Global.indicatorString);
    }

    public boolean tryMove(Direction dir) throws GameActionException {
        if (Global.rc.canMove(dir)) {
            Global.rc.move(dir);
            Global.myLocation = Global.rc.getLocation();
            return true;
        }
        return false;
    }
}

package Speedrun;

import battlecode.common.*;

public class Headquarter extends Robot {
    static MapComms mapComms;
    Spawner spawner;

    public void initRobot() throws GameActionException {
        super.initRobot();
        spawner = new Spawner();
        mapComms = new MapComms();
    }

    public void initTurn() throws GameActionException {
        super.initTurn();
    }

    public void play() throws GameActionException {
        if (Global.rc.getRoundNum() > Parameters.BUILD_ANCHORS_ROUND) {
            while (Global.rc.canBuildAnchor(Anchor.STANDARD))
                Global.rc.buildAnchor(Anchor.STANDARD);
        }
        else {
            if (Global.rc.getRoundNum() > 2)
                while (spawner.trySpawn(RobotType.LAUNCHER));
            while (spawner.trySpawn(RobotType.CARRIER));
        }
    }

    public void endTurn() throws GameActionException {
        super.endTurn();
        mapComms.updateAll();
    }
}

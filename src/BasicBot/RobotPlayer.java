package BasicBot;

import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {

    public static void run(RobotController rc) throws GameActionException {

        while (true) {
            try {
                Robot r = null;
                switch (rc.getType()) {
                    case HEADQUARTERS: 
                        r = new Headquarter(); 
                        break;
                    case CARRIER: 
                        r = new Carrier(); 
                        break;
                    case LAUNCHER: 
                        r = new Launcher(); 
                        break;
                    case BOOSTER: 
                        r = new Booster(); 
                        break;
                    case DESTABILIZER: 
                        r = new Destabilizer();
                        break;
                    case AMPLIFIER: 
                        r = new Amplifier();
                        break;
                }

                r.run(rc);
            } catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
                rc.resign();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
                rc.resign();
            } finally {
                Clock.yield();
            }
        }
    }
}

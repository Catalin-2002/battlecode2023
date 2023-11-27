package ManaOnly;

import battlecode.common.*;

public class MapComms {
    boolean[][] explored;
    boolean[][] impassable;
    boolean[][] island;
    boolean[][] cloud;
    Direction[][] current;
    int enemyHQCount = 0;
    MapLocation[] enemyHQList = new MapLocation[5];
    int alliedHQCount = 0;
    MapLocation[] alliedHQList = new MapLocation[5];
    int[] rawData;
    int timeCycleLength;
    boolean mainCharacter;
    int senseRadius = Parameters.COMM_SENSE_RADIUS;

    public MapComms() throws GameActionException {

        if (Global.rc.getType() == RobotType.HEADQUARTERS){
            if (Global.rc.readSharedArray(0) == 0) {
                Global.rc.writeSharedArray(0, 1);
                mainCharacter = true;
            }
            else {
                mainCharacter = false;
            }
            alliedHQList[alliedHQCount++] = Global.myLocation;
        }

        int width = Global.mapWidth;
        int height = Global.mapHeight;

        explored = new boolean[width+1][height];
        impassable = new boolean[width+1][height];
        island = new boolean[width+1][height];
        cloud = new boolean[width+1][height];
        current = new Direction[width+1][height];

        rawData = new int[(width*height+3) / 4];
        timeCycleLength = (rawData.length + Parameters.INTS_FOR_MAP_COMMS - 1) / Parameters.INTS_FOR_MAP_COMMS;

    }

    public void updateAll() throws GameActionException {
        if (Global.rc.getRoundNum() > 1) {
            if (Global.rc.getType() != RobotType.HEADQUARTERS && (Global.rc.getRoundNum() < 30 || Global.rc.getRoundNum() >= Global.spawningRound + 23)) {
                int preBytecode = Clock.getBytecodeNum();
                updateSensing();
                int postBytecode = Clock.getBytecodeNum();
                Global.indicatorString += "US " + (postBytecode - preBytecode);
            }
            else {
                updateRawDataHeadquarters();
            }
            if (Global.rc.getRoundNum() > 2 || !mainCharacter) {
                int preBytecode = Clock.getBytecodeNum();
                updateAllData();
                int postBytecode = Clock.getBytecodeNum();
                Global.indicatorString += "UD " + (postBytecode - preBytecode);
            }
            if (mainCharacter) {
                Global.indicatorString += " Main ";
                int preBytecode = Clock.getBytecodeNum();
                writeAll();
                int postBytecode = Clock.getBytecodeNum();
                Global.indicatorString += "WA " + (postBytecode - preBytecode);
            }
            /*else if (Global.rc.getRoundNum() == 27) {
                debug();
            }*/
            //if (Global.rc.getRoundNum() != 27)
                //debugHqs();
        }
    }

    public void debug() {
        for (int x = Global.mapWidth; x-- > 0;) {
            for (int y = Global.mapHeight; y-- > 0;) {
                MapLocation ml = new MapLocation(x, y);
                if (!explored[x][y]) {
                    //Global.rc.setIndicatorDot(ml, 0,0,0);
                }
                else if (impassable[x][y]) {
                    Global.rc.setIndicatorDot(ml,100, 0, 0);
                }
                else if (island[x][y]) {
                    Global.rc.setIndicatorDot(ml, 0, 0, 255);
                }
                else if (cloud[x][y]) {
                    Global.rc.setIndicatorDot(ml, 100, 100, 100);
                }
                else if (current[x][y] != Direction.CENTER) {
                    Global.rc.setIndicatorDot(ml, 255, 255, 255);
                }
                else {
                    Global.rc.setIndicatorDot(ml, 0, 255, 0);
                }
            }
        }
    }

    public void debugHqs() {
        for (int i = alliedHQCount; i-- > 0;) {
            MapLocation ml =alliedHQList[i];
            Global.rc.setIndicatorDot(ml, 255, 0, 255);
        }
        for (int i = enemyHQCount; i-- > 0;) {
            MapLocation ml =enemyHQList[i];
            Global.rc.setIndicatorDot(ml, 127, 0, 127);
        }
    }

    private void writeAll() throws GameActionException {
        //debug();
        int round = Global.rc.getRoundNum();
        int s = (round%timeCycleLength)*Parameters.INTS_FOR_MAP_COMMS;
        for (int i = Parameters.INTS_FOR_MAP_COMMS; i-->0;) {
            if (s+i >= rawData.length)
                continue;
            int raw = rawData[s+i];
            Global.rc.writeSharedArray(i, raw);
            //Global.rc.setIndicatorDot(new MapLocation((s+i)*4/Global.mapHeight, (s+i)*4%Global.mapHeight), 0, 0, 0);
            //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+1)/Global.mapHeight, ((s+i)*4+1)%Global.mapHeight), 0, 0, 0);
            //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+2)/Global.mapHeight, ((s+i)*4+2)%Global.mapHeight), 0, 0, 0);
            //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+3)/Global.mapHeight, ((s+i)*4+3)%Global.mapHeight), 0, 0, 0);
        }
    }

    private void updateAllData() throws GameActionException {
        if (Clock.getBytecodesLeft() < 500)
            return;
        int height = Global.mapHeight;
        boolean canCom = Global.rc.canWriteSharedArray(0, 0);
        int round = Global.rc.getRoundNum();
        if (mainCharacter)
            round--;
        int s = (round%timeCycleLength)*Parameters.INTS_FOR_MAP_COMMS;
        for (int i = Parameters.INTS_FOR_MAP_COMMS; i-->0 && Clock.getBytecodesLeft() > 500;) {
            int data = Global.rc.readSharedArray(i);
            if (s+i >= rawData.length)
                continue;
            int raw = rawData[s+i];
            //Global.rc.setIndicatorDot(new MapLocation((s+i)*4/Global.mapHeight, (s+i)*4%Global.mapHeight), 0, 0, 255);
            //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+1)/Global.mapHeight, ((s+i)*4+1)%Global.mapHeight), 0, 0, 255);
            //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+2)/Global.mapHeight, ((s+i)*4+2)%Global.mapHeight), 0, 0, 255);
            //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+3)/Global.mapHeight, ((s+i)*4+3)%Global.mapHeight), 0, 0, 255);
            // If we don't have the updated info, update it
            if ((data | raw) != raw) {
                //Global.rc.setIndicatorDot(new MapLocation((s+i)*4/Global.mapHeight, (s+i)*4%Global.mapHeight), 255, 0, 0);
                //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+1)/Global.mapHeight, ((s+i)*4+1)%Global.mapHeight), 255, 0, 0);
                //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+2)/Global.mapHeight, ((s+i)*4+2)%Global.mapHeight), 255, 0, 0);
                //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+3)/Global.mapHeight, ((s+i)*4+3)%Global.mapHeight), 255, 0, 0);
                int j = 4*(s+i);
                updateCleanData(data>>12, j/height, j%height);
                j++;
                updateCleanData((data>>8) & 15, j/height, j%height);
                j++;
                updateCleanData((data>>4) & 15, j/height, j%height);
                j++;
                updateCleanData(data & 15, j/height, j%height);
                raw |= data;
                rawData[s+i] = raw;
            }
            // If the data in comms is not updated and we can comm, update it
            if (canCom && raw != data && !mainCharacter) {
                Global.rc.writeSharedArray(i, raw);
                //Global.rc.setIndicatorDot(new MapLocation((s+i)*4/Global.mapHeight, (s+i)*4%Global.mapHeight), 0, 255, 0);
                //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+1)/Global.mapHeight, ((s+i)*4+1)%Global.mapHeight), 0, 255, 0);
                //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+2)/Global.mapHeight, ((s+i)*4+2)%Global.mapHeight), 0, 255, 0);
                //Global.rc.setIndicatorDot(new MapLocation(((s+i)*4+3)/Global.mapHeight, ((s+i)*4+3)%Global.mapHeight), 0, 255, 0);
            }
        }
    }

    private void updateSensing() throws GameActionException {
        MapInfo[] miList = Global.rc.senseNearbyMapInfos(senseRadius);
        int[] islands = Global.rc.senseNearbyIslands();
        for (int i = islands.length; i-- > 0;) {
            MapLocation[] list = Global.rc.senseNearbyIslandLocations(senseRadius, islands[i]);
            for (int j = list.length; j-- > 0;) {
                MapLocation ml = list[j];
                island[ml.x][ml.y] = true;
            }
        }

        RobotInfo[] riList = Global.rc.senseNearbyRobots();
        for (int i = riList.length; i-->0;) {
            RobotInfo ri = riList[i];
            if (ri.getType() == RobotType.HEADQUARTERS) {
                MapLocation ml = ri.getLocation();
                int x = ml.x;
                int y = ml.y;
                if (explored[x][y])
                    continue;
                impassable[x][y] = true;
                explored[x][y] = true;
                if (ri.getTeam() == Global.enemies)
                    enemyHQList[enemyHQCount++] = ml;
                else {
                    alliedHQList[alliedHQCount++] = ml;
                }
            }
        }
        updateRawDataHeadquarters();
        for (int i = miList.length; i-- > 0;) {
            MapInfo mi = miList[i];
            MapLocation ml = mi.getMapLocation();
            int x = ml.x;
            int y = ml.y;
            if (explored[x][y])
                continue;
            explored[x][y] = true;
            impassable[x][y] = !mi.isPassable();
            cloud[x][y] = mi.hasCloud();
            current[x][y] = mi.getCurrentDirection();
            updateRawData(x, y);
        }
    }

    private void updateRawData(int x, int y) {
        int rawIdx = x*Global.mapHeight+y;
        int idx = rawIdx/4;
        int offset = (3-rawIdx%4)*4;
        if (impassable[x][y]) {
            rawData[idx] |= (2 << offset);
            //Global.rc.setIndicatorDot(new MapLocation(x, y), 50, 50, 50);
            return;
        }
        if (current[x][y] != Direction.CENTER) {
            switch (current[x][y]) {
                case NORTH: rawData[idx] |= (5 << offset); return;
                case NORTHEAST: rawData[idx] |= (6 << offset); return;
                case EAST: rawData[idx] |= (7 << offset); return;
                case SOUTHEAST: rawData[idx] |= (8 << offset); return;
                case SOUTH: rawData[idx] |= (9 << offset); return;
                case SOUTHWEST: rawData[idx] |= (10 << offset); return;
                case WEST: rawData[idx] |= (11 << offset); return;
                case NORTHWEST: rawData[idx] |= (12 << offset); return;
                default:break;
            }
        }
        if (island[x][y]) {
            if (cloud[x][y]) {
                rawData[idx] |= (15 << offset);
                return;
            }
            rawData[idx] |= (13 << offset);
            return;
        }
        if (cloud[x][y]){
            rawData[idx] |= (14 << offset);
            return;
        }
        rawData[idx] |= (1 << offset);
    }

    private void updateRawDataHeadquarters() {
        for (int i = enemyHQCount; i-- > 0; ){
            MapLocation ml = enemyHQList[i];
            int x = ml.x;
            int y = ml.y;
            int rawIdx = x*Global.mapHeight+y;
            int idx = rawIdx/4;
            int offset = (3-rawIdx%4)*4;
            rawData[idx] &= 0xFFFF ^ (0xF << offset);
            rawData[idx] |= (4 << offset);
        }
        for (int i = alliedHQCount; i-- > 0; ){
            MapLocation ml = alliedHQList[i];
            int x = ml.x;
            int y = ml.y;
            int rawIdx = x*Global.mapHeight+y;
            int idx = rawIdx/4;
            int offset = (3-rawIdx%4)*4;
            rawData[idx] &= 0xFFFF ^ (0xF << offset);
            rawData[idx] |= (3 << offset);
        }
    }

    private void updateCleanData(int data, int x, int y) {
        if (explored[x][y] || data == 0)
            return;
        explored[x][y] = true;
        current[x][y] = Direction.CENTER;
        switch (data) {
            case 2:
                impassable[x][y] = true;
                break;
            case 3:
                impassable[x][y] = true;
                //System.out.println("allied hq at " + x + " " + y);
                alliedHQList[alliedHQCount++] = new MapLocation(x, y);
                break;
            case 4:
                impassable[x][y] = true;
                enemyHQList[enemyHQCount++] = new MapLocation(x, y);
                break;
            case 5:
                current[x][y] = Direction.NORTH;
                break;
            case 6:
                current[x][y] = Direction.NORTHEAST;
                break;
            case 7:
                current[x][y] = Direction.EAST;
                break;
            case 8:
                current[x][y] = Direction.SOUTHEAST;
                break;
            case 9:
                current[x][y] = Direction.SOUTH;
                break;
            case 10:
                current[x][y] = Direction.SOUTHWEST;
                break;
            case 11:
                current[x][y] = Direction.WEST;
                break;
            case 12:
                current[x][y] = Direction.NORTHWEST;
                break;
            case 13:
                island[x][y] = true;
                break;
            case 14:
                cloud[x][y] = true;
                break;
            case 15:
                island[x][y] = true;
                cloud[x][y] = true;
                break;
            default:
                break;
        }
    }
}

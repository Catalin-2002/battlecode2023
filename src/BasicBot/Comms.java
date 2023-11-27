package BasicBot;
import battlecode.common.*;

/*
 * THIS FILE WILL USE THE FOLLOWING CONSTANTS AND DEFINITIONS:
 * 
 * SLOT: one comm number, which can represent 4 tiles
 * CHUNK: a round's communicated tiles
 * CYCLE: the time it takes to communicate the whole map
 * 
 * TILES:
 * 00 = Unknown
 * 01 = Ada. Well
 * 02 = Mana Well
 * 03 = Eli. Well
 * 04 = Empty
 * 05 = Cloud
 * 06 = Wall
 * 07 = Enemy HQ
 * 08 = Wind N
 * 09 = Wind E
 * 10 = Wind S
 * 11 = Wind W
 * 12 = Wind NE
 * 13 = Wind SE
 * 14 = Wind SW
 * 15 = Wind NW
 * 
 * allied HQs will be marked as 'Wall'
 */

public class Comms {
    public final int CHUNK_SLOTS = 56;
    public final int CHUNK_TILES = CHUNK_SLOTS * 4;
    public final int HQ_SLOTS = 2;

    public RobotController rc;

    public int[] mapSlots; // slots of all the tiles of the map
    public int cycleLength; // amount of chunks per cycle
    public int mapSlotsLength; // length of mapSlots[]

    public Comms(RobotController rc) {
        this.rc = rc;
        int size = rc.getMapWidth() * rc.getMapWidth();
        cycleLength = (size + CHUNK_TILES - 1) / CHUNK_TILES; // ceil(size/CHUNK_TILES)
        mapSlotsLength = cycleLength * CHUNK_SLOTS;
        mapSlots = new int[mapSlotsLength];
    }

    // reads the shared values & updates them with any new info
    public void updateTiles(boolean isFirstHQ) throws GameActionException {
        // bytecode saving
        RobotController rc = this.rc;
        int[] mapSlots = this.mapSlots;

        // get round
        int round = rc.getRoundNum();
        if(isFirstHQ) // the first HQ is reading the previous round's tiles, bc. they haven't been replaced yet
            round += (cycleLength - 1); // round-- mod cycleLength
        
        // calculate offset
        int chunkIdx = round % cycleLength;
        int slotOffset = chunkIdx * CHUNK_SLOTS;
        System.out.println("offset: " + slotOffset);
        boolean canWrite = rc.canWriteSharedArray(63, 0);
        boolean shouldWrite = canWrite && !isFirstHQ; // add extra conditions if necessary

        // read all slots & update info
        for(int slotIdx = CHUNK_SLOTS - 1; slotIdx-- > 0; ) {
            // update local value
            int offsetSlotIdx = slotIdx + slotOffset;
            int shared = rc.readSharedArray(slotIdx);
            mapSlots[offsetSlotIdx] |= shared;

            // write if unshared info
            if (shouldWrite && shared != mapSlots[offsetSlotIdx])
                rc.writeSharedArray(slotIdx, mapSlots[offsetSlotIdx]);
        }

        // if first HQ: replace all info by local data
        if (isFirstHQ && canWrite) {
            // recalculate chunkoffset
            round = rc.getRoundNum();
            chunkIdx = round % cycleLength;
            slotOffset = chunkIdx * CHUNK_SLOTS;
            System.out.println("new offset: " + slotOffset);

            // write all slots
            for(int slotIdx = CHUNK_SLOTS - 1; slotIdx-- > 0; ) {
                int offsetSlotIdx = slotIdx + slotOffset;
                rc.writeSharedArray(slotIdx, mapSlots[offsetSlotIdx]);
            }
        }
    }

    // updates the information stored about a tile
    void setTile(int x, int y, int tileValue) {
        // bytecode savings
        RobotController rc = this.rc;
        int[] mapSlots = this.mapSlots;

        // get slot
        int tileIdx = x + y * rc.getMapWidth();
        int slotIdx = tileIdx / 4;
        int slotVal = mapSlots[slotIdx];

        // modify slot part
        int slotPart = tileIdx % 4;
        switch(slotPart) {
            case 0: slotVal |=  tileValue;        break; 
            case 1: slotVal |= (tileValue <<  4); break;
            case 2: slotVal |= (tileValue <<  8); break;
            case 3: slotVal |= (tileValue << 12); break;
        }

        // update slot
        mapSlots[slotIdx] = slotVal;
    }

    // gets the stored information about a tile
    int getTile(int x, int y) {
        // bytecode savings
        RobotController rc = this.rc;
        int[] mapSlots = this.mapSlots;

        // get slot
        int tileIdx = x + y * rc.getMapWidth();
        int slotIdx = tileIdx / 4;
        int slotVal = mapSlots[slotIdx];

        // extract slot part
        int slotPart = tileIdx % 4;
        switch(slotPart) {
            case 0: return  slotVal & 0x0000F       ;
            case 1: return (slotVal & 0x000F0) >>  4;
            case 2: return (slotVal & 0x00F00) >>  8;
            case 3: return (slotVal          ) >> 12;
        }

        // the code should NEVER reach this (but just in case)
        throw new RuntimeException("Invalid slotPart value: " + slotPart);
    }

    // updates the information of the tiles that can be sensed this turn
    void senseTiles() throws GameActionException {
        // bytecode savings
        RobotController rc = this.rc;
        int[] mapSlots = this.mapSlots;

        // for each mapInfo, update that tile using setTile()
        MapInfo[] infos = rc.senseNearbyMapInfos();
        for (int i = infos.length - 1; i-- > 0;) {
            MapInfo info = infos[i];
            MapLocation location = info.getMapLocation();

            // extract location type
            int locationType = 0;
            Direction windDirection = info.getCurrentDirection();
            switch(windDirection) {
                // wind
                case NORTH    : locationType =  8; break;
                case EAST     : locationType =  9; break;
                case SOUTH    : locationType = 10; break;
                case WEST     : locationType = 11; break;
                case NORTHEAST: locationType = 12; break;
                case SOUTHEAST: locationType = 13; break;
                case SOUTHWEST: locationType = 14; break;
                case NORTHWEST: locationType = 15; break;

                // no wind
                case CENTER:
                    if (info.hasCloud())
                        locationType = 5; // Cloud
                    else if (!info.isPassable())
                        locationType = 6; // Wall
                    else {
                        WellInfo well = rc.senseWell(location);
                        if (well != null) { // Resource Well
                            switch(well.getResourceType()) {
                                case ADAMANTIUM: locationType = 1; break;
                                case MANA      : locationType = 2; break;
                                case ELIXIR    : locationType = 3; break;
                            }
                        }
                        else {
                            RobotInfo robot = rc.senseRobotAtLocation(location);
                            if(robot != null && robot.type == RobotType.HEADQUARTERS) {
                                if(robot.team == rc.getTeam())
                                    locationType = 6; // Wall (Allied HQ)
                                else
                                    locationType = 7; // Enemy HQ
                            }
                            else
                                locationType = 4; // Empty
                        }
                    }
                    break;
            }

            // the same as: setTile(location.x, location.y, locationType);
            int tileIdx = location.x + location.y * rc.getMapWidth();
            int slotIdx = tileIdx / 4;
            int slotVal = mapSlots[slotIdx];
            int slotPart = tileIdx % 4;
            switch(slotPart) {
                case 0: slotVal |=  locationType;        break;
                case 1: slotVal |= (locationType <<  4); break;
                case 2: slotVal |= (locationType <<  8); break;
                case 3: slotVal |= (locationType << 12); break;
            }
            mapSlots[slotIdx] = slotVal;
        }
    }

    // prints a color based on the location type stored in mapSlots[]
    // Gray: unknown (0)
    // Green: adamantium well (1)
    // Blue: mana well (2)
    // Magenta: elixir well (3)
    // White: empty (4)
    // Yellow: cloud (5)
    // Black: Wall (6)
    // Red: Enemy HQ (7)
    // Cyan: Wind (8-15)
    void debugLocation(int x, int y) throws GameActionException {
        int tileValue = getTile(x, y);

        int r = 0, g = 255, b = 255; // Wind (default case)
        switch(tileValue) {
            case 0: r = 120; g = 120; b = 120; break; // Unknown
            case 1: r =   0; g = 255; b =   0; break; // Adamantium Well
            case 2: r =   0; g =   0; b = 255; break; // Mana Well
            case 3: r = 255; g =   0; b = 255; break; // Elixir Well
            case 4: r = 255; g = 255; b = 255; break; // Empty
            case 5: r = 255; g = 255; b =   0; break; // Cloud
            case 6: r =   0; g =   0; b =   0; break; // Wall
            case 7: r = 255; g =   0; b =   0; break; // Enemy HQ
        }
        rc.setIndicatorDot(new MapLocation(x, y), r, g, b);
    }

    // warning: VERY expensive (~80k bytecode)
    void debugAllTiles() throws GameActionException {
        for(int x = rc.getMapWidth() - 1; x-- > 0; )
            for(int y = rc.getMapWidth() - 1; y-- > 0; )
                debugLocation(x, y);
    }
}

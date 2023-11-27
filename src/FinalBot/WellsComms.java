package FinalBot;

import battlecode.common.*;

public class WellsComms {
    boolean[][] knowsWell;
    int broadcastType = 0;
    int adaCount = 0, adaIdx = 0;
    MapLocation[] adaWells = new MapLocation[150];
    int elixirCount = 0, elixirIdx = 0;
    MapLocation[] elixirWells = new MapLocation[150];
    int manaCount = 0, manaIdx = 0;
    MapLocation[] manaWells = new MapLocation[150];
    int storedCount = 0;
    int[] storedMessages = new int[450];

    int broadCastIdx = Parameters.INTS_FOR_MAP_COMMS;

    boolean mainCharacter = false;

    public WellsComms() throws GameActionException{
        knowsWell = new boolean[Global.mapWidth][Global.mapHeight];
        if (Global.rc.getType() == RobotType.HEADQUARTERS) {
            int x = Global.rc.readSharedArray(broadCastIdx);
            if (Global.rc.getType() == RobotType.HEADQUARTERS && x == 0) {
                mainCharacter = true;
                Global.rc.writeSharedArray(broadCastIdx, 1);
            }
        }
    }

    public void UpdateAll() throws GameActionException {
        if (Global.rc.getRoundNum() == 1)
            return;
        if (mainCharacter) {
            readDeclaredWells();
            if (Global.rc.getRoundNum() <= 2)
                mainCharacterSense();
            updateBroadcast();
            //debug();
        }
        else if (Global.rc.getRoundNum() > 2) {
            readBroadcast();
            updateSenses();
            declareWells();
        }
    }

    private void debug() {
        for (int i = adaCount; i-- > 0;) {
            Global.rc.setIndicatorDot(adaWells[i], 255, 0, 0);
        }
        for (int i = elixirCount; i-- > 0;) {
            Global.rc.setIndicatorDot(elixirWells[i], 0, 255, 0);
        }
        for (int i = manaCount; i-- > 0;) {
            Global.rc.setIndicatorDot(manaWells[i], 0, 0, 255);
        }
    }

    private void updateBroadcast() throws GameActionException {
        int k = 3;
        boolean done = false;
        MapLocation ml;
        int x, y, msg;
        while (k-- > 0 && !done) {
            switch (broadcastType) {
                case 0:
                    if (adaCount == 0){
                        broadcastType = 1;
                        break;
                    }
                    if (adaIdx == adaCount)
                        adaIdx = 0;
                    ml = adaWells[adaIdx++];
                    x = ml.x;
                    y = ml.y;
                    msg = (x * Global.mapHeight + y) * 3;
                    Global.rc.setIndicatorDot(ml, 0, 255, 0);
                    Global.rc.writeSharedArray(broadCastIdx, msg+1);
                    done = true;
                    broadcastType = 1;
                    break;
                case 1:
                    if (elixirCount == 0){
                        broadcastType = 2;
                        break;
                    }
                    if (elixirIdx == elixirCount)
                        elixirIdx = 0;
                    ml = elixirWells[elixirIdx++];
                    x = ml.x;
                    y = ml.y;
                    msg = (x * Global.mapHeight + y) * 3 + 1;
                    Global.rc.setIndicatorDot(ml, 0, 255, 0);
                    Global.rc.writeSharedArray(broadCastIdx, msg+1);
                    done = true;
                    broadcastType = 2;
                    break;
                case 2:
                    if (manaCount == 0) {
                        broadcastType = 0;
                        break;
                    }
                    if (manaIdx == manaCount)
                        manaIdx = 0;
                    ml = manaWells[manaIdx++];
                    x = ml.x;
                    y = ml.y;
                    msg = (x * Global.mapHeight + y) * 3 + 2;
                    Global.rc.setIndicatorDot(ml, 0, 255, 0);
                    Global.rc.writeSharedArray(broadCastIdx, msg+1);
                    done = true;
                    broadcastType = 0;
                    break;
            }
        }
        if (!done) {
            Global.rc.writeSharedArray(broadCastIdx, 0);
        }
    }

    private void readBroadcast() throws GameActionException {
        int data = Global.rc.readSharedArray(broadCastIdx)-1;
        if (data == 0) return;
        interpretData(data);
    }

    private void interpretData(int data) {
        int x = data / 3 / Global.mapHeight;
        int y = data / 3 % Global.mapHeight;
        if (knowsWell[x][y])
            return;
        knowsWell[x][y] = true;
        int type = data % 3;
        switch (type) {
            case 0: adaWells[adaCount++] = new MapLocation(x, y); break;
            case 1: elixirWells[elixirCount++] = new MapLocation(x, y); break;
            case 2: manaWells[manaCount++] = new MapLocation(x, y); break;
            default: break;
        }
    }

    private void readDeclaredWells() throws GameActionException {
        for (int i = broadCastIdx+1; i <= broadCastIdx + Parameters.INTS_FOR_WELL_DECLARATION; i++) {
            int data = Global.rc.readSharedArray(i)-1;
            if (data == 0)
                break;
            Global.rc.writeSharedArray(i, 0);
            interpretData(data);
        }
    }

    private void declareWells() throws GameActionException {
        if (storedCount == 0 || !Global.rc.canWriteSharedArray(0,0))
            return;
        int freeIdx = broadCastIdx+1;
        int maxIdx = broadCastIdx + Parameters.INTS_FOR_WELL_DECLARATION;
        while (freeIdx <= maxIdx && Global.rc.readSharedArray(freeIdx) != 0) {
            freeIdx++;
        }
        while (storedCount > 0) {
            if (freeIdx > maxIdx) {
                return;
            }
            storedCount--;
            int msg = storedMessages[storedCount];
            int x = msg / 3 / Global.mapHeight;
            int y = msg / 3 % Global.mapHeight;
            if (knowsWell[x][y]) {
                continue;
            }
            Global.rc.writeSharedArray(freeIdx++, msg+1);
        }
    }

    private void updateSenses() {
        WellInfo[] wellInfos = Global.sensedWells;
        for (int i = wellInfos.length; i-- > 0;) {
            WellInfo wi = wellInfos[i];
            MapLocation ml = wi.getMapLocation();
            int x = ml.x;
            int y = ml.y;
            if (knowsWell[x][y])
                continue;
            int type;
            switch (wi.getResourceType()) {
                case ADAMANTIUM: type = 0; break;
                case ELIXIR: type = 1; break;
                case MANA: type = 2; break;
                default: type = 91238652; break; //Cannot happen
            }
            int msg = (x*Global.mapHeight+y)*3+type;
            boolean alreadyQueued = false;
            for (int j = storedCount; j-- > 0;) {
                if (msg == storedMessages[j]){
                    alreadyQueued = true;
                    break;
                }
            }
            if (alreadyQueued)
                continue;
            storedMessages[storedCount++] = msg;
        }
    }

    private void mainCharacterSense() {
        WellInfo[] wellInfos = Global.sensedWells;
        for (int i = wellInfos.length; i-- > 0;) {
            WellInfo wi = wellInfos[i];
            MapLocation ml = wi.getMapLocation();
            int x = ml.x;
            int y = ml.y;
            if (knowsWell[x][y])
                continue;
            knowsWell[x][y] = true;
            switch (wi.getResourceType()) {
                case ADAMANTIUM: adaWells[adaCount++] = new MapLocation(x, y); break;
                case ELIXIR: elixirWells[elixirCount++] = new MapLocation(x, y); break;
                case MANA: manaWells[manaCount++] = new MapLocation(x, y); break;
                default: break; //Cannot happen
            }
        }
    }

}

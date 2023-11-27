package Ajustes;

import battlecode.common.*;

public class Spawner {
    int spawnableLocationsCount = 0;
    MapLocation[] spawnableLocations = new MapLocation[30];

    public Spawner() throws GameActionException{
        generateSpawnableLocations();
    }

    public void generateSpawnableLocations() throws GameActionException{
        MapInfo[] candidates = Global.rc.senseNearbyMapInfos(9);
        for (int i = candidates.length; i-- > 0;) {
            MapInfo mi = candidates[i];
            RobotInfo ri = Global.rc.senseRobotAtLocation(mi.getMapLocation());
            if ((ri == null || ri.getType() != RobotType.HEADQUARTERS) && mi.isPassable()) {
                spawnableLocations[spawnableLocationsCount++] = mi.getMapLocation();
            }
        }
    }

    public boolean trySpawn(RobotType type, MapLocation ml) throws GameActionException{
        if (Global.rc.canBuildRobot(type, ml)) {
            Global.rc.buildRobot(type, ml);
            return true;
        }
        return false;
    }

    public boolean trySpawn(RobotType type) throws GameActionException{
        if (type.getBuildCost(ResourceType.ADAMANTIUM) > Global.rc.getResourceAmount(ResourceType.ADAMANTIUM)
         || type.getBuildCost(ResourceType.MANA) > Global.rc.getResourceAmount(ResourceType.MANA)
         || type.getBuildCost(ResourceType.ELIXIR) > Global.rc.getResourceAmount(ResourceType.ELIXIR)) {
            return false;
        }
        if (type == RobotType.LAUNCHER)
            return trySpawnLauncher();
        for (int i = 5; i-- > 0;) {
            int j = Global.rng.nextInt(spawnableLocationsCount);
            if (trySpawn(type, spawnableLocations[j]))
                return true;
        }
        for (int i = spawnableLocationsCount; i-- > 0;) {
            if (trySpawn(type, spawnableLocations[i]))
                return true;
        }
        return false;
    }

    public boolean trySpawnLauncher() throws GameActionException {
        MapLocation mid = new MapLocation(Global.mapWidth/2, Global.mapHeight/2);
        int bestDist = 100000000;
        MapLocation ans = null;
        for (int i = spawnableLocationsCount; i-- > 0;) {
            MapLocation ml = spawnableLocations[i];
            if (!Global.rc.canBuildRobot(RobotType.LAUNCHER, ml))
                continue;
            int dist = ml.distanceSquaredTo(mid);
            if (dist <= bestDist) {
                bestDist = dist;
                ans = ml;
            }
        }
        return ans != null && trySpawn(RobotType.LAUNCHER, ans);
    }
}

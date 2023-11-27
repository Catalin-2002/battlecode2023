package BasicBot;
import battlecode.common.*;

public class Launcher extends Robot {
    @Override
    void Awake() throws GameActionException {
        super.Awake();

    }

    @Override
    void Update() throws GameActionException {
        super.Update();
        randomExplore();
    }
}

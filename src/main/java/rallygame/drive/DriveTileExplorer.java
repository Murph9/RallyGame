package rallygame.drive;

import rallygame.car.data.Car;
import rallygame.game.IDriveDone;
import rallygame.world.SmallTiled;

public class DriveTileExplorer extends DriveBase {

    public DriveTileExplorer(IDriveDone done, Car car, SmallTiled world) {
        super(done, car, world);
    }
    
    
    /*
    TODO drive tile explorer design:
    an infinite world
    just drive around with random tiles as you go
    maybe only limited to a general direction, i.e. forwards with a width of 20 or so
    - like race the sun
    goals to complete, but completely chill

    SmallTiled:
    - probably very big tiles
    - undesided if the terrain is noise or tile based
    - stuff also needs to happen on the tiles i hope
    */
}

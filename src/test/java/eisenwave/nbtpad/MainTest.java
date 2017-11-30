package eisenwave.nbtpad;

import org.junit.Test;

public class MainTest {
    
    @Test
    public void main() throws Exception {
        System.setProperty("EDITOR", "nano");
        Main.main("edit", "/home/user/SERVERS/TEST2/world/level.dat");
    }
    
}

import static org.junit.jupiter.api.Assertions.*;

class eHOSTTest {

    public static void main(String[]args){
        eHOST.main(new String[]{"/home/brokenjade/Documents/IdeaProjects/EasyCIE_GUI/data/output/ehost"});
    }
    @org.junit.jupiter.api.Test
    void main() {
        eHOST.main(new String[]{"-x /home/brokenjade/Documents/IdeaProjects/EasyCIE_GUI/data/output/ehost"});
    }
}
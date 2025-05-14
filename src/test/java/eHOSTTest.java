import main.eHOST;
class eHOSTTest {    public static void main(String[]args){
        eHOST.main(new String[]{"-w", "data/output/ehost"});
    }
    @org.junit.jupiter.api.Test
    void main() {
        eHOST.main(new String[]{"-w", "data/output/ehost"});
    }
}
//ENVIRONMENTS

public class Test {
    public Test() {}

    public static int test() {
        {
            int a = 0;
        }
        int a = 123;
        return a;
    }
}

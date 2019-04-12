package pipelines.expressions;

public class Enums {
    private Enums() {

    }

    public static <A> A valueOf(Class<?> c1ass, String value) {
        Enum<?> instance = Enum.valueOf((Class<Enum>) c1ass, value);
        return (A) instance;
    }
}

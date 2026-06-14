import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DumpMethods {
    public static void main(String[] args) throws Exception {
        dump("io.papermc.paper.registry.RegistryBuilderFactory");
        dump("io.papermc.paper.registry.data.dialog.action.DialogAction");
    }

    private static void dump(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        System.out.println("Methods of " + className + ":");
        for (Method m : clazz.getMethods()) {
            System.out.println(Modifier.toString(m.getModifiers()) + " " + m.getReturnType().getSimpleName() + " " + m.getName() + "(");
            for (Class<?> p : m.getParameterTypes()) {
                System.out.println("  " + p.getName());
            }
            System.out.println(")");
        }
    }
}

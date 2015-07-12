package technology.unrelenting.clarke;

import me.qmx.jitescript.JiteClass;
import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static me.qmx.jitescript.util.CodegenUtils.c;
import static org.junit.Assert.assertEquals;

public class CompilerTest {

    final Compiler compiler = new Compiler();

    public static class DynamicClassLoader extends ClassLoader {
        public Class define(JiteClass jiteClass) {
            byte[] classBytes = jiteClass.toBytes();
            return super.defineClass(c(jiteClass.getClassName()), classBytes, 0, classBytes.length);
        }
    }

    private Class eval(String code) throws CompilerException {
        return new DynamicClassLoader().define(compiler.compileClasses("class TestClass; " + code).get(0));
    }

    private DynamicClassLoader evalClasses(String code) throws CompilerException {
        DynamicClassLoader classLoader = new DynamicClassLoader();
        for (JiteClass jiteClass : compiler.compileClasses(code))
            classLoader.define(jiteClass);
        return classLoader;
    }

    @Test public void testArithmetic() throws CompilerException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class testClass = eval("ar ∷ int → int = 2 * 1 -;");
        assertEquals(19, testClass.getMethod("ar", int.class).invoke(null, 10));

        testClass = eval("ar ∷ long → long = 2L * 1 - 10 %;");
        assertEquals(9L, testClass.getMethod("ar", long.class).invoke(null, 10L));

        testClass = eval("ar ∷ int → double = 2.5 * 1.0f 1L - + 10_000 /;");
        assertEquals(0.0025, testClass.getMethod("ar", int.class).invoke(null, 10));

        testClass = eval("ar ∷ long → long = dup * 5 swap - dup pop;");
        assertEquals(-95L, testClass.getMethod("ar", long.class).invoke(null, 10L));
    }

    @Test public void testBooleans() throws CompilerException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class testClass = eval("bo ∷ bool → bool = ¬;");
        assertEquals(true, testClass.getMethod("bo", boolean.class).invoke(null, false));

        testClass = eval("bo ∷ bool bool → bool = ∧;");
        assertEquals(false, testClass.getMethod("bo", boolean.class, boolean.class).invoke(null, false, false));
        assertEquals(false, testClass.getMethod("bo", boolean.class, boolean.class).invoke(null, true, false));
        assertEquals(false, testClass.getMethod("bo", boolean.class, boolean.class).invoke(null, false, true));
        assertEquals(true,  testClass.getMethod("bo", boolean.class, boolean.class).invoke(null, true, true));

        testClass = eval("bo ∷ bool bool → bool = ∨;");
        assertEquals(false, testClass.getMethod("bo", boolean.class, boolean.class).invoke(null, false, false));
        assertEquals(true,  testClass.getMethod("bo", boolean.class, boolean.class).invoke(null, true, false));
        assertEquals(true,  testClass.getMethod("bo", boolean.class, boolean.class).invoke(null, false, true));
        assertEquals(true,  testClass.getMethod("bo", boolean.class, boolean.class).invoke(null, true, true));

        testClass = eval("bo ∷ → bool = 1 dup ==;");
        assertEquals(true, testClass.getMethod("bo").invoke(null));

        testClass = eval("bo ∷ → bool = 2L dup ≠;");
        assertEquals(false, testClass.getMethod("bo").invoke(null));

        testClass = eval("bo ∷ → bool = 2L 3 ≥ ¬;");
        assertEquals(true, testClass.getMethod("bo").invoke(null));

        testClass = eval("bo ∷ boolean bool → bool = ¬ ∨ false ==;");
        assertEquals(true, testClass.getMethod("bo", boolean.class, boolean.class).invoke(null, false, true));
    }

    @Test public void testStaticCalls() throws CompilerException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class testClass = eval("testHello ∷ → java.lang.String = hello; hello ∷ → java.lang.String = \"Hello\";");
        assertEquals("Hello", testClass.getMethod("testHello").invoke(null));

        testClass = eval("readdec ∷ java.lang.String → int = java.lang.Integer.parseInt; readbin ∷ java.lang.String → int = \"2\" readdec java.lang.Integer.parseInt;");
        assertEquals(4,   testClass.getMethod("readbin", String.class).invoke(null, "100"));
        assertEquals(100, testClass.getMethod("readdec", String.class).invoke(null, "100"));
    }

    @Test public void testMultipleClasses() throws CompilerException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        DynamicClassLoader classLoader = evalClasses("class One; hello ∷ → java.lang.String = \"One\"; class Two; hello ∷ → java.lang.String = \"Two\";");
        assertEquals("One", classLoader.loadClass("One").getMethod("hello").invoke(null));
        assertEquals("Two", classLoader.loadClass("Two").getMethod("hello").invoke(null));

        classLoader = evalClasses("class One; hello ∷ → java.lang.String = \"One\"; class Two; hello ∷ → java.lang.String = One.hello;");
        assertEquals("One", classLoader.loadClass("Two").getMethod("hello").invoke(null));

        classLoader = evalClasses("class Two; hello ∷ → java.lang.String = One.hello; class One; hello ∷ → java.lang.String = \"One\";");
        assertEquals("One", classLoader.loadClass("Two").getMethod("hello").invoke(null));
    }

}

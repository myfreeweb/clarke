package technology.unrelenting.clarke;

import me.qmx.jitescript.JiteClass;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Compiler {

    public static List<JiteClass> compileClasses(String input)
        throws CompilerException {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        ClarkeLexer lexer = new ClarkeLexer(inputStream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        ClarkeParser parser = new ClarkeParser(tokenStream);
        ClassGenerator generator = new ClassGenerator();
        parser.addParseListener(generator);
        parser.program();
        return generator.generate();
    }

    public static void main(String[] args) {
        try {
            for (String path : args) {
                for (JiteClass jiteClass : compileClasses(StringUtils.join(Files.readAllLines(Paths.get(path), Charset.defaultCharset()), '\n'))) {
                    Files.write(Paths.get(jiteClass.getClassName() + ".class"), jiteClass.toBytes());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}

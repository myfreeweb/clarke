package technology.unrelenting.clarke;

import me.qmx.jitescript.JiteClass;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

import java.util.List;

public class Compiler {

    public List<JiteClass> compileClasses(String input) {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        ClarkeLexer lexer = new ClarkeLexer(inputStream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        ClarkeParser parser = new ClarkeParser(tokenStream);
        ClassGenerator generator = new ClassGenerator();
        parser.addParseListener(generator);
        parser.program();
        return generator.generate();
    }

}

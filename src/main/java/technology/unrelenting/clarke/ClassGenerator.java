package technology.unrelenting.clarke;

import me.qmx.jitescript.CodeBlock;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.Opcodes;
import me.qmx.jitescript.JiteClass;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static me.qmx.jitescript.util.CodegenUtils.sig;

public class ClassGenerator extends ClarkeBaseListener {

    JiteClass jiteClass;
    Stack<Class> classStack;

    public ClassGenerator(String name) {
        this.jiteClass = new JiteClass(name);
        this.classStack = new Stack<Class>();
    }

    public JiteClass generate() {
//        try {
//            Files.write(Paths.get("TestClass.class"), this.jiteClass.toBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return this.jiteClass;
    }

    private Class resolveType(TerminalNode typeID) {
        String typeName = typeID.getText();
        if (typeName.equals("int"))
            return int.class;
        if (typeName.equals("long"))
            return long.class;
        if (typeName.equals("float"))
            return float.class;
        if (typeName.equals("double"))
            return double.class;
        return null;
    }

    private void compilePushLiteral(CodeBlock block, ClarkeParser.LiteralContext literal) {
        if (literal.IntLiteral() != null) {
            block.ldc(Integer.parseInt(literal.IntLiteral().getSymbol().getText()
                    .replace("_", "")));
            this.classStack.push(int.class);
        } else if (literal.LongLiteral() != null) {
            block.ldc(Long.parseLong(literal.LongLiteral().getSymbol().getText()
                    .replace("_", "").replace("l", "").replace("L", "")));
            this.classStack.push(long.class);
        } else if (literal.FloatLiteral() != null) {
            block.ldc(Float.parseFloat(literal.FloatLiteral().getSymbol().getText()
                    .replace("_", "").replace("f", "").replace("F", "")));
            this.classStack.push(float.class);
        } else if (literal.DoubleLiteral() != null) {
            block.ldc(Double.parseDouble(literal.DoubleLiteral().getSymbol().getText()
                    .replace("_", "").replace("d", "").replace("D", "")));
            this.classStack.push(double.class);
        }
    }


    private Class[] buildSignature(ClarkeParser.DefinitionContext ctx) {
        List<Class> signature = new ArrayList<Class>();
        if (ctx.typeSignature() != null) {
            if (ctx.typeSignature().returnType() != null)
                signature.add(resolveType(ctx.typeSignature().returnType().ID()));
            else
                signature.add(null);
            if (ctx.typeSignature().argTypes() != null) {
                for (TerminalNode typeID : ctx.typeSignature().argTypes().ID()) {
                    Class argClass = resolveType(typeID);
                    signature.add(argClass);
                    this.classStack.push(argClass);
                }
            }
        } else {
            signature.add(null);
        }
        return signature.toArray(new Class[signature.size()]);
    }

    private void compileArgumentsLoad(CodeBlock block, Class[] signature) {
        for (int i = 1; i < signature.length; i++) {
            if (signature[i] == int.class)
                block.iload(i - 1);
            else if (signature[i] == long.class)
                block.lload(i - 1);
            else if (signature[i] == float.class)
                block.fload(i - 1);
            else if (signature[i] == double.class)
                block.dload(i - 1);
        }
    }

    private void compileReturn(CodeBlock block, Class returnType) {
        if (returnType == null)
            block.voidreturn();
        else if (returnType == int.class)
            block.ireturn();
        else if (returnType == long.class)
            block.lreturn();
        else if (returnType == float.class)
            block.freturn();
        else if (returnType == double.class)
            block.dreturn();
    }

    @Override public void exitDefinition(ClarkeParser.DefinitionContext ctx) {
        CodeBlock block = CodeBlock.newCodeBlock();
        Class[] signature = buildSignature(ctx);
        compileArgumentsLoad(block, signature);
        for (ClarkeParser.ExprContext expr : ctx.expr()) {
            if (expr.literal() != null)
                compilePushLiteral(block, expr.literal());
            else if (expr.PrimitiveOperation() != null)
                PrimitiveOperations.compilePrimitiveOperation(block, this.classStack, expr.PrimitiveOperation());
        }
        compileReturn(block, signature[0]);
        this.jiteClass.defineMethod(ctx.ID().getText(),
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                sig(signature), block);
    }

}

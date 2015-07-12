package technology.unrelenting.clarke;

import org.apache.commons.lang3.StringUtils;
import org.antlr.v4.runtime.tree.TerminalNode;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.internal.org.objectweb.asm.Opcodes;
import me.qmx.jitescript.JiteClass;

import java.util.*;

import static me.qmx.jitescript.util.CodegenUtils.sig;

public class ClassGenerator extends ClarkeBaseListener {

    JiteClass jiteClass;
    Stack<Class> classStack;
    String classNameSlashed;
    Map<String, Class[]> currentMethodSignatures;
    final Map<String, Map<String, String>> methodSigCache;
    final List<JiteClass> jiteClasses;

    public ClassGenerator() {
        jiteClasses = new LinkedList<JiteClass>();
        methodSigCache = new HashMap<String, Map<String, String>>();
    }

    public List<JiteClass> generate() {
        return jiteClasses;
    }

    private Class resolveType(ClarkeParser.QualifiedNameContext typeID) {
        String typeName = typeID.getText();
        if (typeName.equals("boolean") || typeName.equals("bool"))
            return boolean.class;
        if (typeName.equals("int"))
            return int.class;
        if (typeName.equals("long"))
            return long.class;
        if (typeName.equals("float"))
            return float.class;
        if (typeName.equals("double"))
            return double.class;
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private void compilePushLiteral(CodeBlock block, ClarkeParser.LiteralContext literal) {
        if (literal.BooleanLiteral() != null) {
            block.pushBoolean(literal.BooleanLiteral().getSymbol().getText().equals("true"));
            this.classStack.push(boolean.class);
        } else if (literal.IntLiteral() != null) {
            block.pushInt(Integer.parseInt(literal.IntLiteral().getSymbol().getText()
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
        } else if (literal.StringLiteral() != null) {
            String s = literal.StringLiteral().getText();
            block.ldc(s.substring(1, s.length() - 1));
            this.classStack.push(String.class);
        }
    }

    private void compileMethodCall(CodeBlock block, ClarkeParser.QualifiedNameContext ctx) {
        // TODO: resolve static methods of other classes
        List<TerminalNode> qualifiedName = ctx.ID();
        if (qualifiedName.size() == 1) {
            String methodName = qualifiedName.get(0).getText();
            if (currentMethodSignatures.containsKey(methodName)) {
                block.invokestatic(this.classNameSlashed, methodName, sig(currentMethodSignatures.get(methodName)));
            }
        } else {
            String methodName = qualifiedName.get(qualifiedName.size() - 1).getText();
            String className = StringUtils.join(qualifiedName.subList(0, qualifiedName.size() - 1), ".");
            if (methodSigCache.containsKey(className)) {
                Map<String, String> methodsOfClass = methodSigCache.get(className);
                if (methodsOfClass.containsKey(methodName)) {
                    block.invokestatic(className.replace('.', '/'), methodName, methodsOfClass.get(methodName));
                }
            }
        }
    }

    private Class[] buildSignature(ClarkeParser.MethodDefinitionContext ctx) {
        List<Class> signature = new ArrayList<Class>();
        if (ctx.typeSignature() != null) {
            if (ctx.typeSignature().returnType() != null)
                signature.add(resolveType(ctx.typeSignature().returnType().qualifiedName()));
            else
                signature.add(null);
            if (ctx.typeSignature().argTypes() != null) {
                for (ClarkeParser.QualifiedNameContext typeName : ctx.typeSignature().argTypes().qualifiedName()) {
                    Class argClass = resolveType(typeName);
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
            if (signature[i] == boolean.class || signature[i] == int.class)
                block.iload(i - 1);
            else if (signature[i] == long.class)
                block.lload(i - 1);
            else if (signature[i] == float.class)
                block.fload(i - 1);
            else if (signature[i] == double.class)
                block.dload(i - 1);
            else
                block.aload(i - 1);
        }
    }

    private void compileReturn(CodeBlock block, Class returnType) {
        if (returnType == null)
            block.voidreturn();
        else if (returnType == boolean.class || returnType == int.class)
            block.ireturn();
        else if (returnType == long.class)
            block.lreturn();
        else if (returnType == float.class)
            block.freturn();
        else if (returnType == double.class)
            block.dreturn();
        else
            block.areturn();
    }

    private void compileMethod(ClarkeParser.MethodDefinitionContext ctx, Class[] signature) {
        CodeBlock block = CodeBlock.newCodeBlock();
        compileArgumentsLoad(block, signature);
        for (ClarkeParser.ExprContext expr : ctx.expr()) {
            if (expr.literal() != null)
                compilePushLiteral(block, expr.literal());
            else if (expr.PrimitiveOperation() != null)
                PrimitiveOperations.compilePrimitiveOperation(block, this.classStack, expr.PrimitiveOperation());
            else if (expr.qualifiedName() != null)
                compileMethodCall(block, expr.qualifiedName());
        }
        compileReturn(block, signature[0]);
        this.jiteClass.defineMethod(ctx.qualifiedName().getText(),
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                sig(signature), block);
    }

    @Override public void exitClassDefinition(ClarkeParser.ClassDefinitionContext ctx) {
        String name = ctx.qualifiedName().getText();
        jiteClass = new JiteClass(name);
        classStack = new Stack<Class>();
        classNameSlashed = name.replace('.', '/');
        currentMethodSignatures = new HashMap<String, Class[]>();
        for (ClarkeParser.MethodDefinitionContext methodCtx : ctx.methodDefinition())
            currentMethodSignatures.put(methodCtx.qualifiedName().getText(), buildSignature(methodCtx));
        for (ClarkeParser.MethodDefinitionContext methodCtx : ctx.methodDefinition())
            compileMethod(methodCtx, currentMethodSignatures.get(methodCtx.qualifiedName().getText()));
        Map<String, String> currentMethodSigs = new HashMap<String, String>();
        for (String methodName : currentMethodSignatures.keySet())
            currentMethodSigs.put(methodName, sig(currentMethodSignatures.get(methodName)));
        methodSigCache.put(name, currentMethodSigs);
//        try {
//            Files.write(Paths.get("TestClass.class"), this.jiteClass.toBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        jiteClasses.add(jiteClass);
    }

}

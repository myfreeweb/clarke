package technology.unrelenting.clarke;

import me.qmx.jitescript.internal.org.objectweb.asm.tree.LabelNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.antlr.v4.runtime.tree.TerminalNode;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.internal.org.objectweb.asm.Opcodes;
import me.qmx.jitescript.JiteClass;

import java.lang.reflect.Method;
import java.util.*;

import static me.qmx.jitescript.util.CodegenUtils.sig;

public class ClassGenerator extends ClarkeBaseListener {

    JiteClass jiteClass;
    Stack<Class> classStack;
    String classNameSlashed;
    final Map<String, Map<String, Class[]>> methodSigCache;
    final List<JiteClass> jiteClasses;
    final Map<String, ClarkeParser.ClassDefinitionContext> classesToCompile;

    public ClassGenerator() {
        jiteClasses = new LinkedList<JiteClass>();
        classesToCompile = new HashMap<String, ClarkeParser.ClassDefinitionContext>();
        methodSigCache = new HashMap<String, Map<String, Class[]>>();
    }

    public List<JiteClass> generate()
        throws CompilerException {
        for (String className : classesToCompile.keySet()) {
            jiteClass = new JiteClass(className);
            classNameSlashed = className.replace('.', '/');
            for (ClarkeParser.MethodDefinitionContext methodCtx : classesToCompile.get(className).methodDefinition())
                compileMethod(methodCtx, methodSigCache.get(className).get(methodCtx.qualifiedName().getText()));
//        try {
//            Files.write(Paths.get("TestClass.class"), jiteClass.toBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
            jiteClasses.add(jiteClass);
        }
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
            classStack.push(boolean.class);
        } else if (literal.IntLiteral() != null) {
            block.pushInt(Integer.parseInt(literal.IntLiteral().getSymbol().getText()
                    .replace("_", "")));
            classStack.push(int.class);
        } else if (literal.LongLiteral() != null) {
            block.ldc(Long.parseLong(literal.LongLiteral().getSymbol().getText()
                    .replace("_", "").replace("l", "").replace("L", "")));
            classStack.push(long.class);
        } else if (literal.FloatLiteral() != null) {
            block.ldc(Float.parseFloat(literal.FloatLiteral().getSymbol().getText()
                    .replace("_", "").replace("f", "").replace("F", "")));
            classStack.push(float.class);
        } else if (literal.DoubleLiteral() != null) {
            block.ldc(Double.parseDouble(literal.DoubleLiteral().getSymbol().getText()
                    .replace("_", "").replace("d", "").replace("D", "")));
            classStack.push(double.class);
        } else if (literal.StringLiteral() != null) {
            String s = literal.StringLiteral().getText();
            block.ldc(s.substring(1, s.length() - 1));
            classStack.push(String.class);
        }
    }

    private void compileControlFlow(CodeBlock block, ClarkeParser.ControlFlowExprContext ctx)
        throws CompilerException {
        LabelNode stopLabel = new LabelNode();
        if (classStack.pop() != boolean.class)
            throw new CompilerException("Can't use control flow on non-boolean objects.");
        if (ctx.ifExpr() != null) {
            LabelNode falseLabel = new LabelNode();
            block.ifeq(falseLabel);
            compileExprs(block, ctx.ifExpr().groupExpr(1).expr());
            block.go_to(stopLabel)
                .label(falseLabel);
            compileExprs(block, ctx.ifExpr().groupExpr(0).expr());
            block.label(stopLabel);
        } else if (ctx.whenExpr() != null) {
            block.ifeq(stopLabel);
            compileExprs(block, ctx.whenExpr().groupExpr().expr());
            block.label(stopLabel);
        } else if (ctx.unlessExpr() != null) {
            block.ifne(stopLabel);
            compileExprs(block, ctx.unlessExpr().groupExpr().expr());
            block.label(stopLabel);
        }
    }

    private void compileCachedStaticMethodCall(CodeBlock block, String slashedClassName, String methodName, Class[] signature) {
        if (paramsMatchStack(ArrayUtils.subarray(signature, 1, signature.length + 1))) {
            block.invokestatic(slashedClassName, methodName, sig(signature));
            classStack.push(signature[0]);
        }
    }

    private void compileReflectedStaticMethodCall(CodeBlock block, Class klass, String methodName) {
        for (Method method : klass.getMethods()) {
            if (method.getName().equals(methodName) && (method.getModifiers() & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
                Class[] paramTypes = method.getParameterTypes();
                if (paramsMatchStack(paramTypes)) {
                    block.invokestatic(klass.getCanonicalName().replace('.', '/'), methodName,
                            sig(ArrayUtils.add(paramTypes, 0, method.getReturnType())));
                    classStack.push(method.getReturnType());
                    break;
                }
            }
        }
    }

    private void compileMethodCall(CodeBlock block, ClarkeParser.QualifiedNameContext ctx)
        throws CompilerException {
        List<TerminalNode> qualifiedName = ctx.ID();
        String methodName = qualifiedName.get(qualifiedName.size() - 1).getText();
        String className;
        if (qualifiedName.size() == 1)
            className = jiteClass.getClassName();
        else
            className = StringUtils.join(qualifiedName.subList(0, qualifiedName.size() - 1), ".");
        if (methodSigCache.containsKey(className)) {
            Map<String, Class[]> methodsOfClass = methodSigCache.get(className);
            if (methodsOfClass.containsKey(methodName))
                compileCachedStaticMethodCall(block, className.replace('.', '/'), methodName, methodsOfClass.get(methodName));
        } else {
            try {
                Class klass = ClassLoader.getSystemClassLoader().loadClass(className);
                compileReflectedStaticMethodCall(block, klass, methodName);
            } catch (ClassNotFoundException ex) {
                throw new CompilerException("Could not find class " + className + " on the classpath.", ex);
            }
        }
    }

    private boolean paramsMatchStack(Class[] paramTypes) {
        if (paramTypes.length == 0)
            return true;
        if (classStack.size() < paramTypes.length)
            return false;
        List<Class> poppedClasses = new LinkedList<Class>();
        for (int i = paramTypes.length - 1; i >= 0; i--) {
            Class currentStackClass = classStack.pop();
            poppedClasses.add(currentStackClass);
            if (!paramTypes[i].equals(currentStackClass)) {
                for (Class klass : poppedClasses)
                    classStack.push(klass);
                return false;
            }
        }
        return true;
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

    private void compileExprs(CodeBlock block, Collection<ClarkeParser.ExprContext> exprs)
        throws CompilerException {
        for (ClarkeParser.ExprContext expr : exprs) {
            if (expr.literal() != null)
                compilePushLiteral(block, expr.literal());
            else if (expr.PrimitiveOperation() != null)
                PrimitiveOperations.compilePrimitiveOperation(block, classStack, expr.PrimitiveOperation());
            else if (expr.controlFlowExpr() != null)
                compileControlFlow(block, expr.controlFlowExpr());
            else if (expr.qualifiedName() != null)
                compileMethodCall(block, expr.qualifiedName());
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

    private void compileMethod(ClarkeParser.MethodDefinitionContext ctx, Class[] signature)
        throws CompilerException {
        classStack = new Stack<Class>();
        for (Class argClass : ArrayUtils.subarray(signature, 0, signature.length))
            classStack.push(argClass);
        CodeBlock block = CodeBlock.newCodeBlock();
        compileArgumentsLoad(block, signature);
        compileExprs(block, ctx.expr());
        compileReturn(block, signature[0]);
        jiteClass.defineMethod(ctx.qualifiedName().getText(),
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                sig(signature), block);
    }

    @Override public void exitClassDefinition(ClarkeParser.ClassDefinitionContext ctx) {
        String name = ctx.qualifiedName().getText();
        Map<String, Class[]> methodSignatures = new HashMap<String, Class[]>();
        for (ClarkeParser.MethodDefinitionContext methodCtx : ctx.methodDefinition())
            methodSignatures.put(methodCtx.qualifiedName().getText(), buildSignature(methodCtx));
        methodSigCache.put(name, methodSignatures);
        classesToCompile.put(name, ctx);
    }

}

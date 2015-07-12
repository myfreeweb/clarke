package technology.unrelenting.clarke;

import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.internal.org.objectweb.asm.tree.LabelNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Stack;

public class PrimitiveOperations {

    public static boolean isTwoSlot(Class klass) {
        return klass == long.class || klass == double.class;
    }

    public static boolean isNumeric(Class klass) {
        return klass == boolean.class || klass == int.class || klass == long.class
            || klass == float.class || klass == double.class;
    }

    public static void compileSwap(CodeBlock block, Class upper, Class lower) {
        // https://stackoverflow.com/questions/11340330/java-bytecode-swap-for-double-and-long-values
        boolean upperTwoSlots = isTwoSlot(upper);
        boolean lowerTwoSlots = isTwoSlot(lower);
        if (!upperTwoSlots && !lowerTwoSlots)
            block.swap();
        else if (!upperTwoSlots && lowerTwoSlots)
            block.dup_x2().pop();
        else if (upperTwoSlots && !lowerTwoSlots)
            block.dup2_x1().pop2();
        else if (upperTwoSlots && lowerTwoSlots)
            block.dup2_x2().pop2();
    }

    public static void compileSwap(CodeBlock block, Stack<Class> classStack) {
        Class upper = classStack.pop();
        Class lower = classStack.pop();
        compileSwap(block, upper, lower);
        classStack.push(upper);
        classStack.push(lower);
    }

    public static void compileDup(CodeBlock block, Stack<Class> classStack) {
        Class upper = classStack.pop();
        if (isTwoSlot(upper))
            block.dup2();
        else
            block.dup();
        classStack.push(upper);
        classStack.push(upper);
    }

    public static void compilePop(CodeBlock block, Stack<Class> classStack) {
        Class upper = classStack.pop();
        if (isTwoSlot(upper))
            block.pop2();
        else
            block.pop();
    }

    public static void compilePrintln(CodeBlock block, Stack<Class> classStack) {
        Class upper = classStack.pop();
        if (upper == int.class)
            block.iprintln();
        else
            block.aprintln();
    }

    public static Class castNumericTypes(CodeBlock block, Stack<Class> classStack) {
        Class rightOperandType = classStack.pop();
        Class leftOperandType = classStack.pop();
        Class result = castNumericTypes(block, rightOperandType, leftOperandType);
        classStack.push(result);
        return result;
    }

    public static Class castNumericTypes(CodeBlock block, Class rightOperandType, Class leftOperandType) {
        if (leftOperandType == rightOperandType) {
            return leftOperandType;
        } else if (leftOperandType == long.class && rightOperandType == int.class) {
            block.i2l();
            return long.class;
        } else if (leftOperandType == int.class && rightOperandType == long.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.i2l();
            compileSwap(block, rightOperandType, rightOperandType);
            return long.class;
        } else if (leftOperandType == float.class && rightOperandType == int.class) {
            block.i2f();
            return float.class;
        } else if (leftOperandType == int.class && rightOperandType == float.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.i2f();
            compileSwap(block, rightOperandType, rightOperandType);
            return float.class;
        } else if (leftOperandType == float.class && rightOperandType == long.class) {
            block.l2f();
            return float.class;
        } else if (leftOperandType == long.class && rightOperandType == float.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.l2f();
            compileSwap(block, rightOperandType, rightOperandType);
            return float.class;
        } else if (leftOperandType == double.class && rightOperandType == int.class) {
            block.i2d();
            return double.class;
        } else if (leftOperandType == int.class && rightOperandType == double.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.i2d();
            compileSwap(block, rightOperandType, rightOperandType);
            return double.class;
        } else if (leftOperandType == double.class && rightOperandType == long.class) {
            block.l2d();
            return double.class;
        } else if (leftOperandType == long.class && rightOperandType == double.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.l2d();
            compileSwap(block, rightOperandType, rightOperandType);
            return double.class;
        } else if (leftOperandType == double.class && rightOperandType == float.class) {
            block.f2d();
            return double.class;
        } else if (leftOperandType == float.class && rightOperandType == double.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.f2d();
            compileSwap(block, rightOperandType, rightOperandType);
            return double.class;
        }
        return null;
    }

    public static void compileNumericOperation(CodeBlock block, Stack<Class> classStack, String op) {
        Class operandsClass = castNumericTypes(block, classStack);

        if (op.equals("+")) {
            if (operandsClass == int.class)
                block.iadd();
            else if (operandsClass == long.class)
                block.ladd();
            else if (operandsClass == float.class)
                block.fadd();
            else if (operandsClass == double.class)
                block.dadd();
        } else if (op.equals("-")) {
            if (operandsClass == int.class)
                block.isub();
            else if (operandsClass == long.class)
                block.lsub();
            else if (operandsClass == float.class)
                block.fsub();
            else if (operandsClass == double.class)
                block.dsub();
        } else if (op.equals("*")) {
            if (operandsClass == int.class)
                block.imul();
            else if (operandsClass == long.class)
                block.lmul();
            else if (operandsClass == float.class)
                block.fmul();
            else if (operandsClass == double.class)
                block.dmul();
        } else if (op.equals("/")) {
            if (operandsClass == int.class)
                block.idiv();
            else if (operandsClass == long.class)
                block.ldiv();
            else if (operandsClass == float.class)
                block.fdiv();
            else if (operandsClass == double.class)
                block.ddiv();
        } else if (op.equals("%")) {
            if (operandsClass == int.class)
                block.irem();
            else if (operandsClass == long.class)
                block.lrem();
            else if (operandsClass == float.class)
                block.frem();
            else if (operandsClass == double.class)
                block.drem();
        }
    }

    public static void compileBooleanOperation(CodeBlock block, Stack<Class> classStack, String op)
        throws CompilerException {
        Class upper = classStack.pop();
        if (op.equals("¬")) {
            // if (upper != boolean.class)
            //     throw new Exception("Can't apply logical NOT to a non-boolean");
            LabelNode stopLabel = new LabelNode();
            LabelNode trueLabel = new LabelNode();
            block.ifeq(trueLabel)
                .iconst_0()
                .go_to(stopLabel)
                .label(trueLabel)
                .iconst_1()
                .label(stopLabel);
            classStack.push(boolean.class);
            return;
        }
        Class lower = classStack.pop();
        // ∧ and ∨ are a bit more complicated than what javac generates
        // because we always have both operands already on the stack
        // whereas javac can skip some loading and avoid dealing with the pop
        if (op.equals("∧")) {
            if (upper != boolean.class || lower != boolean.class)
                throw new CompilerException("Can't apply logical AND to a non-boolean");
            LabelNode stopLabel = new LabelNode();
            LabelNode falseLabel = new LabelNode();
            LabelNode falseLabel1 = new LabelNode();
            block.ifeq(falseLabel1)
                .ifeq(falseLabel)
                .iconst_1()
                .go_to(stopLabel)
                .label(falseLabel1)
                .pop()
                .label(falseLabel)
                .iconst_0()
                .label(stopLabel);
            classStack.push(boolean.class);
        } else if (op.equals("∨")) {
            if (upper != boolean.class || lower != boolean.class)
                throw new CompilerException("Can't apply logical OR to a non-boolean");
            LabelNode stopLabel = new LabelNode();
            LabelNode trueLabel = new LabelNode();
            LabelNode trueLabel1 = new LabelNode();
            LabelNode falseLabel = new LabelNode();
            block.ifne(trueLabel1)
                .ifeq(falseLabel)
                .go_to(trueLabel)
                .label(trueLabel1)
                .pop()
                .label(trueLabel)
                .iconst_1()
                .go_to(stopLabel)
                .label(falseLabel)
                .iconst_0()
                .label(stopLabel);
            classStack.push(boolean.class);
        } else if (op.equals("==") || op.equals("≠") || op.equals("<") || op.equals(">")
                || op.equals("≤") || op.equals("≥")) {
            LabelNode falseLabel = new LabelNode();
            LabelNode stopLabel = new LabelNode();
            if (isNumeric(upper) && isNumeric(lower)) {
                Class numClass = castNumericTypes(block, upper, lower);
                if (numClass == int.class) { // WHY
                    if (op.equals("==") || op.equals("≠"))
                        block.if_icmpne(falseLabel);
                    else if (op.equals("<"))
                        block.if_icmpge(falseLabel);
                    else if (op.equals("≤"))
                        block.if_icmpgt(falseLabel);
                    else if (op.equals(">"))
                        block.if_icmple(falseLabel);
                    else if (op.equals("≥"))
                        block.if_icmplt(falseLabel);
                } else {
                    if (numClass == long.class)
                        block.lcmp();
                    else if (numClass == float.class)
                        block.fcmpl();
                    else if (numClass == double.class)
                        block.dcmpl();
                    if (op.equals("==") || op.equals("≠"))
                        block.ifne(falseLabel);
                    else if (op.equals("<"))
                        block.ifge(falseLabel);
                    else if (op.equals("≤"))
                        block.ifgt(falseLabel);
                    else if (op.equals(">"))
                        block.ifle(falseLabel);
                    else if (op.equals("≥"))
                        block.iflt(falseLabel);
                }
            } else {
                block.if_acmpne(falseLabel);
            }
            if (op.equals("≠"))
                block.iconst_0();
            else
                block.iconst_1();
            block.go_to(stopLabel).label(falseLabel);
            if (op.equals("≠"))
                block.iconst_1();
            else
                block.iconst_0();
            block.label(stopLabel);
        }
        classStack.push(boolean.class);
    }

    public static void compilePrimitiveOperation(CodeBlock block, Stack<Class> classStack, TerminalNode operation)
        throws CompilerException {
        String op = operation.getSymbol().getText();
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")
            || op.equals("%"))
            compileNumericOperation(block, classStack, op);
        else if (op.equals("¬") || op.equals("∧") || op.equals("∨") || op.equals("==")
            || op.equals("≠") || op.equals("<") || op.equals(">")
            || op.equals("≤") || op.equals("≥"))
            compileBooleanOperation(block, classStack, op);
        else if (op.equals("dup"))
            compileDup(block, classStack);
        else if (op.equals("swap"))
            compileSwap(block, classStack);
        else if (op.equals("pop"))
            compilePop(block, classStack);
        else if (op.equals("println"))
            compilePrintln(block, classStack);
    }

}

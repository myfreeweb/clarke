package technology.unrelenting.clarke;

import me.qmx.jitescript.CodeBlock;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Stack;

public class PrimitiveOperations {

    public static void compileSwap(CodeBlock block, Class upper, Class lower) {
        // https://stackoverflow.com/questions/11340330/java-bytecode-swap-for-double-and-long-values
        boolean upperTwoSlots = upper == long.class || upper == double.class;
        boolean lowerTwoSlots = lower == long.class || lower == double.class;
        if (!upperTwoSlots && !lowerTwoSlots) {
            block.swap();
        } else if (!upperTwoSlots && lowerTwoSlots) {
            block.dup_x2();
            block.pop();
        } else if (upperTwoSlots && !lowerTwoSlots) {
            block.dup2_x1();
            block.pop2();
        } else if (upperTwoSlots && lowerTwoSlots) {
            block.dup2_x2();
            block.pop2();
        }
    }

    public static Class castNumericTypes(CodeBlock block, Stack<Class> classStack) {
        Class rightOperandType = classStack.pop();
        Class leftOperandType = classStack.pop();
        if (leftOperandType == rightOperandType) {
            classStack.push(leftOperandType);
            return leftOperandType;
        } else if (leftOperandType == long.class && rightOperandType == int.class) {
            block.i2l();
            classStack.push(long.class);
            return long.class;
        } else if (leftOperandType == int.class && rightOperandType == long.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.i2l();
            compileSwap(block, rightOperandType, rightOperandType);
            classStack.push(long.class);
            return long.class;
        } else if (leftOperandType == float.class && rightOperandType == int.class) {
            block.i2f();
            classStack.push(float.class);
            return float.class;
        } else if (leftOperandType == int.class && rightOperandType == float.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.i2f();
            compileSwap(block, rightOperandType, rightOperandType);
            classStack.push(float.class);
            return float.class;
        } else if (leftOperandType == float.class && rightOperandType == long.class) {
            block.l2f();
            classStack.push(float.class);
            return float.class;
        } else if (leftOperandType == long.class && rightOperandType == float.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.l2f();
            compileSwap(block, rightOperandType, rightOperandType);
            classStack.push(float.class);
            return float.class;
        } else if (leftOperandType == double.class && rightOperandType == int.class) {
            block.i2d();
            classStack.push(double.class);
            return double.class;
        } else if (leftOperandType == int.class && rightOperandType == double.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.i2d();
            compileSwap(block, rightOperandType, rightOperandType);
            classStack.push(double.class);
            return double.class;
        } else if (leftOperandType == double.class && rightOperandType == long.class) {
            block.l2d();
            classStack.push(double.class);
            return double.class;
        } else if (leftOperandType == long.class && rightOperandType == double.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.l2d();
            compileSwap(block, rightOperandType, rightOperandType);
            classStack.push(double.class);
            return double.class;
        } else if (leftOperandType == double.class && rightOperandType == float.class) {
            block.f2d();
            classStack.push(double.class);
            return double.class;
        } else if (leftOperandType == float.class && rightOperandType == double.class) {
            compileSwap(block, rightOperandType, leftOperandType);
            block.f2d();
            compileSwap(block, rightOperandType, rightOperandType);
            classStack.push(double.class);
            return double.class;
        }
        return null;
    }

    public static void compilePrimitiveOperation(CodeBlock block, Stack<Class> classStack, TerminalNode operation) {
        String op = operation.getSymbol().getText();
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
        }
    }
}

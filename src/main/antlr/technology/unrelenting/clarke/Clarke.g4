grammar Clarke;

options {
    language = Java;
}

program : classDefinition* EOF ;

classDefinition : 'class' qualifiedName ';' methodDefinition+ ;

methodDefinition : qualifiedName ( '∷' typeSignature )? '=' expr+ ';' ;

typeSignature : argTypes ( '→' returnType )? ;

argTypes : typeName*? ;

returnType : typeName ;

typeName : arrayTypeName | qualifiedName ;

arrayTypeName : qualifiedName '[]' ;

expr : controlFlowExpr | loopExpr | PrimitiveOperation | literal | qualifiedName ;

controlFlowExpr : ifExpr | whenExpr | unlessExpr ;

ifExpr : groupExpr groupExpr 'if' ;

whenExpr : groupExpr 'when' ;

unlessExpr : groupExpr 'unless' ;

loopExpr : whileExpr ;

whileExpr : groupExpr groupExpr 'while' ;

groupExpr : '{' expr+ '}' ;


PrimitiveOperation
    : '+' | '-' | '*' | '/' | '%'
    | '¬' | '∧' | '∨' | '==' | '≠' | '<' | '>' | '≤' | '≥'
    | 'dup' | 'swap' | 'pop' | 'over' | 'println' ;


literal : BooleanLiteral | IntLiteral | LongLiteral | FloatLiteral | DoubleLiteral | StringLiteral ;

BooleanLiteral : 'true' | 'false' ;

IntLiteral : NumericLiteral ;

LongLiteral : NumericLiteral ( 'l' | 'L' ) ;

FloatLiteral : NumericLiteral '.' NumericLiteral ( 'f' | 'F' ) ;

DoubleLiteral : NumericLiteral '.' NumericLiteral ( 'd' | 'D' )? ;

NumericLiteral : NumericChar+ ;

fragment NumericChar
    : [0-9]
    | '_' ;

StringLiteral : '"' ( '\\"' | . )*? '"' ;


qualifiedName : ID ('.' ID)* ;

ID : JavaLetter JavaLetterOrDigit* ; // https://github.com/antlr/grammars-v4/blob/master/java/Java.g4

fragment JavaLetter
    : [a-zA-Z$_] // these are the "java letters" below 0xFF
    | // covers all characters above 0xFF which are not a surrogate
      ~[\u0000-\u00FF\uD800-\uDBFF]
      {Character.isJavaIdentifierStart(_input.LA(-1))}?
    | // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
      [\uD800-\uDBFF] [\uDC00-\uDFFF]
      {Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}? ;

fragment JavaLetterOrDigit
    : [a-zA-Z0-9$_] // these are the "java letters or digits" below 0xFF
    | // covers all characters above 0xFF which are not a surrogate
      ~[\u0000-\u00FF\uD800-\uDBFF]
      {Character.isJavaIdentifierPart(_input.LA(-1))}?
    | // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
      [\uD800-\uDBFF] [\uDC00-\uDFFF]
      {Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}? ;

WS : [ \t\n\r]+ -> skip ;

MULTI_LINE_COMMENT : '/*' .*? '*/' -> skip ;

ONE_LINE_COMMENT : '//' ~[\r\n]* -> skip ;
grammar Clarke;

options {
    language = Java;
}

program : definition* EOF ;

definition : ID ( ( '::' | '∷' ) typeSignature )? '=' expr+ ';' ;

typeSignature : argTypes ( ( '->' | '→') returnType )? ;

argTypes : ID*? ;

returnType : ID ;

expr : literal | ID | PrimitiveOperation ;

PrimitiveOperation : '+' | '-' | '*' | '/' ;


literal : IntLiteral | LongLiteral | FloatLiteral | DoubleLiteral | StringLiteral ;

StringLiteral : '"' ( '\\"' | . )*? '"' ;

IntLiteral : NumericLiteral ;

LongLiteral : NumericLiteral ( 'l' | 'L' ) ;

FloatLiteral : NumericLiteral '.' NumericLiteral ( 'f' | 'F' ) ;

DoubleLiteral : NumericLiteral '.' NumericLiteral ( 'd' | 'D' )? ;

NumericLiteral : NumericChar+ ;

fragment NumericChar
    : [0-9]
    | '_' ;


ID : IDChar+ ;

fragment IDChar // https://github.com/antlr/grammars-v4/blob/master/swift/Swift.g4
    : [0-9] | '.' | [\u0300-\u036F] | [\u1DC0-\u1DFF] | [\u20D0-\u20FF] | [\uFE20-\uFE2F]
    | [a-zA-Z] | '\u00A8' | '\u00AA' | '\u00AD' | '\u00AF' | [\u00B2-\u00B5] | [\u00B7-\u00BA]
    | [\u00BC-\u00BE] | [\u00C0-\u00D6] | [\u00D8-\u00F6] | [\u00F8-\u00FF]
    | [\u0100-\u02FF] | [\u0370-\u167F] | [\u1681-\u180D] | [\u180F-\u1DBF]
    | [\u1E00-\u1FFF]
    | [\u200B-\u200D] | [\u202A-\u202E] | [\u203F-\u2040] | '\u2054' | [\u2060-\u206F]
    | [\u2070-\u20CF] | [\u2100-\u218F] | [\u2460-\u24FF] | [\u2776-\u2793]
    | [\u2C00-\u2DFF] | [\u2E80-\u2FFF]
    | [\u3004-\u3007] | [\u3021-\u302F] | [\u3031-\u303F] | [\u3040-\uD7FF]
    | [\uF900-\uFD3D] | [\uFD40-\uFDCF] | [\uFDF0-\uFE1F] | [\uFE30-\uFE44]
    | [\uFE47-\uFFFD] ;

WS : [ \t\n\r]+ -> skip ;

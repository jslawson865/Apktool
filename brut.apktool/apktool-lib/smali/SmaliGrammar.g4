grammar SmaliGrammar;

file
    :   (methodDecl | otherLine)* EOF
    ;

methodDecl
    :   METHOD_DIRECTIVE headerLine body* END_METHOD_DIRECTIVE
    ;

headerLine
    :   (~'\n')* '\n'
    ;

body
    :   registerDecl
    |   labelDecl
    |   instructionLine
    ;

registerDecl
    :   (LOCALS_DIRECTIVE | REGISTERS_DIRECTIVE) NUMBER '\n'
    ;

labelDecl
    :   LABEL '\n'
    ;

instructionLine
    :   (~'\n')* '\n'
    ;

METHOD_DIRECTIVE
    :   '.method'
    ;

END_METHOD_DIRECTIVE
    :   '.end method'
    ;

LOCALS_DIRECTIVE
    :   '.locals'
    ;

REGISTERS_DIRECTIVE
    :   '.registers'
    ;

LABEL
    :   ':' [a-zA-Z0-9_\-$]+
    ;

NUMBER
    :   [0-9]+
    ;

WS
    :   [ \t\r\n]+ -> skip
    ;

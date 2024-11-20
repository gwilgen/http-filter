grammar Filter;

@header
{
// same folder in pom
package org.gbm.filter;
}

expression
    : '(' expression ')'                            #parenthesis
    | 'not' expression                              #negation
    | expression ('and'|'or') expression            #operation
    | arg ('eq'|'ne'|'lt'|'gt'|'le'|'ge'|'in') arg  #comparation
    ;


arg
    : ID '(' arg (',' arg)* ')'                     #function
    | ID                                            #field
    | STRING                                        #text
    | NUM                                           #number
;

// LEXER

ID     : ('a'..'z'|'A'..'Z')+;
STRING : '"' (' '..'~')+? '"';
NUM    : '-'?'0'..'9'+([.]'0'..'9')?;
WS     : [ \t\n\r]+ -> skip ;
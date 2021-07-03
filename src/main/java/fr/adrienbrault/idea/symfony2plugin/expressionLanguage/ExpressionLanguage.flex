package fr.adrienbrault.idea.symfony2plugin.expressionLanguage;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static fr.adrienbrault.idea.symfony2plugin.expressionLanguage.psi.ExpressionLanguageTypes.*;

%%

%{
  public ExpressionLanguageLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class ExpressionLanguageLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

SPACE=[ \t\n\x0B\f\r]+
NUMBER=[0-9]+(\.[0-9]*)?([Ee][+\-][0-9]+)?
NULL=NULL|null
TRUE=TRUE|true
FALSE=FALSE|false
ID=[:letter:][a-zA-Z_0-9]*
STRING=('([^'\\]|\\.)*'|\"([^\"\\]|\\.)*\")
SYNTAX=[?:.,]

%%
<YYINITIAL> {
  {WHITE_SPACE}      { return WHITE_SPACE; }

  "||"               { return OP_OR; }
  "or"               { return OP_OR_KW; }
  "&&"               { return OP_AND; }
  "and"              { return OP_AND_KW; }
  "|"                { return OP_BIT_OR; }
  "^"                { return OP_BIT_XOR; }
  "&"                { return OP_BIT_AND; }
  "==="              { return OP_IDENTICAL; }
  "=="               { return OP_EQ; }
  "!=="              { return OP_NOT_IDENTICAL; }
  "!="               { return OP_NEQ; }
  "<"                { return OP_LT; }
  ">"                { return OP_GT; }
  ">="               { return OP_GTE; }
  "<="               { return OP_LTE; }
  "not in"           { return OP_NOT_IN; }
  "in"               { return OP_IN; }
  "matches"          { return OP_MATCHES; }
  ".."               { return OP_RANGE; }
  "+"                { return OP_PLUS; }
  "-"                { return OP_MINUS; }
  "~"                { return OP_CONCAT; }
  "*"                { return OP_MUL; }
  "/"                { return OP_DIV; }
  "%"                { return OP_MOD; }
  "**"               { return OP_POW; }
  "!"                { return OP_NOT; }
  "not"              { return OP_NOT_KW; }
  "("                { return L_ROUND_BRACKET; }
  ")"                { return R_ROUND_BRACKET; }
  "{"                { return L_CURLY_BRACKET; }
  "}"                { return R_CURLY_BRACKET; }
  "["                { return L_SQUARE_BRACKET; }
  "]"                { return R_SQUARE_BRACKET; }

  {SPACE}            { return SPACE; }
  {NUMBER}           { return NUMBER; }
  {NULL}             { return NULL; }
  {TRUE}             { return TRUE; }
  {FALSE}            { return FALSE; }
  {ID}               { return ID; }
  {STRING}           { return STRING; }
  {SYNTAX}           { return SYNTAX; }

}

[^] { return BAD_CHARACTER; }

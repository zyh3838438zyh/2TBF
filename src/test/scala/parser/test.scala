package ttbf.parser
import org.scalatest.funsuite._

class ParserSuite extends AnyFunSuite {
  import ttbf.common.ast._
  test("var declarations") {
    val fn = (str: String, ast: ASTVarDecls) => assert(TTBFParser.parse(TTBFParser.astVarDecls, str).get == ast)
    fn("""var a:integer;""", List(ASTVarDecl(ASTId("a"), ASTInt)))
    fn(
      """var a,b:integer;""",
      List(ASTVarDecl(ASTId("a"), ASTInt), ASTVarDecl(ASTId("b"), ASTInt))
    )
    fn(
      """var a:integer;b:integer;""",
      List(ASTVarDecl(ASTId("a"), ASTInt), ASTVarDecl(ASTId("b"), ASTInt))
    )
  }
  test("expressions") {
    val fn = (str: String, ast: ASTAsg) => assert(TTBFParser.parse(TTBFParser.astAsg, str).get == ast)
    fn("a:=a;", ASTAsg(ASTVar(ASTId("a")), ASTVar(ASTId("a"))))
    fn(
      "a:=a+b;",
      ASTAsg(
        ASTVar(ASTId("a")),
        ASTPlus(ASTVar(ASTId("a")), ASTVar(ASTId("b")))
      )
    )
  }
  test("parse a+b program") {
    val prog = """
program aplusb;
var
    a, b: integer;
    c: integer;
begin
    read(a);
    read(b);
    a:=a+b;
    write(a+b);
end.
"""
    val ast = ASTProg(
      List(
        ASTVarDecl(ASTId("a"), ASTInt),
        ASTVarDecl(ASTId("b"), ASTInt),
        ASTVarDecl(ASTId("c"), ASTInt)
      ),
      List(),
      List(
        ASTRead(ASTVar(ASTId("a"))),
        ASTRead(ASTVar(ASTId("b"))),
        ASTAsg(
          ASTVar(ASTId("a")),
          ASTPlus(ASTVar(ASTId("a")), ASTVar(ASTId("b")))
        ),
        ASTWrite(ASTPlus(ASTVar(ASTId("a")), ASTVar(ASTId("b"))))
      )
    )

    val fn = (str: String, ast: ASTProg) => assert(TTBFParser.parse(TTBFParser.astProg, str).get == ast)
    fn(prog, ast)
  }
  test("parse subrt") {
    val prog = """
procedure aplusb(a,b: integer, var c,d: integer);
var
    e,f: integer;
begin
    e:=a+b;
    f:=c+d;
    d:=a-(b+c)-d-e-f;
end;"""
    TTBFParser.parse(TTBFParser.astSubrt, prog).get
  }
}

class CodegenSuite extends AnyFunSuite {
  import ttbf.codegen._
  import ttbf.common.ast._
  import ttbf.common.instrm._

  test("genExpr at global level") {
    val decl      = "var a,b: integer;"
    val globalEnv = env.declsToGlobalEnv(TTBFParser.parse(TTBFParser.astVarDecls, decl).get)
    val localEnv  = env.LocalEnv(Map())

    val expr = ASTPlus(ASTMinus(ASTVar(ASTId("a")), ASTVar(ASTId("b"))), ASTVar(ASTId("a")))
    // println {
    //   InstrM.getInstrList(Codegen.genExpr(expr)(localEnv, globalEnv))
    // }
  }

  test("genStmt Assignment at global level") {
//     val prog = """var a,b: integer;
// BEGIN
// a:=1;
// b:=2;
// a:=a+b+a-b;
// END."""
    val prog    = """var a,b: integer;
BEGIN
read(a);
read(b);
write(a+b);
END."""
    val astProg = TTBFParser.parse(TTBFParser.astProg, prog).get
    InstrM.getInstrList(Codegen.genProg(astProg))
  }
  test("declaring some procedure and functions") {
//     val prog = """
// var n:integer;
// function f(n:integer):integer;
// begin
// if n then
//   if n - 1 then
//     f := f(n-1) + f(n-2);
//   else
//     f := 1;
// else
//     f := 1;
// end;
// begin
// read(n);
// write(f(n));
// end."""
//     val prog = """
// var a,b,c,n:integer;
// begin
// read(n);
// a:=0;
// b:=1;
// c:=1;
// while n do begin
// c:=a+b;
// a:=b;
// b:=c;
// write(c);
// n:=n-1;
// end;
// write(c);
// end.
// """
    val prog = """
    var 
    i:integer;a: array [0..100] of integer;
    begin
    a[0]:=1;
    a[1]:=1;
    i:=2;
    while 100 - i do begin
      a[i] := a[i-1]+a[i-2];
      i := i + 1;
    end;
    write(a[99]);
    end."""
    val astProg = TTBFParser.parse(TTBFParser.astProg, prog)
    // println(TTBFParser.parse(TTBFParser.astExpra, "f(1)"));
    println(astProg)
    println {
      InstrM.getInstrs(Codegen.genProg(astProg.get))
    }
  }
}

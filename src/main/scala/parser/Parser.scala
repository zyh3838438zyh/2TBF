package ttbf.parser

import scala.util.parsing.combinator._
import ttbf.common.ast._

object TTBFParser extends RegexParsers {
  override def skipWhitespace = true
  def astConst: Parser[Int] =
    """(\+|\-)?(0|[1-9]\d*)""".r ^^ { str =>
        str.toInt
      }
  private class CaseinsensitiveLifter(str: String) {
    def ic: Parser[String] = ("""(?i)\Q""" + str + """\E""").r
    // def ic: Parser[String] = str.r
  }
  import scala.language.implicitConversions
  private implicit def liftString(str: String): CaseinsensitiveLifter = new CaseinsensitiveLifter(str)

  def astId: Parser[ASTId] =
    "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { id =>
        ASTId(id.intern())
      };
  def astProg: Parser[ASTProg] =
    ("program".ic ~ astId ~ ";").? ~>
        astVarDecls.? ~
        rep(astSubrt) ~
        astBlockDot ^^ {
        case globalVars ~ subrts ~ mainBody =>
          ASTProg(globalVars.getOrElse(Nil), subrts, mainBody)
      }

  def astVarDecls: Parser[ASTVarDecls] =
    ("var".ic ~> rep1(astVarDeclClause)).? ^^ {
        case Some(cls) => cls.flatten
        case None      => Nil
      }
  def astVarDeclClause: Parser[List[ASTVarDecl]] =
    repsep(astId, ",") ~ ":" ~ astVarType <~ ";" ^^ {
        case ids ~ _ ~ valType =>
          ids.map(id => ASTVarDecl(id, valType))
      }
  def astVarType: Parser[ASTVarType] =
    astBaseVarType |
        ("array".ic ~ "[" ~ astConst ~ ".." ~ astConst ~ "]" ~ "of".ic ~ astBaseVarType ^^ {
            case _ ~ "[" ~ lb ~ ".." ~ ub ~ "]" ~ _ ~ varType =>
              ASTArr(lb, ub, varType)
          })
  def astBaseVarType: Parser[ASTBaseVarType] =
    "integer".ic ^^^ ASTInt |
        "char".ic ^^^ ASTChar
  def astSubrt: Parser[ASTSubrt] =
    astProc | astFun
  def astProc: Parser[ASTProc] =
    "procedure".ic ~ astId ~ "(" ~ repsep(astParam, ",") ~ ")" ~ ";" ~ astSubrtBody ^^ {
        case _ ~ procName ~ _ ~ params ~ _ ~ _ ~ subrt => {
          val actualParams = params.flatten
          ASTProc(procName, actualParams, subrt._1, subrt._2)
        }
      }
  def astFun: Parser[ASTFun] =
    "function".ic ~ astId ~
        "(" ~ repsep(astParam, ",") ~ ")" ~ ":" ~ astBaseVarType ~ ";" ~ astSubrtBody ^^ {
        case _ ~ procName ~ _ ~ params ~ _ ~ _ ~ retType ~ _ ~ subrt => {
          val actualParams = params.flatten
          ASTFun(procName, actualParams, subrt._1, subrt._2, retType)
        }
      }
  def astSubrtBody: Parser[(ASTVarDecls, List[ASTStmt])] =
    astVarDecls.? ~ astBlockSemicolon ^^ {
        case varDecls ~ stmts => (varDecls.getOrElse(Nil), stmts)
      }
  def astParam: Parser[List[ASTVarDecl]] =
    "var".ic.? ~ repsep(astId, ",") ~ ":" ~ astVarType ^^ {
        case varModifier ~ varNames ~ _ ~ varType => {
          val isRef = varModifier.isDefined
          varNames.map(
            varName =>
              if (isRef)
                new ASTVarDecl(varName, varType) with PassByReference
              else
                ASTVarDecl(varName, varType)
          )
        }
      }
  def astBlock(endMarker: String): Parser[List[ASTStmt]] =
    "begin".ic ~> rep(astStmt) <~ "end".ic ~ endMarker
  val astBlockDot       = astBlock(".")
  val astBlockSemicolon = astBlock(";")

  val astSubrtCall: Parser[ASTSubrtCall] = {
    astId ~ "(" ~ repsep(astExpr, ",") ~ ")" <~ ";" ^^ {
      case funName ~ _ ~ args ~ _ => {
        ASTSubrtCall(funName, args)
      }
    }
  }

  def astExpra: Parser[ASTExpr] = {
    (astConst ^^ {
      ASTConst(_)
    }) | (astId ~ "[" ~ astExpr ~ "]" ^^ {
      case id ~ _ ~ idx ~ _ => ASTIdxedVar(id, idx)
    }) | (astId ~ "(" ~ repsep(astExpr, ",") ~ ")" ^^ {
      case funName ~ _ ~ args ~ _ => {
        ASTFunCall(funName, args)
      }
    }) | (astId ^^ {
      ASTVar(_)
    }) | ("(" ~> astExpr <~ ")")
  }

  def astExpr: Parser[ASTExpr] = {
    astExpra ~ rep("\\+|\\-".r ~ astExpra) ^^ {
      case lv ~ rest => {
        rest.foldLeft(lv)((a, opb) => {
          opb match {
            case "+" ~ b => ASTPlus(a, b)
            case _ ~ b   => ASTMinus(a, b)
          }
        })
      }
    }
  }

  def astAsg: Parser[ASTAsg] = {
    astExpr ~ ":=" ~ astExpr ~ ";" ^^ {
      case lval ~ _ ~ rval ~ _ => ASTAsg(lval, rval)
    }
  }

  def astStmt: Parser[ASTStmt] = {
    astAsg | astRead | astWrite |
    astSubrtCall | astIf | astWhile |
    astBlockSemicolon ^^ { stmts => ASTBlock(stmts)}
  }

  def astWhile: Parser[ASTWhile] = {
    "while".ic ~ astExpr ~ "do".ic ~ astStmt ^^ {
      case _ ~ cond ~ _ ~ stmt => ASTWhile(cond, stmt)
    }
  }

  def astRead: Parser[ASTRead] = {
    "read".ic ~ "(" ~> astExpr <~ ")" ~ ";" ^^ {
      ASTRead(_)
    }
  }

  def astWrite: Parser[ASTWrite] = {
    "write".ic ~ "(" ~> astExpr <~ ")" ~ ";" ^^ {
      ASTWrite(_)
    }
  }

  def parseProg(program: String) = parse(astProg, program)

  def astIf: Parser[ASTIf] = {
    "if".ic ~ astExpr ~ "then" ~ astStmt  ~ ("else" ~> astStmt).? ^^ {
      case _ ~ cond ~ _ ~ thn ~ els => ASTIf(cond, thn, els)
    }
  }

}

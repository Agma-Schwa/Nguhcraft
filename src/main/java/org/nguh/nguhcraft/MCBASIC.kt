package org.nguh.nguhcraft

import com.mojang.brigadier.StringReader
import net.minecraft.command.EntitySelector
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.entity.Entity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * BASIC (sort of) implementation in which minecraft commands are
 * statements.
 *
 * In actuality, this programming language is more ‘basic’ than
 * ‘BASIC’, but the name still works imo.
 *
 * Yes, doing this nonsense is literally easier than trying to figure
 * out how to hook into '/execute if' and '/return', and also, those
 * are stupid because I can’t even use them to query whether a player
 * is dead or alive.
 */
object MCBASIC {
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /** Global procedures. */
    val GlobalProcs = mutableMapOf<String, Procedure>()

    /** The AST of the program. */
    private sealed class CachedAST {
        /** The program has not yet been compiled after it was last changed. */
        class NotCached() : CachedAST()

        /** The program has been compiled and is ready to execute. */
        class Cached(val Root: RootStmt) : CachedAST()

        /** The program contains a syntax error. */
        class Error(val Err: SyntaxException) : CachedAST()
    }

    /** A procedure that has a name and can be stored on disk. */
    class Procedure(val Name: String, val Code: Program = Program()) {
        private val File get() = "$Name.mcbas"

        fun LoadFrom(Dir: Path) {
            val F = Dir.resolve(File)
            if (!F.exists()) return
            Code.DeserialiseFrom(F.readText())
        }

        fun SaveTo(Dir: Path) {
            Dir.toFile().mkdirs()
            Dir.resolve(File).toFile().writeText(Code.Serialise())
        }

        companion object {
            fun LoadGlobalProc(F: File) {
                if (!F.exists()) throw IllegalArgumentException("File does not exist")
                val Name = F.nameWithoutExtension
                val P = Procedure(Name)
                P.Code.DeserialiseFrom(F.readText())
                GlobalProcs[Name] = P
            }
        }
    }

    /**
     * A program that can be executed and modified.
     *
     * Programs are canonically stored as source text, which is compiled
     * and cached the first time it is executed after being modified.
     */
    class Program(
        /**
         * The source text of the program, which may not constitute
         * a syntactically valid program.
         */
        private val SourceLines: MutableList<String> = mutableListOf<String>()
    ) {

        /** The compiled representation. */
        private var AST: CachedAST = CachedAST.NotCached()

        /** Add a line to the program. */
        fun Add(Line: String) = ClearCache().also { SourceLines.add(Line.trim()) }

        /** Clear the program. */
        fun Clear() = ClearCache().also { SourceLines.clear() }

        /** Clear the cached program state. */
        fun ClearCache() { AST = CachedAST.NotCached() }

        /** Compile the program. */
        fun Compile() {
            AST = Compiler(SourceLines.joinToString("\n").trim()).CompileAST()
        }

        /** Load a program from a serialised representation. */
        fun DeserialiseFrom(S: String) = ClearCache().also {
            SourceLines.clear()
            if (!S.isEmpty()) SourceLines.addAll(S.split("\n"))
        }

        /** Delete a line from the program. */
        fun Delete(R: ClosedRange<Int>) = ClearCache().also { SourceLines.subList(R.start, R.endInclusive + 1).clear() }

        /** The indicator that displays the programs state. */
        fun DisplayIndicator() = when (AST) {
            is CachedAST.Error -> "%"
            is CachedAST.Cached -> ""
            is CachedAST.NotCached -> "*"
        }

        /** Print the program. */
        fun DisplaySource(
            MT: MutableText,
            Indent: Int,
            ClickEventFactory: ((Line: Int, Text: String) -> String)? = null
        ): MutableText {
            val IndentStr = " ".repeat(Indent)
            if (SourceLines.isEmpty()) {
                MT.append(IndentStr).append(Text.literal("<empty>").formatted(Formatting.GRAY))
                return MT
            }

            SourceLines.forEachIndexed { I, S ->
                val T = Text.literal(S).formatted(Formatting.AQUA)
                if (ClickEventFactory != null) T.styled { it.withClickEvent(
                    ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND,
                        ClickEventFactory(I, S)
                    )
                )}

                MT.append("\n$IndentStr[$I] ").append(T)
            }
            return MT
        }

        /**
         * Execute the program.
         *
         * @throws Exception If there was a syntax error or an unexpected error executing the program.
         */
        @Throws(Exception::class)
        fun ExecuteAndThrow(S: ServerCommandSource) {
            when (AST) {
                is CachedAST.Cached -> Executor(S).ExecuteAST((AST as CachedAST.Cached).Root)
                is CachedAST.Error -> throw (AST as CachedAST.Error).Err
                is CachedAST.NotCached -> {
                    Compile()
                    ExecuteAndThrow(S)
                }
            }
        }

        /** Insert a line into the program. */
        fun Insert(Line: Int, Text: String) = ClearCache().also { SourceLines.add(Line, Text.trim()) }

        /** Whether the program is empty. */
        fun IsEmpty(): Boolean = SourceLines.isEmpty()

        /** The line count of the program. */
        fun LineCount(): Int = SourceLines.size

        /** Print the AST. */
        fun Listing(MT: MutableText) {
            when (AST) {
                is CachedAST.Cached -> Writer(MT).also { (AST as CachedAST.Cached).Root.Display(it) }
                is CachedAST.Error -> MT.append(Text.literal("Listing not available due to syntax error").formatted(Formatting.RED))
                is CachedAST.NotCached -> MT.append(Text.literal("Listing not available until compiled").formatted(Formatting.GRAY))
            }
        }

        /** Save the program as a string. */
        fun Serialise(): String = SourceLines.joinToString("\n")

        /** Set a line. */
        operator fun set(Line: Int, Text: String) = ClearCache().also { SourceLines[Line] = Text.trim() }
    }

    private class Executor(CommandSource: ServerCommandSource) {
        val Source: ServerCommandSource = CommandSource.withReturnValueConsumer {
            Success, Value -> CommandReturnValue = if (Success) Value else 0
        }

        var CommandReturnValue: Int = 0
        fun ExecuteAST(AST: RootStmt) {
            try {
                AST.Execute(this)
            } catch (_: ReturnException) {
                // Ignored.
            } catch (T: Throwable) {
                throw T
            }
        }
    }

    private class Writer(
        val MT: MutableText,
        var IndentWidth: Int = 0
    ) {
        fun Indent() { IndentWidth += 4 }
        fun Outdent() { IndentWidth -= 4 }
        fun StartLine() { MT.append("\n" + " ".repeat(IndentWidth)) }
        fun Write(S: String, F: Formatting? = null) {
            val T = Text.literal(S)
            if (F != null) T.formatted(F)
            MT.append(T)
        }
        fun WriteStmt(S: Stmt) {
            StartLine()
            S.DisplayAsStmt(this)
        }
    }

    enum class Builtin {
        /** Test whether Entity::isAlive is true. */
        IS_ENTITY_ALIVE,

        /** Test whether Entity is a ‘GM’, i.e. a player in creative or spectator mode. */
        IS_GM,
    }

    /** Root of the AST class hierarchy. */
    private abstract class Stmt {
        abstract fun Display(W: Writer)
        open fun DisplayAsStmt(W: Writer) { Display(W) }
        abstract fun Execute(E: Executor)
    }

    /** A statement that returns a value. */
    private abstract class Expr : Stmt() {
        final override fun Execute(E: Executor) { Evaluate(E) }
        abstract fun Evaluate(E: Executor): Any
    }

    /** Root of the AST itself. */
    private class RootStmt(val Statements: List<Stmt>) : Stmt() {
        override fun Display(W: Writer) { for (S in Statements) W.WriteStmt(S) }
        override fun Execute(E: Executor) { for (S in Statements) S.Execute(E) }
    }

    /** A list of statements. */
    private class Block(val Statements: List<Stmt>) : Stmt() {
        override fun Execute(E: Executor) { for (S in Statements) S.Execute(E) }
        override fun Display(W: Writer) {
            W.Write("begin", Formatting.GOLD)
            W.Indent()
            for (S in Statements) W.WriteStmt(S)
            W.Outdent()
            W.StartLine()
            W.Write("end", Formatting.GOLD)
        }
    }

    /** A call to a builtin function. */
    private class BuiltinCallExpr(val Func: Builtin, val Args: List<Expr>) : Expr() {
        init {
            when (Func) {
                Builtin.IS_ENTITY_ALIVE -> {
                    if (Args.size != 1) throw SyntaxException("'alive?' expects exactly one argument")
                    if (Args[0] !is EntitySelectorExpr) throw SyntaxException("'alive?' expects an entity selector")
                }
                Builtin.IS_GM -> {
                    if (Args.size != 1) throw SyntaxException("'gm?' expects exactly one argument")
                    if (Args[0] !is EntitySelectorExpr) throw SyntaxException("'gm?' expects an entity selector")
                }
            }
        }

        override fun Display(W: Writer) {
            W.Write(when (Func) {
                Builtin.IS_ENTITY_ALIVE -> "alive?"
                Builtin.IS_GM -> "gm?"
            }, Formatting.GREEN)

            W.Write("(", Formatting.GOLD)
            for ((I, A) in Args.withIndex()) {
                if (I != 0) W.Write(", ", Formatting.GOLD)
                A.Display(W)
            }
            W.Write(")", Formatting.GOLD)
        }

        override fun Evaluate(E: Executor): Any {
            when (Func) {
                Builtin.IS_ENTITY_ALIVE -> {
                    val Ent: Entity = (Args[0] as EntitySelectorExpr).EvaluateToSingleEntity(E)
                    return if (Ent.isAlive) 1 else 0
                }
                Builtin.IS_GM -> {
                    val Ent: Entity = (Args[0] as EntitySelectorExpr).EvaluateToSingleEntity(E)
                    return if (Ent is ServerPlayerEntity && (Ent.isCreative || Ent.isSpectator)) 1 else 0
                }
            }
        }
    }

    /** An expression that is actually a Minecraft command. */
    private class CommandExpr(val Command: String) : Expr() {
        override fun Display(W: Writer) {
            W.Write("`", Formatting.GOLD)
            DisplayAsStmt(W)
            W.Write("`", Formatting.GOLD)
        }

        override fun DisplayAsStmt(W: Writer) {
            val Cmd = Command.split(WHITESPACE_REGEX)[0]
            val Rest = Command.drop(Cmd.length).trimStart()
            W.Write("/$Cmd", Formatting.GREEN)
            if (Rest.isNotEmpty()) W.Write(" $Rest", Formatting.YELLOW)
        }

        override fun Evaluate(E: Executor): Any {
            val CM = E.Source.server.commandManager
            val Wrapped = "return run $Command" // Wrap w/ 'return' to get the return value.
            CM.execute(CM.dispatcher.parse(Wrapped, E.Source), Wrapped)
            return E.CommandReturnValue
        }
    }

    /** An expression that selects an entity. */
    private class EntitySelectorExpr(val Sel: EntitySelector, val SelStr: String): Expr() {
        override fun Display(W: Writer) {
            W.Write(SelStr, Formatting.AQUA)
        }

        override fun Evaluate(E: Executor): Any {
            return Sel.getEntities(E.Source)
        }

        fun EvaluateToSingleEntity(E: Executor): Entity {
            val Entities = (Evaluate(E) as List<*>)
            if (Entities.size != 1) throw SyntaxException("Expected exactly one entity for selector '$Sel'")
            return Entities[0] as Entity
        }
    }

    /** An 'if' statement, which does what you’d expect. */
    private class IfStmt(val Cond: Expr, val Body: Stmt): Stmt() {
        override fun Display(W: Writer) {
            W.Write("if ", Formatting.GOLD)
            Cond.Display(W)
            W.Write(" then", Formatting.GOLD)
            if (Body !is Block) W.Indent()
            W.WriteStmt(Body)
            if (Body !is Block) W.Outdent()
        }

        override fun Execute(E: Executor) {
            val Result = Cond.Evaluate(E)
            if (Result !is Int) throw IllegalStateException("Invalid type should have been caught at compile time")
            if (Result != 0) Body.Execute(E)
        }
    }

    /** A statement that returns from the current procedure or program. */
    private class ReturnStmt() : Stmt() {
        override fun Display(W: Writer) { W.Write("return", Formatting.GOLD) }
        override fun Execute(E: Executor) { throw ReturnException.INSTANCE }
    }

    /**
     * Why bother implementing returning from out of several levels
     * deep in an AST that we walk recursively to evaluate it if we
     * can just cheat instead?
     *
     * In other words, this exception is thrown to return from a function.
     */
    private class ReturnException private constructor() : Exception() {
        companion object { val INSTANCE = ReturnException() }
    }

    /** A syntax or semantic error. */
    private class SyntaxException(Msg: String) : Exception(Msg)

    /** Class that compiles a program. */
    private class Compiler(val SourceCode: String) {
        private enum class TokenKind {
            // A series of characters that starts with '/'.
            Command,

            // A series of characters that starts with a backquote.
            QuotedCommand,

            // A parsed entity selector.
            EntitySelector,

            // Punctuators.
            LParen,
            RParen,
            Comma,

            // A builtin function.
            BuiltinFunction,

            // Keywords.
            KwThen,
            KwIf,
            KwReturn,

            // End of file.
            Eof,
        }

        companion object {
            private val KEYWORDS = mapOf(
                "if" to TokenKind.KwIf,
                "return" to TokenKind.KwReturn,
                "then" to TokenKind.KwThen,
            )

            private val BUILTIN_FUNCTIONS = mapOf(
                "alive?" to Builtin.IS_ENTITY_ALIVE,
                "gm?" to Builtin.IS_GM,
            )
        }

        data class Token(
            val Kind: TokenKind,
            val Value: String,
            val Func: Builtin? = null,
            val Sel: EntitySelector? = null,
        )

        var Code = SourceCode
        lateinit var Tok: Token
        fun CompileAST(): CachedAST {
            try {
                Tok = Next()
                val List = mutableListOf<Stmt>()
                while (Tok.Kind != TokenKind.Eof) CompileStmt()?.let { List.add(it) }
                return CachedAST.Cached(RootStmt(List))
            } catch (E: SyntaxException) {
                // Find the line we’re on.
                var Line = SourceCode.take(SourceCode.length - Code.length).count { it == '\n' }
                return CachedAST.Error(SyntaxException("Near line $Line: ${E.message}"))
            }
        }

        /** Get the next token. */
        private fun Next(): Token {
            Tok = NextImpl()
            return Tok
        }

        /** Call Next() instead of this. */
        private fun NextImpl(): Token {
            Code = Code.trimStart()
            if (Code.isEmpty()) return Token(TokenKind.Eof, "")
            val C = Code.first()
            when (C) {
                '(' -> { Code = Code.drop(1); return Token(TokenKind.LParen, "(") }
                ')' -> { Code = Code.drop(1); return Token(TokenKind.RParen, ")") }
                '/' -> {
                    // Yeet '/'. It must be followed by a non-whitespace character.
                    Code = Code.drop(1)
                    if (Code.isEmpty() || Code.first().isWhitespace())
                        throw SyntaxException("Expected command name after '/'")

                    // The rest of the line is the command.
                    val Command = Code.takeWhile { it != '\n' }.trimEnd()
                    Code = Code.drop(Command.length + 1)
                    return Token(TokenKind.Command, Command)
                }
                '`' -> {
                    // Yeet '`' and take everything up to the next '`'. Also yeet a
                    // single '/' if the command starts with one.
                    Code = Code.drop(1)
                    if (Code.startsWith('/')) Code = Code.drop(1)
                    val Command = Code.takeWhile { it != '`' }
                    Code = Code.drop(Command.length)
                    if (!Code.startsWith('`')) throw SyntaxException("Expected closing backquote")
                    Code = Code.drop(1)
                    return Token(TokenKind.QuotedCommand, Command)
                }
                '@' -> {
                    val SR = StringReader(Code)
                    val Sel = EntityArgumentType.entities().parse(SR)
                    Code = SR.string.drop(SR.cursor)
                    return Token(TokenKind.EntitySelector, SR.string.take(SR.cursor), Sel = Sel)
                }
                else -> {
                    val Kw = Code.takeWhile { it.isLetterOrDigit() || it == '_' || it == '?' }

                    // Check for keywords.
                    KEYWORDS[Kw]?.let {
                        Code = Code.drop(Kw.length)
                        return Token(it, Kw)
                    }

                    // Check for built-in functions. These must be followed by a '('.
                    BUILTIN_FUNCTIONS[Kw]?.let {
                        Code = Code.drop(Kw.length).trimStart()
                        if (Code.startsWith("(")) return Token(TokenKind.BuiltinFunction, Kw, it)
                        throw SyntaxException("Reference to builtin function '$Kw' must be called")
                    }

                    throw SyntaxException("Unknown token: '$Kw'")
                }
            }
        }

        /**
        * Compile an expression.
        *
        * <expr> ::= QUOTED-COMMAND | <expr-call>
        * <expr-call> ::= BUILTIN-FUNCTION "(" [ <expr> ] { "," <expr> } [ "," ] ")"
        */
        private fun CompileExpr(): Expr {
            when (Tok.Kind) {
                TokenKind.BuiltinFunction -> {
                    val Func = Tok.Func!!
                    val Args = mutableListOf<Expr>()
                    Next()
                    if (Tok.Kind != TokenKind.LParen) throw SyntaxException("Expected '(' after builtin function")
                    Next()
                    while (Tok.Kind != TokenKind.RParen && Tok.Kind != TokenKind.Eof) {
                        Args.add(CompileExpr())
                        if (Tok.Kind == TokenKind.Comma) Next()
                    }
                    if (Tok.Kind != TokenKind.RParen) throw SyntaxException("Expected ')'")
                    Next()
                    return BuiltinCallExpr(Func, Args)
                }

                TokenKind.QuotedCommand -> {
                    val Cmd = CommandExpr(Tok.Value)
                    Next()
                    return Cmd
                }

                TokenKind.EntitySelector -> {
                    val Sel = EntitySelectorExpr(Tok.Sel!!, Tok.Value)
                    Next()
                    return Sel
                }

                // In general, the input to a command will be everything until the
                // end of the line, so an unquoted command that isn’t a statement
                // seems like a bad idea, so disallow it.
                TokenKind.Command -> throw SyntaxException("Unquoted command is not allowed here. Enclose it in `backquotes` instead.")
                else -> throw SyntaxException("Unexpected token: '${Tok.Value}'")
            }
        }

        /**
         * Compile a statement.
         *
         * <stmt> ::= <stmt-cmd> | <stmt-return> | <stmt-if>
         */
        private fun CompileStmt(): Stmt? {
            when (Tok.Kind) {
                TokenKind.Eof -> return null

                /** <stmt-cmd> ::= COMMAND | QUOTED-COMMAND */
                TokenKind.Command, TokenKind.QuotedCommand -> {
                    // '/return' is not allowed because it doesn’t work the
                    // way you’d expect it to.
                    if (Tok.Value.startsWith("return"))
                        throw SyntaxException("'/return' cannot be used as a command. Use a 'return' statement instead.")
                    val Cmd = CommandExpr(Tok.Value)
                    Next()
                    return Cmd
                }

                /** <stmt-return> ::= RETURN */
                TokenKind.KwReturn -> {
                    Next()
                    return ReturnStmt()
                }

                /** <stmt-return> :: IF <expr> THEN <stmt> */
                TokenKind.KwIf -> {
                    Next()
                    val Cond = CompileExpr()
                    if (!IsCondition(Cond)) throw SyntaxException("Expected condition after 'if'")
                    if (Tok.Kind != TokenKind.KwThen) throw SyntaxException("Expected 'then'")
                    Next()
                    val Body = CompileStmt() ?: throw SyntaxException("Expected statement after 'then'")
                    return IfStmt(Cond, Body)
                }

                else -> throw SyntaxException("Unexpected token: '${Tok.Value}'")
            }
        }

        /** Check whether an expression can be used as a condition. */
        private fun IsCondition(E: Expr): Boolean {
            return when (E) {
                is EntitySelectorExpr -> false
                is CommandExpr -> true
                is BuiltinCallExpr -> when (E.Func) {
                    Builtin.IS_ENTITY_ALIVE, Builtin.IS_GM -> true
                }

                else -> throw SyntaxException("Expected expression")
            }
        }
    }
}
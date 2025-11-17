package org.nguh.nguhcraft.server

import com.mojang.brigadier.StringReader
import com.mojang.logging.LogUtils
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.world.entity.Entity
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

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
    private val LOGGER = LogUtils.getLogger()
    private val WHITESPACE_REGEX = "\\s+".toRegex()
    private const val FILE_EXTENSION = ".mcbas"

    /** The AST of the program. */
    private sealed class CachedAST {
        /** The program has not yet been compiled after it was last changed. */
        class NotCached() : CachedAST()

        /** The program has been compiled and is ready to execute. */
        class Cached(val Root: RootStmt) : CachedAST()

        /** The program contains a syntax error. */
        class Error(val Err: SyntaxException) : CachedAST()
    }

    /**
     * A procedure that has a name and can be stored on disk.
     *
     * Procedures are canonically stored as source text, which is compiled
     * and cached the first time it is executed after being modified.
     *
     * Use ProtectionManager#GetOrCreate(Managed) to create a procedure
     * instead of constructing one of these directly.
     */
    class Procedure internal constructor(
        /**
         * The procedure name. This is the *path* to it relative to the
         * 'procedures' directory, but without the file extension.
         */
        val Name: String,

        /** The source text of the procedure, which need not be syntactically valid. */
        private val SourceLines: MutableList<String> = mutableListOf<String>()
    ) {
        /**
         * If a procedure is 'managed', it cannot be deleted by the user.
         *
         * This is used for e.g. region triggers, which should only be
         * deleted when the corresponding region is.
         */
        var Managed = false; set(M) {
            // Disallow changing a managed procedure to unmanaged so as
            // to prevent e.g. region triggers from being deleted by the
            // user.
            if (field && !M) throw IllegalArgumentException("Invalid use of system procedure '$Name'")
            field = M
        }

        /** The file path of the procedure. */
        val Path get() = Path("$Name$FILE_EXTENSION")

        /** The compiled representation. */
        private var AST: CachedAST = CachedAST.NotCached()

        /** Add a line to the procedure. */
        fun Add(Line: String) = ClearCache().also { SourceLines.add(Line.trim()) }

        /** Clear the procedure. */
        fun Clear() = ClearCache().also { SourceLines.clear() }

        /** Clear the cached procedure state. */
        fun ClearCache() { AST = CachedAST.NotCached() }

        /** Compile the procedure. */
        fun Compile() {
            AST = Compiler(SourceLines.joinToString("\n").trim()).CompileAST()
        }

        /** Load a procedure from a serialised representation. */
        fun DeserialiseFrom(S: String) = ClearCache().also {
            SourceLines.clear()
            if (!S.isEmpty()) SourceLines.addAll(S.split("\n"))
        }

        /** Delete a line from the procedure. */
        fun Delete(R: ClosedRange<Int>) = ClearCache().also { SourceLines.subList(R.start, R.endInclusive + 1).clear() }

        /** The indicator that displays the procedures state. */
        fun DisplayIndicator() = when (AST) {
            is CachedAST.Error -> "%"
            is CachedAST.Cached -> ""
            is CachedAST.NotCached -> "*"
        }

        /** Print the procedure. */
        fun DisplaySource(
            MT: MutableComponent,
            Indent: Int
        ): MutableComponent {
            val IndentStr = " ".repeat(Indent)
            if (SourceLines.isEmpty()) {
                MT.append(IndentStr).append(Component.literal("<empty>").withStyle(ChatFormatting.GRAY))
                return MT
            }

            SourceLines.forEachIndexed { I, S ->
                MT.append("${if (I == 0) "" else "\n"}$IndentStr[$I] ").append(
                    Component.literal(S)
                    .withStyle(ChatFormatting.AQUA)
                    .withStyle { it.withClickEvent(
                        ClickEvent.SuggestCommand("/procedure set $Name $I $S")
                    )
                })
            }
            return MT
        }

        /**
         * Execute the procedure.
         *
         * @throws Exception If there was a syntax error or an unexpected error executing the procedure.
         */
        @Throws(Exception::class)
        fun ExecuteAndThrow(S: CommandSourceStack) {
            when (AST) {
                is CachedAST.Cached -> Executor(S).ExecuteAST((AST as CachedAST.Cached).Root)
                is CachedAST.Error -> throw (AST as CachedAST.Error).Err
                is CachedAST.NotCached -> {
                    Compile()
                    ExecuteAndThrow(S)
                }
            }
        }

        /** Insert a line into the procedure. */
        fun Insert(Line: Int, Text: String) = ClearCache().also { SourceLines.add(Line, Text.trim()) }

        /** Whether the procedure is empty. */
        fun IsEmpty(): Boolean = SourceLines.isEmpty()

        /** The line count of the procedure. */
        fun LineCount(): Int = SourceLines.size

        /** Print the AST. */
        fun Listing(MT: MutableComponent) {
            when (AST) {
                is CachedAST.Cached -> Writer(MT).also { (AST as CachedAST.Cached).Root.Display(it) }
                is CachedAST.Error -> MT.append(Component.literal("Listing not available due to syntax error").withStyle(ChatFormatting.RED))
                is CachedAST.NotCached -> MT.append(Component.literal("Listing not available until compiled").withStyle(ChatFormatting.GRAY))
            }
        }

        /** Save the procedure as a string. */
        fun Serialise(): String = SourceLines.joinToString("\n")

        /** Set a line. */
        operator fun set(Line: Int, Text: String) = ClearCache().also { SourceLines[Line] = Text.trim() }
    }

    /** Helper that manages procedure storage. */
    class ProcedureManager(private val S: MinecraftServer): Manager() {
        /**
         * Save directory.
         *
         * This needs to be initialised when the save data is first loaded
         * because the session holding the paths may not exist before that
         * point.
         */
        private var SaveDir: Path? = null

        /** All loaded  procedures. */
        private val LoadedProcs = mutableMapOf<String, Procedure>()

        /** Procedures as an immutable list. */
        val Procedures get(): Collection<Procedure> = LoadedProcs.values

        /** Delete a managed or unmanaged procedure. */
        fun Delete(P: Procedure) {
            LoadedProcs.remove(P.Name)
            val Dir = SaveDir?.resolve(P.Path)
            if (Dir == null || !Dir.exists()) return
            Dir.toFile().delete()
        }

        /** Get an existing procedure. */
        fun GetExisting(Name: String) = LoadedProcs[Name]

        /** Get or create a user-defined procedure. */
        @Throws(IllegalArgumentException::class)
        fun GetOrCreate(Name: String): Procedure {
            // Prevent use of slashes in user-defined procedures.
            if (Name.contains("/")) throw IllegalArgumentException("Invalid procedure name")
            return GetOrCreateImpl(Name, false)
        }

        /**
         * Actually create/retrieve a procedure.
         *
         * This can also be called directly if you don’t care whether
         * the procedure you get back is managed or not.
         */
        @Throws(IllegalArgumentException::class)
        private fun GetOrCreateImpl(Name: String, Managed: Boolean? = null): Procedure {
            LoadedProcs[Name]?.let {
                if (Managed != null) it.Managed = Managed
                return it
            }

            // Prevent attempts to escape the procedures directory.
            if (Name.startsWith("/") || Name.contains(".."))
                throw IllegalArgumentException("Invalid procedure name")

            val Proc = Procedure(Name)
            if (Managed != null) Proc.Managed = Managed
            LoadedProcs[Name] = Proc
            return Proc
        }

        /** Get or create a managed procedure. */
        @Throws(IllegalArgumentException::class)
        fun GetOrCreateManaged(Name: String) = GetOrCreateImpl(Name, true)

        /** Load stored procedures. */
        override fun ReadData(RV: ValueInput) {
            SaveDir = S.getWorldPath(LevelResource.ROOT).resolve("nguhcraft").resolve("procedures")
            if (SaveDir == null || !SaveDir!!.exists()) return
            val Dir = SaveDir!!.toFile()
            for (F in Dir.walkTopDown()) {
                if (!F.isFile || !F.name.endsWith(FILE_EXTENSION)) continue
                try {
                    val Name = F.relativeTo(Dir).path.dropLast(FILE_EXTENSION.length)
                    val P = GetOrCreateImpl(Name)
                    P.DeserialiseFrom(F.readText())
                } catch (E : Exception) {
                    LOGGER.error("Could not load stored procedure '{}': {}", F.path, E.message)
                }
            }
        }

        /**
         * Save stored procedures.
         *
         * Empty procedures are not saved.
         */
        override fun WriteData(WV: ValueOutput) {
            if (SaveDir == null) return
            val Dir = SaveDir!!.toFile()
            Dir.mkdirs()
            for (Proc in LoadedProcs.values) {
                try {
                    val S = Proc.Serialise()
                    if (S.isEmpty()) continue
                    val ProcPath = SaveDir!!.resolve(Proc.Path)
                    ProcPath.parent.toFile().mkdirs()
                    ProcPath.toFile().writeText(S)
                } catch (E: Exception) {
                    LOGGER.error("Could not save stored procedure '{}': {}", Proc.Name, E.message)
                }
            }
        }
    }

    private class Executor(CommandSource: CommandSourceStack) {
        val Source: CommandSourceStack = CommandSource.withCallback {
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
        val MT: MutableComponent,
        var IndentWidth: Int = 0
    ) {
        var FirstLine = true
        fun Indent() { IndentWidth += 4 }
        fun Outdent() { IndentWidth -= 4 }
        fun StartLine() {
            if (FirstLine) FirstLine = false
            else MT.append("\n")
            MT.append(" ".repeat(IndentWidth))
        }
        fun Write(S: String, F: ChatFormatting? = null) {
            val T = Component.literal(S)
            if (F != null) T.withStyle(F)
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
            W.Write("begin", ChatFormatting.GOLD)
            W.Indent()
            for (S in Statements) W.WriteStmt(S)
            W.Outdent()
            W.StartLine()
            W.Write("end", ChatFormatting.GOLD)
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
            }, ChatFormatting.GREEN)

            W.Write("(", ChatFormatting.GOLD)
            for ((I, A) in Args.withIndex()) {
                if (I != 0) W.Write(", ", ChatFormatting.GOLD)
                A.Display(W)
            }
            W.Write(")", ChatFormatting.GOLD)
        }

        override fun Evaluate(E: Executor): Any {
            when (Func) {
                Builtin.IS_ENTITY_ALIVE -> {
                    val Ent: Entity = (Args[0] as EntitySelectorExpr).EvaluateToSingleEntity(E)
                    return if (Ent.isAlive) 1 else 0
                }
                Builtin.IS_GM -> {
                    val Ent: Entity = (Args[0] as EntitySelectorExpr).EvaluateToSingleEntity(E)
                    return if (Ent is ServerPlayer && (Ent.isCreative || Ent.isSpectator)) 1 else 0
                }
            }
        }
    }

    /** An expression that is actually a Minecraft command. */
    private class CommandExpr(val Command: String) : Expr() {
        override fun Display(W: Writer) {
            W.Write("`", ChatFormatting.GOLD)
            DisplayAsStmt(W)
            W.Write("`", ChatFormatting.GOLD)
        }

        override fun DisplayAsStmt(W: Writer) {
            val Cmd = Command.split(WHITESPACE_REGEX)[0]
            val Rest = Command.drop(Cmd.length).trimStart()
            W.Write("/$Cmd", ChatFormatting.GREEN)
            if (Rest.isNotEmpty()) W.Write(" $Rest", ChatFormatting.YELLOW)
        }

        override fun Evaluate(E: Executor): Any {
            val CM = E.Source.server.commands
            val Wrapped = "return run $Command" // Wrap w/ 'return' to get the return value.
            CM.performCommand(CM.dispatcher.parse(Wrapped, E.Source), Wrapped)
            return E.CommandReturnValue
        }
    }

    /** An expression that selects an entity. */
    private class EntitySelectorExpr(val Sel: EntitySelector, val SelStr: String): Expr() {
        override fun Display(W: Writer) {
            W.Write(SelStr, ChatFormatting.AQUA)
        }

        override fun Evaluate(E: Executor): Any {
            return Sel.findEntities(E.Source)
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
            W.Write("if ", ChatFormatting.GOLD)
            Cond.Display(W)
            W.Write(" then", ChatFormatting.GOLD)
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

    /** A statement that returns from the current procedure. */
    private class ReturnStmt() : Stmt() {
        override fun Display(W: Writer) { W.Write("return", ChatFormatting.GOLD) }
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
                    val Sel = EntityArgument.entities().parse(SR)
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

val MinecraftServer.ProcedureManager get() = Manager.Get<MCBASIC.ProcedureManager>(this)
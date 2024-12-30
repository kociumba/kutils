package org.kociumba.kutils.client

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.RelativeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.universal.UDesktop
import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import net.minecraft.client.MinecraftClient
import org.kociumba.kutils.client.imgui.ImGuiKutilsTransparentTheme
import org.kociumba.kutils.log
import xyz.breadloaf.imguimc.Imguimc
import xyz.breadloaf.imguimc.interfaces.Renderable
import xyz.breadloaf.imguimc.interfaces.Theme
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.Throws

// migrate this to imgui now that we have it working
/**
 * Old elementa calculator ui
 */
@Deprecated(
    message = "Migrate to imgui ui",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ImCalcUI")
)
class CalcScreen : WindowScreen(ElementaVersion.V2) {
    private val textInput: UIComponent
    private val resultText: UIComponent
    private var inputBuffer: List<Char> = ArrayList(1000)
    private var input = ""
    private var outputBuffer = BasicState<String>("")

    init {
        val container = UIContainer().constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 200.pixels()
            height = 50.pixels()
        } childOf window

        val inputBox = UIBlock(Color(50, 50, 50)).constrain {
            x = CenterConstraint()
            y = 0.pixels()
            width = 100.percent()
            height = 20.pixels()
        } childOf container

        textInput = UITextInput("Enter calculation").constrain {
            x = 2.pixels()
            y = 2.pixels()
            width = RelativeConstraint(1f) - 4.pixels()
        } childOf inputBox

        resultText = UIText("Result: ").constrain {
            x = CenterConstraint()
            y = SiblingConstraint(5f)
        } childOf container

        resultText.bindText(outputBuffer)

        textInput.grabWindowFocus()

        textInput.onMouseClick { textInput.grabWindowFocus() }

        textInput.onKeyType { typedChar, keyCode ->
            log.info("typedChar: $typedChar, keyCode: $keyCode")
            input = textInput.getText()
            calculate()

            if (keyCode == 257) { // why is this enter in minecraft ???
                calculateAndCopy()
//                calculatorState.calcScreen?.calculateAndCopy()
//                displayScreen(calculatorState.prevScreen)
            }
        }
    }

    private fun calculate() {
        try {
            val tokens = Calculator.lex(input)
            val rpnTokens = Calculator.shuntingYard(tokens)
            val result = Calculator.evaluate(rpnTokens)
            outputBuffer.set(result.toPlainString())
        } catch (e: CalculatorException) {
            log.warn("Error: ${e.message} at offset ${e.offset}, length ${e.length}, input: $input, result: ${outputBuffer.get()}")
        }
    }

    fun calculateAndCopy() {
        calculate()
        UDesktop.setClipboardString(outputBuffer.get())
    }
}

/**
 * New ImGui calculator ui (WIP)
 */
object ImCalcUI : Renderable {
    private var inputBuffer = ImString("", 256)
    private var outputBuffer = ImString("", 256)
    private var enterPressed = false
    private var window = MinecraftClient.getInstance().window

    override fun getName(): String? {
        return "calculator ui"
    }

    override fun getTheme(): Theme? {
        return ImGuiKutilsTransparentTheme()
    }

    // TODO: overhaul the calculator (not up to the standard of the other uis)
    //  labels: enhancement
    /**
     * > Turns out I don't need a mixin for this, but the input focus is very aggressive, so maby add a setting to disable it ?
     */
    override fun render() {
        ImGui.setNextWindowPos(window.width / 2f - 150f, window.height / 2f - 100f)
        ImGui.setNextWindowSize(300f, 200f)

        ImGui.begin("Calculator", ImGuiWindowFlags.NoDecoration or
                ImGuiWindowFlags.NoDocking or
                ImGuiWindowFlags.NoTitleBar or
                ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.NoBackground or
                ImGuiWindowFlags.NoNav
        )
        ImGui.setWindowFocus("Calculator")

        ImGui.text("Input:")
        ImGui.sameLine()

        ImGui.setKeyboardFocusHere()
        if (ImGui.inputText("##input", inputBuffer)) {
            try {
                val tokens = Calculator.lex(inputBuffer.get())
                val rpnTokens = Calculator.shuntingYard(tokens)
                val result = Calculator.evaluate(rpnTokens)
                outputBuffer.set(result.toPlainString())
            } catch (e: CalculatorException) {
                log.warn("Error: ${e.message} at offset ${e.offset}, length ${e.length}, input: ${inputBuffer.get()}, result: ${outputBuffer.get()}")
            }
        }

        if (ImGui.isKeyPressed(ENTER)) {
            enterPressed = true
        }

        if (ImGui.isKeyPressed(ESCAPE)) {
            displayingCalc = false
            Imguimc.pullRenderableAfterRender(this)
            enterPressed = false
        }

        ImGui.text("Result:")
        ImGui.sameLine()
        ImGui.text(outputBuffer.get())

        if (enterPressed) {
            UDesktop.setClipboardString(outputBuffer.get())
//                UMinecraft.getMinecraft().setScreen(null)
            displayingCalc = false
            Imguimc.pullRenderableAfterRender(this) // will this work xd ??? It does 😎
            enterPressed = false
        }

//        if (ImGui.button("Copy Result")) {
//            UDesktop.setClipboardString(outputBuffer.get())
//        }

        ImGui.end()
    }

    fun reset() {
        enterPressed = false
    }
}

enum class TokenType {
    NUMBER, BINOP, LPAREN, RPAREN, POSTOP
}

data class Token(
    var type: TokenType,
    var operatorValue: String = "",
    var numericValue: Long = 0,
    var exponent: Int = 0,
    var tokenStart: Int = 0,
    var tokenLength: Int = 0
)

class CalculatorException(message: String, val offset: Int, val length: Int) : Exception(message)

// why is kdoc different than javadoc, just why did they change it?
/**
 * This is just more or less a port of the calculator from NotEnoughUpdates
 *
 * The only real differences are that this has its own ui instead of supporting calculations on signs,
 * aside from that all I did was porting it to kotlin.
 *
 * Huge credit to [NEU](https://github.com/Moulberry/NotEnoughUpdates)
 */
object Calculator {
    private const val binops = "+-*/x"
    private const val postops = "mkbts"
    private const val digits = "0123456789"

    private fun readDigitsInto(token: Token, source: String, decimals: Boolean) {
        val startIndex = token.tokenStart + token.tokenLength
        for (j in startIndex until source.length) {
            val d = source[j]
            val d0 = digits.indexOf(d)
            if (d0 != -1) {
                if (decimals) token.exponent--
                token.numericValue = token.numericValue * 10 + d0
                token.tokenLength++
            } else {
                return
            }
        }
    }

    @Throws(CalculatorException::class)
    fun lex(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < source.length) {
            val c = source[i]
            if (c.isWhitespace()) {
                i++
                continue
            }
            val token = Token(tokenStart = i, type = TokenType.NUMBER)
            when {
                binops.contains(c) -> {
                    token.tokenLength = 1
                    token.type = TokenType.BINOP
                    token.operatorValue = c.toString()
                }

                postops.contains(c) -> {
                    token.tokenLength = 1
                    token.type = TokenType.POSTOP
                    token.operatorValue = c.toString()
                }

                c == ')' -> {
                    token.tokenLength = 1
                    token.type = TokenType.RPAREN
                    token.operatorValue = ")"
                }

                c == '(' -> {
                    token.tokenLength = 1
                    token.type = TokenType.LPAREN
                    token.operatorValue = "("
                }

                c == '.' -> {
                    token.tokenLength = 1
                    readDigitsInto(token, source, true)
                    if (token.tokenLength == 1) throw CalculatorException("Invalid number literal", i, 1)
                }

                digits.contains(c) -> {
                    readDigitsInto(token, source, false)
                    if (i + token.tokenLength < source.length && source[i + token.tokenLength] == '.') {
                        token.tokenLength++
                        readDigitsInto(token, source, true)
                    }
                }

                else -> throw CalculatorException("Unknown thing $c", i, 1)
            }
            tokens.add(token)
            i += token.tokenLength
        }
        return tokens
    }

    private fun getPrecedence(token: Token): Int {
        return when (token.operatorValue) {
            "+", "-" -> 0
            "*", "/", "x" -> 1
            else -> throw CalculatorException(
                "Unknown operator ${token.operatorValue}",
                token.tokenStart,
                token.tokenLength
            )
        }
    }

    @Throws(CalculatorException::class)
    fun shuntingYard(toShunt: List<Token>): List<Token> {
        val op = ArrayDeque<Token>()
        val out = mutableListOf<Token>()

        for (token in toShunt) {
            when (token.type) {
                TokenType.NUMBER -> out.add(token)
                TokenType.BINOP -> {
                    val p = getPrecedence(token)
                    while (op.isNotEmpty()) {
                        val l = op.first()
                        if (l.type == TokenType.LPAREN) break
                        val pl = getPrecedence(l)
                        if (pl >= p) {
                            out.add(op.removeFirst())
                        } else {
                            break
                        }
                    }
                    op.addFirst(token)
                }

                TokenType.LPAREN -> op.addFirst(token)
                TokenType.RPAREN -> {
                    while (true) {
                        if (op.isEmpty()) throw CalculatorException(
                            "Unbalanced right parenthesis",
                            token.tokenStart,
                            token.tokenLength
                        )
                        val l = op.removeFirst()
                        if (l.type == TokenType.LPAREN) break
                        out.add(l)
                    }
                }

                TokenType.POSTOP -> out.add(token)
            }
        }

        while (op.isNotEmpty()) {
            val l = op.removeFirst()
            if (l.type == TokenType.LPAREN) throw CalculatorException(
                "Unbalanced left parenthesis",
                l.tokenStart,
                l.tokenLength
            )
            out.add(l)
        }

        return out
    }

    @Throws(CalculatorException::class)
    fun evaluate(rpnTokens: List<Token>): BigDecimal {
        val values = ArrayDeque<BigDecimal>()
        try {
            for (token in rpnTokens) {
                when (token.type) {
                    TokenType.NUMBER -> values.addFirst(BigDecimal(token.numericValue).scaleByPowerOfTen(token.exponent))
                    TokenType.BINOP -> {
                        val right = values.removeFirst().setScale(2, RoundingMode.HALF_UP)
                        val left = values.removeFirst().setScale(2, RoundingMode.HALF_UP)
                        var result: BigDecimal = BigDecimal.ZERO
                        try {
                            result = when (token.operatorValue) {
                                "x", "*" -> left.multiply(right)
                                "/" -> left.divide(right, RoundingMode.HALF_UP)
                                "+" -> left.add(right)
                                "-" -> left.subtract(right)
                                else -> throw CalculatorException(
                                    "Unknown operation ${token.operatorValue}",
                                    token.tokenStart,
                                    token.tokenLength
                                )
                            }
                        } catch (e: ArithmeticException) {
                            log.error("error evaluating expression: ", CalculatorException(
                                "invalid operation (did you devide by zero?)",
                                token.tokenStart,
                                token.tokenLength
                            ))
                        }
                        values.addFirst(result.setScale(2, RoundingMode.HALF_UP))
                    }

                    TokenType.POSTOP -> {
                        val value = values.removeFirst()
                        val result = when (token.operatorValue) {
                            "s" -> value.multiply(BigDecimal(64))
                            "k" -> value.multiply(BigDecimal(1_000))
                            "m" -> value.multiply(BigDecimal(1_000_000))
                            "b" -> value.multiply(BigDecimal(1_000_000_000))
                            "t" -> value.multiply(BigDecimal("1000000000000"))
                            else -> throw CalculatorException(
                                "Unknown operation ${token.operatorValue}",
                                token.tokenStart,
                                token.tokenLength
                            )
                        }
                        values.addFirst(result.setScale(2, RoundingMode.HALF_UP))
                    }

                    else -> throw CalculatorException(
                        "Did not expect unshunted token in RPN",
                        token.tokenStart,
                        token.tokenLength
                    )
                }
            }
            return values.removeFirst().stripTrailingZeros()
        } catch (e: NoSuchElementException) {
            throw CalculatorException("Unfinished expression", 0, 0)
        }
    }
}

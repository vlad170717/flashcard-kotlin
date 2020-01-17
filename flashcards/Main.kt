package flashcards
import java.io.File
import java.util.Scanner

enum class CommandKey {
    importKey, exportKey, none
}

val loglines: MutableList<String> = MutableList<String>(0, { _ -> ""} )
var importFileName = ""
var exportFileName = ""

fun main(args: Array<String>) {
    val cards: MutableMap<String, String> = mutableMapOf()
    val mistakes: MutableMap<String, Int> = mutableMapOf()

    var mode = CommandKey.none
    for (i in 0..args.lastIndex) {
        when (mode) {
            CommandKey.none -> {
                mode = if (args[i].equals("-import")) CommandKey.importKey
                else if(args[i].equals("-export")) CommandKey.exportKey
                else CommandKey.none
            }
            CommandKey.importKey -> {
                importFileName = args[i]
                mode = CommandKey.none
            }
            CommandKey.exportKey -> {
                exportFileName = args[i]
                mode = CommandKey.none
            }
        }
    }

    if (importFileName.length > 0) importFile(importFileName, cards, mistakes)
    
    commandLoop(cards, mistakes)

    if (exportFileName.length > 0) exportFile(exportFileName, cards, mistakes)

    printlnLog("Bye bye!")
}

private fun commandLoop(cards: MutableMap<String, String>, mistakes: MutableMap<String, Int>) {
    val scanner = Scanner(System.`in`)
    do {
        val cmd = printTextScanLine("Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):", scanner)
        if (cmd == "add") cmdAdd(cards, scanner)
        if (cmd == "remove") cmdRemove(cards, scanner)
        if (cmd == "ask") cmdAsk(cards, mistakes, scanner)
        if (cmd == "export") cmdExport(cards, mistakes, scanner)
        if (cmd == "import") cmdImport(cards, mistakes, scanner)
        if (cmd == "log") cmdLog(scanner)
        if (cmd == "hardest card") cmdHardestCard(mistakes)
        if (cmd == "reset stats") {
            mistakes.clear()
            printlnLog("Card statistics has been reset.")
        }
    } while (cmd != "exit")
}

fun getRandomInt(max: Int): Int {
    return Math.floor(Math.random() * Math.floor(max.toDouble())).toInt()
}

fun cmdAdd(cards: MutableMap<String, String>, scanner: Scanner) {
    val term = printTextScanLine("The card:", scanner)
    if (cards.containsKey(term)) {
        printlnLog("The card \"$term\" already exists.")
        return
    }
    val definition = printTextScanLine("The definition of the card:", scanner)
    if (cards.containsValue(definition)) {
        printlnLog("The definition \"$definition\" already exists.")
        return
    }
    cards[term] = definition
    printlnLog("The pair (\"$term\":\"$definition\") has been added.")
}

fun cmdAsk(cards: MutableMap<String, String>, mistakes: MutableMap<String, Int>, scanner: Scanner) {
    val quantity = printTextScanLine("How many times to ask?", scanner).toInt()
    for (i in 1..quantity) {
        val (term, definition: String) = getCardByIndex(cards, i)
        val userDefinition = printTextScanLine("Print the definition of \"$term\":", scanner)
        if (definition.equals(userDefinition, true)) {
            printlnLog("Correct answer")
        } else {
            mistakeIncrement(mistakes, term)
            if (!cards.containsValue(userDefinition)) {
                printlnLog("Wrong answer. The correct one is \"$definition\".")
            } else {
                val otherDefiniton = cards.filter{ (k,v) -> v.equals(userDefinition) }
                for (el in otherDefiniton) {
                    printlnLog("Wrong answer. The correct one is \"$definition\", you've just written the definition of \"${el.key}\".")
                    break
                }
            }
        }

    }
}

fun getCardByIndex(cards: MutableMap<String, String>, i: Int): Pair<String, String> {
    var term = ""
    var definition = ""
    var cardNum = (i - 1) % cards.size
    for (el in cards) {
        if (cardNum == 0) {
            term = el.key
            definition = el.value
            break
        }
        cardNum -= 1
    }
    return Pair(term, definition)
}

fun mistakeIncrement(mistakes: MutableMap<String, Int>, term: String) {
    if (mistakes.containsKey(term)) {
        mistakes[term]?.inc()
    } else {
        mistakes[term] = 1
    }
}

fun cmdRemove(cards: MutableMap<String, String>, scanner: Scanner) {
    val term = printTextScanLine("The card:", scanner)
    if (!cards.containsKey(term)) {
        printlnLog("Can't remove \"$term\": there is no such card.")
        return
    }
    cards.remove(term)
    printlnLog("The card has been removed.")
}

fun cmdLog(scanner: Scanner) {
    val fileName = printTextScanLine("File name:", scanner)
    var text = ""
    for (el in loglines) {
        text += el + "\n"
    }
    File(fileName).writeText(text)
    printlnLog("The log has been saved.")
    println(loglines)
}

fun cmdExport(cards: MutableMap<String, String>, mistakes: MutableMap<String, Int>, scanner: Scanner) {
    val fileName = printTextScanLine("File name:", scanner)
    exportFile(fileName, cards, mistakes)
}

private fun exportFile(fileName: String, cards: MutableMap<String, String>, mistakes: MutableMap<String, Int>) {
    var text = ""
    for (el in cards) {
        val errors = if (mistakes.containsKey(el.key)) mistakes[el.key] else 0
        text = text + el.key + "\n" + el.value + "\n" + errors.toString()
    }
    File(fileName).writeText(text)
    printlnLog("${cards.size} cards have been saved.")
}

fun cmdImport(cards: MutableMap<String, String>, mistakes: MutableMap<String, Int>, scanner: Scanner) {
    val fileName = printTextScanLine("File name:", scanner)
    importFile(fileName, cards, mistakes)
}

private fun importFile(fileName: String, cards: MutableMap<String, String>, mistakes: MutableMap<String, Int>) {
    val file = File(fileName)
    if (file.isFile()) {
        val lines = File(fileName).readLines()
        for (i in 0..lines.size - 2 step 3) {
            cards[lines[i]] = lines[i + 1]
            mistakes[lines[i]] = lines[i + 2].toInt()
        }
        printlnLog("${lines.size / 3} cards have been loaded.")
    } else {
        printlnLog("File not found.")
    }
}

fun cmdHardestCard(mistakes: MutableMap<String, Int>) {
    if (mistakes.size == 0){
        printlnLog("There are no cards with errors.")
    } else {
        var errorsCount = 0
        var max = 0
        for (el in mistakes) {
            if (el.value > max) {
                max = el.value
                errorsCount = 1
            } else if (el.value == max) {
                errorsCount += 1
            }
        }
        val isare = if (errorsCount <= 1) "is" else "are"
        val suffixMulti = if (errorsCount <= 1) "" else "s"
        var string = "The hardest card $isare "
        var printedErros = 0
        for (el in mistakes) {
            if (el.value == max) {
                string += "\"${el.key}\""
                printedErros += 1
                if (printedErros < errorsCount) string += ", "
            }
        }

        string += ". You have $errorsCount error$suffixMulti answering it."
        printlnLog(string)
    }

}

fun printTextScanLine(message: String, scanner: Scanner): String {
    printlnLog(message)
    val input = scanner.nextLine().trim(' ')
    saveToLog(input)
    return input
}

fun printlnLog(message: String) {
    loglines.add(message)
    println(message)

}

fun saveToLog(message: String) {
    loglines.add(message)
}

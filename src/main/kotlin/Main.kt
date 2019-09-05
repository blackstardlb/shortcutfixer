import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.time.Instant
import kotlin.streams.toList


val runtime: Runtime = Runtime.getRuntime()
val gson = Gson()
val backUpFile = File("${System.getProperty("user.home")}/.shortcuts_backup")

val shortcuts = arrayListOf(
    "org.gnome.settings-daemon.plugins.media-keys/logout",
    "org.gnome.settings-daemon.plugins.media-keys/screensaver",
    "org.gnome.settings-daemon.plugins.media-keys/terminal",
    "org.gnome.desktop.wm.keybindings/cycle-group",
    "org.gnome.desktop.wm.keybindings/begin-move",
    "org.gnome.desktop.wm.keybindings/begin-resize",
    "org.gnome.desktop.wm.keybindings/toggle-shaded"
)
var areShortCutsDisabled: Boolean = false
var instant: Instant = Instant.now()
fun main() {
    Thread.sleep(10000)
    val classNameToActivateFor: String = "jetbrains-idea-ce";
    var lastClassName: String? = null

    if (getBackups().isNotEmpty()) {
        resetShortcuts()
    }
    while (true) {
        try {
            Thread.sleep(500)
            val xdoid = runCommand("xdotool getactivewindow")
            println(xdoid)
            val xprop = runCommand("xprop -id ${xdoid[0]}")

            val currentClassName =
                getLineStartingWith(xprop, "WM_CLASS(STRING)")
                    ?.substringAfter("= ")
                    ?.split(", ")
                    ?.firstOrNull()
                    ?.replace("\"", "")

            if (lastClassName != currentClassName) {
                lastClassName = currentClassName;

                if (currentClassName == classNameToActivateFor) {
                    disableShortcuts()

                } else if (areShortCutsDisabled) {
                    resetShortcuts()
                }
                // Focus window changed;
            }
        } catch (e: Exception) {
            resetShortcuts()
            e.printStackTrace()
        }
    }
}

fun runCommand(command: String): List<String> {
    val process = runtime.exec(command);
    process.waitFor();
    return process.inputStream.bufferedReader().lines().toList();
}

fun List<String>.printLines() {
    this.forEach { println(it) }
}

fun getLineStartingWith(lines: List<String>, prefix: String): String? {
    return lines.firstOrNull { it.startsWith(prefix) }
}

fun disableShortcuts() {
    areShortCutsDisabled = true
    backupShortcuts()
    println("Disabling shortcuts")
    shortcuts.forEach { println(runCommand("gsettings set ${it.replace("/", " ")} []")) }
}

fun resetShortcuts() {
    areShortCutsDisabled = false
    println("Enabling shortcuts")
    getBackups().forEach{ (shortcut, value) ->
        println(
            runCommand(
                "gsettings set ${shortcut.replace(
                    "/",
                    " "
                )} $value"
            )
        )
    }
    if (backUpFile.exists()) {
        backUpFile.delete()
    }
}

fun backupShortcuts() {
    if (!backUpFile.exists()) backUpFile.createNewFile()
    val shortcutMap = shortcuts.map { Pair(it, readKey(it)) }.toMap()
    backUpFile.writeText(gson.toJson(shortcutMap));
}

fun getBackups(): Map<String, String> {
    if (backUpFile.exists()) {
        return gson.fromJson(backUpFile.readText(),  hashMapOf<String, String>().javaClass);
    }
    return emptyMap()
}

fun readKey(key: String): String? {
    return runCommand("gsettings get ${key.replace("/", " ")}").firstOrNull()
}

fun JsonObject.toStringMap(): Map<String, String> {
    return this.keySet().map { Pair(it, this.getAsJsonPrimitive(it).asString) }.toMap()
}
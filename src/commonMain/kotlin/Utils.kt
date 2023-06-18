import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import models.Config
import net.mamoe.yamlkt.Yaml
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

fun String.cutMiddleTo10() = if (length > 10) substring(0, 4) + "…" + substring(length - 5) else this

suspend inline fun <Input, Output> Iterable<Input>.parallelMap(
  crossinline mapper: suspend (Input) -> Output,
): List<Output> = coroutineScope {
  map {
    async { mapper(it) }
  }.awaitAll()
}

fun loadConfigFileAsString(configPath: Path) = Buffer().apply {
  FileSystem.SYSTEM.source(configPath).use { source ->
    source.buffer().readAll(this)
  }
}.readUtf8()

fun getLastMondayAsLocal(now: Instant = Clock.System.now()): LocalDate {
  val timeZone = TimeZone.currentSystemDefault()
  val today = now.toLocalDateTime(timeZone).date
  val daysSinceMonday = today.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
  return today.minus(daysSinceMonday, DateTimeUnit.DAY)
}

fun Config.Companion.fromFile(path: String): Config {
  if (!path.endsWith(".yaml") && !path.endsWith(".yml"))
    throw IllegalArgumentException("Must be a YAML file ($path).")

  val ioPath = path.toPath(normalize = true)
  val fileContent = loadConfigFileAsString(ioPath)
  return Yaml.Default.decodeFromString(fileContent)
}

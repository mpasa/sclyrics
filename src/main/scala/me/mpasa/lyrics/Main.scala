package me.mpasa.lyrics

import cats.effect.ExitCode
import cats.implicits._
import monix.eval.{Task, TaskApp}
import monix.reactive.Observable

import scala.concurrent.duration.DurationLong

object Main extends TaskApp {

  private val RELOAD_EVERY = 5.seconds

  /** Gets the current playing song or a message if nothing is currently playing
    * It assumes the `playerctl` command is installed
    *
    * @return the current playing song or an error message
    */
  private def playingSong: Either[LyricsError, Song] = {
    val artistProcess= os.proc('playerctl, 'metadata, "xesam:artist").call(check = false)
    val titleProcess = os.proc('playerctl, 'metadata, "xesam:title").call(check = false)

    for {
      artist <- if (artistProcess.exitCode == 0) artistProcess.out.trim.asRight else NoSongPlaying.asLeft
      title <- if (titleProcess.exitCode == 0) titleProcess.out.trim.asRight else NoSongPlaying.asLeft
    } yield Song(artist, title)
  }

  /**
    * Spawns a less subprocess using the current stdout and returns it
    *
    * @param content the content to send to the less command
    * @return the less process so it can be destroyed afterwards
    */
  private def less(content: String) = {
    os.proc("less", "-R").spawn(stdin = content, stdout = os.Inherit)
  }

  /** Handles the result of getting the lyrics
    *
    * @param state the last state to be update
    * @param result either the lyrics or an error indicating what happened
    * @return an updated state
    */
  private def handleLyrics(state: State, result: Either[LyricsError, (Song, String)]): State = {
    result match {
      case Right((song, lyrics)) =>
        val songInfo = Console.GREEN + s"${song.artist} - ${song.title}" + Console.RESET
        state.destroyProcess()
        state.copy(subprocess = less(songInfo + "\n" + lyrics).some, error = None)

      case Left(error) => error match {
        case SongDidntChange =>
          state.copy(error = SongDidntChange.some)
        case LyricsNotFound(song, errorMessage) if !state.error.contains(error) =>
          val songInfo = Console.GREEN + s"${song.artist} - ${song.title}" + Console.RESET
          state.destroyProcess()
          state.copy(subprocess = less(songInfo + "\n" + errorMessage).some, error = error.some)
        case _ =>
          state
      }
    }
  }

  /** Gets an state and updates it according to the current playing song
    * An state is just information about the song being played, the subprocess (less) showing content and an optional
    * error
    *
    * @param lastState the state to be updated
    * @return the new state
    */
  private def update(lastState: State): State = playingSong match {
    case Right(currentSong) =>
      val newState = lastState.copy(song = currentSong.some)
      val result = for {
        song <- if (!lastState.song.contains(currentSong)) currentSong.asRight else SongDidntChange.asLeft
        lyrics <- GeniusScraper.scrap(song)
      } yield song -> lyrics
      handleLyrics(newState, result)

    case Left(NoSongPlaying) if !lastState.error.contains(NoSongPlaying) =>
      lastState.destroyProcess()
      lastState.copy(song = None, subprocess = less("No song playing").some, error = NoSongPlaying.some)

    case _ =>
      lastState
  }

  def run(args: List[String]): Task[ExitCode] = {
    var st = State(song = None, subprocess = None, error = None)
    Observable
      .intervalWithFixedDelay(0.seconds, RELOAD_EVERY)
      .mapEval { _ =>
        Task {
          st = update(st)
        }
      }
      .completedL
      .as(ExitCode.Success)
  }
}
package me.mpasa.lyrics

sealed trait LyricsError
final case class FatalError(message: String) extends LyricsError
final case object NoSongPlaying extends LyricsError
final case object SongDidntChange extends LyricsError

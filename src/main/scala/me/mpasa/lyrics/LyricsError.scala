package me.mpasa.lyrics

sealed trait LyricsError

final case class LyricsNotFound(song: Song, message: String) extends LyricsError
final case object NoSongPlaying extends LyricsError
final case object SongDidntChange extends LyricsError

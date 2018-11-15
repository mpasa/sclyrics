package me.mpasa.lyrics

import os.SubProcess

case class Song(artist: String, title: String)

case class State(song: Option[Song], subprocess: Option[SubProcess], error: Option[LyricsError]) {
  def destroyProcess(): Unit = subprocess.foreach(_.destroy())
}

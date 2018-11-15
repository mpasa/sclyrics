package me.mpasa.lyrics

import java.net.URLEncoder

import cats.implicits._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

sealed trait Scraper {
  def scrap(song: Song): Either[LyricsError, String]
}

/** An scraper for Genius
  *
  * 1. It searches the address of the song using Google
  * 2. Parses the Genius page
  */
case object GeniusScraper extends Scraper {

  /** Fetches an URL and gets its JSOUP document
    * If the page returns an error, the try fails
    *
    * @param url the address to be fetched
    * @return the JSOUP document of the URL
    */
  private def fetchURL(url: String): Try[Document] = Try {
    Jsoup
      .connect(url)
      .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36")
      .timeout(10000)
      .followRedirects(true)
      .get()
  }


  /** Searches Google for the first Genius result of the given song
    *
    * @param song the song were trying to get the results of
    * @return the URL of the given song or an error message
    */
  private def searchSongUrl(song: Song): Either[LyricsError, String] = {
    val query = URLEncoder.encode(s"${song.artist} ${song.title} genius lyrics", "UTF-8")
    val url = s"https://www.google.com/search?q=$query"
    fetchURL(url) match {
      case Success(document) =>
        val results = document.select("a").iterator().asScala.toSeq.map(_.attr("href"))
        val resultsGenius = results.filter(_.startsWith("https://genius.com"))
        resultsGenius.headOption.toRight(FatalError("Cannot find lyrics"))
      case _ =>
        FatalError("Unable to search the lyrics on Google").asLeft
    }
  }


  /** Scraps the lyrics of a song from a Genius song page
    *
    * @param songUrl the Genius URL of a song
    * @return lyrics on Either's right side if they've been found, left with error if they couldn't be found
    */
  private def scrapLyrics(songUrl: String): Either[LyricsError, String] = {
    fetchURL(songUrl) match {
      case Success(document) =>
        val lyrics = document.select(".lyrics")
        if (lyrics.size() > 0) {
          Right(lyrics.first().wholeText().trim)
        } else {
          Left(FatalError("Cannot find lyrics"))
        }
      case _ => Left(FatalError("Unable to connect to the Genius song URL"))
    }
  }


  override def scrap(song: Song): Either[LyricsError, String] = {
    for {
      url <- searchSongUrl(song)
      lyrics <- scrapLyrics(url)
    } yield lyrics
  }
}

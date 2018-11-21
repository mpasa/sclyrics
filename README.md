# Readme
A small project written in Scala to fetch and display the lyrics of the current playing song on the command line. The lyrics will be updated when a new song is played.


## Dependencies
It assumes that you system has 2 installed tools:

- `playerctl` to fetch the playing song information.
- `less` to display the results.


## Running
1. `java -jar sclyrics.jar`


## Running from source
1. `sbt assembly`
2. `java -jar target/scala-2.12/sclyrics.jar`

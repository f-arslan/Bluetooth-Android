package com.espressodev.bluetooth.playground


enum class Team {
    CORINTHIANS,
    LIVERPOOL
}

data class Fan(private val name: String) : NewsObserver {
    override fun notify(team: Team, message: String) {
        println("Hello $name, please check the following news about $team: \n$message\n")
    }
}

interface NewsObserver {
    fun notify(team: Team, message: String)
}


class NewsPublisher(private val team: Team) {
    private val observers: MutableMap<Team, MutableList<NewsObserver>> = mutableMapOf(team to mutableListOf())

    fun subscribe(observer: NewsObserver) {
        observers.getOrPut(team) { mutableListOf() }.add(observer)
    }

    fun unsubscribe(observer: NewsObserver) {
        observers[team]?.remove(observer)
    }

    fun notify(message: String) {
        observers[team]?.forEach { observer ->
            observer.notify(team, message)
        }
    }
}

class NewsSystemBuilder(team: Team) {
    private val newsPublisher: NewsPublisher = NewsPublisher(team)

    fun subscribe(fan: Fan) = apply { newsPublisher.subscribe(fan) }

    fun unsubscribe(fan: Fan) = apply { newsPublisher.unsubscribe(fan) }

    fun notify(message: String) = apply { newsPublisher.notify(message) }

    companion object {
        fun forTeam(team: Team) = NewsSystemBuilder(team)
    }
}

fun main() {

    val ricardo = Fan("Ricardo")
    val maria = Fan("Maria")

    NewsSystemBuilder.forTeam(Team.CORINTHIANS)
        .subscribe(ricardo)
        .subscribe(maria)
        .notify("Corinthians has won the FIFA World Cup 2 times.")
        .notify("Great news! Roger Guedes scored a hat trick against Palmeiras!")
        .unsubscribe(ricardo)
        .notify("Corinthians will face Flamengo in the COPA DO BRASIL semi-finals.")

    NewsSystemBuilder.forTeam(Team.LIVERPOOL)
        .subscribe(ricardo)
        .notify("Luis Diaz is set to return to Liverpool team training this week having been sidelined since October with a knee injury")
}


package io.legado.app.ui.widget.components.icon

import androidx.compose.runtime.Composable
import io.legado.app.ui.main.MainDestination

object AppIcons {

    val Search = Symbols.Search
    val MoreVert = Symbols.MoreVert
    val Edit = Symbols.Edit
    val Delete = Symbols.Delete
    val Close = Symbols.Close
    val Back = Symbols.ArrowBack
    val Filter = Symbols.FilterList
    val Settings = Symbols.Settings
    val BugReport = Symbols.BugReport
    val PrecisionSearch = Symbols.MyLocation
    val UnPrecisionSearch = Symbols.LocationSearching
    val History = Symbols.History
    val Replay = Symbols.Replay
    val MoreCircle = Symbols.MoreHoriz
    val Check = Symbols.Check
    val Group = Symbols.Sell

    fun mainDestination(destination: MainDestination, selected: Boolean): MaterialSymbol {
        return when (destination) {
            MainDestination.Home -> Symbols.Home
            MainDestination.Bookshelf -> Symbols.LibraryBooks
            MainDestination.Explore -> Symbols.Explore
            MainDestination.Rss -> Symbols.RssFeed
            MainDestination.My -> Symbols.Person
        }
    }
}

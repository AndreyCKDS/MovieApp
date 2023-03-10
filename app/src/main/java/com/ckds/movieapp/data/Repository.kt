package com.ckds.movieapp.data

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.ckds.movieapp.data.api.AppApi
import com.ckds.movieapp.data.db.AppDatabase
import com.ckds.movieapp.data.db.entities.StoredMovie
import com.ckds.movieapp.data.db.entities.StoredSeries
import com.ckds.movieapp.data.model.auth.AuthRequest
import com.ckds.movieapp.data.model.movie.Movie
import com.ckds.movieapp.data.model.series.Series
import com.ckds.movieapp.data.model.user.FavoriteRequest
import com.ckds.movieapp.utils.Resource
import com.ckds.movieapp.utils.networkBoundResource
import kotlinx.coroutines.delay
import javax.inject.Inject
class Repository @Inject constructor(
    private val appApi: AppApi,
    private val appDb: AppDatabase,
    private val sharedPreferences: SharedPreferences
) {

    private val appDao = appDb.getDao()

    // Movie

    fun getPopularMovies() = networkBoundResource(
        query = {
            appDao.getStoredMovies("popular")
        },
        fetch = {
            delay(1000)
            appApi.getPopularMovies().movies
        },
        saveFetchResult = { movies ->
            appDb.withTransaction {
                appDao.deleteStoredMoviesIds("popular")
                appDao.insertMoviesId(moviesId = movies.map {
                    StoredMovie(id = it.id!!, category = "popular"
                ) })
                appDao.insertMovies(movies = movies)
                appDao.deleteUnusedMovies()
            }
        }
    )

    suspend fun searchMovies(query: String, page: Int? = null): List<Movie> {
        return try {
            appApi.searchMovies(query = query, page = page).movies
        } catch (throwable: Throwable) {
            appDao.searchMovies(query = query)
        }
    }

    suspend fun getMovieDetails(movieId: Int) = appApi.getMovieDetails(movieId = movieId)

    suspend fun getMovieCredits(movieId: Int) = appApi.getMovieCredits(movieId = movieId)

    suspend fun addMovieToFavourite(movie: Movie) {
        appDao.addMovieToFavorite(movie = StoredMovie(id = movie.id!!, category = "favorite"))
        appDao.insertMovies(movies = listOf(movie))
        try {
            appDao.getSession().let { session ->
                appApi.markAsFavorite(sessionId = session.session_id,favoriteRequest = FavoriteRequest(
                    media_type = "movie",
                    media_id = movie.id,
                    favorite = true)
                )
            }
        } catch (throwable: Throwable) {}
    }

    suspend fun deleteMovieFromFavourite(movieId: Int) {
        appDao.deleteMovieFromFavorite(movieId = movieId)
        appDao.deleteUnusedMovies()
        try{
            appDao.getSession().let { session ->
                appApi.markAsFavorite(sessionId = session.session_id,favoriteRequest = FavoriteRequest(
                    media_type = "movie",
                    media_id = movieId,
                    favorite = false)
                )
            }
        } catch (throwable: Throwable) {}
    }

    suspend fun checkIfMovieIsFavorite(movieId: Int) =
        appDao.checkIfMovieIsFavorite(movieId = movieId)

    // Series

    fun getPopularSeries() = networkBoundResource(
        query = {
            appDao.getStoredSeries("popular")
        },
        fetch = {
            delay(1000)
            appApi.getPopularSeries().series
        },
        saveFetchResult = { series ->
            appDb.withTransaction {
                appDao.deleteStoredSeriesIds("popular")
                appDao.insertPopularSeriesId(series.map {
                    StoredSeries(id = it.id!!, category = "popular"
                    ) })
                appDao.insertSeries(series)
                appDao.deleteUnusedSeries()
            }
        }
    )

    suspend fun searchSeries(query: String, page: Int? = null): List<Series> {
        return try {
            appApi.searchSeries(query = query, page = page).series
        } catch (throwable: Throwable) {
            appDao.searchSeries(query = query)
        }
    }

    suspend fun getSeriesDetails(tvId: Int) =
        appApi.getSeriesDetails(tvId = tvId)

    suspend fun getSeriesCredits(tvId: Int) =
        appApi.getSeriesCredits(tvId = tvId)

    suspend fun addSeriesToFavourite(series: Series) {
        appDao.addSeriesToFavorite(series = StoredSeries(id = series.id!!, category = "favorite"))
        appDao.insertSeries(series = listOf(series))
        try {
            appDao.getSession().let { session ->
                appApi.markAsFavorite(sessionId = session.session_id,favoriteRequest = FavoriteRequest(
                    media_type = "tv",
                    media_id = series.id,
                    favorite = true)
                )
            }
        } catch (throwable: Throwable) {}
    }

    suspend fun deleteSeriesFromFavourite(tvId: Int) {
        appDao.deleteSeriesFromFavorite(tvId = tvId)
        appDao.deleteUnusedSeries()
        try {
            appDao.getSession().let { session ->
                appApi.markAsFavorite(sessionId = session.session_id,favoriteRequest = FavoriteRequest(
                    media_type = "tv",
                    media_id = tvId,
                    favorite = false)
                )
            }
        }  catch (throwable: Throwable) {}
    }

    suspend fun checkIfSeriesIsFavorite(tvId: Int) =
        appDao.checkIfSeriesIsFavorite(tvId = tvId)

    // Auth

    fun getSessionState() = sharedPreferences.getBoolean("SESSION_IS_ACTIVE", false)

    fun setSessionState(state: Boolean) = sharedPreferences.edit().putBoolean("SESSION_IS_ACTIVE", state).apply()

    suspend fun authenticate(login: String, password: String): Resource<Boolean> {
        try {
            appApi.getAuthenticationToken().let { token ->
                appApi.authenticateToken(authRequest = AuthRequest(
                    username = login,
                    password = password,
                    request_token = token.request_token)
                ).let { authToken ->
                    setSessionState(state = authToken.success)
                    val session = appApi.createSession(requestToken = authToken)
                    session.success.let { success ->
                        if (success)
                        appDao.insertSession(session = session)
                    }
                    return Resource.Success(data = session.success)
                }
            }
        } catch (throwable: Throwable) {
            return Resource.Error(throwable = throwable)
        }
    }

    suspend fun logOut() {
        if (getSessionState()) {
            appDao.getSession().let { session ->
                appApi.deleteSession(sessionId = session)
                setSessionState(state = false)
                appDao.deleteSession()
            }
        }
    }

    // User

    suspend fun getAccountDetails() =
        appDao.getSession().session_id.let { sessionId ->
            appApi.getAccountDetails(sessionId = sessionId)
        }

    fun getFavoriteMovies() = networkBoundResource(
        query = {
            appDao.getStoredMovies("favorite")
        },
        fetch = {
            delay(1000)
            appDao.getSession().let { session ->
                appApi.getFavoriteMovies(sessionId = session.session_id).movies
            }
        },
        saveFetchResult = { movies ->
            appDb.withTransaction {
                appDao.deleteStoredMoviesIds("favorite")
                appDao.insertMoviesId(moviesId = movies.map {
                    StoredMovie(id = it.id!!, category = "favorite"
                    ) })
                appDao.insertMovies(movies = movies)
                appDao.deleteUnusedMovies()
            }
        }
    )

    fun getFavoriteSeries() = networkBoundResource(
        query = {
            appDao.getStoredSeries("favorite")
        },
        fetch = {
            delay(1000)
            appDao.getSession().let { session ->
                appApi.getFavoriteSeries(sessionId = session.session_id).series
            }
        },
        saveFetchResult = { series ->
            appDb.withTransaction {
                appDao.deleteStoredSeriesIds("favorite")
                appDao.insertPopularSeriesId(series.map {
                    StoredSeries(id = it.id!!, category = "favorite"
                    ) })
                appDao.insertSeries(series)
                appDao.deleteUnusedSeries()
            }
        }
    )

}


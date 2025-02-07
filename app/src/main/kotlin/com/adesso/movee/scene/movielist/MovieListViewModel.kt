package com.adesso.movee.scene.movielist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adesso.movee.base.BaseViewModel
import com.adesso.movee.domain.FetchNowPlayingMoviesUseCase
import com.adesso.movee.internal.util.UseCase
import com.adesso.movee.scene.movielist.model.MovieUiModel
import javax.inject.Inject
import kotlinx.coroutines.launch

class MovieListViewModel @Inject constructor(
    private val fetchNowPlayingMoviesUseCase: FetchNowPlayingMoviesUseCase
) : BaseViewModel(), MovieItemListener {

    private val _nowPlayingMovies = MutableLiveData<List<MovieUiModel>>()
    val nowPlayingMovies: LiveData<List<MovieUiModel>> get() = _nowPlayingMovies

    init {
        fetchNowPlayingMovies()
    }

    internal fun fetchNowPlayingMovies() = viewModelScope.launch {
        fetchNowPlayingMoviesUseCase
            .run(UseCase.None)
            .either(::handleFailure, ::postNowPlayingMovies)
    }

    private fun postNowPlayingMovies(movies: List<MovieUiModel>) {
        _nowPlayingMovies.value = movies
    }

    override fun onMovieItemClick(movie: MovieUiModel) {
        navigate(MovieListFragmentDirections.actionMovieFragmentToMovieDetailFragment(movie.id))
    }
}

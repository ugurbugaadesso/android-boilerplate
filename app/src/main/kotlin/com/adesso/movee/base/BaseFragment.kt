package com.adesso.movee.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.adesso.movee.BR
import com.adesso.movee.R
import com.adesso.movee.internal.extension.observeNonNull
import com.adesso.movee.internal.extension.showPopup
import com.adesso.movee.internal.util.functional.lazyThreadSafetyNone
import com.adesso.movee.navigation.NavigationCommand
import com.adesso.movee.scene.main.MainActivity
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.DaggerFragment
import java.lang.reflect.ParameterizedType
import javax.inject.Inject

abstract class BaseFragment<VM : BaseViewModel, B : ViewDataBinding> :
    DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    internal lateinit var binder: B

    @get:LayoutRes
    abstract val layoutId: Int

    private var snackbar: Snackbar? = null
    private var networkStateBar: Snackbar? = null

    open fun initialize() {
        // Do nothing in here. Child classes should implement when necessary
    }

    @Suppress("UNCHECKED_CAST")
    protected open val viewModel by lazyThreadSafetyNone {
        val persistentViewModelClass = (javaClass.genericSuperclass as ParameterizedType)
            .actualTypeArguments[0] as Class<VM>
        return@lazyThreadSafetyNone ViewModelProvider(this, viewModelFactory)
            .get(persistentViewModelClass)
    }

    protected inline fun <reified VM : ViewModel> activityViewModels(): Lazy<VM> {
        return activityViewModels { viewModelFactory }
    }

    protected inline fun <reified VM : ViewModel> viewModels(): Lazy<VM> {
        return viewModels { viewModelFactory }
    }

    protected inline fun <reified VM : ViewModel> parentViewModels(): Lazy<VM> {
        return requireParentFragment().viewModels { viewModelFactory }
    }

    protected inline fun <reified VM : ViewModel> navGraphViewModels(@IdRes navGraphId: Int):
        Lazy<VM> {
        return navGraphViewModels(navGraphId) { viewModelFactory }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binder = DataBindingUtil.inflate(inflater, layoutId, container, false)
        binder.lifecycleOwner = viewLifecycleOwner
        binder.setVariable(BR.viewModel, viewModel)

        return binder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeNavigation()
        observeFailure()

        initialize()
    }

    private fun observeNavigation() {
        viewModel.navigation.observeNonNull(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { command ->
                handleNavigation(command)
            }
        }
    }

    protected open fun handleNavigation(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.ToDirection -> {
                findNavController().navigate(command.directions, getExtras())
            }
            is NavigationCommand.ToDeepLink -> {
                (activity as? MainActivity)
                    ?.navController
                    ?.navigate(command.deepLink.toUri(), null, getExtras())
            }
            is NavigationCommand.Popup -> {
                with(command) {
                    context?.showPopup(model, listener)
                }
            }
            is NavigationCommand.Back -> findNavController().navigateUp()
        }
    }

    private fun observeFailure() {
        viewModel.failurePopup.observeNonNull(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { popupModel ->
                requireContext().showPopup(popupModel)
            }
        }
    }

    private fun showSnackBarMessage(message: String) {
        context?.let {
            try { snackbar?.dismiss() } catch (ex: Exception) { ex.printStackTrace() }
            snackbar = Snackbar.make(
                binder.root,
                message,
                Snackbar.LENGTH_LONG
            )
            snackbar?.setTextColor(
                ContextCompat.getColor(it, R.color.colorAccent)
            )
            snackbar?.setBackgroundTint(
                ContextCompat.getColor(it, R.color.colorPrimary)
            )
            snackbar?.show()
        }
    }

    private fun showNetworkSnackBar() {
        initNetworkSnackBarIfNeeded()
        networkStateBar?.show()
    }

    private fun hideNetworkSnackBar() {
        networkStateBar?.dismiss()
    }

    private fun initNetworkSnackBarIfNeeded() {
        if (networkStateBar == null) {
            context?.let {
                networkStateBar = Snackbar.make(
                    binder.root,
                    R.string.common_error_network_connection,
                    Snackbar.LENGTH_INDEFINITE
                )
                networkStateBar?.setTextColor(
                    ContextCompat.getColor(it, R.color.colorAccent)
                )
                networkStateBar?.setBackgroundTint(
                    ContextCompat.getColor(it, R.color.colorPrimary)
                )
            }
        }
    }

    open fun getExtras(): FragmentNavigator.Extras = FragmentNavigatorExtras()
}

package org.covidwatch.android.ui.temporarycontactnumbers

import android.app.Application
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import org.covidwatch.android.CovidWatchApplication
import org.covidwatch.android.CovidWatchTcnManager
import org.covidwatch.android.R
import org.covidwatch.android.data.BluetoothViewModel
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.databinding.FragmentTemporaryContactNumbersBinding
import org.covidwatch.android.ui.temporarycontactnumbers.adapters.FragmentDataBindingComponent
import org.tcncoalition.tcnclient.TcnClient
import java.util.concurrent.TimeUnit

class TemporaryContactNumbersFragment : Fragment() {

    private lateinit var temporaryContactNumbersViewModel: TemporaryContactNumbersViewModel
    private lateinit var vm: BluetoothViewModel
    private lateinit var binding: FragmentTemporaryContactNumbersBinding
    private var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent(this)
    private var hasAlreadyNotified = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = context ?: return null

        val database = CovidWatchDatabase.getInstance(context)
        val viewModelTemporary: TemporaryContactNumbersViewModel by viewModels(factoryProducer = {
            TemporaryContactNumbersViewModelFactory(
                database.temporaryContactNumberDAO(),
                context.applicationContext as Application
            )
        })
        temporaryContactNumbersViewModel = viewModelTemporary
        vm = ViewModelProvider(this).get(BluetoothViewModel::class.java)

        binding =
            DataBindingUtil.inflate<FragmentTemporaryContactNumbersBinding>(
                inflater,
                R.layout.fragment_temporary_contact_numbers,
                container,
                false,
                dataBindingComponent
            ).apply {
                lifecycleOwner = this@TemporaryContactNumbersFragment
            }

        val adapter = TemporaryContactNumbersAdapter()
        binding.temporaryContactNumbersRecyclerview.adapter = adapter
        binding.temporaryContactNumbersRecyclerview.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            (activity?.application as? CovidWatchApplication)?.scheduleRefreshOneTime()
            Handler().postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, TimeUnit.SECONDS.toMillis(1))
        }

        viewModelTemporary.temporaryContactEvents.observe(
            viewLifecycleOwner,
            Observer { adapter.submitList(it) })

        viewModelTemporary.firstExposedTemporaryContactNumber.observe(
            viewLifecycleOwner,
            Observer {
                if (it != null && !hasAlreadyNotified) {
                    val activity = activity ?: return@Observer
                    hasAlreadyNotified = true
                    val alertDialog: AlertDialog? = activity.let {
                        val builder = AlertDialog.Builder(activity)
                        builder.setMessage(R.string.notification_current_user_was_exposed)
                        builder.apply {
                            setPositiveButton(getString(R.string.title_ok),
                                DialogInterface.OnClickListener { _, _ ->
                                })
                        }
                        // Create the AlertDialog
                        builder.create()
                    }
                    alertDialog?.show()
                }
            })

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_temporary_contact_numbers, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.download_self_reports -> {
                val alertDialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setMessage(R.string.message_download_self_reports)
                    builder.apply {
                        setPositiveButton(getString(R.string.title_download),
                            DialogInterface.OnClickListener { _, _ ->
                                (activity?.application as? CovidWatchApplication)?.scheduleRefreshOneTime()
                            })
                        setNegativeButton(getString(R.string.title_cancel),
                            DialogInterface.OnClickListener { _, _ ->
                                // User cancelled the dialog
                            })
                    }
                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog?.show()
            }
            R.id.upload_self_report -> {
                val alertDialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setMessage(R.string.message_dialog_self_report)
                    builder.apply {
                        setPositiveButton(
                            getString(R.string.title_upload)
                        ) { _, _ ->
                            //TODO: move uploading to a separate class and leave TcnManager only
                            // with generating report part
                            (TcnClient.tcnManager as CovidWatchTcnManager).generateAndUploadReport()
                        }
                        setNegativeButton(getString(R.string.title_cancel), null)
                    }
                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog?.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

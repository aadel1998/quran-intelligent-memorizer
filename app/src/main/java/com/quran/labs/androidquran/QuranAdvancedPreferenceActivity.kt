package com.quran.labs.androidquran

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.model.bookmark.BookmarkImportExportModel
import com.quran.labs.androidquran.service.util.PermissionUtil
import com.quran.labs.androidquran.ui.fragment.QuranAdvancedSettingsFragment
import com.quran.labs.androidquran.ui.modern.ModernAdvancedSettingsScreen
import com.quran.labs.androidquran.ui.util.ToastCompat
import com.quran.labs.androidquran.util.QuranFileUtils
import com.quran.labs.androidquran.util.QuranScreenInfo
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.labs.androidquran.util.RecordingLogTree
import com.quran.labs.androidquran.util.StorageUtils
import dev.zacsweers.metro.Inject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DisposableMaybeObserver
import io.reactivex.rxjava3.observers.DisposableSingleObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class QuranAdvancedPreferenceActivity : AppCompatActivity() {

  companion object {
    private const val SI_LOCATION_TO_WRITE = "SI_LOCATION_TO_WRITE"
    private const val REQUEST_WRITE_TO_SDCARD_PERMISSION = 1
  }

  private var locationToWrite: String? = null
  private var exportSubscription: Disposable? = null
  private var logsSubscription: Disposable? = null

  @Inject
  lateinit var bookmarkImportExportModel: BookmarkImportExportModel

  @Inject
  lateinit var quranFileUtils: QuranFileUtils

  @Inject
  lateinit var quranScreenInfo: QuranScreenInfo

  private var isExportingState by mutableStateOf(false)
  private var storageSummaryState by mutableStateOf<String?>("Calculating...")
  private var currentStorageLocationState by mutableStateOf("Internal Storage")
  private var isMultiStorageState by mutableStateOf(false)

  private val importFileLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
      val intent = Intent(this, QuranImportActivity::class.java).apply {
        data = result.data?.data
      }
      startActivity(intent)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val quranSettings = QuranSettings.getInstance(this)
    if (quranSettings.isModernUiEnabled) {
      (application as QuranApplication).applicationComponent.inject(this)
      initModernStorageInfo()

      setContent {
        QuranTheme {
          ModernAdvancedSettingsScreen(
            onBackClick = { finish() },
            onImportBookmarks = ::launchImportPicker,
            onExportBookmarks = ::exportBookmarks,
            onExportCsv = ::exportBookmarksCsv,
            onSendLogs = ::sendDebugLogs,
            currentStorageLocation = currentStorageLocationState,
            storageSpaceSummary = storageSummaryState,
            isMultiStorageAvailable = isMultiStorageState,
            onStorageLocationClick = ::onStorageLocationClicked,
            isExporting = isExportingState
          )
        }
      }
      return
    }

    setContentView(R.layout.preferences)

    val root = findViewById<ViewGroup>(R.id.root)
    ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
      )
      root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = insets.top
        leftMargin = insets.left
        rightMargin = insets.right
      }

      windowInsets
    }

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    toolbar.setTitle(R.string.prefs_category_advanced)
    setSupportActionBar(toolbar)
    val ab = supportActionBar
    ab?.setDisplayHomeAsUpEnabled(true)

    if (savedInstanceState != null) {
      locationToWrite = savedInstanceState.getString(SI_LOCATION_TO_WRITE)
    }

    val fm = supportFragmentManager
    val fragment = fm.findFragmentById(R.id.content)
    if (fragment == null) {
      fm.beginTransaction()
        .replace(R.id.content, QuranAdvancedSettingsFragment())
        .commit()
    }
  }

  private fun initModernStorageInfo() {
    val quranSettings = QuranSettings.getInstance(this)
    currentStorageLocationState = quranSettings.appCustomLocation ?: "Internal Storage"

    lifecycleScope.launch {
      val locations = try {
        StorageUtils.getAllStorageLocations(this@QuranAdvancedPreferenceActivity)
      } catch (e: Exception) {
        emptyList()
      }
      isMultiStorageState = locations.size > 1

      val appSize = withContext(Dispatchers.IO) {
        try {
          quranFileUtils.getAppUsedSpace(this@QuranAdvancedPreferenceActivity)
        } catch (e: Exception) {
          -1
        }
      }

      val primary = locations.firstOrNull()
      if (primary != null && appSize > 0) {
        val appMb = appSize / (1024 * 1024)
        val freeGb = primary.getFreeSpace() / 1024
        storageSummaryState = "Used: ${appMb} MB • Free: ${freeGb} GB"
      } else {
        storageSummaryState = currentStorageLocationState
      }
    }
  }

  private fun launchImportPicker() {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
      type = "*/*"
      putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/*", "text/*"))
    }
    importFileLauncher.launch(intent)
  }

  private fun exportBookmarks() {
    if (exportSubscription != null) return
    isExportingState = true

    exportSubscription = bookmarkImportExportModel.exportBookmarksObservable()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeWith(object : DisposableSingleObserver<Uri>() {
        override fun onSuccess(uri: Uri) {
          isExportingState = false
          exportSubscription = null
          shareExportedFile(uri, getString(R.string.prefs_export_title))
        }

        override fun onError(e: Throwable) {
          isExportingState = false
          exportSubscription = null
          ToastCompat.makeText(
            this@QuranAdvancedPreferenceActivity,
            R.string.export_data_error,
            Toast.LENGTH_LONG
          ).show()
        }
      })
  }

  private fun exportBookmarksCsv() {
    if (exportSubscription != null) return
    isExportingState = true

    exportSubscription = bookmarkImportExportModel.exportBookmarksCSVObservable()
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeWith(object : DisposableSingleObserver<Uri>() {
        override fun onSuccess(uri: Uri) {
          isExportingState = false
          exportSubscription = null
          shareExportedFile(uri, getString(R.string.prefs_export_csv_title))
        }

        override fun onError(e: Throwable) {
          isExportingState = false
          exportSubscription = null
          ToastCompat.makeText(
            this@QuranAdvancedPreferenceActivity,
            R.string.export_data_error,
            Toast.LENGTH_LONG
          ).show()
        }
      })
  }

  private fun shareExportedFile(uri: Uri, title: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      putExtra(Intent.EXTRA_STREAM, uri)
      setDataAndType(uri, "application/json")
    }
    val intents = packageManager.queryIntentActivities(shareIntent, 0)
    if (intents.size > 1) {
      startActivity(Intent.createChooser(shareIntent, title))
    } else {
      val exportedPath = File(getExternalFilesDir(null), "backups")
      val exported = getString(R.string.exported_data, exportedPath.toString())
      ToastCompat.makeText(this, exported, Toast.LENGTH_LONG).show()
    }
  }

  private fun sendDebugLogs() {
    if (logsSubscription != null) return

    logsSubscription = Observable.fromIterable(Timber.forest())
      .filter { tree: Timber.Tree? -> tree is RecordingLogTree }
      .firstElement()
      .map { tree: Timber.Tree -> (tree as RecordingLogTree).getLogs() }
      .map { logs: String ->
        "${QuranUtils.getDebugInfo(applicationContext, quranScreenInfo)}\n\n$logs"
      }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeWith(object : DisposableMaybeObserver<String>() {
        override fun onSuccess(logs: String) {
          logsSubscription = null
          val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.logs_email)))
            putExtra(Intent.EXTRA_TEXT, logs)
            putExtra(Intent.EXTRA_SUBJECT, "Quran Android Logs")
          }
          startActivity(
            Intent.createChooser(intent, getString(R.string.prefs_send_logs_title))
          )
        }

        override fun onError(e: Throwable) {
          logsSubscription = null
        }

        override fun onComplete() {
          logsSubscription = null
        }
      })
  }

  private fun onStorageLocationClicked() {
    ToastCompat.makeText(
      this,
      storageSummaryState ?: currentStorageLocationState,
      Toast.LENGTH_SHORT
    ).show()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    if (locationToWrite != null) {
      outState.putString(SI_LOCATION_TO_WRITE, locationToWrite)
    }
    super.onSaveInstanceState(outState)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  fun requestWriteExternalSdcardPermission(newLocation: String) {
    if (PermissionUtil.canRequestWriteExternalStoragePermission(this)) {
      QuranSettings.getInstance(this).setSdcardPermissionsDialogPresented()
      locationToWrite = newLocation
      ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        REQUEST_WRITE_TO_SDCARD_PERMISSION
      )
    } else {
      ToastCompat.makeText(this, R.string.please_grant_permissions, Toast.LENGTH_SHORT).show()
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    if (requestCode == REQUEST_WRITE_TO_SDCARD_PERMISSION) {
      if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && locationToWrite != null) {
        val fragment = supportFragmentManager.findFragmentById(R.id.content)
        if (fragment is QuranAdvancedSettingsFragment) {
          val location = locationToWrite
          if (location != null) {
            fragment.moveFiles(location)
          }
        }
      }
      locationToWrite = null
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun onDestroy() {
    exportSubscription?.dispose()
    logsSubscription?.dispose()
    super.onDestroy()
  }
}

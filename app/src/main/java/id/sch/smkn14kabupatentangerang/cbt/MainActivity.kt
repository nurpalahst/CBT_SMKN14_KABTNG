package id.sch.smkn14kabupatentangerang.cbt

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutLauncher: LinearLayout
    private lateinit var layoutWebContainer: LinearLayout
    private lateinit var tvModeStatus: TextView

    private var isStudentKioskMode = true
    private val URL_SISWA = "https://cbt.smkn14kabupatentangerang.sch.id/siswa/login.php"
    private val URL_GURU  = "https://cbt.smkn14kabupatentangerang.sch.id/admin/login.php"
    private val CUSTOM_UA = " CBT-SMKN14-Exambro/3.0 (Android; KioskLock)"
    private val PIN_PROKTOR_DEFAULT = "1414"

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_RESULT_CODE = 1001

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. BLOKIR SCREENSHOT & PEREKAMAN LAYAR (FLAG_SECURE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        // Jaga layar tetap menyala selama ujian
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        layoutLauncher     = findViewById(R.id.layoutLauncher)
        layoutWebContainer = findViewById(R.id.layoutWebContainer)
        webView            = findViewById(R.id.webView)
        progressBar        = findViewById(R.id.progressBar)
        tvModeStatus       = findViewById(R.id.tvModeStatus)

        val btnMasukSiswa  = findViewById<Button>(R.id.btnMasukSiswa)
        val btnMasukGuru   = findViewById<Button>(R.id.btnMasukGuru)
        val btnRefresh     = findViewById<Button>(R.id.btnRefresh)
        val btnKeluar      = findViewById<Button>(R.id.btnKeluar)

        setupWebView()

        // Tombol 1: Masuk Sebagai Peserta Ujian (Kiosk Lock Aktif)
        btnMasukSiswa.setOnClickListener {
            isStudentKioskMode = true
            startExamSession(URL_SISWA)
        }

        // Tombol 2: Masuk Sebagai Guru / Panitia / Proktor (Tanpa Kiosk Lock agar mudah kelola file)
        btnMasukGuru.setOnClickListener {
            isStudentKioskMode = false
            startExamSession(URL_GURU)
        }

        btnRefresh.setOnClickListener {
            webView.reload()
        }

        btnKeluar.setOnClickListener {
            if (isStudentKioskMode) {
                tampilkanDialogKeluarSiswa()
            } else {
                keluarAplikasi()
            }
        }
    }

    private fun startExamSession(targetUrl: String) {
        layoutLauncher.visibility = View.GONE
        layoutWebContainer.visibility = View.VISIBLE

        if (isStudentKioskMode) {
            tvModeStatus.text = "🔒 MODE UJIAN TERKUNCI (ANTI-CURANG AKTIF)"
            enableImmersiveFullscreen()
            // Aktifkan Android Screen Pinning / Lock Task Mode (Mengunci Home & Recent Apps)
            try {
                startLockTask()
            } catch (e: Exception) {
                Toast.makeText(this, "Sematkan layar untuk memulai ujian", Toast.LENGTH_SHORT).show()
            }
        } else {
            tvModeStatus.text = "🛡️ PORTAL MANAJEMEN GURU / PANITIA / PROKTOR"
        }

        val headers = mapOf("X-CBT-SMKN14-App" to "SMKN14-Verified")
        webView.loadUrl(targetUrl, headers)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val ws = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.databaseEnabled = true
        ws.loadsImagesAutomatically = true
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        ws.cacheMode = WebSettings.LOAD_DEFAULT
        ws.setSupportZoom(true)
        ws.builtInZoomControls = true
        ws.displayZoomControls = false

        // Pasang Custom User-Agent Rahasia agar dikenali oleh Server PHP SMKN 14
        ws.userAgentString = ws.userAgentString + CUSTOM_UA

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // Izinkan hanya domain sekolah SMKN 14 Kabupaten Tangerang
                if (url.contains("smkn14kabupatentangerang.sch.id")) {
                    val headers = mapOf("X-CBT-SMKN14-App" to "SMKN14-Verified")
                    view?.loadUrl(url, headers)
                    return true
                }
                Toast.makeText(this@MainActivity, "Tautan luar diblokir selama ujian!", Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Informasi CBT SMKN 14")
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result?.confirm() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Konfirmasi Ujian")
                    .setMessage(message)
                    .setPositiveButton("Ya") { _, _ -> result?.confirm() }
                    .setNegativeButton("Batal") { _, _ -> result?.cancel() }
                    .setCancelable(false)
                    .show()
                return true
            }

            // Dukungan Upload File CSV/Gambar untuk Mode Guru/Admin
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback
                val intent = fileChooserParams?.createIntent()
                return try {
                    if (intent != null) startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE)
                    true
                } catch (e: Exception) {
                    this@MainActivity.filePathCallback = null
                    false
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            val results = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun tampilkanDialogKeluarSiswa() {
        val currentUrl = webView.url ?: ""
        // Jika siswa sudah di halaman login atau selesai ujian, izinkan keluar langsung dengan konfirmasi
        if (currentUrl.contains("login.php") || currentUrl.contains("selesai.php") || currentUrl.contains("index.php")) {
            AlertDialog.Builder(this)
                .setTitle("Keluar Aplikasi CBT")
                .setMessage("Apakah Anda sudah menyelesaikan ujian dan ingin keluar dari aplikasi?")
                .setPositiveButton("Ya, Keluar") { _, _ -> keluarAplikasi() }
                .setNegativeButton("Batal", null)
                .show()
            return
        }

        // Jika masih di dalam lembar soal (kerjakan.php), wajib masukkan PIN Pengawas (1414)
        val inputPin = EditText(this)
        inputPin.hint = "Masukkan PIN Proktor (Default: 1414)"
        inputPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("🔒 Ujian Sedang Berlangsung!")
            .setMessage("Anda tidak dapat keluar aplikasi saat mengerjakan soal kecuali menyelesaikan ujian atau memasukkan PIN Proktor:")
            .setView(inputPin)
            .setPositiveButton("Buka Kunci & Keluar") { _, _ ->
                if (inputPin.text.toString().trim() == PIN_PROKTOR_DEFAULT) {
                    keluarAplikasi()
                } else {
                    Toast.makeText(this, "PIN Proktor tidak sesuai!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Lanjutkan Ujian", null)
            .show()
    }

    private fun keluarAplikasi() {
        try {
            stopLockTask()
        } catch (e: Exception) {}
        finishAndRemoveTask()
    }

    private fun enableImmersiveFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isStudentKioskMode && layoutWebContainer.visibility == View.VISIBLE) {
            enableImmersiveFullscreen()
        }
    }

    // Cegah tombol Back menutup aplikasi secara tidak sengaja saat ujian
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isStudentKioskMode && webView.canGoBack()) {
                webView.goBack()
                return true
            }
            Toast.makeText(this, "Tombol Kembali dikunci selama sesi ujian!", Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

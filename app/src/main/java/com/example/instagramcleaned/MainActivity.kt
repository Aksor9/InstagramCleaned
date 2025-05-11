package com.example.instagramcleaned // Reemplaza con tu nombre de paquete si es diferente

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.instagramcleaned.R
// Imports necesarios
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.activity.addCallback
import android.content.pm.ActivityInfo // Para la orientación
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.util.Log // Para logs de depuración si son necesarios

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var mainContainer: ConstraintLayout

    // Variables para la subida de archivos
    private var uploadMessage: ValueCallback<Array<Uri>>? = null

    // --- ActivityResultLauncher para seleccionar archivos ---
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        var results: Array<Uri>? = null
        // Comprobar si el usuario seleccionó algo
        if (result.resultCode == Activity.RESULT_OK) {
            val dataString: String? = result.data?.dataString
            val clipData: android.content.ClipData? = result.data?.clipData
            if (clipData != null) { // Múltiples archivos seleccionados
                results = Array(clipData.itemCount) { i ->
                    clipData.getItemAt(i).uri
                }
            } else if (dataString != null) { // Un solo archivo seleccionado
                results = arrayOf(Uri.parse(dataString))
            }
            // A veces la cámara devuelve la URI de otra forma (aunque no estamos lanzando cámara explícitamente aquí)
            // Esto es un fallback por si acaso
            if (results == null && result.data?.extras?.containsKey("data") == true) {
                // Manejar imagen de cámara (requiere más lógica si se usa ACTION_IMAGE_CAPTURE explícitamente)
                // Por ahora, con ACTION_GET_CONTENT, esto es menos probable
                println(">>> File Chooser: Camera data received (not fully handled in this basic setup)")
            }

        }
        uploadMessage?.onReceiveValue(results) // Devolver resultado (o null si canceló/falló)
        uploadMessage = null // Limpiar el callback
    }


    // --- Script JavaScript (Mismo que funcionaba, sin espaciador) ---
    private val instagramCleanerJs = """
    (function() {
        'use strict';

        // --- CONFIGURATION (Textos en Español) ---
        const ALL_CAUGHT_UP_TEXT = "Estás al día";
        const SUGGESTED_POSTS_TEXT = "Publicaciones sugeridas";
        const REELS_BUTTON_ARIA_LABEL = "Reels"; // Confirmado por el HTML

        // --- SCRIPT LOGIC ---
        const FEED_SELECTOR = 'main[role="main"]';
        const ELEMENT_SELECTOR = 'article, div.x1lliihq, div.xvbhtw8';

        let processingScheduled = false;
        let foundCaughtUpMarker = false;
        let markerElement = null;

        function hideElements() {
            // --- Ocultar Botón de Reels ---
             try {
                 const reelsButtonSelectorHas = 'a:has(svg[aria-label="' + REELS_BUTTON_ARIA_LABEL + '"])';
                 let reelsButton = document.querySelector(reelsButtonSelectorHas);

                 if (!reelsButton) {
                      const reelsSvgs = document.querySelectorAll('svg[aria-label="' + REELS_BUTTON_ARIA_LABEL + '"]');
                      reelsSvgs.forEach(svg => {
                           const anchor = svg.closest('a[href="/reels/"]');
                           if (anchor) {
                               reelsButton = anchor; return;
                           }
                      });
                 }

                 if (reelsButton && reelsButton.style.display !== 'none') {
                    const parentContainer = reelsButton.closest('div[role="listitem"], li, div.x1n2onr6');
                     if (parentContainer) {
                         // console.log(">>> Hiding Reels button container");
                         if (parentContainer.style.display !== 'none') parentContainer.style.display = 'none';
                     } else {
                         // console.log(">>> Hiding Reels button anchor itself");
                         if (reelsButton.style.display !== 'none') reelsButton.style.display = 'none';
                     }
                 }
            } catch (e) { console.error("Error hiding Reels button:", e); }

            // --- Lógica para Ocultar Contenido del Feed ---
            const feedContainer = document.querySelector(FEED_SELECTOR);
            if (!feedContainer) return;

            const potentialElements = Array.from(feedContainer.querySelectorAll(ELEMENT_SELECTOR));
            let startHidingIndex = -1;

            if (!foundCaughtUpMarker) {
                for (let i = 0; i < potentialElements.length; i++) {
                    const element = potentialElements[i];
                    const caughtUpSpan = Array.from(element.querySelectorAll('span')).find(
                        span => span.textContent.trim() === ALL_CAUGHT_UP_TEXT
                    );

                    if (caughtUpSpan) {
                        // console.log(">>> MARKER TEXT FOUND ('Estás al día') in element:", element);
                        markerElement = element;
                        if (markerElement.style.display !== 'none') markerElement.style.display = 'none';
                        foundCaughtUpMarker = true;
                        startHidingIndex = i + 1;
                        break;
                    }
                     const suggestedHeader = element.querySelector('h3');
                     if (!foundCaughtUpMarker && suggestedHeader && suggestedHeader.textContent === SUGGESTED_POSTS_TEXT) {
                         // console.log(">>> MARKER FOUND (Suggested Header)!");
                         markerElement = element;
                         if (markerElement.style.display !== 'none') markerElement.style.display = 'none';
                         foundCaughtUpMarker = true;
                         startHidingIndex = i + 1;
                         break;
                     }
                }
            } else {
                const currentMarkerIndex = potentialElements.findIndex(el => el === markerElement);
                startHidingIndex = (currentMarkerIndex !== -1) ? currentMarkerIndex + 1 : 0;
                 // if(currentMarkerIndex === -1) console.warn(">>> Marker element lost, hiding from index 0.");
            }

            if (startHidingIndex !== -1) {
                for (let i = startHidingIndex; i < potentialElements.length; i++) {
                    const elementToHide = potentialElements[i];
                    if (elementToHide.tagName.toLowerCase() === 'article') {
                         if (elementToHide.style.display !== 'none') {
                             elementToHide.style.display = 'none';
                         }
                    }
                }
            }

             feedContainer.querySelectorAll('article a[href*="/reel/"]').forEach(reelLink => {
                 const article = reelLink.closest('article');
                 if (article && article.style.display !== 'none') {
                     article.style.display = 'none';
                 }
             });
        }

        function scheduleProcessing() {
            if (processingScheduled) return;
            processingScheduled = true;
            requestAnimationFrame(() => {
                hideElements();
                processingScheduled = false;
            });
        }

        const observer = new MutationObserver((mutationsList) => {
            let relevantChange = false;
            for (const mutation of mutationsList) {
                if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                   relevantChange = true; break;
                }
            }
            if (relevantChange) { scheduleProcessing(); }
        });

         let lastUrl = location.href;
          const navigationObserver = new MutationObserver(() => {
            const url = location.href;
            if (url !== lastUrl) {
              lastUrl = url;
              // console.log(">>> Navigation detected, resetting marker flag.");
              foundCaughtUpMarker = false;
              markerElement = null;
              observer.disconnect();
              setTimeout(initializeFeedObserver, 500);
            }
          });

        function initializeFeedObserver() {
            const targetNode = document.querySelector(FEED_SELECTOR);
            if (targetNode) {
                // console.log(">>> Initializing feed observer.");
                hideElements();
                try { observer.disconnect(); } catch(e) {}
                observer.observe(targetNode, { childList: true, subtree: true });
            } else {
                setTimeout(initializeFeedObserver, 500);
            }
        }

        function initializeNavigationObserver() {
             try {
                 navigationObserver.observe(document.body, {subtree: true, childList: true});
             } catch (e) { console.error("Failed to start navigation observer:", e); }
        }

        const intervalId = setInterval(hideElements, 750);
        window.addEventListener('unload', () => { clearInterval(intervalId); });

        initializeFeedObserver();
        setTimeout(initializeNavigationObserver, 1000);

    })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        // --- Forzar Orientación Vertical ---
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_main)

        WebView.setWebContentsDebuggingEnabled(true) // Mantener para depuración

        mainContainer = findViewById(R.id.mainContainer)
        webView = findViewById(R.id.webView)

        // --- Aplicar Insets ---
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
        }

        // --- Configuración del WebView ---
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true // Permitir acceso a archivos para subidas
        webView.settings.allowContentAccess = true
        // --- Ocultar Scrollbars ---
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 8a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        // --- WebViewClient (para inyectar JS y errores) ---
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(instagramCleanerJs) { /* No hacer nada con el resultado */ }
                println(">>> Injected JavaScript on: $url")
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    super.onReceivedError(view, request, error)
                    val errorCode = error?.errorCode ?: 0
                    val description = error?.description ?: "Unknown Error"
                    val failingUrl = request?.url?.toString() ?: "Unknown URL"
                    println(">>> WebView Error: $errorCode - $description on $failingUrl")
                }
            }
        }

        // --- WebChromeClient (para File Chooser) ---
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // Si ya hay una petición pendiente, cancelarla
                uploadMessage?.onReceiveValue(null)
                uploadMessage = filePathCallback

                val intent = fileChooserParams?.createIntent() // Usa el intent creado por los parámetros si es posible
                if (intent != null) {
                    try {
                        // Permitir selección múltiple si se especifica
                        if (fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                        // Añadir categoría OPENABLE para asegurar que se puedan abrir los archivos
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        // Especificar que queremos imágenes (ajusta si necesitas otros tipos)
                        // intent.type = "image/*" // Ya suele venir en createIntent, pero por si acaso

                        println(">>> Launching File Chooser Intent")
                        fileChooserLauncher.launch(intent)
                        return true // Indicamos que hemos manejado la petición
                    } catch (e: Exception) {
                        println(">>> Cannot launch File Chooser Intent: $e")
                        uploadMessage?.onReceiveValue(null) // Notificar fallo
                        uploadMessage = null
                        return false // No pudimos manejarlo
                    }
                } else {
                    // Fallback si createIntent() falla (menos común)
                    val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*" // Solo imágenes como fallback
                    }
                    try {
                        println(">>> Launching File Chooser Intent (Fallback)")
                        fileChooserLauncher.launch(contentSelectionIntent)
                        return true
                    } catch (e: Exception) {
                        println(">>> Cannot launch File Chooser Intent (Fallback): $e")
                        uploadMessage?.onReceiveValue(null)
                        uploadMessage = null
                        return false
                    }
                }
            }
            // Puedes añadir aquí overrides para onGeolocationPermissionsShowPrompt si necesitas geolocalización
            // o para onPermissionRequest si necesitas cámara/microfono directamente desde JS
        }


        // --- Manejar Botón Atrás ---
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        // --- Cargar Instagram ---
        webView.loadUrl("https://www.instagram.com")
    }

    override fun onDestroy() {
        // Limpiar referencia para evitar memory leaks
        mainContainer.removeView(webView)
        webView.stopLoading()
        webView.settings.javaScriptEnabled = false
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
        println(">>> MainActivity - onDestroy")
    }

    // Logs opcionales
    /*
    override fun onResume() { super.onResume(); println(">>> MainActivity - onResume") }
    override fun onPause() { super.onPause(); println(">>> MainActivity - onPause") }
    override fun onStop() { super.onStop(); println(">>> MainActivity - onStop") }
    */
}
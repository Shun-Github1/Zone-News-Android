package com.searcher.zonenews.ui.web;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import android.webkit.JavascriptInterface;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.searcher.zonenews.R;
import com.searcher.zonenews.base.BaseActivity;
import com.searcher.zonenews.selfview.ProgressWebView;
import com.searcher.zonenews.utils.SharedPreferenceUtils;
import com.searcher.zonenews.utils.SwipeGestureHelper;
import com.jaeger.library.StatusBarUtil;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import android.widget.TextView;

import java.util.Objects;

public class WebActivity extends BaseActivity {
    private ProgressWebView mWebView;
    private String type;
    private boolean isTranslated = false;
    private String originalUrl = "";

    // MaterialToolbar
    private Toolbar toolbar;
    private ShapeableImageView mediaIcon;
    private TextView mediaName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        applyStatusBarStyle();
        type = getIntent().getStringExtra("type");
        initView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initView() {
        // Initialize MaterialToolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize media display views
        mediaIcon = findViewById(R.id.media_icon);
        mediaName = findViewById(R.id.media_name);

        // Enable back button and hide title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mWebView = findViewById(R.id.agree_web);

        WebSettings settings = this.mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setAllowContentAccess(true);
        // settings.setUseWideViewPort(true);
        settings.setDisplayZoomControls(false);
        settings.setDisplayZoomControls(false); // Remove deprecated ZoomDensity logic
        if (Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(settings.getMixedContentMode());
        }
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // Allow mixed content when translating
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        this.mWebView.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Always keep navigation inside this WebView (do NOT hand off to Chrome)
                if (url != null) {
                    view.loadUrl(url);
                }
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    view.loadUrl(request.getUrl().toString());
                }
                return true;
            }

            public void onPageFinished(WebView webView, String str) {
                webView.getSettings().setJavaScriptEnabled(true);
                // Keep title as original domain (publisher domain)
                super.onPageFinished(webView, str);
            }
        });
        if (!TextUtils.isEmpty(type)) {
            switch (type) {
                case "detail": {
                    settings.setUseWideViewPort(true);
                    String content = SharedPreferenceUtils.getString(Objects.requireNonNull(getMContext()), "content");
                    mWebView.loadDataWithBaseURL(null, getHtmlData(content), "text/html", "UTF-8", null);
                    break;
                }

                default: {
                    settings.setUseWideViewPort(true);
                    String url = getIntent().getStringExtra("url");
                    if (TextUtils.isEmpty(url)) {
                        Toast.makeText(this, R.string.open_in_browser, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    if (!url.startsWith("http")) {
                        url = "https://" + url;
                    }
                    originalUrl = url;

                    // Get media information from intent
                    String publisherIcon = getIntent().getStringExtra("publisherIcon");
                    String publisherName = getIntent().getStringExtra("publisherName");

                    // Display media information in toolbar
                    if (publisherIcon != null && !publisherIcon.isEmpty()) {
                        Glide.with(this)
                                .load(publisherIcon)
                                .error(R.drawable.ic_image_not_supported_24)
                                .into(mediaIcon);
                    }

                    if (publisherName != null && !publisherName.isEmpty()) {
                        mediaName.setText(publisherName);
                    } else {
                        // Fallback to host if no publisher name
                        try {
                            Uri uri = Uri.parse(originalUrl);
                            String host = uri.getHost();
                            if (host != null) {
                                mediaName.setText(host);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    mWebView.loadUrl(originalUrl);
                    break;
                }
            }

        }

        // Setup swipe gesture for back navigation
        setupSwipeGesture();

    }

    private String getHtmlData(String str) {
        return "<html>"
                + "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> <style>img{max-width: 100%; width:auto; height:auto!important;}</style></head>"
                + "<body>" + str + "</body></html>";
    }

    /**
     * JavaScript Interface for WebView communication
     */
    public class WebAppInterface {
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(WebActivity.this, toast, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load JavaScript from raw resources
     */
    private String loadJavaScriptFromRaw(int resourceId) {
        StringBuilder script = new StringBuilder();
        try {
            InputStream inputStream = getResources().openRawResource(resourceId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                script.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            // Error loading JavaScript resource
        }
        return script.toString();
    }

    // Legacy widget toggle no longer used; kept as empty to avoid references
    private void toggleGoogleTranslate() {
    }

    /**
     * Setup swipe gesture detection for back navigation
     */
    private void setupSwipeGesture() {
        SwipeGestureHelper swipeGestureHelper = new SwipeGestureHelper(() -> {
            // Handle swipe right gesture - finish the activity (go back)
            finish();
            return null; // Explicitly return null for Unit type
        });

        // Apply swipe gesture to the WebView
        mWebView.setOnTouchListener(swipeGestureHelper);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_browser_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_share) {
            String url = originalUrl;
            if (url == null)
                url = "";
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
            return true;
        } else if (id == R.id.action_translate) {
            if (TextUtils.isEmpty(originalUrl))
                return true;
            String toLoad;
            if (!isTranslated) {
                String base = "https://translate.google.com/translate?sl=auto&tl=en&u=";
                toLoad = base + Uri.encode(originalUrl);
            } else {
                toLoad = originalUrl;
            }
            isTranslated = !isTranslated;
            mWebView.loadUrl(toLoad);
            return true;
        } else if (id == R.id.action_open_in_browser) {
            String url = originalUrl;
            if (url == null)
                url = "";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

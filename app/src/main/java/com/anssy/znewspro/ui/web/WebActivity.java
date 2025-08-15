package com.anssy.znewspro.ui.web;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


import com.anssy.znewspro.R;
import com.anssy.znewspro.base.BaseActivity;
import com.anssy.znewspro.selfview.ProgressWebView;
import com.anssy.znewspro.utils.SharedPreferenceUtils;
import com.jaeger.library.StatusBarUtil;

import java.util.Objects;


public class WebActivity extends BaseActivity {
    private ProgressWebView mWebView;
    private TextView mTitleTv;
    private String type;
    private ImageView ivShare, ivTranslate, ivOpenInBrowser;
    private boolean isTranslated = false;
    private String originalUrl = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        applyStatusBarStyle();
        type = getIntent().getStringExtra("type");
        initView();
    }
    private TextView mTransTv;
    @SuppressLint("SetJavaScriptEnabled")
    private void initView() {
        mTitleTv = findViewById(R.id.title_tv);
        mWebView = findViewById(R.id.agree_web);
        mTransTv = findViewById(R.id.trans_tv);
        mTransTv.setVisibility(View.GONE);
        ivShare = findViewById(R.id.iv_share);
        ivTranslate = findViewById(R.id.iv_translate);
        ivOpenInBrowser = findViewById(R.id.iv_open_in_browser);

        ivShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = originalUrl;
                if (url == null) url = "";
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
            }
        });
        ivTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(originalUrl)) return;
                String toLoad;
                if (!isTranslated) {
                    String base = "https://translate.google.com/translate?sl=auto&tl=en&u=";
                    toLoad = base + Uri.encode(originalUrl);
                } else {
                    toLoad = originalUrl;
                }
                isTranslated = !isTranslated;
                mWebView.loadUrl(toLoad);
            }
        });
        ivOpenInBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = originalUrl;
                if (url == null) url = "";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

        WebSettings settings = this.mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setAllowContentAccess(true);
//        settings.setUseWideViewPort(true);
        settings.setDisplayZoomControls(false);
        int screenDensity = getResources().getDisplayMetrics().densityDpi;
        WebSettings.ZoomDensity zoomDensity = WebSettings.ZoomDensity.MEDIUM;
        switch (screenDensity) {
            case DisplayMetrics.DENSITY_LOW:
                zoomDensity = WebSettings.ZoomDensity.CLOSE;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                zoomDensity = WebSettings.ZoomDensity.MEDIUM;
                break;
            case DisplayMetrics.DENSITY_HIGH:
                zoomDensity = WebSettings.ZoomDensity.FAR;
                break;
        }
        settings.setDefaultZoom(zoomDensity);
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
        if (!TextUtils.isEmpty(type)){
            switch (type) {
                case "detail": {
                    settings.setUseWideViewPort(true);
                    mTitleTv.setText(R.string.details);
                    String content = SharedPreferenceUtils.getString(Objects.requireNonNull(getMContext()), "content");
                    mWebView.loadDataWithBaseURL(null, getHtmlData(content), "text/html", "UTF-8", null);
                    break;
                }
                
                default: {
                    settings.setUseWideViewPort(true);
                    mTransTv.setVisibility(View.GONE);
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
                    try {
                        Uri uri = Uri.parse(originalUrl);
                        String host = uri.getHost();
                        if (host != null) mTitleTv.setText(host);
                    } catch (Exception ignored) {}
                    mWebView.loadUrl(originalUrl);
                    break;
                }
            }

        }

    }


    private String getHtmlData(String str) {
        return "<html>" + "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> <style>img{max-width: 100%; width:auto; height:auto!important;}</style></head>" + "<body>" + str + "</body></html>";
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
            Log.e("WebActivity", "Error loading JavaScript", e);
        }
        return script.toString();
    }

    // Legacy widget toggle no longer used; kept as empty to avoid references
    private void toggleGoogleTranslate() {}
}

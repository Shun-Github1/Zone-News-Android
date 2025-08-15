// Google Translate Widget Integration Script
function initGoogleTranslate() {
    // Check if Google Translate is already loaded
    if (window.google && window.google.translate) {
        console.log('Google Translate already loaded');
        return;
    }
    
    // Create Google Translate element
    var translateDiv = document.createElement('div');
    translateDiv.id = 'google_translate_element';
    translateDiv.style.cssText = `
        position: fixed;
        top: 60px;
        right: 10px;
        z-index: 9999;
        background: white;
        border: 1px solid #ccc;
        border-radius: 8px;
        padding: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        max-width: 250px;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    `;
    
    // Insert at the beginning of body
    if (document.body) {
        document.body.insertBefore(translateDiv, document.body.firstChild);
    } else {
        document.addEventListener('DOMContentLoaded', function() {
            document.body.insertBefore(translateDiv, document.body.firstChild);
        });
    }
    
    // Load Google Translate script
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
    document.getElementsByTagName('head')[0].appendChild(script);
    
    // Initialize Google Translate
    window.googleTranslateElementInit = function() {
        new google.translate.TranslateElement({
            pageLanguage: 'auto',
            includedLanguages: 'en,zh,zh-CN,zh-TW,ja,ko,es,fr,de,pt,ru,ar,hi,th,vi',
            layout: google.translate.TranslateElement.InlineLayout.SIMPLE,
            autoDisplay: false
        }, 'google_translate_element');
        
        // Style the Google Translate widget
        setTimeout(function() {
            var closeBtn = document.createElement('button');
            closeBtn.innerHTML = '×';
            closeBtn.style.cssText = `
                position: absolute;
                top: -8px;
                right: -8px;
                width: 24px;
                height: 24px;
                border: none;
                background: #f44336;
                color: white;
                border-radius: 50%;
                cursor: pointer;
                font-size: 14px;
                line-height: 1;
                font-weight: bold;
                box-shadow: 0 2px 4px rgba(0,0,0,0.2);
            `;
            closeBtn.onclick = function() {
                translateDiv.style.display = 'none';
            };
            translateDiv.style.position = 'relative';
            translateDiv.appendChild(closeBtn);
            
            // Style the translate select dropdown
            var selectElements = translateDiv.querySelectorAll('select');
            for (var i = 0; i < selectElements.length; i++) {
                selectElements[i].style.cssText = `
                    padding: 4px;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    background: white;
                    font-size: 12px;
                    margin: 2px;
                `;
            }
        }, 1500);
    };
}

function toggleGoogleTranslate() {
    var translateDiv = document.getElementById('google_translate_element');
    if (translateDiv) {
        translateDiv.style.display = translateDiv.style.display === 'none' ? 'block' : 'none';
    } else {
        initGoogleTranslate();
    }
}

// Auto-initialize if called directly
if (typeof Android !== 'undefined') {
    initGoogleTranslate();
}
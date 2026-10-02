package io.legado.app.browser.engine

/**
 * Site-specific layout fixes injected after page load (SPEC-013).
 * Apple.com sticky globalnav often looks vertically squashed under SOFTWARE /
 * imperfect viewport; enforce a minimum bar height as a belt-and-suspenders fix.
 */
object BrowserSiteCssFixes {
    fun scriptForUrl(url: String): String? {
        val lower = url.lowercase()
        if (!lower.contains("apple.com")) return null
        return APPLE_NAV_MIN_HEIGHT_JS
    }

    // language=JavaScript
    private const val APPLE_NAV_MIN_HEIGHT_JS = """
(function(){
  try {
    if (document.getElementById('browsesth-apple-nav-fix')) return;
    var s = document.createElement('style');
    s.id = 'browsesth-apple-nav-fix';
    s.textContent = [
      'nav, header, .globalnav, #globalnav, .ac-gn-header, #ac-globalnav {',
      '  min-height: 48px !important;',
      '}',
      '.globalnav-content, .ac-gn-content, #ac-gn-cupcake {',
      '  min-height: 48px !important;',
      '  align-items: center !important;',
      '}',
      '.globalnav-link, .ac-gn-link, .globalnav-image, .ac-gn-apple {',
      '  min-height: 44px !important;',
      '  display: inline-flex !important;',
      '  align-items: center !important;',
      '}'
    ].join('\\n');
    (document.head || document.documentElement).appendChild(s);
  } catch (e) {}
})();
"""
}

package io.legado.app.browser.engine

/**
 * Site-specific layout fixes injected after page load (SPEC-013 / SPEC-020 / SPEC-073).
 * Apple.com sticky globalnav often looks vertically squashed under SOFTWARE /
 * imperfect viewport; enforce a healthier bar height as belt-and-suspenders.
 * Not injected on history back/forward (SPEC-073) to avoid scroll reset.
 */
object BrowserSiteCssFixes {
    fun scriptForUrl(url: String): String? {
        val lower = url.lowercase()
        if (lower.contains("apple.com")) return APPLE_NAV_MIN_HEIGHT_JS
        return null
    }

    // language=JavaScript
    private const val APPLE_NAV_MIN_HEIGHT_JS = """
(function(){
  try {
    var id = 'browsesth-apple-nav-fix';
    var old = document.getElementById(id);
    if (old) old.remove();
    var s = document.createElement('style');
    s.id = id;
    s.textContent = [
      'html { -webkit-text-size-adjust: 100% !important; text-size-adjust: 100% !important; }',
      '#globalnav, .globalnav, nav#globalnav, header.globalnav,',
      '#ac-globalnav, .ac-gn-header, [data-analytics-activitymap-region-id=\"global nav\"] {',
      '  min-height: 52px !important;',
      '  height: auto !important;',
      '  max-height: none !important;',
      '  box-sizing: border-box !important;',
      '}',
      '#globalnav .globalnav-content, .globalnav-content, .ac-gn-content,',
      '#ac-gn-cupcake, .globalnav-list, .ac-gn-list {',
      '  min-height: 52px !important;',
      '  height: auto !important;',
      '  align-items: center !important;',
      '  box-sizing: border-box !important;',
      '}',
      '.globalnav-link, .ac-gn-link, .globalnav-image, .ac-gn-apple,',
      '.globalnav-item, .ac-gn-item, a.globalnav-link-apple {',
      '  min-height: 44px !important;',
      '  line-height: normal !important;',
      '  display: inline-flex !important;',
      '  align-items: center !important;',
      '  box-sizing: border-box !important;',
      '}',
      '.globalnav-link-text, .ac-gn-link-text {',
      '  line-height: 1.2 !important;',
      '}'
    ].join('\\n');
    (document.head || document.documentElement).appendChild(s);
  } catch (e) {}
})();
"""
}

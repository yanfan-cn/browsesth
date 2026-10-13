# PLAN-113: Implement SPEC-113

| Plan ID | PLAN-113 |
| Spec | [../features/SPEC-113.md] |
| Status | Ready |
| Log | [../logs/LOG-113.md] |
| Updated | 2026-10-12 |

## Evidence

- capsuleAutoCollapse default true: ReadAloudSettings.kt, ReadAloudSettingsRepository.kt
- Translate default true: BrowserReaderPrefs.autoTranslateOnOpen
- More moved into SystemMenuPage: SystemMenuPage.kt lines ~90-100; title bar stubs in ReadBookMenuBarTitle.kt
- Bookmarks mixed: BrowserBookmarksScreen.kt merges BrowseStore + Room
- Join list icon LibraryBooks: SystemMenuPage.kt add_to_bookshelf
- Capsule settings only: ReadAloudCapsule.kt width 128dp
- TTS engines nav: OpenTtsEnginesAndVoices + MainRouteRequestBus / MainRouteCloudTtsEngines

## Approach

1. Defaults + migrate flags spec113_v1 in App.kt / BrowserReaderPrefs.
2. Capsule: add voice button + bus ReadAloudVoiceRequestBus → OpenTtsEnginesAndVoices path (same as settings engines entry).
3. Restore More in title bar → ShowSheet(MoreActions); strip SystemMenuPage More* rows; add MoreConfig action in ReaderMoreActionsSheet.
4. Strings zh: 加入列表; toolbox/chrome: 加入收藏; icons → Symbols.Book.
5. Refactor BrowserBookmarksScreen to HorizontalPager(收藏, 书签) with TabRow; split list sources; folder/add UI only on 收藏.

## Steps

| # | Result | Evidence |
|---|--------|----------|
| 1 | Spec/Plan written | files |
| 2 | Defaults + migrate | code + compile |
| 3 | Capsule voice + More fix | code |
| 4 | Icons/strings + bookmarks split | code |
| 5 | assembleAppDebug + LOG | APK + LOG-113 |

## Tests

- TC-01..07 map to AC-01..07 (manual device)
- Build: GRADLE_USER_HOME=D:\\cursor\\.gradle-home; assembleAppDebug

## Rollback

Revert SPEC-113 migrate flags / UI wiring; keep data stores intact.

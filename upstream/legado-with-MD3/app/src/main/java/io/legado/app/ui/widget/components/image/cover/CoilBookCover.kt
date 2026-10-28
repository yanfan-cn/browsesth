package io.legado.app.ui.widget.components.image.cover

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.File
import io.legado.app.ui.widget.components.image.cover.CoverPaletteStore
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withSave
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalAppUiConfiguration
import org.koin.compose.koinInject
import io.legado.app.model.BookCover as BookCoverModel

private const val SharedCoverRadiusCacheMaxSize = 256

/** 鐢ㄦ埛鍦ㄦ崲灏侀潰椤甸€夌殑鈥滈粯璁ゅ皝闈⑩€濓紝涓庣┖鍦板潃涓€鏍疯〃绀鸿繖鏈功娌℃湁鐪熷疄灏侀潰銆?*/
internal const val DefaultCoverPath = "use_default_cover"
private val sharedCoverRadiusCache = mutableStateMapOf<String, Dp>()

/**
 * 杩欐湰涔︽槸鍚︽湁鐪熷疄灏侀潰鍦板潃鍙姞杞姐€?
 *
 * 涓?[usesDefaultBookCover] 鐨勫尯鍒細杩欓噷鍙湅鍦板潃鏈韩锛屼笉璇?Compose 閰嶇疆锛?
 * 鍥犳棰勭儹杩欑被闈炵粍鍚堝満鏅篃鑳藉鐢ㄥ悓涓€鍒ゅ畾銆?
 */
internal fun isDefaultCoverPath(path: String?): Boolean =
    path.isNullOrBlank() || path == DefaultCoverPath

/**
 * 灏侀潰鍦ㄦ簮椤甸潰鐨勫渾瑙掔紦瀛樿鍙栧叆鍙ｏ細灏侀潰绂诲紑婧愰〉闈紙Visible鈫扸isible 瀹氭牸锛夋椂鍐欏叆锛?
 * 闃呰绔?sharedBounds 鐨勮捣濮嬪渾瑙掔敱瀹冩彁渚涳紝淇濊瘉杞満涓ょ鍦嗚琛旀帴杩炵画銆?
 */
internal fun sharedCoverSourceRadius(sharedCoverKey: String?): Dp? =
    sharedCoverKey?.let { sharedCoverRadiusCache[it] }

@Composable
internal fun usesDefaultBookCover(path: String?): Boolean {
    return LocalAppUiConfiguration.current.cover.useDefaultCover || isDefaultCoverPath(path)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BookCoverImage(
    name: String?,
    author: String?,
    path: String?,
    modifier: Modifier = Modifier,
    sourceOrigin: String? = null,
    memoryCacheKey: String? = null,
    // 鏈功 bookUrl锛堝埆鍚嶇紦瀛橀敭锛変笌涔︽灦鏈湴浼樺厛鏍囧織锛岄€忎紶缁?buildCoverImageRequest銆?
    bookUrl: String? = null,
    preferCache: Boolean = false,
    ignoreUseDefaultCover: Boolean = false,
    showLoadingPlaceholder: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
    onLoadFinish: (() -> Unit)? = null,
    onSuccess: (() -> Unit)? = null,
    onError: (() -> Unit)? = null,
    sharedCoverKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    requestBuilder: ImageRequest.Builder.() -> Unit = {},
) {
    val context = LocalContext.current
    val isNight = LegadoTheme.isDark
    val coverSettings = LocalAppUiConfiguration.current.cover

    // SPEC-185: nothing extractable (blank, default marker, or the local cover file is
    // gone) → random palette cover; the seed keeps one book on one cover.
    val paletteCover = remember(name, author, bookUrl, path) {
        val seed = name ?: author ?: bookUrl
        val missingLocal = path != null && path.startsWith("/") && !File(path).exists()
        if (seed != null && (path.isNullOrBlank() || path == DefaultCoverPath || missingLocal)) {
            CoverPaletteStore.pickForSeed(context, seed)
        } else {
            null
        }
    }
    val effectivePath = paletteCover ?: path

    // SPEC-160: color/gradient covers skip default-cover substitution.
    if (GeneratedCover.isGenerated(effectivePath)) {
        Box(
            modifier = modifier.then(
                with(sharedTransitionScope) {
                    if (this != null && animatedVisibilityScope != null && sharedCoverKey != null) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(sharedCoverKey),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    } else {
                        Modifier
                    }
                }
            )
        ) {
            GeneratedCoverFace(path = effectivePath!!, title = name, modifier = Modifier.fillMaxSize())
            onLoadFinish?.invoke()
            onSuccess?.invoke()
        }
        return
    }

    val useDefault = (!ignoreUseDefaultCover && coverSettings.useDefaultCover) ||
            effectivePath.isNullOrBlank() ||
            effectivePath == DefaultCoverPath
    val finalPath = if (useDefault) null else effectivePath
    val defaultCoverPaths =
        if (isNight) coverSettings.defaultCoverDark else coverSettings.defaultCover

    val randomPath = remember(name, author, path, isNight, defaultCoverPaths) {
        BookCoverModel.getRandomDefaultPath(
            seed = name ?: author ?: path ?: "",
            isNight = isNight
        )
    }

    val hasCustomDefault = !randomPath.isNullOrBlank()
    val customDefaultMemoryCacheKey =
        if (finalPath == null && sharedCoverKey != null) {
            "$sharedCoverKey:default:$randomPath"
        } else {
            randomPath
        }
    var isOnlineCoverLoaded by remember(finalPath) { mutableStateOf(false) }
    var onlineCoverLoadFailed by remember(finalPath) { mutableStateOf(false) }

    // SPEC-194: 网络封面加载失败也回落配色封面（操作者 2026-10-28 重申：
    // 网页/本地书籍无图可提取时一律配色封面，含阅读列表卡/页与阅读记录）。
    val failurePalette = remember(finalPath) {
        val seed = name ?: author ?: bookUrl
        if (seed != null) CoverPaletteStore.pickForSeed(context, seed) else null
    }

    LaunchedEffect(finalPath) {
        if (finalPath == null) {
            isOnlineCoverLoaded = false
            onlineCoverLoadFailed = false
        }
    }

    val isUsingDefaultCover = finalPath == null || onlineCoverLoadFailed
    val showLoadingDefault = sharedCoverKey == null && !isOnlineCoverLoaded
    val showCustomDefault = hasCustomDefault &&
        !isOnlineCoverLoaded &&
        (isUsingDefaultCover || showLoadingDefault)
    val showDefaultIcon = !hasCustomDefault &&
        (
            isUsingDefaultCover ||
                (showLoadingPlaceholder && showLoadingDefault)
        )
    Box(
        modifier = modifier.then(
            with(sharedTransitionScope) {
                if (this != null && animatedVisibilityScope != null && sharedCoverKey != null) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(sharedCoverKey),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                } else {
                    Modifier
                }
            }
        )
    ) {
        if (showCustomDefault) {
            AsyncImage(
                model = buildCoverImageRequest(
                    context = context,
                    data = randomPath,
                    sourceOrigin = null,
                    loadOnlyWifi = false,
                    crossfade = showLoadingPlaceholder,
                    memoryCacheKey = customDefaultMemoryCacheKey,
                ),
                contentDescription = null,
                imageLoader = koinInject(),
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showDefaultIcon) {
            SymbolIcon(Symbols.Book,
                contentDescription = null,
                tint = LegadoTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxSize(0.35f)
                    .align(Alignment.Center)
            )
        }

        if (finalPath != null) {
            if (onlineCoverLoadFailed && failurePalette != null) {
                // SPEC-194: 加载失败不再显示默认图标，改用配色封面（自带书名绘制）。
                GeneratedCoverFace(
                    path = failurePalette,
                    title = name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = buildCoverImageRequest(
                        context = context,
                        data = finalPath,
                        sourceOrigin = sourceOrigin,
                        loadOnlyWifi = false,
                        crossfade = showLoadingPlaceholder,
                        memoryCacheKey = coverMemoryCacheKey(
                            sharedCoverKey = sharedCoverKey,
                            explicitKey = memoryCacheKey,
                            path = finalPath,
                        ),
                        bookUrl = bookUrl,
                        preferCache = preferCache,
                        configure = requestBuilder,
                    ),
                    contentDescription = null,
                    imageLoader = koinInject(),
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = {
                        isOnlineCoverLoaded = true
                        onlineCoverLoadFailed = false
                        onSuccess?.invoke()
                        onLoadFinish?.invoke()
                    },
                    onError = {
                        isOnlineCoverLoaded = false
                        onlineCoverLoadFailed = true
                        onError?.invoke()
                        onLoadFinish?.invoke()
                    }
                )
            }
        } else {
            LaunchedEffect(Unit) {
                onLoadFinish?.invoke()
            }
        }
    }
}

// 鏀规垚BookCover
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CoilBookCover(
    name: String?,
    author: String?,
    path: String?,
    radius: Dp = 4.dp,
    modifier: Modifier = Modifier.width(64.dp),
    sourceOrigin: String? = null,
    // 鏈功 bookUrl + 涔︽灦鏈湴浼樺厛鏍囧織锛岄€忎紶缁?BookCoverImage
    bookUrl: String? = null,
    preferCache: Boolean = false,
    onLoadFinish: (() -> Unit)? = null,
    onError: (() -> Unit)? = null,
    ignoreUseDefaultCover: Boolean = false,
    showLoadingPlaceholder: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedCoverKey: String? = null,
    /**
     * 鍐呭妯＄硦鍗婂緞锛屼綔鐢ㄤ簬灏侀潰鍥句笌鍗犱綅鏂囧瓧杩欎簺**鍏变韩鍏冪礌鍐呴儴鐨勫瓙鑺傜偣**銆?
     *
     * 涔嬫墍浠ヨ浼犺繘鏉ヨ€屼笉鏄璋冪敤鏂瑰湪澶栭潰濂?`Modifier.blur`锛氬叡浜厓绱犺浆鍦烘椂锛?
     * overlay 鍙細鎼繍 sharedBounds 鑺傜偣鑷繁鐨勫唴瀹癸紝鍔犲湪绁栧厛涓婄殑妯＄硦浼氳钀戒笅锛?
     * 琛ㄧ幇灏辨槸"鍔ㄧ敾涓€寮€濮嬫ā绯婄獊鐒舵病浜?銆?
     */
    contentBlur: Dp = 0.dp,
    /**
     * 鐩栧湪灏侀潰涔嬩笂鐨勫彔鍔犲眰锛堥伄缃┿€佺偣闃点€侀攣鏍団€︼級锛屾覆鏌撳湪鍏变韩鑺傜偣**鍐呴儴**銆?
     *
     * 鏀炬垚鍏勫紵鑺傜偣鐨勮瘽杞満鏃朵笉浼氳 overlay 甯﹁蛋锛屼細鍑虹幇"瑁呴グ鍋滃湪鍘熷湴銆佸彧鏈夊皝闈㈠湪椋?銆?
     */
    overlayContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val coverSettings = LocalAppUiConfiguration.current.cover
    val isNight = LegadoTheme.isDark

    // SPEC-185: same no-cover fallback as BookCoverImage (stable seed per book).
    val paletteCover = remember(name, author, bookUrl, path) {
        val seed = name ?: author ?: bookUrl
        val missingLocal = path != null && path.startsWith("/") && !File(path).exists()
        if (seed != null && (path.isNullOrBlank() || path == DefaultCoverPath || missingLocal)) {
            CoverPaletteStore.pickForSeed(context, seed)
        } else {
            null
        }
    }
    val effectivePath = paletteCover ?: path

    // SPEC-160: generated color covers are first-class paths.
    if (GeneratedCover.isGenerated(effectivePath)) {
        val transitionRadius = rememberSharedCoverTransitionRadius(
            sharedCoverKey = sharedCoverKey,
            radius = radius,
            animatedVisibilityScope = animatedVisibilityScope
        )
        val shape = remember(transitionRadius) { RoundedCornerShape(transitionRadius) }
        Box(
            modifier = modifier
                .aspectRatio(5f / 7f)
                .then(
                    with(sharedTransitionScope) {
                        if (this != null && animatedVisibilityScope != null && sharedCoverKey != null) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(sharedCoverKey),
                                animatedVisibilityScope = animatedVisibilityScope,
                                clipInOverlayDuringTransition = OverlayClip(shape)
                            )
                        } else Modifier
                    }
                )
                .then(
                    if (coverSettings.showShadow) {
                        Modifier.shadow(4.dp, shape)
                    } else Modifier
                )
                .clip(shape)
        ) {
            GeneratedCoverFace(
                path = effectivePath!!,
                title = name,
                modifier = Modifier.fillMaxSize(),
            )
            overlayContent?.invoke(this)
            onLoadFinish?.invoke()
        }
        return
    }

    val useDefault = (!ignoreUseDefaultCover && coverSettings.useDefaultCover) ||
            effectivePath.isNullOrBlank() ||
            effectivePath == DefaultCoverPath
    val finalPath = if (useDefault) null else effectivePath
    val defaultCoverPaths =
        if (isNight) coverSettings.defaultCoverDark else coverSettings.defaultCover

    val randomPath = remember(name, author, effectivePath, isNight, defaultCoverPaths) {
        BookCoverModel.getRandomDefaultPath(
            seed = name ?: author ?: effectivePath ?: "",
            isNight = isNight
        )
    }

    val hasCustomDefault = !randomPath.isNullOrBlank()
    var isOnlineCoverLoaded by remember(finalPath) { mutableStateOf(false) }
    var onlineCoverLoadFailed by remember(finalPath) { mutableStateOf(false) }

    LaunchedEffect(finalPath) {
        if (finalPath == null) {
            isOnlineCoverLoaded = false
            onlineCoverLoadFailed = false
        }
    }

    val transitionRadius = rememberSharedCoverTransitionRadius(
        sharedCoverKey = sharedCoverKey,
        radius = radius,
        animatedVisibilityScope = animatedVisibilityScope
    )
    val shape = remember(transitionRadius) { RoundedCornerShape(transitionRadius) }
    val contentBlurModifier = if (contentBlur > 0.dp) {
        Modifier.blur(contentBlur, BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(5f / 7f)
            .then(
                with(sharedTransitionScope) {
                    if (this != null && animatedVisibilityScope != null && sharedCoverKey != null) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(sharedCoverKey),
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition = OverlayClip(shape)
                        )
                    } else Modifier
                }
            )
            .then(
                if (coverSettings.showShadow) {
                    Modifier.shadow(4.dp, shape)
                } else Modifier
            )
            .background(
                if (!hasCustomDefault && !isOnlineCoverLoaded) {
                    LegadoTheme.colorScheme.surfaceContainerLow
                } else Color.Transparent,
                shape
            )
            .clip(shape)
    ) {
        BookCoverImage(
            name = name,
            author = author,
            path = path,
            modifier = Modifier
                .fillMaxSize()
                .then(contentBlurModifier),
            sourceOrigin = sourceOrigin,
            bookUrl = bookUrl,
            preferCache = preferCache,
            ignoreUseDefaultCover = ignoreUseDefaultCover,
            showLoadingPlaceholder = showLoadingPlaceholder,
            onSuccess = {
                isOnlineCoverLoaded = true
                onlineCoverLoadFailed = false
                onLoadFinish?.invoke()
            },
            onError = {
                isOnlineCoverLoaded = false
                onlineCoverLoadFailed = true
                onError?.invoke()
                onLoadFinish?.invoke()
            },
            sharedCoverKey = sharedCoverKey
        )

        if (
            finalPath == null ||
            (
                // SPEC-194: 加载失败时内层已渲染配色封面（自带书名），不再叠文字占位。
                sharedCoverKey == null &&
                    showLoadingPlaceholder &&
                    !isOnlineCoverLoaded
                )
        ) {
            // 鍗犱綅鏂囧瓧锛堥粯璁ゅ皝闈笂鐨勪功鍚?浣滆€咃級涔熶竴璧锋ā绯婏細
            // 瀹冮湶鐨勬槸鐪熷疄瀛楃涓诧紝閿佸畾鎬佷笉鑳芥瘮姝ｅ父鎬佹洿娓呮櫚
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(contentBlurModifier)
            ) {
                CoverTextOverlay(
                    name = name,
                    author = author,
                    isNight = isNight
                )
            }
        }

        // 閬僵/鐐归樀/閿佹爣绛夊彔鍔犲眰娓叉煋鍦ㄥ叡浜妭鐐瑰唴閮紝杞満鏃朵細闅忓皝闈竴璧风Щ鍔?
        overlayContent?.invoke(this)
    }
}


/**
 * 杞満涓ょ鐨勫渾瑙掞細璧风偣鐢ㄦ簮椤甸潰缂撳瓨涓嬫潵鐨勫渾瑙掞紝缁堢偣鐢ㄦ湰鑺傜偣鐨?[radius]锛?
 * 鏈熼棿闅忚浆鍦鸿繘搴︽彃鍊硷紝閬垮厤涓ょ鍦嗚涓嶄竴鑷存椂璺冲彉銆傝劚鏁忓皝闈㈠鐢ㄥ悓涓€瀹炵幇銆?
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberSharedCoverTransitionRadius(
    sharedCoverKey: String?,
    radius: Dp,
    animatedVisibilityScope: AnimatedVisibilityScope?
): Dp {
    if (sharedCoverKey == null || animatedVisibilityScope == null) {
        return radius
    }

    val transition = animatedVisibilityScope.transition
    val startRadius = sharedCoverRadiusCache[sharedCoverKey] ?: radius
    val animatedRadiusValue by transition.animateFloat(
        label = "book-cover-corner-radius"
    ) { state ->
        if (state == EnterExitState.Visible) radius.value else startRadius.value
    }

    LaunchedEffect(
        sharedCoverKey,
        radius,
        transition.currentState,
        transition.targetState
    ) {
        if (
            transition.currentState == EnterExitState.Visible &&
            transition.targetState == EnterExitState.Visible
        ) {
            sharedCoverRadiusCache[sharedCoverKey] = radius
            if (sharedCoverRadiusCache.size > SharedCoverRadiusCacheMaxSize) {
                sharedCoverRadiusCache.keys
                    .firstOrNull { it != sharedCoverKey }
                    ?.let(sharedCoverRadiusCache::remove)
            }
        }
    }

    return animatedRadiusValue.dp
}

/**
 * Determine if text is primarily Latin-script.
 * Returns true if more than 30% of characters are Latin letters.
 */
private fun isLatinBasedText(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    val latinRatio = text.count { it in 'A'..'Z' || it in 'a'..'z' }.toFloat() / text.length
    return latinRatio > 0.3f
}

@Composable
private fun CoverTextOverlay(
    name: String?,
    author: String?,
    isNight: Boolean
) {
    val coverSettings = LocalAppUiConfiguration.current.cover
    val showName = if (isNight) coverSettings.showNameDark else coverSettings.showName
    val showAuthor =
        (if (isNight) coverSettings.showAuthorDark else coverSettings.showAuthor) && showName

    if (!showName && !showAuthor) return

    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val textColor = if (coverSettings.useDefaultColor) {
        secondaryColor
    } else {
        if (isNight) coverSettings.textColorDark else coverSettings.textColor
    }
    val shadowColor =
        if (isNight) coverSettings.shadowColorDark else coverSettings.shadowColor
    val configIsHorizontal = coverSettings.infoOrientation == "1"
    // If text contains Latin letters, force horizontal layout
    val isHorizontal = configIsHorizontal || isLatinBasedText(name)

    // Paints, StaticLayout and per-character positions are built in the cache block so they are
    // rebuilt only when the size or the settings above change, not on every draw pass.
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val viewWidth = size.width
                val viewHeight = size.height
                if (viewWidth <= 0f || viewHeight <= 0f) {
                    return@drawWithCache onDrawBehind { }
                }

                val namePaint = if (showName && !name.isNullOrBlank()) {
                    Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                        textSize = viewWidth / 8f
                        color = textColor
                        if (coverSettings.showShadow) {
                            setShadowLayer(4f, 2f, 2f, shadowColor)
                        }
                    }
                } else null

                val nameMaxWidth = (viewWidth * 0.8f).toInt().coerceAtLeast(1)
                val nameTextPaint = if (namePaint != null && isHorizontal) {
                    TextPaint(namePaint).apply { textAlign = Paint.Align.LEFT }
                } else null
                val nameLayout = if (nameTextPaint != null && name != null) {
                    StaticLayout.Builder
                        .obtain(name, 0, name.length, nameTextPaint, nameMaxWidth)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setMaxLines(3)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .build()
                } else null
                val nameLayoutX = (viewWidth - nameMaxWidth) / 2f
                val nameLayoutY = viewHeight * 0.08f

                val nameStrokePaint =
                    if (namePaint != null && !isHorizontal && coverSettings.showStroke) {
                        Paint(namePaint).apply {
                            color = Color.White.toArgb()
                            style = Paint.Style.STROKE
                            strokeWidth = namePaint.textSize / 10
                            clearShadowLayer()
                        }
                    } else null
                val nameCharDraws = if (namePaint != null && name != null && !isHorizontal) {
                    val charHeight = namePaint.fontMetrics.let { it.bottom - it.top }
                    var startX = viewWidth * 0.16f
                    var startY = viewHeight * 0.16f
                    name.map { char ->
                        val draw = Triple(char.toString(), startX, startY)
                        startY += charHeight
                        if (startY > viewHeight * 0.8f) {
                            startX += namePaint.textSize * 1.2f
                            startY = viewHeight * 0.2f
                        }
                        draw
                    }
                } else emptyList()

                val authorPaint = if (showAuthor && !author.isNullOrBlank()) {
                    Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        textSize = viewWidth / 12f
                        color = textColor
                        if (coverSettings.showShadow) {
                            setShadowLayer(4f, 1f, 1f, shadowColor)
                        }
                    }
                } else null

                val authorText = if (authorPaint != null && author != null && isHorizontal) {
                    TextUtils.ellipsize(
                        author,
                        TextPaint(authorPaint),
                        viewWidth * 0.9f,
                        TextUtils.TruncateAt.END
                    ).toString()
                } else null
                val authorStrokePaint =
                    if (authorPaint != null && isHorizontal && coverSettings.showStroke) {
                        Paint(authorPaint).apply {
                            color = Color.White.toArgb()
                            style = Paint.Style.STROKE
                            strokeWidth = authorPaint.textSize / 10
                            clearShadowLayer()
                        }
                    } else null
                val authorCharDraws = if (authorPaint != null && author != null && !isHorizontal) {
                    val charHeight = authorPaint.fontMetrics.let { it.bottom - it.top }
                    val startX = viewWidth * 0.84f
                    var startY = (viewHeight * 0.16f - (author.length * charHeight))
                        .coerceAtLeast(viewHeight * 0.2f)
                    author.map { char ->
                        val draw = Triple(char.toString(), startX, startY)
                        startY += charHeight
                        draw
                    }
                } else emptyList()

                onDrawBehind {
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas

                        if (nameLayout != null && nameTextPaint != null) {
                            nativeCanvas.withSave {
                                translate(nameLayoutX, nameLayoutY)
                                if (coverSettings.showStroke) {
                                    nameTextPaint.style = Paint.Style.STROKE
                                    nameTextPaint.strokeWidth = nameTextPaint.textSize / 12
                                    val originalColor = nameTextPaint.color
                                    nameTextPaint.color = Color.White.toArgb()
                                    nameTextPaint.clearShadowLayer()
                                    nameLayout.draw(this)
                                    nameTextPaint.style = Paint.Style.FILL
                                    nameTextPaint.color = originalColor
                                    if (coverSettings.showShadow) {
                                        nameTextPaint.setShadowLayer(4f, 2f, 2f, shadowColor)
                                    }
                                }
                                nameLayout.draw(this)
                            }
                        } else if (namePaint != null) {
                            nameCharDraws.forEach { (text, x, y) ->
                                if (nameStrokePaint != null) {
                                    nativeCanvas.drawText(text, x, y, nameStrokePaint)
                                }
                                nativeCanvas.drawText(text, x, y, namePaint)
                            }
                        }

                        if (authorPaint != null) {
                            if (authorText != null) {
                                if (authorStrokePaint != null) {
                                    nativeCanvas.drawText(
                                        authorText,
                                        viewWidth / 2,
                                        viewHeight * 0.75f,
                                        authorStrokePaint
                                    )
                                }
                                nativeCanvas.drawText(
                                    authorText,
                                    viewWidth / 2,
                                    viewHeight * 0.75f,
                                    authorPaint
                                )
                            } else {
                                authorCharDraws.forEach { (text, x, y) ->
                                    nativeCanvas.drawText(text, x, y, authorPaint)
                                }
                            }
                        }
                    }
                }
            }
    )
}

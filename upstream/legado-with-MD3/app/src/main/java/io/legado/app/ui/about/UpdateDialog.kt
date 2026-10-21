package io.legado.app.ui.about

import android.os.Build
import android.os.Bundle
import android.view.View
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.base.BaseBottomSheetDialogFragment
import io.legado.app.constant.AppConst
import io.legado.app.databinding.DialogUpdateBinding
import io.legado.app.help.update.AppUpdate
import io.legado.app.utils.gone
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin

// SPEC-144 TC-4：仅保留「更新日志」展示（VIEW_LOG），检查更新/下载入口已随启动与关于页入口一并移除。
class UpdateDialog() : BaseBottomSheetDialogFragment(R.layout.dialog_update) {

    constructor(updateInfo: AppUpdate.UpdateInfo) : this() {
        arguments = Bundle().apply {
            putString("newVersion", updateInfo.tagName)
            putString("updateBody", updateInfo.updateLog)
        }
    }

    private val binding by viewBinding(DialogUpdateBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.bottomSheetTitle.setText(R.string.about_installed_version_title)
        binding.tvCurrentVersion.text = BuildConfig.VERSION_NAME
        binding.tvVersion.text = BuildConfig.VERSION_NAME
        binding.tvAbi.text = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        binding.tvVariable.text = AppConst.appInfo.appVariant.toString()
        binding.ivAward.gone()
        binding.btnUpdate.gone()
        binding.llCurrent.gone()
        binding.tvCurrentVersion.gone()
        binding.tvUrl.gone()

        val updateBody = arguments?.getString("updateBody")
        if (updateBody.isNullOrBlank()) {
            toastOnUi(R.string.about_no_data)
            dismiss()
            return
        }

        binding.textView.post {
            Markwon.builder(requireContext())
                .usePlugin(GlideImagesPlugin.create(requireContext()))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .build()
                .setMarkdown(binding.textView, updateBody)
        }
    }
}
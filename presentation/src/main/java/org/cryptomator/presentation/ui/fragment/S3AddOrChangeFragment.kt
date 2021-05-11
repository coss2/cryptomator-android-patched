package org.cryptomator.presentation.ui.fragment

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.android.material.switchmaterial.SwitchMaterial
import org.cryptomator.generator.Fragment
import org.cryptomator.presentation.R
import org.cryptomator.presentation.model.S3CloudModel
import org.cryptomator.presentation.presenter.S3AddOrChangePresenter
import org.cryptomator.util.crypto.CredentialCryptor
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_setup_s3.accessKeyEditText
import kotlinx.android.synthetic.main.fragment_setup_s3.bucketEditText
import kotlinx.android.synthetic.main.fragment_setup_s3.createCloudButton
import kotlinx.android.synthetic.main.fragment_setup_s3.displayNameEditText
import kotlinx.android.synthetic.main.fragment_setup_s3.endpointEditText
import kotlinx.android.synthetic.main.fragment_setup_s3.ll_custom_s3
import kotlinx.android.synthetic.main.fragment_setup_s3.regionEditText
import kotlinx.android.synthetic.main.fragment_setup_s3.secretKeyEditText
import kotlinx.android.synthetic.main.fragment_setup_s3.toggleCustomS3
import timber.log.Timber

@Fragment(R.layout.fragment_setup_s3)
class S3AddOrChangeFragment : BaseFragment() {

	@Inject
	lateinit var s3AddOrChangePresenter: S3AddOrChangePresenter

	private var cloudId: Long? = null

	private val s3CloudModel: S3CloudModel?
		get() = arguments?.getSerializable(ARG_S3_CLOUD) as? S3CloudModel

	override fun setupView() {
		createCloudButton.setOnClickListener { createCloud() }
		createCloudButton.setOnEditorActionListener { _, actionId, _ ->
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				createCloud()
			}
			false
		}

		showEditableCloudContent(s3CloudModel)

		toggleCustomS3.setOnClickListener { switch ->
			toggleUseAmazonS3((switch as SwitchMaterial).isChecked)
		}
	}

	private fun toggleUseAmazonS3(checked: Boolean) = if (checked) {
		ll_custom_s3.visibility = View.GONE
	} else {
		ll_custom_s3.visibility = View.VISIBLE
	}

	private fun showEditableCloudContent(s3CloudModel: S3CloudModel?) {
		s3CloudModel?.let {
			cloudId = s3CloudModel.id()
			displayNameEditText.setText(s3CloudModel.username())
			accessKeyEditText.setText(decrypt(s3CloudModel.accessKey()))
			secretKeyEditText.setText(decrypt(s3CloudModel.secretKey()))
			bucketEditText.setText(s3CloudModel.s3Bucket())

			regionEditText.setText(s3CloudModel.s3Region())

			if (it.s3Endpoint().isNotEmpty()) {
				toggleCustomS3.isChecked = false
				ll_custom_s3.visibility = View.VISIBLE
				endpointEditText.setText(s3CloudModel.s3Endpoint())
			} else {
				toggleCustomS3.isChecked = false
				ll_custom_s3.visibility = View.VISIBLE
			}
		}
	}

	private fun decrypt(text: String?): String {
		return if (text != null) {
			try {
				CredentialCryptor //
						.getInstance(activity?.applicationContext) //
						.decrypt(text)
			} catch (e: RuntimeException) {
				Timber.tag("S3AddOrChangeFragment").e(e, "Unable to decrypt password, clearing it")
				""
			}
		} else ""
	}

	private fun createCloud() {
		val accessKey = accessKeyEditText.text.toString().trim()
		val secretKey = secretKeyEditText.text.toString().trim()
		val bucket = bucketEditText.text.toString().trim()
		val displayName = displayNameEditText.text.toString().trim()

		if (toggleCustomS3.isChecked) {
			s3AddOrChangePresenter.checkUserInput(accessKey, secretKey, bucket, null, regionEditText.text.toString().trim(), cloudId, displayName)
		} else {
			s3AddOrChangePresenter.checkUserInput(accessKey, secretKey, bucket, endpointEditText.text.toString().trim(), regionEditText.text.toString().trim(), cloudId, displayName)
		}
	}

	fun hideKeyboard() {
		hideKeyboard(bucketEditText)
	}

	companion object {

		private const val ARG_S3_CLOUD = "S3_CLOUD"

		fun newInstance(cloudModel: S3CloudModel?): S3AddOrChangeFragment {
			val result = S3AddOrChangeFragment()
			val args = Bundle()
			args.putSerializable(ARG_S3_CLOUD, cloudModel)
			result.arguments = args
			return result
		}
	}

}

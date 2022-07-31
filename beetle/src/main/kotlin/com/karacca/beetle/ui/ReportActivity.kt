package com.karacca.beetle.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.karacca.beetle.R
import com.karacca.beetle.network.BeetleRepository
import com.karacca.beetle.network.model.Collaborator
import com.karacca.beetle.network.model.Label
import com.karacca.beetle.ui.adapter.CollaboratorAdapter
import com.karacca.beetle.ui.adapter.LabelAdapter
import com.karacca.beetle.ui.widget.HorizontalItemDecorator
import kotlinx.coroutines.launch
import org.bouncycastle.util.io.pem.PemReader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec

/**
 * @author karacca
 * @date 13.07.2022
 */

internal class ReportActivity : AppCompatActivity(), TextWatcher {

    private lateinit var screenshot: Uri

    private lateinit var beetleRepository: BeetleRepository

    private lateinit var toolbar: MaterialToolbar
    private lateinit var imageView: AppCompatImageView
    private lateinit var screenshotCardView: MaterialCardView
    private lateinit var logsCardView: MaterialCardView
    private lateinit var titleEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText

    private lateinit var assigneesRecyclerView: RecyclerView
    private lateinit var labelsRecyclerView: RecyclerView

    private lateinit var collaboratorAdapter: CollaboratorAdapter
    private lateinit var labelAdapter: LabelAdapter

    private val openEditActivity = registerForActivityResult(
        object : ActivityResultContract<Uri, Uri>() {
            override fun createIntent(context: Context, input: Uri?): Intent {
                return Intent(context, EditActivity::class.java).apply {
                    putExtra(ARG_SCREENSHOT, input)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Uri {
                return intent!!.getParcelableExtra(ARG_SCREENSHOT)!!
            }
        }
    ) {
        screenshot = it
        imageView.setImageURI(null)
        imageView.setImageURI(screenshot)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        screenshot = intent.extras!!.getParcelable(ARG_SCREENSHOT)!!
        val organization = intent.extras!!.getString(ARG_ORGANIZATION)!!
        val repository = intent.extras!!.getString(ARG_REPOSITORY)!!

        beetleRepository = BeetleRepository(getPrivateKey(application), organization, repository)

        lifecycleScope.launch {
            setCollaborators(beetleRepository.getCollaborators())
            setLabels(beetleRepository.getLabels())
        }

        toolbar = findViewById(R.id.toolbar)
        imageView = findViewById(R.id.image_view_screenshot)
        screenshotCardView = findViewById(R.id.card_view_screenshot)
        logsCardView = findViewById(R.id.card_view_logs)
        titleEditText = findViewById(R.id.edit_text_title)
        descriptionEditText = findViewById(R.id.edit_text_description)
        assigneesRecyclerView = findViewById(R.id.recycler_view_assignees)
        labelsRecyclerView = findViewById(R.id.recycler_view_labels)

        toolbar.setNavigationOnClickListener { finish() }
        imageView.setImageURI(screenshot)
        screenshotCardView.setOnClickListener {
            openEditActivity.launch(screenshot)
        }

        titleEditText.addTextChangedListener(this)
        descriptionEditText.addTextChangedListener(this)

        toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener if (it.itemId == R.id.send) {
                createIssue()
                true
            } else {
                false
            }
        }

        collaboratorAdapter = CollaboratorAdapter {
            val collaborators = collaboratorAdapter.currentList
            collaborators[it].selected = !collaborators[it].selected
            collaboratorAdapter.submitList(collaborators)
        }

        assigneesRecyclerView.apply {
            addItemDecoration(HorizontalItemDecorator(8))
            adapter = collaboratorAdapter
            layoutManager = LinearLayoutManager(
                this@ReportActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        labelAdapter = LabelAdapter {
            val labels = labelAdapter.currentList
            labels[it].selected = !labels[it].selected
            labelAdapter.submitList(labels)
        }

        labelsRecyclerView.apply {
            addItemDecoration(HorizontalItemDecorator(8))
            adapter = labelAdapter
            layoutManager = LinearLayoutManager(
                this@ReportActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(p0: Editable?) {
        val isTitleMissing = titleEditText.text.isNullOrEmpty()
        val isDescriptionMissing = descriptionEditText.text.isNullOrEmpty()
        toolbar.menu[0].isEnabled = !isTitleMissing && !isDescriptionMissing
    }

    private fun getPrivateKey(context: Context): PrivateKey {
        val file = context.assets.open("beetle.pem")
        val inputStreamReader = InputStreamReader(file)
        val readerBufferedFile = BufferedReader(inputStreamReader)
        val reader = PemReader(readerBufferedFile)
        val privateKeyPem = reader.readPemObject()

        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyPem.content))
    }

    private fun setCollaborators(collaborators: List<Collaborator>) {
        collaboratorAdapter.submitList(collaborators)
    }

    private fun setLabels(labels: List<Label>) {
        labelAdapter.submitList(labels)
    }

    private fun createIssue() {
        val title = titleEditText.text?.toString() ?: ""
        val description = descriptionEditText.text?.toString() ?: ""
        val assignees = collaboratorAdapter.currentList.filter { it.selected }.map { it.login }
        val labels = labelAdapter.currentList.filter { it.selected }.map { it.name }

        lifecycleScope.launch {
            beetleRepository.createIssue(title, description, assignees, labels, screenshot)
            finish()
        }
    }

    companion object {
        const val ARG_SCREENSHOT = "screenshot"
        const val ARG_ORGANIZATION = "organization"
        const val ARG_REPOSITORY = "repository"
    }
}

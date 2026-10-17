package com.quran.labs.androidquran.ui.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.quran.data.dao.BookmarksDao
import com.quran.data.model.bookmark.Tag
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.CollectionNames
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.presenter.bookmark.TagBookmarkPresenter
import com.quran.labs.androidquran.ui.modern.ModernTagSheet
import com.quran.labs.androidquran.ui.modern.TagItemModel
import com.quran.labs.androidquran.util.QuranSettings
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HasMemberInjections
open class TagBookmarkDialog : DialogFragment() {
  private var adapter: TagsAdapter? = null

  @Inject
  lateinit var tagBookmarkPresenter: TagBookmarkPresenter

  @Inject
  lateinit var quranSettings: QuranSettings

  @Inject
  lateinit var bookmarksDao: BookmarksDao

  private val modernTagsState = mutableStateOf<List<TagItemModel>>(emptyList())
  private val modernCheckedTagsState = mutableStateOf<Set<String>>(emptySet())

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (shouldInject()) {
      (context.applicationContext as QuranApplication).applicationComponent.inject(this)
    }
  }

  open fun shouldInject() = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = arguments
    if (args != null) {
      val bookmarkIds = args.getStringArray(EXTRA_BOOKMARK_IDS)
      if (bookmarkIds != null) {
        tagBookmarkPresenter.setBookmarksMode(bookmarkIds)
      }
    }
  }

  private fun createTagsListView(): ListView {
    val context = requireContext()
    val adapter = TagsAdapter(context, tagBookmarkPresenter)
    this.adapter = adapter
    val listview = ListView(context)
    listview.adapter = adapter
    listview.choiceMode = ListView.CHOICE_MODE_MULTIPLE
    listview.onItemClickListener =
      OnItemClickListener { _: AdapterView<*>?, view: View, position: Int, _: Long ->
        if (adapter.isAddTagPosition(position)) {
          tagBookmarkPresenter.addTag()
          return@OnItemClickListener
        }
        val tag = adapter.getItem(position) ?: return@OnItemClickListener
        val isChecked = tagBookmarkPresenter.toggleTag(tag.id)
        val viewTag = view.tag
        if (viewTag is ViewHolder) {
          viewTag.checkBox.isChecked = isChecked
        }
      }
    return listview
  }

  open fun showAddTagDialog() {
    val context: Context? = activity
    if (context is OnBookmarkTagsUpdateListener) {
      (context as OnBookmarkTagsUpdateListener).onAddTagSelected()
    }
  }

  fun setData(tags: List<Tag>?, checkedTags: HashSet<String>) {
    modernTagsState.value = tags?.map {
      TagItemModel(it.id, CollectionNames.displayName(requireContext(), it), it.isDefault)
    } ?: emptyList()
    modernCheckedTagsState.value = checkedTags.toSet()

    adapter?.setData(tags, checkedTags)
    adapter?.notifyDataSetChanged()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    if (quranSettings.isModernUiEnabled) {
      val dialog = Dialog(requireContext())
      dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
      val composeView = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
          QuranTheme {
            val tags by modernTagsState
            val selectedIds by modernCheckedTagsState
            ModernTagSheet(
              visible = true,
              targetTitle = getString(R.string.tag_dlg_title),
              tags = tags,
              selectedTagIds = selectedIds,
              onToggleTag = { tagId ->
                val isChecked = tagBookmarkPresenter.toggleTag(tagId)
                modernCheckedTagsState.value = if (isChecked) {
                  selectedIds + tagId
                } else {
                  selectedIds - tagId
                }
              },
              onCreateTag = { name ->
                lifecycleScope.launch(Dispatchers.IO) {
                  bookmarksDao.addTag(name)
                }
              },
              onDeleteTag = { tagId ->
                lifecycleScope.launch(Dispatchers.IO) {
                  val tagObj = tags.find { it.id == tagId }
                  if (tagObj != null && !tagObj.isDefault) {
                    val actualTag = Tag(tagObj.id, tagObj.name)
                    bookmarksDao.removeTags(listOf(actualTag))
                  }
                }
              },
              onSave = {
                tagBookmarkPresenter.saveChanges()
                dismiss()
              },
              onDismiss = { dismiss() }
            )
          }
        }
      }
      dialog.setContentView(composeView)
      dialog.window?.apply {
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setGravity(Gravity.BOTTOM)
        setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
      }
      return dialog
    }

    val builder = Builder(requireActivity(), R.style.QuranDialogTheme)
    builder.setView(createTagsListView())
    builder.setPositiveButton(R.string.dialog_ok) { _: DialogInterface?, _: Int -> }
    builder.setNegativeButton(com.quran.mobile.common.ui.core.R.string.cancel) { _: DialogInterface?, _: Int -> dismiss() }
    return builder.create()
  }

  override fun onStart() {
    super.onStart()
    tagBookmarkPresenter.bind(this)

    val dialog = dialog
    if (dialog is AlertDialog) {
      val positive = dialog.getButton(Dialog.BUTTON_POSITIVE)
      positive.setOnClickListener {
        tagBookmarkPresenter.saveChanges()
        dismiss()
      }
    }
  }

  override fun onStop() {
    tagBookmarkPresenter.unbind(this)
    super.onStop()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // If in dialog mode, don't do anything (or else it will cause exception)
    return if (showsDialog) {
      super.onCreateView(inflater, container, savedInstanceState)
    } else createTagsListView()
    // If not in dialog mode, treat as normal fragment onCreateView
  }

  class TagsAdapter internal constructor(
    private val context: Context, presenter: TagBookmarkPresenter
  ) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val tagBookmarkPresenter: TagBookmarkPresenter = presenter
    private val newTagString: String = context.getString(R.string.new_tag)
    private var tags: List<Tag> = emptyList()
    private var checkedTags = HashSet<String>()

    fun setData(tags: List<Tag>?, checkedTags: HashSet<String>) {
      this.tags = (tags ?: emptyList())
      this.checkedTags = checkedTags
    }

    override fun getCount(): Int = tags.size + 1

    override fun getItem(position: Int): Tag? = tags.getOrNull(position)

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      var holder: ViewHolder
      val view = if (convertView == null) {
        val view = inflater.inflate(R.layout.tag_row, parent, false)
        holder = ViewHolder().apply {
          checkBox = view.findViewById(R.id.tag_checkbox)
          tagName = view.findViewById(R.id.tag_name)
          addImage = view.findViewById(R.id.tag_add_image)
        }
        view.tag = holder
        view
      } else {
        convertView
      }

      holder = view.tag as ViewHolder
      if (isAddTagPosition(position)) {
        holder.apply {
          addImage.visibility = View.VISIBLE
          checkBox.visibility = View.GONE
          tagName.text = newTagString
        }
      } else {
        val tag = requireNotNull(getItem(position))
        val id = tag.id
        val name = CollectionNames.displayName(context, tag)
        holder.apply {
          addImage.visibility = View.GONE
          checkBox.visibility = View.VISIBLE
          checkBox.isChecked = checkedTags.contains(id)
          tagName.text = name
          checkBox.setOnClickListener { tagBookmarkPresenter.toggleTag(id) }
        }
      }
      return view
    }

    fun isAddTagPosition(position: Int): Boolean = position == tags.size
  }

  internal class ViewHolder {
    lateinit var checkBox: CheckBox
    lateinit var tagName: TextView
    lateinit var addImage: ImageView
  }

  interface OnBookmarkTagsUpdateListener {
    fun onAddTagSelected()
  }

  companion object {
    const val TAG = "TagBookmarkDialog"
    private const val EXTRA_BOOKMARK_IDS = "bookmark_ids"
    fun newInstance(bookmarkId: String): TagBookmarkDialog {
      return newInstance(arrayOf(bookmarkId))
    }

    fun newInstance(bookmarkIds: Array<String>?): TagBookmarkDialog {
      val args = Bundle()
      args.putStringArray(EXTRA_BOOKMARK_IDS, bookmarkIds)
      val dialog = TagBookmarkDialog()
      dialog.arguments = args
      return dialog
    }
  }
}

package com.example.enhancedviewer

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.enhancedviewer.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import kotlin.streams.toList

class MainActivity : AppCompatActivity() {
    private val OPEN_REQUEST_CODE = 41
    private lateinit var binding: ActivityMainBinding
    private lateinit var textAdapter: TextAdapter
    private val data: ArrayList<String> = arrayListOf()
    private lateinit var recyclerViewLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        recyclerViewLayoutManager = binding.recyclerView.layoutManager as LinearLayoutManager

        val onScrollListener: RecyclerView.OnScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val first = recyclerViewLayoutManager.findFirstVisibleItemPosition()

                    binding.nowLine.text = (first + 1).toString()
                }
            }

        binding.recyclerView.addOnItemTouchListener(
            object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    // 스크롤 중일 경우 스크롤 멈춤
                    if (e.action == MotionEvent.ACTION_DOWN)
                        binding.recyclerView.clipToPadding = false
                    //스크롤 중이 아닐 경우
                    else if (e.action == MotionEvent.ACTION_UP && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                        val movement: Int
                        val smoothScroller: LinearSmoothScroller
                        val center = binding.recyclerView.height / 2

                        if (center < e.y) {
                            // 증앙 아래 터치 시 아래로 한페이지 이동
                            movement =
                                recyclerViewLayoutManager.findLastCompletelyVisibleItemPosition() + 1
                            smoothScroller = object : LinearSmoothScroller(baseContext) {
                                override fun getVerticalSnapPreference(): Int {
                                    return SNAP_TO_START
                                }
                            }
                        } else {
                            // 중앙 위 터치 시 위로 한페이지 이동
                            movement =
                                recyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition() - 1
                            smoothScroller = object : LinearSmoothScroller(baseContext) {
                                override fun getVerticalSnapPreference(): Int {
                                    return SNAP_TO_END
                                }
                            }
                        }

                        binding.recyclerView.clipToPadding = true

                        smoothScroller.targetPosition = movement
                        recyclerViewLayoutManager.startSmoothScroll(smoothScroller)
                    }

                    return false
                }

                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {

                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                    TODO("not implemented")
                }
            }
        )

        binding.recyclerView.addOnScrollListener(onScrollListener)

        openFileExplorer()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //툴바 이벤트 등록
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.bookmark -> {
                val fileName = title.toString()

                if (File("$filesDir/$fileName").exists()) {
                    val bookmarkList =
                        openFileInput(fileName).bufferedReader().lines().toList().map {
                            Integer.parseInt(it)
                        }

                    val temp = arrayListOf<String>().apply {
                        for (i in bookmarkList) {
                            add(data[i])
                        }
                    }

                    val bookmarkData = temp.toTypedArray()

                    val dialog = AlertDialog.Builder(this).apply {
                        setTitle("이동할 북마크를 선택하세요.")

                        setItems(bookmarkData, DialogInterface.OnClickListener { dialog, id ->
                            recyclerViewLayoutManager.scrollToPositionWithOffset(bookmarkList[id], 0)

                            dialog.dismiss()
                        })
                    }

                    dialog.show()
                }

                true
            }
            R.id.bookmarkAdd -> {
                // 북마크 파일 이름은 열린 파일 이름으로 설정함
                val fileName = title.toString()

                val position = recyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition()

                val dialog = AlertDialog.Builder(this).apply {
                    setTitle("북마크를 저장하시겠습니까?")
                    setMessage(data[position])

                    setPositiveButton("확인") { dialog, which ->
                        openFileOutput(fileName, MODE_APPEND).use {
                            it.write(("$position\n").toByteArray())
                            Toast.makeText(baseContext, "북마크가 저장되었습니다.", Toast.LENGTH_SHORT).show()

                            it.close()
                        }
                    }

                    setNegativeButton("취소") { dialog, which ->
                        dialog.dismiss()
                    }
                }

                dialog.show()
                true
            }
            R.id.pageSearch -> {
                val edit = EditText(this).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                }

                val dialog = AlertDialog.Builder(this).apply {
                    setTitle("페이지 이동")
                    setMessage("페이지를 입력하세요.")
                    setView(edit)

                    setPositiveButton("확인") { dialog, which ->
                        val value = edit.text.toString().toInt() - 1
                        val input = if (value < 0) 0
                        else if (value >= textAdapter.itemCount) textAdapter.itemCount - 1
                        else value

                        recyclerViewLayoutManager.scrollToPositionWithOffset(input, 0)
                    }
                    setNegativeButton("취소") { dialog, which ->
                        dialog.dismiss()
                    }
                }

                dialog.show()
                true
            }
            R.id.search -> {
                true
            }
            R.id.fontSizeSetting -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        openFileExplorer()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                OPEN_REQUEST_CODE -> {
                    data?.let { it ->
                        it.data?.let { it ->
                            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                                val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                cursor.moveToFirst()
                                title = cursor.getString(name)
                            }

                            val content = Filter().arrangement(readFile(it))

                            initRecycler(content)

                            binding.totalLine.text = textAdapter.itemCount.toString()
                        }
                    }
                }
            }
        } else {
            //아무런 파일도 선택되지 않았을 때 종료
            finish()
        }
    }

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }

        startActivityForResult(intent, OPEN_REQUEST_CODE)
    }

    private fun readFile(uri: Uri): String {
        val stringBuilder = StringBuilder()

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))

            while (true) {
                val currentLine = reader.readLine() ?: break

                stringBuilder.append(Filter.deleteGarbageText(currentLine) + "\n")
            }

            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val target: String = stringBuilder.toString()

        return target.substring(0, target.length - 1) // 마지막 엔터 제외
    }

    private fun initRecycler(textList: ArrayList<String>) {
        textAdapter = TextAdapter(this, binding.recyclerView.height)
        binding.recyclerView.adapter = textAdapter

        data.apply {
            clear()
            for (i in textList) {
                add(i)
            }
        }

        textAdapter.data = data
        textAdapter.notifyDataSetChanged()
    }
}
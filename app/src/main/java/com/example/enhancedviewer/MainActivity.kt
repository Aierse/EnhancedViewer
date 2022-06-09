package com.example.enhancedviewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.enhancedviewer.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private val OPEN_REQUEST_CODE = 41
    private lateinit var binding: ActivityMainBinding
    private lateinit var textAdapter: TextAdapter
    private val data: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val onScrollListener: RecyclerView.OnScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
                    val first = layoutManager.findFirstVisibleItemPosition()

                    binding.nowLine.text = (first + 1).toString()
                }
            }

        binding.recyclerView.addOnItemTouchListener(
            object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    if (e.action == MotionEvent.ACTION_DOWN)
                        binding.recyclerView.clipToPadding = false
                    else if (e.action == MotionEvent.ACTION_UP && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
                        var movement: Int = 0
                        val smoothScroller: LinearSmoothScroller
                        val center = binding.recyclerView.height / 2

                        if (center < e.y) {
                            // 증앙 아래 터치 시 마지막 요소를 맨 위까지 스크롤
                            movement = layoutManager.findLastCompletelyVisibleItemPosition() + 1
                            smoothScroller = object : LinearSmoothScroller(baseContext) {
                                override fun getVerticalSnapPreference(): Int {
                                    return SNAP_TO_START
                                }
                            }
                        } else {
                            movement = layoutManager.findFirstCompletelyVisibleItemPosition() - 1
                            smoothScroller = object : LinearSmoothScroller(baseContext) {
                                override fun getVerticalSnapPreference(): Int {
                                    return SNAP_TO_END
                                }
                            }
                        }

                        binding.recyclerView.clipToPadding = true

                        smoothScroller.targetPosition = movement
                        layoutManager.startSmoothScroll(smoothScroller)
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
                true
            }
            R.id.bookmarkAdd -> {
                true
            }
            R.id.pageSearch -> {
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
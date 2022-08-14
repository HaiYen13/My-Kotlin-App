package com.example.mykotlinapp.detail

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.mykotlinapp.R
import com.example.mykotlinapp.R.id.img_favorite
import com.example.mykotlinapp.model.ImageModel
import com.example.mykotlinapp.utils.DebugHelper

class DetailActivity : AppCompatActivity() {
    var imgFavorite: ImageView ?= null
    var imgDownload: ImageView ?= null
    var mViewPager : ViewPager ?= null
    var list: ArrayList<ImageModel> ?= null
    var pos = 0
    private var mPagerAdapter : PagerAdapter ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        list = intent.getSerializableExtra("myList") as ArrayList<ImageModel>?
        DebugHelper.logDebug("DetailActivity.onCreate","$list")
        pos = intent.getIntExtra("pos", 0)
        DebugHelper.logDebug("DetailActivity.onCreate.pos", "$pos")
        initView()
        initAction()
    }

    private fun initView() {
        imgFavorite = findViewById(img_favorite)
        imgDownload = findViewById(R.id.imgDownload)
        mViewPager = findViewById(R.id.vp_detail)
    }
    private fun initAction() {
        mPagerAdapter = ViewPagerAdapter(this, list)
        mViewPager!!.adapter = mPagerAdapter
        mViewPager!!.currentItem = pos
        onPageChange(pos)
        mViewPager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
            }

            override fun onPageSelected(position: Int) {
                onPageChange(position)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }


        })
    }

    private fun onPageChange(position: Int) {
        val model = list!![position]
        if (model.isFavorited == 1) {
            imgFavorite!!.setImageResource(R.drawable.ic_favorite_selected)
        } else imgFavorite!!.setImageResource(R.drawable.ic_favorite)
    }

    override fun onBackPressed() {
        val intent = Intent()
        intent.putExtra("currentPosition", mViewPager?.currentItem)
        DebugHelper.logDebug("DetailActivity.onBackPressed.pos", "${mViewPager?.currentItem}")
        intent.putExtra("newList", list)
        setResult(123, intent)
        super.onBackPressed()
    }
}
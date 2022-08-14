package com.example.mykotlinapp.home

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mykotlinapp.R
import com.example.mykotlinapp.detail.DetailActivity
import com.example.mykotlinapp.home.HomeViewModel.Companion.fileNames
import com.example.mykotlinapp.model.ImageModel
import com.example.mykotlinapp.utils.DebugHelper
import com.example.mykotlinapp.utils.SQLiteHistoryHelper


class HomeFragment: Fragment(), HomeAdapter.OnItemClickListener {

    private var rcvCatogory: RecyclerView? = null
    var imgMultiDown: ImageView? = null
    var tvMultiDown: TextView? = null
    private var mHomeAdapter: HomeAdapter? = null
    var movieList: ArrayList<ImageModel>? = null
    var savedRecycleLayoutState : Parcelable? = null
    var historyModel: ArrayList<ImageModel?>? =null

    var isDownBoxSelected: Boolean ?= null
    private var map: HashMap<Int, ImageModel> = hashMapOf()
    //    val loadDataListener: LoadDataListener? = null
    private var currentPage: Int = 1
    private var isLoadingData = false
    private var urls: ArrayList<String> ?= null
    private val RQ_WRITE_PERMISSION = 2810
    private var progressDialog: ProgressBar ?= null
    private var mprogressDialog: ProgressDialog ?= null

    private var sqLiteHistoryHelper : SQLiteHistoryHelper?= null
    companion object{
        const val historyTable = "History"
    }
    private val homeViewModel by lazy {
        HomeViewModel(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        initView(view)
        initAction()
        urls = ArrayList()
        sqLiteHistoryHelper = SQLiteHistoryHelper(context)
        historyModel = ArrayList()
        savedInstanceState ?. run{
            savedRecycleLayoutState = this.getParcelable("positionScroll")
            currentPage = savedInstanceState.getInt("currentPage")
            DebugHelper.logDebug("onCreate.currentPageSaved ", "$currentPage ")
        }
        if (currentPage > fileNames.size) currentPage = fileNames.size
        if (currentPage < 0) currentPage = 0

        movieList = ArrayList()
        progressDialog = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        mprogressDialog = ProgressDialog(context)
        progressDialog?.run {
//            setMessage("Downloading")
//            isIndeterminate = true
//            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
             max = 100
//            setIcon(R.drawable.ic_download)
//            setCancelable(true)
            DebugHelper.logDebug("Progress", "init")
        }
        mprogressDialog?.run {
            setMessage("Downloading")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(true)
        }
        initAdapter(movieList!!)
        homeViewModel.liveData.observe(this.viewLifecycleOwner) { it ->
            it?.let {
                if(currentPage==1)
                    movieList?.addAll(it)
            }

            if (currentPage == 1) mHomeAdapter?.updateData(movieList)
            else  mHomeAdapter?.addLoadMoreData(
                    it
                )



            DebugHelper.logDebug("HomeFragment.observe it.size","${movieList?.size}")

            savedRecycleLayoutState?.let {
                rcvCatogory?.layoutManager?.onRestoreInstanceState(savedRecycleLayoutState)
            }
//            isLoadingData = false
            requireView().findViewById<View>(R.id.loadingPanel).visibility = View.GONE
        }
        homeViewModel.progressData.observe(this.viewLifecycleOwner){
            it?.run {
                progressDialog?.progress = it
                mprogressDialog?.progress = it
                DebugHelper.logDebug("HomeFragment.progress.observe", "$it")
                DebugHelper.logDebug("HomeFragment.progress.observe", "${mprogressDialog?.progress}")
                if(this >= 100){
                    progressDialog?.visibility = View.GONE
                    mprogressDialog?.dismiss()
                    DebugHelper.logDebug("progress"," gone")
                    Toast.makeText(context, "Downloaded", Toast.LENGTH_SHORT).show()
                }
            }
        }
        homeViewModel.getData(currentPage)
        return view
    }

    private fun initAdapter(movieList: ArrayList<ImageModel>) {
        mHomeAdapter = HomeAdapter(movieList, context, this)
        rcvCatogory?.let {
            it.layoutManager = GridLayoutManager(context, 2)
            it.adapter = mHomeAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        savedRecycleLayoutState = rcvCatogory?.layoutManager?.onSaveInstanceState()
        outState.putParcelable("rcvScroll", savedRecycleLayoutState)
        outState.putInt("currentPage", currentPage)
        super.onSaveInstanceState(outState)
    }

    private fun initAction() {
        rcvCatogory?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                //Todo: Loadmore
                if (dy > 0) {
                    val visibleItemCount = recyclerView.layoutManager?.childCount // So luong item hien tai
                    val totalItemCount = recyclerView.layoutManager?.itemCount
                    val layoutManager = recyclerView.layoutManager
                    val pastVisibleItems= (layoutManager as GridLayoutManager) //index cua phan tu cuoi cung
                        .findLastVisibleItemPosition()

                    if (!isLoadingData && currentPage < 3) {
                        if (visibleItemCount != null && totalItemCount != null
                            && visibleItemCount + pastVisibleItems >= totalItemCount) {
                            DebugHelper.logDebug("LoadPage", "${currentPage+1}")
                            DebugHelper.logDebug("HomeFragment.initAction", "visibleItemCount $visibleItemCount")
                            DebugHelper.logDebug("HomeFragment.initAction", "pastVisibleItems $pastVisibleItems")
                            DebugHelper.logDebug("HomeFragment.initAction", "Total visibleItemCount + pastVisibleItems ${visibleItemCount + pastVisibleItems}")
                            DebugHelper.logDebug("HomeFragment.initAction", "Total $totalItemCount")
                            isLoadingData = true
                            requireView().findViewById<View>(R.id.loadingPanel).visibility = View.VISIBLE
                            loadMore()
                        }
                    }

                }
            }
        })
        imgMultiDown?.setOnClickListener {
            if (isDownBoxSelected == true) {
                imgMultiDown?.setImageResource(R.drawable.ic_box)
                tvMultiDown?.visibility = View.GONE
                isDownBoxSelected = false
            } else {
                imgMultiDown?.setImageResource(R.drawable.ic_box_selected)
                tvMultiDown?.visibility = View.VISIBLE
                isDownBoxSelected = true
            }
            mHomeAdapter?.onSelectChange(isDownBoxSelected!!)
        }
        tvMultiDown?.setOnClickListener {
            if (isStoragePermissionGranted()) {
                for ((_, value) in map) {
                    urls?.add(value.url)
                    historyModel?.add(value)
                }
                progressDialog?.visibility = View.VISIBLE
                mprogressDialog?.show()
                DebugHelper.logDebug("progressDialog", "Visible")
                urls?.let { it1 -> homeViewModel.download(it1) }
                historyModel?.let {
                        it1 ->
                    sqLiteHistoryHelper?.
                    insertListHistory(it1, historyTable) }
            }
        }
    }

    private fun loadMore() {
        currentPage++
        isLoadingData = false
        homeViewModel.getData(currentPage)
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && Build.VERSION.SDK_INT< Build.VERSION_CODES.R){
            if(context?.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==PackageManager.PERMISSION_GRANTED){
                true
            }else{
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    RQ_WRITE_PERMISSION)
                false
            }
        }else
            true

    }

    private fun initView(view: View) {
        DebugHelper.logDebug("HomeFrag.initView", "Start InitView")
        rcvCatogory = view.findViewById(R.id.rcvHome)
        imgMultiDown = view.findViewById(R.id.imgMultiDown)
        tvMultiDown = view.findViewById(R.id.tvDownload)
        progressDialog = view.findViewById(R.id.progressBar)
    }

    override fun onClick(model: ImageModel, position: Int) {
        val intent = Intent(context, DetailActivity::class.java)

        intent.putExtra("myList", mHomeAdapter?.dataList)
        intent.putExtra("pos", position)

        startActivityForResult(intent, 123)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            val newPos: Int = data?.getIntExtra("currentPosition", 0) ?: 0

            val newList : ArrayList<ImageModel> = data?.getSerializableExtra("newList")
                    as ArrayList<ImageModel>
            initAdapter(newList)
            rcvCatogory?.smoothScrollToPosition(newPos)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onItemChecked(model: ImageModel, position: Int, isChecked: Boolean) {
        map.let {
            if (isChecked) {
                it[model.id] = model
            }else{
                if(it[model.id] != null){
                    it.remove(model.id)
                }
            }
        }
    }

}
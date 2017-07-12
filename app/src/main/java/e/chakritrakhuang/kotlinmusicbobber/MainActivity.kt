package e.chakritrakhuang.kotlinmusicbobber

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() , LoaderManager.LoaderCallbacks<Collection<MusicItem>> , SearchView.OnQueryTextListener {

    @Bind(R.id.recycler_view)
    internal var recyclerView : RecyclerView? = null

    @Bind(R.id.empty_view)
    internal var emptyView : View? = null

    private var adapter : MusicAdapter? = null
    private var emptyViewObserver : EmptyViewObserver? = null

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        adapter = MusicAdapter(this)
        recyclerView !!.setLayoutManager(LinearLayoutManager(this , LinearLayoutManager.VERTICAL , false))
        recyclerView !!.setAdapter(adapter)
        emptyViewObserver = EmptyViewObserver(emptyView !!)
        emptyViewObserver !!.bind(recyclerView !!)
        val filter = MusicFilter(ContextCompat.getColor(this , R.color.colorAccent))
        adapter !!.withFilter(filter)
        ItemClickSupport.addTo(recyclerView)
                .setOnItemClickListener(object : ItemClickSupport.OnItemClickListener {
                    fun onItemClick(parent : RecyclerView , view : View , position : Int , id : Long) {
                        val item = adapter !!.getItem(position)
                        if (! isServiceRunning(MusicService::class.java)) {
                            MusicService.setTracks(this@MainActivity , adapter !!.snapshot.toTypedArray<MusicItem>())
                        }
                        MusicService.playTrack(this@MainActivity , item)
                    }
                })

        // check if we can draw overlays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ! Settings.canDrawOverlays(this@MainActivity)) {
            val listener = object : DialogInterface.OnClickListener() {

                @TargetApi(Build.VERSION_CODES.M)
                fun onClick(dialog : DialogInterface , which : Int) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION , Uri.parse("package:" + packageName))
                        startActivityForResult(intent , OVERLAY_PERMISSION_REQ_CODE)
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        onPermissionsNotGranted()
                    }
                }
            }
            AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.permissions_title))
                    .setMessage(getString(R.string.draw_over_permissions_message))
                    .setPositiveButton(getString(R.string.btn_continue) , listener)
                    .setNegativeButton(getString(R.string.btn_cancel) , listener)
                    .setCancelable(false)
                    .show()
            return
        }
        checkReadStoragePermission()
    }

    protected fun onActivityResult(requestCode : Int , resultCode : Int , data : Intent) {
        super.onActivityResult(requestCode , resultCode , data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ! Settings.canDrawOverlays(this@MainActivity)) {
                onPermissionsNotGranted()
            } else {
                checkReadStoragePermission()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode : Int , @NonNull permissions : Array<String> , @NonNull grantResults : IntArray) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults)
        if (requestCode == EXT_STORAGE_PERMISSION_REQ_CODE) {
            for (i in permissions.indices) {
                if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i]) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    loadMusic()
                    return
                }
            }
            onPermissionsNotGranted()
        }
    }

    override fun onResume() {
        super.onResume()
        MusicService.setState(this , false)
    }

    override fun onPause() {
        super.onPause()
        MusicService.setState(this , true)
    }

    /**
     * Check if we have necessary permissions.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun checkReadStoragePermission() {
        if (ContextCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this , Manifest.permission.READ_EXTERNAL_STORAGE)) {
                val onClickListener = object : DialogInterface.OnClickListener() {
                    fun onClick(dialog : DialogInterface , which : Int) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            ActivityCompat.requestPermissions(this@MainActivity , arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE) , EXT_STORAGE_PERMISSION_REQ_CODE)
                        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                            onPermissionsNotGranted()
                        }
                        dialog.dismiss()
                    }
                }
                AlertDialog.Builder(this)
                        .setTitle(R.string.permissions_title)
                        .setMessage(R.string.read_ext_permissions_message)
                        .setPositiveButton(R.string.btn_continue , onClickListener)
                        .setNegativeButton(R.string.btn_cancel , onClickListener)
                        .setCancelable(false)
                        .show()
                return
            }
            ActivityCompat.requestPermissions(this@MainActivity , arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE) , EXT_STORAGE_PERMISSION_REQ_CODE)
            return
        }
        loadMusic()
    }

    /**
     * Load music.
     */
    private fun loadMusic() {
        supportLoaderManager.initLoader(MUSIC_LOADER_ID , null , this)
    }

    /**
     * Permissions not granted. Quit.
     */
    private fun onPermissionsNotGranted() {
        Toast.makeText(this , R.string.toast_permissions_not_granted , Toast.LENGTH_SHORT).show()
        finish()
    }

    fun onCreateOptionsMenu(menu : Menu) : Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.main , menu)
        val searchItem = menu.findItem(R.id.item_search)
        val searchView = MenuItemCompat.getActionView(searchItem) as SearchView
        searchView.setOnQueryTextListener(this)
        return true
    }

    fun onCreateLoader(id : Int , args : Bundle) : Loader<Collection<MusicItem>>? {
        return if (id == MUSIC_LOADER_ID) MusicLoader(this) else null
    }

    fun onLoadFinished(loader : Loader<Collection<MusicItem>> , data : Collection<MusicItem>) {
        adapter !!.addAll(data)
        adapter !!.notifyItemRangeInserted(0 , data.size())
        MusicService.setTracks(this , data.toArray(arrayOfNulls<MusicItem>(data.size())))
    }

    fun onLoaderReset(loader : Loader<Collection<MusicItem>>) {
        val size = adapter !!.itemCount
        adapter !!.clear()
        adapter !!.notifyItemRangeRemoved(0 , size)
    }

    fun onQueryTextSubmit(query : String) : Boolean {
        return false
    }

    fun onQueryTextChange(newText : String) : Boolean {
        adapter !!.filter !!.filter(newText)
        return true
    }

    override fun onDestroy() {
        emptyViewObserver !!.unbind()
        super.onDestroy()
    }

    /**
     * Check if service is running.
     *
     * @param serviceClass
     * @return
     */
    private fun isServiceRunning(@NonNull serviceClass : Class<*>) : Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.getClassName()) {
                return true
            }
        }
        return false
    }

    companion object {

        private val MUSIC_LOADER_ID = 1
        private val OVERLAY_PERMISSION_REQ_CODE = 1
        private val EXT_STORAGE_PERMISSION_REQ_CODE = 2
    }
}
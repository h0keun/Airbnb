package com.airbnb

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.util.MarkerIcons
import com.naver.maps.map.widget.LocationButtonView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//네이버 맵 api 를 이용하는 두가지 경우가 있는데
//하나는 프래그먼트로 하나는 맵뷰로 둘의 차이는 프래그먼트는
//생영주기를 신경쓰지 않아도 되지만 커스텀이 어렵
//앱뷰는 여러 커스텀이 가능하지만 생명주기상 이슈가 있어서
//아래처럼 직접 설정해주어야함함
class MainActivity : AppCompatActivity(), OnMapReadyCallback, Overlay.OnClickListener {

    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private val mapView: MapView by lazy {
        findViewById(R.id.mapView)
    }

    private val viewPager: ViewPager2 by lazy {
        findViewById(R.id.houseViewPager)
    }

    private val recyclerView: RecyclerView by lazy{
        findViewById(R.id.recyclerView)
    }

    private val currentLocationButton: LocationButtonView by lazy {
        findViewById(R.id.currentLocationButton)
    }

    private val bottomSheetTitleTextView : TextView by lazy {
        findViewById(R.id.bottomSheetTitleTextView)
    }

    private val viewPagerAdapter = HouseViewPagerAdapter(itemClicked = {
        val intent = Intent()
            .apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT,"[여기 어떰?? 지금 예약해!!] \n 장소 : ${it.title} \n 가격 : ${it.price} \n 사진보기 : ${it.imgUrl}")
                type = "text/plain"
            }
        startActivity(Intent.createChooser(intent, null))
    })
    private val recyclerAdapter = HouseListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)

        // MapReady 라는 함수의 인자로는 OnMapReadyCallback 이라는
        // 인터페이스의 구현체가 담긴것이 들어가야하는데,
        // 현재 OnMapReadyCallback 을 MainActivity 에서 implement 했고,
        // OnMapReady 에 대한 구현을 메인엑티비티에 직접 해주었기 때문에
        // 메인액티비티가 OnMapReadyCallback 의 구현체라고도 할 수 있다
        // 때문에 아래에 this 가 가능
        mapView.getMapAsync(this)

        viewPager.adapter = viewPagerAdapter
        recyclerView.adapter = recyclerAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val selectedHouseModel = viewPagerAdapter.currentList[position]
                val cameraUpdate = CameraUpdate.scrollTo(LatLng(selectedHouseModel.lat, selectedHouseModel.lng))
                    .animate(CameraAnimation.Easing)

                naverMap.moveCamera(cameraUpdate)
            }
        })
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        naverMap.maxZoom = 15.0
        naverMap.minZoom = 4.0

        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.631467, 126.831822))
        naverMap.moveCamera(cameraUpdate)

        val uiSetting = naverMap.uiSettings
        uiSetting.isLocationButtonEnabled = false
        currentLocationButton.map = naverMap

        locationSource = FusedLocationSource(this@MainActivity, LOCATION_PERMISSION_REQUEST_CODE)
        naverMap.locationSource = locationSource

        getHouseListFromAPI()
    }

    private fun getHouseListFromAPI() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(HouseService::class.java).also {
            it.getHouseList()
                .enqueue(object : Callback<HouseDto> {
                    @SuppressLint("SetTextI18n")
                    override fun onResponse(call: Call<HouseDto>, response: Response<HouseDto>) {
                        if (response.isSuccessful.not()) {
                            //실패처리구현
                            return
                        }

                        response.body()?.let { dto ->
                            updateMarkers(dto.items)
                            viewPagerAdapter.submitList(dto.items)
                            recyclerAdapter.submitList(dto.items)

                            bottomSheetTitleTextView.text = "${dto.items.size}개의 숙소"
                        }
                    }

                    override fun onFailure(call: Call<HouseDto>, t: Throwable) {
                        // 실패처리구현
                    }

                })
        }
    }

    private fun updateMarkers(house: List<HouseModel>) {
        house.forEach { house ->
            val marker = Marker()
            marker.position = LatLng(house.lat, house.lng)
            marker.onClickListener = this

            marker.map = naverMap // 이외에 다양한 기능들은 공식문서참조
            marker.tag = house.id
            marker.icon = MarkerIcons.BLACK
            //marker.iconTintColor = Color.RED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated) {
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onClick(overlay: Overlay): Boolean {
        val selectedModel = viewPagerAdapter.currentList.firstOrNull {
            it.id == overlay.tag
        }
        selectedModel?. let{
            val position = viewPagerAdapter.currentList.indexOf(it)
            viewPager.currentItem = position
        }

        return true
    }
}
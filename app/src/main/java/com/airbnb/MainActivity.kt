package com.airbnb

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.util.MarkerIcons
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
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource

    private val mapView: MapView by lazy {
        findViewById(R.id.mapView)
    }

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
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        naverMap.maxZoom = 15.0
        naverMap.minZoom = 4.0

        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.631467, 126.831822))
        naverMap.moveCamera(cameraUpdate)

        val uiSetting = naverMap.uiSettings
        uiSetting.isLocationButtonEnabled = true

        locationSource = FusedLocationSource(this@MainActivity, LOCATION_PERMISSION_REQUEST_CODE)
        naverMap.locationSource = locationSource

//        val marker = Marker()
//        marker.position = LatLng(37.623577, 126.825321)
//        marker.map = naverMap // 다양한 다른 기능들은 네이버 공식 가이드라인에..
//        marker.icon = MarkerIcons.BLACK
//        marker.iconTintColor = Color.YELLOW


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
                    override fun onResponse(call: Call<HouseDto>, response: Response<HouseDto>) {
                        if (response.isSuccessful.not()) {
                            //실패처리구현
                            return
                        }

                        response.body()?.let { dto ->
                            updateMarkers(dto.items)
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
            // todo 마커클릭리스터
            marker.map = naverMap
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
}
package com.sonnhe.voicecommand.phonehelper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class MapService {

    private Context mContext;
    private String mStartLoc, mEndLoc;
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();


    public MapService(Context context){

        mContext = context;
        mLocationClient = new LocationClient(mContext);
        //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);

        LocationClientOption option = new LocationClientOption();

        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        option.setCoorType("bd09ll");

        option.setOpenGps(true);

        option.setIsNeedAddress(true);

        option.setLocationNotify(true);

        option.SetIgnoreCacheException(false);

        option.setWifiCacheTimeOut(5*60*1000);

        option.setEnableSimulateGps(false);

        mLocationClient.setLocOption(option);

    }

    /**
     * 检查手机上是否安装了指定的软件
     * @param context
     * @param packageName：应用包名
     * @return
     * */
    private static boolean isAvilible(Context context, String packageName){
        //获取packagemanager
        final PackageManager packageManager = context.getPackageManager();
        //获取所有已安装程序的包信息
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        //用于存储所有已安装程序的包名
        List<String> packageNames = new ArrayList<String>();
        //从pinfo中将包名字逐一取出，压入pName list中
        if(packageInfos != null){
            for(int i = 0; i < packageInfos.size(); i++){
                String packName = packageInfos.get(i).packageName;
                packageNames.add(packName);
            }
        }
        //判断packageNames中是否有目标程序的包名，有TRUE，没有FALSE
        return packageNames.contains(packageName);
    }

    public void startGuide(String startLoc, String endLoc){
        mStartLoc = startLoc;
        mEndLoc = endLoc;
        if (isAvilible(mContext, "com.autonavi.minimap")) {
            //调用高德地图
            try {
                Intent intent;
                if (startLoc.equals("CURRENT_ORI_LOC")) {
                    //导航（无起始点）
                    intent = Intent.getIntent("androidamap://route?sourceApplication=softname" + "&dname=" + endLoc + "&dev=0&m=0&t=0");
                } else {
                    //导航（有起始点）
                    intent = Intent.getIntent("androidamap://route?sourceApplication=softname" + "&sname=" + startLoc + "&dname=" + endLoc + "&dev=0&m=0&t=0");
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                mContext.startActivity(intent);

                PendingIntent pendingIntent =
                        PendingIntent.getActivity(mContext, 0, intent, 0);
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }

            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }else if(isAvilible(mContext,"com.baidu.BaiduMap")){
            //调用百度地图
            try {
                Intent intent;
                if(startLoc.equals("CURRENT_ORI_LOC")){
                    //导航（无起始点）
                    intent = Intent.getIntent("baidumap://map/direction?destination=" + endLoc +"&coord_type=bd09ll&mode=diving&sy=3&index=0&target=1&src=andr.sonnhe.voicecommandapplication");
                }else{
                    //导航（有起始点）
                    intent = Intent.getIntent("baidumap://map/direction?origin=" + startLoc + "&destination=" + endLoc + "&coord_type=bd09ll&mode=diving&sy=3&index=0&target=1&src=andr.sonnhe.voicecommandapplication");
                }
                mContext.startActivity(intent);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else{
            //调用百度web地图
        Log.e("mLocationClient.start", "mLocationClient.start");
            mLocationClient.start();
        }
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            Log.e("onReceiveLocation", "onReceiveLocation: ");
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明

            double latitude = location.getLatitude();    //获取纬度信息
            double longitude = location.getLongitude();    //获取经度信息
            float radius = location.getRadius();    //获取定位精度，默认值为0.0f

            String coorType = location.getCoorType() ;
            //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准

            int errorCode = location.getLocType();
            //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
            String city = location.getCity();
            String uristr;
            Log.e("mStartLoc", mStartLoc);
            if(mStartLoc.equals("CURRENT_ORI_LOC")){
                //导航（无起始点）
                Log.e("MapService", "http://api.map.baidu.com/direction?origin=latlng:"+ latitude +","+ longitude +"|name:我的位置&destination="+ mEndLoc +"&mode=driving&region="+ city +"&output=html&src=webapp.sonnhe.voicecommandapplication");
                uristr = "http://api.map.baidu.com/direction?origin=latlng:"+ latitude +","+ longitude +"|name:我的位置&destination="+ mEndLoc +"&mode=driving&region="+ city +"&output=html&src=webapp.sonnhe.voicecommandapplication";
            }else{
                //导航（有起始点）
                uristr = "http://api.map.baidu.com/direction?origin=" + mStartLoc + "&destination=" + mEndLoc + "&mode=driving&region="+ city +"&output=html&src=webapp.sonnhe.voicecommandapplication";
            }
            mLocationClient.stop();
            Uri uri = Uri.parse(uristr);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            mContext.startActivity(intent);
        }
    }

}

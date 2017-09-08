package com.zt.tz.pluginmain;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String TAG="zhangtong";
    private Button mBtn;
    private ImageView mImageView;
    private List<Map<String,String>> mPluginList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtn = (Button) findViewById(R.id.btn_plugin);
        mImageView = (ImageView) findViewById(R.id.iv_plugin);

    }
    public void onClick(View view){
        PopupWindow popupWindow = new PopupWindow(this);
        View v=View.inflate(this,R.layout.popup_plugin_item,null);
        popupWindow.setContentView(v);
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.popu_bg));
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        //查找插件
        mPluginList =findPluginList();
        if(mPluginList ==null|| mPluginList.size()==0){
            Toast.makeText(this, "no skin", Toast.LENGTH_SHORT).show();
            return;
        }
        ListView lv_plugin = (ListView) v.findViewById(R.id.lv_plugin);
        SimpleAdapter simpleAdapter=new SimpleAdapter(this, mPluginList,android.R.layout.simple_list_item_1,new String[]{"label"},new int[]{android.R.id.text1});
        lv_plugin.setAdapter(simpleAdapter);
        lv_plugin.setOnItemClickListener(this);
        popupWindow.showAsDropDown(view);

    }

    private List<Map<String, String>> findPluginList() {
        List<Map<String, String>> list=new ArrayList<Map<String,String>>();
        PackageManager packageManager=this.getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
        //当前的包信息
        try {
            PackageInfo currentPackageInfo=this.getPackageManager().getPackageInfo(getPackageName(),0);
            Log.d(TAG, "findPluginList: "+currentPackageInfo.packageName+"current"+currentPackageInfo.sharedUserId);
            for (PackageInfo packageInfo : installedPackages) {
                    String pkgName=packageInfo.packageName;
                    String sharedUserId=packageInfo.sharedUserId;
                //跳过sharedUserId为空，sharedUserId不匹配，以及本身程序。
                if(sharedUserId==null||!sharedUserId.equals(currentPackageInfo.sharedUserId)||pkgName.equals(currentPackageInfo.packageName)){
                    Log.d(TAG, "findPluginList: "+sharedUserId+"  "+currentPackageInfo.sharedUserId);
                    continue;
                }
                //加载插件
                Map<String,String> pluginMap=new HashMap<String,String>();
                //获取插件程序的名称
                String label=packageInfo.applicationInfo.loadLabel(packageManager).toString();
                //获取包名称
                pluginMap.put("packageName",pkgName);
                pluginMap.put("label",label);
                //添加插件
                list.add(pluginMap);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //加载插件资源
        //1.获取插件上下文（获取插件加载器）
        Map<String,String> map= mPluginList.get(position);
        Context context=findPluginContext(map);
        int resId=findPluginResid(context, map);
        if(resId!=0){
            Drawable drawable = context.getResources().getDrawable(resId);
            mImageView.setImageDrawable(drawable);
        }
    }
    /**
     * @param plugincontext
     * @param map
     * @return
     */
    private int findPluginResid(Context plugincontext, Map<String, String> map) {
        String packageName=map.get("packageName");
        try {
            //创建类加载器
            ClassLoader classLoader=new PathClassLoader(plugincontext.getPackageResourcePath(),PathClassLoader.getSystemClassLoader());
            //获取资源类
            Class<?> forName = Class.forName(packageName + ".R$drawable", true, classLoader);
            Field[] fields=forName.getFields();
            for(Field field:fields){
                String name=field.getName();
                if(name.equals("skin")){
                    Log.d(TAG, "findPluginResid: skin");
                    return field.getInt(R.drawable.class);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Context findPluginContext(Map<String, String> map) {
        try {
            return this.createPackageContext(map.get("packageName"),CONTEXT_IGNORE_SECURITY|Context.CONTEXT_INCLUDE_CODE);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}

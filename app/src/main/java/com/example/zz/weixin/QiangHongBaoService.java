package com.example.zz.weixin;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.util.List;

/**
 * Created by Administrator on 2016/12/26 0026.
 */
public class QiangHongBaoService extends AccessibilityService {
    static final String TAG="QiangHongBao";

    static final String WECHAT_PACKAGENAME="com.tencent.mm";
    //微信的包名
    static final String WX_HONGBAO_TEXT_KEY="[微信红包]";
    //红包消息的关键字
    static final String QQ_HONGBAO_TEXT_KEY="[QQ红包]";

    private boolean caihangbao=false;

    Handler handler=new Handler();

    @Override
    //接收事件，如触发了通知栏变化、界面变化等
    public void onAccessibilityEvent(AccessibilityEvent event) {

        final int eventType=event.getEventType();//ClassName
        //通知栏事件
        if(eventType==AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
        {
            List<CharSequence> texts=event.getText();
            if(!texts.isEmpty())
            {
                for(CharSequence t:texts)
                {
                    String text=String.valueOf(t);
                    if(text.contains(WX_HONGBAO_TEXT_KEY)||text.contains(QQ_HONGBAO_TEXT_KEY))
                    {
                        openNotify(event);
                        break;
                    }
                }
            }
        }
        else if(eventType==AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE)
        {
            openHongBao(event);//从微信主界面进入聊天界面
        }
        else if(eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        {
            openHongBao(event);//处理微信聊天界面
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openNotify(AccessibilityEvent event) {
        if(event.getParcelableData()==null||!(event.getParcelableData()instanceof Notification))
        {
            return;
        }
        //将微信的通知栏消息打开
        Notification notification= (Notification) event.getParcelableData();
        PendingIntent pendingIntent=notification.contentIntent;
        try
        {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openHongBao(AccessibilityEvent event) {
        CharSequence className=event.getClassName();
        checkScreen(getApplicationContext());
        if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(className))
        {
            checkKey1();
            //点中了红包，下一步就是去拆红包
        }
        else if("com.tencent.mm.ui.LauncherUI".equals(className)||"com.tencent.mobileqq.activity.ChatActivity".equals(className))
        {
            checkKey2();
            //在聊天界面去点中红包
        }
        else
        {
            checkKey2();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void checkKey1() {
        AccessibilityNodeInfo nodeInfo=getRootInActiveWindow();
        if(nodeInfo==null)
            return;
        List<AccessibilityNodeInfo> list=nodeInfo.findAccessibilityNodeInfosByText("拆红包");
        if(list==null||list.size()==0)
        {
            list=nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b2c");
        }
        for(AccessibilityNodeInfo n:list)
        {
            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void checkKey2() {
        AccessibilityNodeInfo nodeInfo=getRootInActiveWindow();
        if(nodeInfo==null)
        {
            Log.w(TAG,"rootWindow为空");
            return;
        }
        List<AccessibilityNodeInfo> wxList=nodeInfo.findAccessibilityNodeInfosByText("领取红包");
        List<AccessibilityNodeInfo> qqList=nodeInfo.findAccessibilityNodeInfosByText("QQ红包");
        if(!qqList.isEmpty())
        {
            qqList=nodeInfo.findAccessibilityNodeInfosByText("QQ红包");
            for(AccessibilityNodeInfo n:qqList)
            {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }
        if(!wxList.isEmpty())
        {
            //界面上的红包总个数
            int totalCount=wxList.size();
            for(int i=totalCount-1;i>=0;i--) {
                //如果为领取过该红包，则执行点击
                AccessibilityNodeInfo parent = wxList.get(i).getParent();
                if (parent != null) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    break;
                }
            }
        }
        else if (!qqList.isEmpty())
        {
            int totalCount=qqList.size();
            //领取最近发的红包
            for(int i=totalCount-1;i>=0;i--)
            {
                AccessibilityNodeInfo parent = qqList.get(i).getParent();
                Log.i(TAG,"领取红包"+parent);
                if (parent != null) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    break;
                }
            }
        }
    }

    private void checkScreen(Context applicationContext) {
        //检查屏幕是否亮着并且唤醒屏幕
        PowerManager pm=(PowerManager)applicationContext.getSystemService(applicationContext.POWER_SERVICE);
        if(!pm.isScreenOn())
        {
            wakeUpAndUnlock(applicationContext);
        }
    }

    private void wakeUpAndUnlock(Context applicationContext) {
        KeyguardManager km= (KeyguardManager) applicationContext.getSystemService(applicationContext.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock k1=km.newKeyguardLock("unlock");
        //解锁
        k1.disableKeyguard();
        //获取电源管理器对象
        PowerManager pm= (PowerManager) applicationContext.getSystemService(applicationContext.POWER_SERVICE);
        // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
        PowerManager.WakeLock w1=pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.SCREEN_DIM_WAKE_LOCK,"bright");
        //点亮屏幕
        w1.acquire();
        //释放
        w1.release();
    }

    //接受按键事件
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return super.onKeyEvent(event);
    }

    //如授权中断或者将服务杀死
    @Override
    public void onInterrupt() {
        Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();
    }

    //连接服务后，一般是在授权成功后会接受到
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "连接抢红包服务", Toast.LENGTH_SHORT).show();
    }
    //打开通知栏消息

}

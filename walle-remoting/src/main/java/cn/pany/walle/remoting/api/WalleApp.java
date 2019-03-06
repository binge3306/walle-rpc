package cn.pany.walle.remoting.api;

import cn.pany.walle.common.URL;
import cn.pany.walle.common.constants.WalleConstant;
import cn.pany.walle.common.model.InterfaceDetail;
import cn.pany.walle.common.model.ServerInfo;
import cn.pany.walle.common.utils.UrlUtils;
import cn.pany.walle.remoting.client.WalleClient;
import cn.pany.walle.remoting.registry.WalleRegistry;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by pany on 18/1/22.
 */
@Slf4j
public class WalleApp {
    private static final Logger log = LoggerFactory.getLogger(WalleApp.class);

    private String appName;

    private Set<WalleClient> walleClientSet=new HashSet<>();

    private WalleRegistry walleRegistry;
    private  AppState appState=AppState.INIT;
    private  String version;
    private String appPath;
    enum AppState{
        INIT(1),INITED(2),CLOSE(9);

        private int code;
        AppState(int code){this.code=code;}
    }

    public WalleApp(String appName, WalleRegistry walleRegistry) {
        this.walleRegistry = walleRegistry;
        this.appName = appName;
    }
    public WalleApp(String appName, WalleRegistry walleRegistry,String version) {
        this.walleRegistry = walleRegistry;
        this.appName = appName;
        this.version= version;
    }
    PathChildrenCache childrenCache = null;
    /*从zookeeper获取服务端的信息
    进行连接
    对所需接口*/
    public synchronized boolean init()  {
        if(appState==AppState.INITED){
            return true;
        }else if(appState==AppState.CLOSE){
            return false;
        }

        try {
            if (walleRegistry.isRegister()) {
                walleRegistry.register();
            }
            appPath=WalleRegistry.ZK_SPLIT + appName +WalleRegistry.ZK_SPLIT + WalleRegistry.WALLE_SERVER_DEFULT ;
            List<String> serverList = walleRegistry.getChildrenList(appPath);

            //ip:port#version@protocol
            for (String serverDetail : serverList) {
                URL url = UrlUtils.parseURL(serverDetail, null);

                byte[] interfaceListByte = walleRegistry.
                getData(appPath + WalleRegistry.ZK_SPLIT + serverDetail);
                List<InterfaceDetail> interfaceList =
                        JSON.parseObject(new String(interfaceListByte), ServerInfo.class).getInterfaceDetailList();

                WalleClient walleClient = new WalleClient(this,url,interfaceList,walleRegistry);
                if(!walleClientSet.contains(walleClient)){
                    //连接成功后会在afterConnetion里walleApp.getWalleClientSet().add(this);
                    walleClient.init();
                }


            }

            if(childrenCache==null){
                try {
                    childrenCache = new PathChildrenCache(this.getWalleRegistry().register(), WalleRegistry.ZK_SPLIT +  getAppName()+WalleRegistry.ZK_SPLIT + WalleRegistry.WALLE_SERVER_DEFULT, true);

                    PathChildrenCacheListener childrenCacheListener = new PathChildrenCacheListener() {
                        @Override
                        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                            log.info("zk监听开始进行事件分析");
                            if(event.getData()==null){
                                log.info("childEvent event.getData is null,data is :" + event.toString());
                                return;
                            }
                            ChildData data = event.getData();
                            ServerInfo serverInfo;
                            String lastPath;
                            if (data.getData() == null) {
                                log.info("childEvent data is null,path is :" + data.getPath());
                                return;
                            }
                            String dataStr = new String(data.getData());
                            switch (event.getType()) {
                                case CHILD_ADDED:
                                    serverInfo=JSON.parseObject(new String(data.getData()), ServerInfo.class);
//                            data.getPath().substring(data.getPath().lastIndexOf(WalleRegistry.ZK_SPLIT));
                                    lastPath=data.getPath().substring(data.getPath().lastIndexOf(WalleRegistry.ZK_SPLIT)+1);
                                    log.info("CHILD_ADDED : " + data.getPath() + "  数据:" + new String(data.getData()));

                                    WalleClient walleClient = new WalleClient(WalleApp.this, UrlUtils.parseURL(lastPath, null), serverInfo.getInterfaceDetailList(), WalleApp.this.getWalleRegistry());
                                    if (! getWalleClientSet().contains(walleClient)) {
                                        getWalleClientSet().add(walleClient);
                                        walleClient.init();
                                    }

                                    break;
                                case CHILD_REMOVED:
//                            serverInfo=JSON.parseObject(new String(data.getData()), ServerInfo.class);
                                    lastPath=data.getPath().substring(data.getPath().lastIndexOf(WalleRegistry.ZK_SPLIT)+1);

                                    log.info("CHILD_REMOVED : " + data.getPath() + "  数据:" + new String(data.getData()));

                                    for(WalleClient walleClientTemp :WalleApp.this.getWalleClientSet()){
                                        if( walleClientTemp.getUrl().getAddress().equals(lastPath)){
                                            log.info("client REMOVED : " + walleClientTemp.getUrl().getAddress());
                                            walleClientTemp.close();
                                            getWalleClientSet().remove(walleClientTemp);
                                        }
//                            todo 假如在注销的时候，对应的服务又再启动呢？
/*                            todo (改动比较大，后续改动,目前不清理影响不大……)
                              todo 方案1：加入标识状态，先标识为不可用，后续通过定时检查再把可用的启动
                              todo 同时当调用失败的时候，如果是网络断开，也先标识为不可用,期间就算是服务不断的重启，也不理，通过后续检查
*/
//                            for(InterfaceDetail interfaceDetail : serverInfo.getInterfaceDetailList()){
//                                String invokerUrl = InvokerUtil.formatInvokerUrl(interfaceDetail.getClassName(),null,interfaceDetail.getVersion());;
//                                WalleInvoker walleInvoker =WalleInvoker.walleInvokerMap.get(invokerUrl);
//                                if(walleInvoker!=null && walleInvoker.getClients()!=null){
//                                    walleInvoker = new WalleInvoker<>(interfaceDetail.getClass(),invokerUrl);
//                                    if(walleInvoker.getClients().contains(walleClient)){
//                                        walleInvoker.getClients().remove(walleClient);
//                                    }
//                                }
//                            }
                                    }
                                    break;
                                case CHILD_UPDATED:
//                            serverInfo=JSON.parseObject(new String(data.getData()), ServerInfo.class);
                                    log.info("CHILD_UPDATED : " + data.getPath() + "  数据:" + new String(data.getData()));
                                    break;
                                default:
                                    break;
                            }
                        }
                    };
                    childrenCache.getListenable().addListener(childrenCacheListener);
                    log.info("Register zk app watcher path:[{}] successfully!", WalleRegistry.ZK_SPLIT+ WalleConstant.NAME_SPACE+WalleRegistry.ZK_SPLIT +  getAppName());
                    childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
                } catch (Exception e) {
                    log.error("",e);
                }

            }

        } catch (Exception e) {
            log.error("", e);
            return false;
        }
        appState=AppState.INITED;
        return true;
    }

    public void  getInterFace(String appName){
        try {
            getWalleRegistry().getData(WalleRegistry.ZK_SPLIT+appName);
        } catch (Exception e) {
          log.error("",e);
        }
    }


    public Set<WalleClient> getWalleClientSet() {
        return walleClientSet;
    }

    public void setWalleClientSet(Set<WalleClient> walleClientSet) {
        this.walleClientSet = walleClientSet;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public WalleRegistry getWalleRegistry() {
        return walleRegistry;
    }

    public void setWalleRegistry(WalleRegistry walleRegistry) {
        this.walleRegistry = walleRegistry;
    }
}

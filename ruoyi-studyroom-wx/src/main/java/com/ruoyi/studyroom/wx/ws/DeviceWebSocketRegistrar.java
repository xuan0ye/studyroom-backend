package com.ruoyi.studyroom.wx.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

/**
 * 把门禁 WebSocket 端点注册到容器。
 *
 * <p>本项目没有 spring-websocket(本地仓库里只有 Boot3/Spring6 的版本, 与本项目 Boot2.6/Spring5 不兼容),
 * 因此不用 ServerEndpointExporter, 直接取 Undertow 暴露的 {@link ServerContainer} 程序化注册。</p>
 *
 * <p>设备会拨哪个路径, 商家文档没有写明({@code F1setServerInfo} 只给 address+port),
 * 故默认在多个候选路径上同时注册, 并由 {@code studyroom.access.ws.paths} 配置覆盖。</p>
 *
 * @author studyroom
 */
@Slf4j
@Component
public class DeviceWebSocketRegistrar implements ServletContextInitializer, ServletContextAware, ApplicationListener<ApplicationReadyEvent> {

    /**
     * 设备会拨哪个路径, 商家文档没写明({@code F1setServerInfo} 只给 address+port)。
     * 现已知出厂服务器地址形态为 {@code http://server.yefiot.com/server/access/},
     * 故把 /server/access 的带/不带尾斜杠、以及多一段的变体全部注册。
     * 注意 JSR-356 是<b>精确匹配</b>: 注册了 /server 并<b>不会</b>匹配 /server/access/。
     */
    @Value("${studyroom.access.ws.paths:/,/ws,/device-ws,/websocket,/face,/face-device,/server/access,/server/access/,/server/access/{tail},/serve/access,/serve,/access,/server}")
    private String paths;

    private ServletContext servletContext;
    private volatile boolean registered = false;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /** 容器启动阶段先试一次 */
    @Override
    public void onStartup(ServletContext servletContext) {
        this.servletContext = servletContext;
        register("ServletContextInitializer");
    }

    /** 应用完全就绪后再兜一次(此时 ServerContainer 一定已就位) */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        register("ApplicationReadyEvent");
    }

    private synchronized void register(String phase) {
        if (registered) {
            return;
        }
        ServerContainer container = findContainer();
        if (container == null) {
            log.error("[门禁WS] ({}) 未找到 ServerContainer, 端点未注册 —— WebSocket 通道不可用", phase);
            return;
        }
        int ok = 0;
        for (String raw : paths.split(",")) {
            String path = raw.trim();
            if (path.isEmpty()) {
                continue;
            }
            try {
                ServerEndpointConfig config = ServerEndpointConfig.Builder
                    .create(DeviceWebSocketEndpoint.class, path)
                    .build();
                container.addEndpoint(config);
                ok++;
                log.info("[门禁WS] ({}) 端点已注册: {}", phase, path);
            } catch (DeploymentException e) {
                log.warn("[门禁WS] ({}) 端点注册失败 {}: {}", phase, path, e.getMessage());
            } catch (Exception e) {
                log.warn("[门禁WS] ({}) 端点注册异常 {}: {}", phase, path, e.getMessage());
            }
        }
        if (ok > 0) {
            registered = true;
            log.info("[门禁WS] 共注册 {} 个设备接入路径: {}", ok, paths);
        }
    }

    private ServerContainer findContainer() {
        if (servletContext == null) {
            return null;
        }
        // 不同 Undertow 版本/API 组合下属性名不同, 依次尝试
        String[] attrs = {
            "javax.websocket.server.ServerContainer",
            "jakarta.websocket.server.ServerContainer",
            ServerContainer.class.getName()
        };
        for (String a : attrs) {
            Object v = servletContext.getAttribute(a);
            if (v instanceof ServerContainer) {
                log.info("[门禁WS] ServerContainer 来源属性: {}", a);
                return (ServerContainer) v;
            }
        }
        return null;
    }
}

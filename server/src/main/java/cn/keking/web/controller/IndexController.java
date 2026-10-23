package cn.keking.web.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面跳转
 *
 * 说明：原演示页与文档页（首页、接入说明、版本发布记录、赞助开源、技术支持）已全部移除，
 * 对外统一返回 401，仅保留预览相关接口（/onlinePreview 等）。
 *
 * @author yudian-it
 * @date 2017/12/27
 */
@Controller
public class IndexController {

    @GetMapping({"/", "/index"})
    public String root(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return "/main/denied";
    }
}

package com.example.vupworld.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VUP出道局 API")
                        .description("30天VUP出道模拟器 - SSM课程设计接口文档")
                        .version("1.0.0"))
                .tags(List.of(
                        new Tag().name("认证").description("用户注册、登录、当前用户"),
                        new Tag().name("VUP").description("创建和管理VUP角色"),
                        new Tag().name("每日流程").description("日会话、行动提交、下一天推进"),
                        new Tag().name("行动").description("可用行动列表"),
                        new Tag().name("直播").description("标题选择、标题刷新"),
                        new Tag().name("事件").description("正式事件处理"),
                        new Tag().name("日报").description("每日日报查询"),
                        new Tag().name("结局").description("结局复盘查询"),
                        new Tag().name("重生").description("重开新一轮"),
                        new Tag().name("NPC").description("NPC查房与互动"),
                        new Tag().name("粉丝群").description("粉丝群议题"),
                        new Tag().name("弹幕").description("弹幕展示"),
                        new Tag().name("组合技").description("组合技发现"),
                        new Tag().name("人设标签").description("人设标签展示"),
                        new Tag().name("梗").description("梗生命周期"),
                        new Tag().name("热度简报").description("热度简报"),
                        new Tag().name("观众期待").description("观众期待展示"),
                        new Tag().name("结局预演").description("结局预演台"),
                        new Tag().name("米线工具").description("风险债务工具"),
                        new Tag().name("每日运势").description("每日运势"),
                        new Tag().name("阶段复盘").description("阶段复盘节点"),
                        new Tag().name("成就").description("成就系统"),
                        new Tag().name("系统").description("系统配置检查")
                ));
    }
}

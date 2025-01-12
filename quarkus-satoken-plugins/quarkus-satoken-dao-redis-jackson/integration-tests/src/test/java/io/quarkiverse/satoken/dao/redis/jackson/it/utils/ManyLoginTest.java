package io.quarkiverse.satoken.dao.redis.jackson.it.utils;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.session.TokenSign;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import io.quarkus.test.junit.QuarkusTest;

/**
 * ManyLoginTest
 *
 * @author nayan
 * @date 2022/4/12 2:11 PM
 */
@QuarkusTest
public class ManyLoginTest {
    // 持久化Bean 
    SaTokenDao dao = SaManager.getSaTokenDao();

    // 开始 
    @BeforeAll
    public static void beforeClass() {
        System.out.println("\n------------ 多端登录测试 star ...");
    }

    // 结束 
    @AfterAll
    public static void afterClass() {
        //    	System.out.println("\n---------- 多端登录测试 end ... \n");
    }

    // 测试：并发登录、共享token、同端 
    @Test
    public void login() {
        SaManager.setConfig(new SaTokenConfig());

        StpUtil.login(10001);
        String token1 = StpUtil.getTokenValue();

        StpUtil.login(10001);
        String token2 = StpUtil.getTokenValue();

        Assertions.assertEquals(token1, token2);
    }

    // 测试：并发登录、共享token、不同端 
    @Test
    public void login2() {
        SaManager.setConfig(new SaTokenConfig());

        StpUtil.login(10002, "APP");
        String token1 = StpUtil.getTokenValue();

        StpUtil.login(10002, "PC");
        String token2 = StpUtil.getTokenValue();

        Assertions.assertNotEquals(token1, token2);
    }

    // 测试：并发登录、不共享token
    @Test
    public void login3() {
        SaManager.setConfig(new SaTokenConfig().setIsShare(false));

        StpUtil.login(10003);
        String token1 = StpUtil.getTokenValue();

        StpUtil.login(10003);
        String token2 = StpUtil.getTokenValue();

        Assertions.assertNotEquals(token1, token2);
    }

    // 测试：禁并发登录，后者顶出前者 
    @Test
    public void login4() {
        SaManager.setConfig(new SaTokenConfig().setIsConcurrent(false));

        StpUtil.login(10004);
        String token1 = StpUtil.getTokenValue();

        StpUtil.login(10004);
        String token2 = StpUtil.getTokenValue();

        // token不同 
        Assertions.assertNotEquals(token1, token2);

        // token1会被标记为：已被顶下线 
        Assertions.assertEquals(dao.get("satoken:login:token:" + token1), "-4");

        // User-Session里的 token1 签名会被移除 
        List<TokenSign> tokenSignList = StpUtil.getSessionByLoginId(10004).getTokenSignList();
        for (TokenSign tokenSign : tokenSignList) {
            Assertions.assertNotEquals(tokenSign.getValue(), token1);
        }
    }

    // 测试：多端登录，一起强制注销 
    @Test
    public void login5() {
        SaManager.setConfig(new SaTokenConfig());

        StpUtil.login(10005, "APP");
        String token1 = StpUtil.getTokenValue();

        StpUtil.login(10005, "PC");
        String token2 = StpUtil.getTokenValue();

        StpUtil.login(10005, "h5");
        String token3 = StpUtil.getTokenValue();

        // 注销 
        StpUtil.logout(10005);

        // 三个Token应该全部无效 
        Assertions.assertNull(dao.get("satoken:login:token:" + token1));
        Assertions.assertNull(dao.get("satoken:login:token:" + token2));
        Assertions.assertNull(dao.get("satoken:login:token:" + token3));

        // User-Session也应该被清除掉 
        Assertions.assertNull(StpUtil.getSessionByLoginId(10005, false));
        Assertions.assertNull(dao.getSession("satoken:login:session:" + 10005));
    }

    // 测试：多端登录，一起强制踢下线 
    @Test
    public void login6() {
        SaManager.setConfig(new SaTokenConfig());

        StpUtil.login(10006, "APP");
        String token1 = StpUtil.getTokenValue();

        StpUtil.login(10006, "PC");
        String token2 = StpUtil.getTokenValue();

        StpUtil.login(10006, "h5");
        String token3 = StpUtil.getTokenValue();

        // 注销 
        StpUtil.kickout(10006);

        // 三个Token应该全部无效 
        Assertions.assertEquals(dao.get("satoken:login:token:" + token1), "-5");
        Assertions.assertEquals(dao.get("satoken:login:token:" + token2), "-5");
        Assertions.assertEquals(dao.get("satoken:login:token:" + token3), "-5");

        // User-Session也应该被清除掉 
        Assertions.assertNull(StpUtil.getSessionByLoginId(10006, false));
        Assertions.assertNull(dao.getSession("satoken:login:session:" + 10006));
    }

    // 测试：多账号模式，在一个账号体系里登录成功，在另一个账号体系不会校验通过 
    @Test
    public void login7() {
        SaManager.setConfig(new SaTokenConfig());

        StpUtil.login(10008);
        String token1 = StpUtil.getTokenValue();

        StpLogic stp = new StpLogic("user");

        Assertions.assertNotNull(StpUtil.getLoginIdByToken(token1));
        Assertions.assertNull(stp.getLoginIdByToken(token1));
    }
}

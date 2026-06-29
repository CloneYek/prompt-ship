package com.xiaoyu.promptship.constant;

/**
 * 应用常量
 *
 * @author xiaoyu
 * @since 1.0
 */
public interface AppConstant {

    /**
     * 精选应用最小优先级
     */
    int FEATURED_MIN_PRIORITY = 1;

    /**
     * 用户每页最大数量
     */
    int MAX_PAGE_SIZE = 20;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";

}

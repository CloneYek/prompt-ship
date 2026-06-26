package com.xiaoyu.promptship.generator;

import cn.hutool.core.io.resource.FileResource;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * MyBatis-Flex 代码生成器
 * <p>
 * 连接数据库读取表结构，一键生成 Entity、Mapper、MapperXml、Service、ServiceImpl、Controller 六层代码。
 * 直接运行 main 方法即可，产物输出至当前项目的 src/main/java 下。
 * </p>
 *
 * <h3>前置条件</h3>
 * <ol>
 *   <li>application.yml 中已配置正确的 spring.datasource（URL 须含 {@code useInformationSchema=true} 以正确获取表注释）</li>
 *   <li>数据库中已建好对应的表</li>
 * </ol>
 *
 * <h3>使用方式</h3>
 * <ol>
 *   <li>修改 {@link #TABLE_NAMES} 数组，填入要生成的表名</li>
 *   <li>运行 main 方法</li>
 * </ol>
 *
 * @author xiaoyu
 * @since 1.0
 */
public class CodeGenerator {

    /** 需要生成代码的数据库表名 */
    private static final String[] TABLE_NAMES = {"user"};

    /** 项目根包路径 */
    private static final String BASE_PACKAGE = "com.xiaoyu.promptship";

    /** 代码作者，将写入生成的 Javadoc @author 中 */
    private static final String AUTHOR = "xiaoyu";

    /** application.yml 相对于项目根目录的路径 */
    private static final String YML_PATH = "src/main/resources/application.yml";

    /**
     * 入口方法：读取数据源配置 → 构建全局生成策略 → 执行生成。
     */
    public static void main(String[] args) {
        HikariDataSource dataSource = buildDataSource();
        GlobalConfig globalConfig = createGlobalConfig();
        Generator generator = new Generator(dataSource, globalConfig);
        generator.generate();
        System.out.println("代码生成完成，请查看 " + BASE_PACKAGE + " 包下的 entity/mapper/service/controller 目录");
    }

    /**
     * 从 application.yml 中读取数据源配置并构建 HikariDataSource。
     *
     * <p>注意：若 URL 中未包含 {@code useInformationSchema=true}，
     * 将自动追加该参数，否则 MySQL 无法返回表和字段的注释信息。</p>
     *
     * @return 配置好的数据源
     */
    @SuppressWarnings("unchecked")
    private static HikariDataSource buildDataSource() {
        // FileResource 相对路径会拼接到 classpath 根目录（target/classes/），
        // 因此用 user.dir 拼出项目根目录的绝对路径来加载
        String absolutePath = System.getProperty("user.dir") + "/" + YML_PATH;
        FileResource resource = new FileResource(absolutePath);

        // YamlUtil.load(Reader) 返回 Map，手动逐层取值比 Dict.getByPath 更可靠
        Map<String, Object> yml = YamlUtil.load(resource.getReader(StandardCharsets.UTF_8));
        Map<String, Object> spring = (Map<String, Object>) yml.get("spring");
        Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
        String url = String.valueOf(datasource.get("url"));
        String username = String.valueOf(datasource.get("username"));
        String password = String.valueOf(datasource.get("password"));

        // 确保 URL 包含 useInformationSchema=true，否则无法获取表/字段注释
        if (!url.contains("useInformationSchema")) {
            url += (url.contains("?") ? "&" : "?") + "useInformationSchema=true";
        }

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    /**
     * 构建全局生成配置，涵盖包路径、表策略以及六类产物的生成开关与选项。
     *
     * <p>产物清单：Entity / Mapper / MapperXml / Service / ServiceImpl / Controller。</p>
     *
     * @return 配置完成的 GlobalConfig 实例
     */
    private static GlobalConfig createGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();

        // ==================== 包配置 ====================
        // 默认输出目录为 user.dir + "/src/main/java"，与当前项目结构一致，不单独指定
        globalConfig.getPackageConfig()
                .setBasePackage(BASE_PACKAGE)
                .setEntityPackage(BASE_PACKAGE + ".model.entity");

        // ==================== 策略配置 ====================
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)       // 白名单：只生成指定表
                .setLogicDeleteColumn("isDelete");    // 逻辑删除字段

        // ==================== Entity 生成 ====================
        globalConfig.enableEntity()
                .setWithLombok(true)                 // 使用 Lombok 替代 getter/setter
                .setJdkVersion(21);                  // JDK 14+ 建议设置

        // ==================== Mapper 生成 ====================
        globalConfig.enableMapper();

        // ==================== MapperXml 生成 ====================
        globalConfig.enableMapperXml();

        // ==================== Service 生成 ====================
        globalConfig.enableService();

        // ==================== ServiceImpl 生成 ====================
        globalConfig.enableServiceImpl();

        // ==================== Controller 生成 ====================
        globalConfig.enableController();

        // ==================== Javadoc 注释 ====================
        globalConfig.getJavadocConfig()
                .setAuthor(AUTHOR)
                .setSince("1.0");

        return globalConfig;
    }

}

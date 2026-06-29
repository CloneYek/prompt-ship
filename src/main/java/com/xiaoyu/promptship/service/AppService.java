package com.xiaoyu.promptship.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.xiaoyu.promptship.model.dto.AppCreateRequest;
import com.xiaoyu.promptship.model.dto.AppQueryRequest;
import com.xiaoyu.promptship.model.dto.AppUpdateMyRequest;
import com.xiaoyu.promptship.model.dto.AppUpdateRequest;
import com.xiaoyu.promptship.model.entity.App;
import com.xiaoyu.promptship.model.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

/**
 * 应用 服务层。
 *
 * @author xiaoyu
 * @since 1.0
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用（用户用）
     *
     * @param request     创建请求
     * @param httpRequest HTTP 请求
     * @return 新应用 id
     */
    long createApp(AppCreateRequest request, HttpServletRequest httpRequest);

    /**
     * 更新自己的应用（用户用，仅允许修改名称）
     *
     * @param request     更新请求
     * @param httpRequest HTTP 请求
     * @return 脱敏后的应用信息
     */
    AppVO updateApp(AppUpdateMyRequest request, HttpServletRequest httpRequest);

    /**
     * 删除自己的应用（用户用）
     *
     * @param id          应用 id
     * @param httpRequest HTTP 请求
     * @return 是否成功
     */
    boolean deleteApp(Long id, HttpServletRequest httpRequest);

    /**
     * 根据 id 获取应用（脱敏）
     *
     * @param id 应用 id
     * @return 脱敏后的应用信息
     */
    AppVO getAppVOById(Long id);

    /**
     * 分页获取当前用户的应用列表（脱敏，支持根据名称查询，每页最多 20 个）
     *
     * @param queryRequest 查询请求
     * @param httpRequest  HTTP 请求
     * @return 脱敏后的应用分页数据
     */
    Page<AppVO> listMyAppVOByPage(AppQueryRequest queryRequest, HttpServletRequest httpRequest);

    /**
     * 分页获取精选应用列表（脱敏，支持根据名称查询，每页最多 20 个）
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的应用分页数据
     */
    Page<AppVO> listGoodAppVOByPage(AppQueryRequest queryRequest);

    // region 管理员

    /**
     * 删除应用（管理员用）
     *
     * @param id 应用 id
     * @return 是否成功
     */
    boolean deleteAppByAdmin(Long id);

    /**
     * 更新应用（管理员用，支持更新名称、封面、优先级）
     *
     * @param request 更新请求
     * @return 脱敏后的应用信息
     */
    AppVO updateAppByAdmin(AppUpdateRequest request);

    /**
     * 分页获取应用列表（管理员用，支持根据除时间外的字段查询，每页数量不限）
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的应用分页数据
     */
    Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest queryRequest);

    /**
     * 根据 id 获取应用（未脱敏，管理员用）
     *
     * @param id 应用 id
     * @return 应用完整信息
     */
    App getAppVOByIdByAdmin(Long id);

    // endregion

    /**
     * 创建应用并与 AI 对话生成代码（流式）
     *
     * @param request     创建请求（提示词、应用名称）
     * @param httpRequest HTTP 请求
     * @return 流式代码内容（首个元素为应用元数据 JSON）
     */
    Flux<String> chatToGenCode(AppCreateRequest request, HttpServletRequest httpRequest);

    /**
     * 部署应用（用户用）。将 code_output 目录下的文件复制到 code_deploy 目录，
     * 生成唯一的 deployKey 作为子目录名，返回可公开访问的 URL。
     *
     * @param appId       应用 id
     * @param httpRequest HTTP 请求
     * @return 可访问的部署 URL，格式为 ${部署域名}/{deployKey}
     */
    String deployApp(Long appId, HttpServletRequest httpRequest);
}

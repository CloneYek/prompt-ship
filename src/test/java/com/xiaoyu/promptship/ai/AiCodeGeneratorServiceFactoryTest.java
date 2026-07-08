package com.xiaoyu.promptship.ai;

import com.xiaoyu.promptship.ai.model.MultiFileCodeResult;
import com.xiaoyu.promptship.core.AiCodeGeneratorFacade;
import com.xiaoyu.promptship.core.vue.VueSkeletonCopier;
import com.xiaoyu.promptship.exception.BusinessException;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceFactoryTest {
    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private AiCodeGeneratorFacade facade;

    @Resource
    private VueSkeletonCopier vueSkeletonCopier;



    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorServiceFactory.aiCodeGeneratorService().generateMultiFileCode("生成一个美丽的“你好世界”页面");
        assertNotNull(result);
        System.out.println(result);
    }

   @Test
   void generateVue(){
        long testAppId =1L;

       try {
           // 1、手动准备好骨架目录
           vueSkeletonCopier.copySkeleton(testAppId);
           // 2、调用AI生成
           TokenStream stream = facade.generateVueAppStream("创建一个个人博客网站", testAppId);
           CompletableFuture<Void> future = new CompletableFuture<>();
           stream
                   .onPartialResponse(p -> System.out.print(p))
                   .onToolExecuted(t -> System.out.println("[工具] " + t.request().name()))
                   .onCompleteResponse(r -> future.complete(null))
                   .onError(future::completeExceptionally)
                   .start();

           future.get(5, TimeUnit.MINUTES);

           // 3. 检查 AI 生成的文件
           File dir = new File("tmp/code_output/vue_app_1/src");
           assert dir.exists();
           Files.list(dir.toPath()).forEach(System.out::println);
       } catch (Exception e) {
           throw new BusinessException(ErrorCode.SYSTEM_ERROR,e.getClass().getSimpleName() + ": " + e.getMessage());
       }
   }
}
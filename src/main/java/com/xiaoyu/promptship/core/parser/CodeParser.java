package com.xiaoyu.promptship.core.parser;

/**
 * 代码解析器策略接口
 * @param <T>
 */
public interface CodeParser<T> {
    T parseCode(String code);
}

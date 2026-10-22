package com.alvis.media.viewmodel.video;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 动漫库筛选面板所需的元数据。
 *
 * <p>每个维度给出「取值 + 中文标签 + 当前条件下的命中数量」，
 * 前端据此渲染 B 站风格的多维筛选面板。
 *
 * @author 奇趣
 */
@Data
public class VideoFilterOptionsResponseVM implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 未筛选时的动漫总数 */
    private Integer total;

    /** 地区维度 */
    private List<Option> countries;

    /** 语言维度 */
    private List<Option> languages;

    /** 动画类型维度 */
    private List<Option> types;

    /** 题材维度 */
    private List<Option> genres;

    /** 可选年份下界 */
    private Integer minYear;

    /** 可选年份上界 */
    private Integer maxYear;

    /** 筛选维度中的单个可选项 */
    @Data
    public static class Option implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 数据库中的原始取值，如 JP / ja / TV / Action */
        private String value;

        /** 面向用户的中文标签 */
        private String label;

        /** 命中数量 */
        private Integer count;
    }
}

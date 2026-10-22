package com.alvis.media.viewmodel.admin.message;

import lombok.Data;


@Data
public class MessageResponseVM {
    private Integer id;

    private String title;

    private String content;

    private String sendUserName;

    private String receives;

    private Integer receiveUserCount;

    private Integer readCount;

    private String createTime;

}

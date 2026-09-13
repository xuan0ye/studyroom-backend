package com.ruoyi.studyroom.domain.bo;

import lombok.Data;

@Data
public class AccessFaceUploadBo {
    private Long roomId;
    private String facePhotoBase64;
    private String facePhotoUrl;
}

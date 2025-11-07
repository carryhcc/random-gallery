package com.example.randomGallery.entity.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PicVO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private Long groupId;
    private String picName;
    private String picUrl;
}

package com.example.randomGallery.entity.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RandomGalleryItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer groupId;

    private String groupName;

    private String picUrl;
}



package com.example.randomGallery.entity.VO;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PicGroupVO implements Serializable {

    public String groupName;

    public List<String> urlList;
}

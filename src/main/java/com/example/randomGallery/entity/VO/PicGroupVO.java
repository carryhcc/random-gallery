package com.example.randomGallery.entity.VO;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PicGroupVO implements Serializable {

    public String groupName;

    public List<String> urlList;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getUrlList() {
        return urlList;
    }

    public void setUrlList(List<String> urlList) {
        this.urlList = urlList;
    }
}

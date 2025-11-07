package com.example.randomGallery.entity.QO;

import com.example.randomGallery.entity.common.PageQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PicQry  extends PageQuery implements Serializable {

    /**
     * 分组ID
     */
    private Long groupId;
}

package mybatis.plus.wrap.casade.module.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.List;

@TableName("base_convert")
@Data
public class BaseConvert {

    private boolean isList;
    private boolean isPrimitive;
    private String targetType;

    private String targetPropertyName;

    private String sourcePropertyPath;

    private String format;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long parentId;

    private String labelName;

    private String remark;

    @TableField(exist = false)
    private List<BaseConvert> children;

    @TableField(exist = false)
    private List<BaseConvert> parent;
}

package mybatis.plus.wrap.casade.module.mapper;

import mybatis.plus.wrap.MyBaseMapper;
import mybatis.plus.wrap.casade.module.domain.BaseConvert;
import org.apache.ibatis.annotations.*;

@Mapper
public interface BaseConvertMapper extends MyBaseMapper<BaseConvert> {

    @Select(value = "select * from base_convert where id=#{id}")
    @Results({
          @Result(property="children",many = @Many(select = "com.ruoyi.framework.base.mapper.BaseConvertMapper.findByMap"),column = "{parentId=id}")
    })
    BaseConvert getDetailById(@Param("id") Long id);


   /* @Select(value = "select * from base_convert where parent_id=#{parentId}")
    List<BaseConvert> findByMap(Map<String,Object> params);*/
}

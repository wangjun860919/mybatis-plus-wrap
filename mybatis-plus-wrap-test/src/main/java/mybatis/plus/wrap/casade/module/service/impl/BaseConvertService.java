package mybatis.plus.wrap.casade.module.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import mybatis.plus.wrap.CascadeOption;
import mybatis.plus.wrap.CascadeType;
import mybatis.plus.wrap.casade.module.domain.BaseConvert;
import mybatis.plus.wrap.casade.module.mapper.BaseConvertMapper;
import mybatis.plus.wrap.casade.module.service.IBaseConvertService;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class BaseConvertService extends ServiceImpl<BaseConvertMapper, BaseConvert> implements IBaseConvertService {

    @Autowired
    SqlSessionTemplate sqlSessionTemplate;

    @Override
    public void testSaveAll(BaseConvert baseConvert) throws Exception{
          CascadeOption.buildNew("children").cascadeExpression("parentId=id")
                  .cascadeType(CascadeType.DELETE_BREAK_JOIN);
         this.baseMapper.saveOrUpdateChild(baseConvert,CascadeOption.getMap());
//        this.baseMapper.updateById(baseConvert);
    }

    @Override
    public void testSaveAllComparator(BaseConvert convert) throws Exception{
        CascadeOption.buildNew("children").cascadeExpression("parentId=id")
                .cascadeType(CascadeType.DELETE_BREAK_JOIN).<BaseConvert>comparator(
                        (o1,o2)-> {
                            return (o1.getTargetPropertyName()==null || o2.getTargetPropertyName()==null)?false:
                                    StringUtils.equals(o1.getTargetPropertyName(),o2.getTargetPropertyName());
                        });
        this.baseMapper.saveOrUpdateChild(convert,CascadeOption.getMap());
    }

    @Override
    public void testClearSaveAll(BaseConvert convert) throws Exception{
        CascadeOption.buildNew("children").cascadeExpression("parentId=id")
                .cascadeType(CascadeType.CLEAR_EXISTS_BEFORE_SAVE);
        this.baseMapper.saveOrUpdateChild(convert,CascadeOption.getMap());
    }


    @Override
    public BaseConvert getDetailById(Long id) throws Exception {
        CascadeOption.buildNew("children").cascadeExpression("parentId=id").build("parent").cascadeExpression("id=id");
        return baseMapper.loadCascadeById(id,CascadeOption.getMap());
    }
}

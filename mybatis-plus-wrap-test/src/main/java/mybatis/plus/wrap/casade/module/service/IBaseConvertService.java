package mybatis.plus.wrap.casade.module.service;


import com.baomidou.mybatisplus.extension.service.IService;
import mybatis.plus.wrap.casade.module.domain.BaseConvert;

public interface IBaseConvertService extends IService<BaseConvert> {

     void testSaveAll(BaseConvert baseConvert) throws Exception;

    void testSaveAllComparator(BaseConvert convert) throws Exception;

     void testClearSaveAll(BaseConvert convert) throws Exception;

    BaseConvert getDetailById(Long id) throws Exception;

}

import mybatis.plus.wrap.SqlSessionTemplateHoler;
import mybatis.plus.wrap.casade.Application;
import mybatis.plus.wrap.casade.module.domain.BaseConvert;
import mybatis.plus.wrap.casade.module.service.IBaseConvertService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes= Application.class,webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class MangaServiceIntegrationTest {

    @Autowired
    IBaseConvertService baseConvertService;

//    @Test
//    @Rollback(false)
    public void post() throws Exception {
        BaseConvert convert=new BaseConvert();
        convert.setLabelName("测试BB");
        convert.setId(64l);

        BaseConvert convertA=new BaseConvert();
        convertA.setLabelName("测试BB1");
        convertA.setFormat("1111");
        convertA.setId(95L);
        convertA.setTargetType("string");
        convertA.setPrimitive(true);
        BaseConvert convertB=new BaseConvert();
        convertB.setFormat("222FF");
        convertB.setId(75L);
        convertB.setLabelName("测试KB2");
        convertB.setTargetType("string");
        convertB.setPrimitive(true);

        BaseConvert convertC=new BaseConvert();
        convertC.setFormat("333");
        convertC.setId(96L);
        convertC.setLabelName("测试KB3");
        convertC.setTargetType("string");
        convertC.setPrimitive(true);

        List<BaseConvert> convertList=new ArrayList<>();
        convertList.add(convertA);
        convertList.add(convertB);
        convertList.add(convertC);
        convert.setChildren(convertList);
        baseConvertService.testSaveAll(convert);

    }


    @Test
    @Rollback(false)
    public void post2() throws Exception {
        BaseConvert convert=new BaseConvert();
        convert.setLabelName("CC");
        convert.setId(110L);

        BaseConvert convertA=new BaseConvert();
        convertA.setLabelName("CC1");
        convertA.setFormat("F1");
        convertA.setTargetType("string");
        convertA.setTargetPropertyName("propertyNameK");
        convertA.setPrimitive(true);

        BaseConvert convertB=new BaseConvert();
        convertB.setFormat("F2");
        convertB.setTargetPropertyName("propertyNameB");
        convertB.setLabelName("CC2");
        convertB.setTargetType("string");
        convertB.setPrimitive(true);

        BaseConvert convertC=new BaseConvert();
        convertC.setFormat("F2");
        convertC.setTargetPropertyName("propertyName3");
        convertC.setLabelName("CC2");
        convertC.setId(116L);
        convertC.setTargetType("string");
        convertC.setPrimitive(true);


        List<BaseConvert> convertList=new ArrayList<>();
        convertList.add(convertA);
        convertList.add(convertB);
        convertList.add(convertC);
        convert.setChildren(convertList);
        baseConvertService.testSaveAllComparator(convert);
    }

    @Test
    @Rollback(false)
    public void post3() throws Exception {
        BaseConvert baseConvert= baseConvertService.getDetailById(110L);
        System.out.println(baseConvert.getChildren().size());
    }


}

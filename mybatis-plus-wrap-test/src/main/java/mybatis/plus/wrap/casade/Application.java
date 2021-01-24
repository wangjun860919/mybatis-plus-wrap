package mybatis.plus.wrap.casade;

import mybatis.plus.wrap.SqlSessionTemplateHoler;
import mybatis.plus.wrap.plugin.ParamConvertMapBasePlugin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication()
@EnableTransactionManagement
@Import({SqlSessionTemplateHoler.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

package mybatis.plus.wrap;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class SqlSessionTemplateHoler implements ApplicationListener<ContextRefreshedEvent> {

    public static SqlSessionTemplate sqlSessionTemplate;

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(event.getApplicationContext().getParent() == null){
            ApplicationContext applicationContext=event.getApplicationContext();
            sqlSessionTemplate=applicationContext.getBean(SqlSessionTemplate.class);
        }
    }
}

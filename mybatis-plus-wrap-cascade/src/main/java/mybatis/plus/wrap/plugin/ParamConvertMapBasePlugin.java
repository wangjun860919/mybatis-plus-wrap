package mybatis.plus.wrap.plugin;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionManager;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;

@Intercepts({
        @Signature(type = Executor.class,method = "query",args = {MappedStatement.class,Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type =Executor.class,method = "query",args = {MappedStatement.class,Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type =ParameterHandler.class,method = "setParameters",args = {PreparedStatement.class}),
        @Signature(type =Executor.class,method = "update",args = {MappedStatement.class,Object.class}),
})
@Component
public class ParamConvertMapBasePlugin implements Interceptor {

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target,this);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args=invocation.getArgs();
        Method mapperMethod=invocation.getMethod();
        MappedStatement mappedStatement=null;
//        if(args[0]!=null && MappedStatement.class.isInstance(args[0])){
//            mappedStatement=(MappedStatement)args[0];
//        }
//        Object parameters=null;
//        if(args[1]!=null){
//            parameters=args[1];
//        }
        return  invocation.proceed();
    }
}

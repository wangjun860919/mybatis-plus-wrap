package mybatis.plus.wrap;


import com.alibaba.druid.support.spring.stat.SpringStatUtils;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import mybatis.plus.wrap.reflect.ReflectUtils;
import org.apache.ibatis.annotations.Case;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.MyBatisExceptionTranslator;
import org.mybatis.spring.SqlSessionHolder;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface  MyBaseMapper<T> extends BaseMapper<T> {


    default List<T> findByMap(final Map<String, Object> map){
        Class <?> cls = currentModelClass();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(cls);
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        return executeBatch(sqlSession -> {
            List datas=sqlSession.selectList(sqlStatement(SqlMethod.SELECT_BY_MAP),new HashMap(){{
                this.put("cm",map);
            }});
            return datas;
        });
    }


    default  T loadCascadeById(Object idValue,Map<String, CascadeOption> cascadeOptionMap)throws Exception{
        Class <?> cls = currentModelClass();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(cls);
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        Exception[] exception={null};
        Object robj= executeBatchAssignModel(sqlSession -> {
            Object mobj=sqlSession.selectOne(sqlStatement(SqlMethod.SELECT_BY_ID, cls),idValue);
            Assert.notNull(mobj,"error: can not get instance by id:"+String.valueOf(idValue));
            if(exception[0]==null){
                cascadeOptionMap.entrySet().forEach(s->{
                    CascadeOption cascadeOption=s.getValue();
                    try {
                        final MetaObject ometa=SystemMetaObject.forObject(mobj);
                        Class<?> currentClass = propertyParameterizedType(s.getKey());
                        TableInfo ssTableInfo = TableInfoHelper.getTableInfo(currentClass);
                        cascadeOption.childType(currentClass);
                        final Map<String,Object> findMap=CascadeOption.getJoinFindMap(ometa,cascadeOption);
                        List datas = sqlSession.selectList(sqlStatement(SqlMethod.SELECT_BY_MAP, currentClass), new HashMap() {{
                            this.put("cm", findMap);
                        }});
                        Class<?> fieldClass=ReflectUtils.getAccessibleField(mobj,s.getKey()).getType();
                        if(datas!=null && datas.size()>0){
                            if(List.class.isAssignableFrom(fieldClass)){
                                ReflectUtils.setFieldValue(mobj,s.getKey(),datas);
                            }else{
                                ReflectUtils.setFieldValue(mobj,s.getKey(),datas.get(0));
                            }
                        }
                    } catch (Exception e) {
                        exception[0]=e;
                        return;
                    }
                });
            }
            return mobj;
        },cls);
        if(exception[0]!=null){
            throw exception[0];
        }
        return (T)robj;
    }

    default T saveOrUpdateChild(T t, Map<String, CascadeOption> cascadeOptionMap) throws Exception{
        Class <?> cls = currentModelClass();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(cls);
        final MetaObject ometa=SystemMetaObject.forObject(t);
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        Exception[] exception={null};
        boolean su=saveOrUpdate(t);
        Object mainKeyValue=ReflectUtils.getFieldValue(t,tableInfo.getKeyProperty());
        Assert.isTrue(su,"error: can not save main-bean");
        cascadeOptionMap.entrySet().forEach(s->{
            Assert.isTrue(ometa.hasGetter(s.getKey()),String.format("error: class:%s is not contain the property:%s",cls.getName(),s.getKey()));
            if(exception[0]==null){
                try {
                    Class<?> currentClass = propertyParameterizedType(s.getKey());
                    TableInfo ssTableInfo = TableInfoHelper.getTableInfo(currentClass);
                    s.getValue().childType(currentClass);

                    final Object newObj=ometa.getValue(s.getKey());
                    final Map<String,Object> findMap=CascadeOption.getJoinFindMap(ometa,s.getValue());

                    //先删除后插入模式
                    if (s.getValue().getCascadeType().equals(CascadeType.CLEAR_EXISTS_BEFORE_SAVE)) {
                        executeBatchAssignModel(sqlSession -> {
                            sqlSession.delete(sqlStatement(SqlMethod.DELETE_BY_MAP, currentClass), new HashMap() {{
                                this.put("cm", findMap);
                            }});
                            if (newObj != null && List.class.isAssignableFrom(newObj.getClass())) {
                                ((List) newObj).forEach(ns -> {
                                         CascadeOption.setJoin(t,ns,s.getValue());
                                         sqlSession.insert(sqlStatement(SqlMethod.INSERT_ONE, currentClass), ns);
                                });
                            } else {
                                CascadeOption.setJoin(t,newObj,s.getValue());
                                sqlSession.insert(sqlStatement(SqlMethod.INSERT_ONE, currentClass), newObj);
                            }
                            return newObj;
                        }, ometa.getGetterType(s.getKey()));
                    } else { //比较更新模式
                        List<Object> source = executeBatchAssignModel(sqlSession -> {
                            List datas = sqlSession.selectList(sqlStatement(SqlMethod.SELECT_BY_MAP, currentClass), new HashMap() {{
                                this.put("cm", findMap);
                            }});
                            return datas;
                        }, currentClass);
                        Map<String,Object> sourceMap=new HashMap<>();
                        if(Collection.class.isAssignableFrom(cls.getDeclaredField(s.getKey()).getType())){
                            sourceMap.put("storage",source);
                            sourceMap.put("new",ometa.getValue(s.getKey()));
                        }else{
                            sourceMap.put("storage",source.size()>0?source.get(0):null);
                            sourceMap.put("new",ometa.getValue(s.getKey()));
                        }
                        //提取比较结果
//                        Map<String,Object> relationshipMap=findRelationshipMap.apply(sourceMap,s.getValue());
                        Relationship relationship=Relationship.create(ReflectUtils.getFieldValue(t,s.getKey()),source,s.getValue());
                        Object existsNewChildren=relationship.getExistsNewChildren(); //新增对象已在数据库存在的部分
                        Object notExistsNewChildren=relationship.getNotExistsNewChildren(); //新增对象在数据库中不存在的部分
                        Object notExistsStorageChildren=relationship.getNotExistsStorageChildren(); //数据库中已存在，并且需要删除或解除关联关系的部分

                        //合并处理需要保存和更新的对象
                        List<Object> saveOrUpdateList=new ArrayList<>();
                        if(Objects.nonNull(existsNewChildren)){
                            if(Collection.class.isInstance(existsNewChildren)){
                                saveOrUpdateList.addAll((Collection)existsNewChildren);
                            }else{
                                saveOrUpdateList.add(existsNewChildren);
                            }
                        }

                        if(Objects.nonNull(notExistsNewChildren)){
                            if(Collection.class.isInstance(notExistsNewChildren)){
                                saveOrUpdateList.addAll((Collection)notExistsNewChildren);
                            }else{
                                saveOrUpdateList.add(notExistsNewChildren);
                            }
                        }

                        //处理解除管理关系的部分
                        if(Objects.nonNull(notExistsStorageChildren)){
                            if(Collection.class.isInstance(notExistsNewChildren)){
                                ((Collection)notExistsStorageChildren).forEach(ss->{
                                     if(s.getValue().getCascadeType().equals(CascadeType.DELETE_BREAK_JOIN)){
                                         Object keyValue=ReflectUtils.getFieldValue(ss,ssTableInfo.getKeyProperty());
                                         boolean ch=executeBatch(sqlSession ->{
                                             int line=sqlSession.delete(sqlStatement(SqlMethod.DELETE_BY_ID,currentClass),keyValue);
                                             return line>0?true:false;
                                         });
                                         if(!ch){
                                             throw new RuntimeException(s.getKey()+"属性删除失败！");
                                         }
                                     }else if(s.getValue().getCascadeType().equals(CascadeType.UPDETE_NULL_BREAK_JOIN)){
                                         boolean ch=executeBatch(sqlSession ->{
                                             CascadeOption.setJoinNull(ss,s.getValue());
                                             int line=sqlSession.update(sqlStatement(SqlMethod.UPDATE_BY_ID,currentClass),CascadeOption.setPrefix("et",ss));
                                             return line>0?true:false;
                                         });
                                         if(!ch){
                                             throw new RuntimeException(s.getKey()+"属性更新失败！");
                                         }
                                     }
                                });
                            }else{
                                saveOrUpdateList.add(notExistsNewChildren);
                            }
                        }

                        if(saveOrUpdateList.size()>0){
                                ((List)saveOrUpdateList).forEach(ss->{
                                    CascadeOption.setJoin(t,ss,s.getValue());
                                    boolean isSuccess=saveOrUpdateAssignAction(ss,s.getValue().getSaveActionMethod(),s.getValue().getUpdateActionMethod());
                                    if(!isSuccess){
                                        throw new RuntimeException(s.getKey()+"属性保存失败！");
                                    }
                                });
                        }
                    }
                }catch (Exception e){
                    exception[0]=e;
                }
            }
        });
        if(exception[0]!=null){
             throw exception[0];
        }
        return t;
    }


     default boolean checkDbExists(Object t){
         TableInfo tableInfo = TableInfoHelper.getTableInfo(t.getClass());
         Object keyValue=ReflectUtils.getFieldValue(t,tableInfo.getKeyProperty());
         if(keyValue==null){
             return false;
         }else{
             return executeBatch(sqlSession ->{
                 Object obj=sqlSession.selectOne(sqlStatement(SqlMethod.SELECT_BY_ID,t.getClass()),keyValue);
                 if(obj!=null){
                     return true;
                 }
                 return false;
             });
         }
     }

    default boolean saveOrUpdate(T t){
        return saveOrUpdateAssignAction(t,null,null);
    }

    default boolean saveOrUpdateAssignAction(Object t, String insertAction, String updateAction){
        TableInfo tableInfo = TableInfoHelper.getTableInfo(t.getClass());
        Object keyValue=ReflectUtils.getFieldValue(t,tableInfo.getKeyProperty());
        String insertPrefix=null;
        String updatePrefix[]={null};
        if(insertAction==null){

        }
        if(updateAction==null){
            updatePrefix[0]="et";
        }
        String insertActionStr=insertAction==null?sqlStatement(SqlMethod.INSERT_ONE,t.getClass()):sqlStatementByAction(insertAction,t.getClass());
        String updateActionStr=updateAction==null?sqlStatement(SqlMethod.UPDATE_BY_ID,t.getClass()):sqlStatementByAction(updateAction,t.getClass());
        if(keyValue==null && !checkDbExists(t)){
            boolean check[]={false};
            final String crefix=insertPrefix;
            executeBatch(sqlSession -> {
                int line=sqlSession.insert(insertActionStr,CascadeOption.setPrefix(crefix,t));
                check[0]=line>0?true:false;
                return check[0];
            });
            return check[0];
        }else{
            boolean check[]={false};
            executeBatch(sqlSession ->{
                Object obj=sqlSession.selectOne(sqlStatement(SqlMethod.SELECT_BY_ID,t.getClass()),keyValue);
                if(obj!=null){
                    final String crefix=updatePrefix[0];
                    int line=sqlSession.update(updateActionStr,CascadeOption.setPrefix(crefix,t));
                    check[0]=line>0?true:false;
                }else{
                    final String crefix=insertPrefix;
                    int line=sqlSession.insert(insertActionStr,CascadeOption.setPrefix(crefix,t));
                    check[0]=line>0?true:false;
                }
                return check[0];
            });
            return check[0];
        }
    }



    default <R> R executeBatch(Function<SqlSessionTemplate, R> fun) {
        return executeBatchAssignModel(fun,currentModelClass());
    }

    default <R> R executeBatchAssignModel(Function<SqlSessionTemplate, R> fun, Class tClass) {
        R apply=null;
        SqlSessionTemplate sqlSessionTemplate=SqlSessionTemplateHoler.sqlSessionTemplate;
        if (sqlSessionTemplate != null) {
            apply = fun.apply(sqlSessionTemplate);
        }else{
            SqlSessionFactory sqlSessionFactory = GlobalConfigUtils.currentSessionFactory(tClass);
            SqlSessionHolder sqlSessionHolder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sqlSessionFactory);
            SqlSession sqlSession =sqlSessionFactory.openSession(true);
            apply = fun.apply(sqlSessionTemplate);
        }

        return apply;
    }



    default String sqlStatement(SqlMethod sqlMethod) {
        return SqlHelper.table(currentModelClass()).getSqlStatement(sqlMethod.getMethod());
    }

    default String sqlStatement(SqlMethod sqlMethod, Class modelClass) {
        return SqlHelper.table(modelClass).getSqlStatement(sqlMethod.getMethod());
    }

    default String sqlStatementByAction(String actionName, Class modelClass) {
        return SqlHelper.table(modelClass).getSqlStatement(actionName);
    }


    /**
     * 建议重写该方法，返回T——数据库表实体类的Class类型
     *
     * @return T.class
     */
    default Class <T> currentModelClass() {
        try {
            String baseMapperClassTypeName = this.getClass().getGenericInterfaces()[0].getTypeName();
            Class <? extends BaseMapper> baseMapperClass = (Class <? extends BaseMapper>) Class.forName(baseMapperClassTypeName);
            ParameterizedType parameterizedType = (ParameterizedType) baseMapperClass.getGenericInterfaces()[0];
//            String entityClassName = parameterizedType.getActualTypeArguments()[0].getTypeName();
            return (Class <T>) parameterizedType.getActualTypeArguments()[0];
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("请重写该方法，返回T的Class类型", e);
        }
//        return (Class <T>) ReflectionKit.getSuperClassGenericType(getClass(), 1);
    }

    default Class<?> propertyParameterizedType(String propertyName) throws Exception{
        Class <?> cls = currentModelClass();
        Type propertyClass=cls.getDeclaredField(propertyName).getGenericType();
        if(ParameterizedType.class.isInstance(propertyClass)){
            ParameterizedType propertyParameterizedType = (ParameterizedType)propertyClass;
            return (Class) propertyParameterizedType.getActualTypeArguments()[0];
        }else{
            return (Class) propertyClass;
        }
    }





}

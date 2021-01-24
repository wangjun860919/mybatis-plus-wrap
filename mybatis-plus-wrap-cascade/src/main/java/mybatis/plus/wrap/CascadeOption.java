package mybatis.plus.wrap;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.Getter;
import mybatis.plus.wrap.reflect.ReflectUtils;
import org.apache.ibatis.reflection.MetaObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Getter
public class CascadeOption{

    //处理模式
    private CascadeType cascadeType=CascadeType.CLEAR_EXISTS_BEFORE_SAVE;
    private String cascadeExpression; //建立关联关系的参数，需要同子类查找的。值为 childProperty1=parentProperty1
    private Class<?> childType; //无须初始化
    BiFunction<?,?,Boolean> comparator;

    private Map<String, TableFieldInfo> tableFieldInfoMap;

    private String updateActionMethod;
    private String saveActionMethod;
    private String deleteActionMethod;
    private String propertyName;

    private static ThreadLocal<Map<String,CascadeOption>> cascadeOptionMapLocal=new ThreadLocal<Map<String,CascadeOption>>(){
        @Override
        protected Map<String, CascadeOption> initialValue() {
            return null;
        }
    };


    private CascadeOption(String propertyName){
          this.propertyName=propertyName;
    }

    public static CascadeOption buildNew(String propertyName){
         CascadeOption cascadeOption=new CascadeOption(propertyName);
         Map<String,CascadeOption> map=new HashMap<>();
         map.put(propertyName,cascadeOption);
         cascadeOptionMapLocal.set(map);
         return cascadeOption;
    }

    public  CascadeOption build(String propertyName){
        Map<String,CascadeOption> map=cascadeOptionMapLocal.get();
        if(map.containsKey(propertyName)){
            return map.get(propertyName);
        }else{
            CascadeOption cascadeOption=new CascadeOption(propertyName);
            cascadeOptionMapLocal.get().put(propertyName,cascadeOption);
            return cascadeOption;
        }
    }

    public static Map<String,CascadeOption> getMap(){
        return cascadeOptionMapLocal.get();
    }


    public CascadeOption updateAction(String updateActionMethod){
        this.updateActionMethod=updateActionMethod;
        return this;
    }

    public CascadeOption saveAction(String saveActionMethod){
        this.saveActionMethod=saveActionMethod;
        return this;
    }

    public CascadeOption deleteAction(String deleteActionMethod){
        this.deleteActionMethod=deleteActionMethod;
        return this;
    }

    public  <K> CascadeOption comparator(BiFunction<K,K,Boolean>  comparator){
        this.comparator=comparator;
        return this;
    }

    public <K> BiFunction<K, K, Boolean> getComparator() {
        return (BiFunction<K, K, Boolean>)comparator;
    }

    public CascadeOption cascadeExpression(String cascadeExpression){
        this.cascadeExpression=cascadeExpression;
        return this;
    }

    public CascadeOption cascadeType(CascadeType cascadeType){
        this.cascadeType=cascadeType;
        return this;
    }

    public CascadeOption childType(Class<?> childType){
        this.childType=childType;
        TableInfo ssTableInfo = TableInfoHelper.getTableInfo(childType);
        Objects.requireNonNull(ssTableInfo,"can not get ssTableInfo by class:"+childType.getName());
        tableFieldInfoMap=ssTableInfo.getFieldList().stream().collect(Collectors.toMap(f->f.getProperty(), f->f));
        return this;
    }





    public static Map<String,Object> getJoinFindMap(MetaObject ometa,CascadeOption cascadeOption){
        String[] es=cascadeOption.getCascadeExpression().split(",");
        Map<String,Object> findMap=new HashMap<>();
        Objects.requireNonNull(cascadeOption.getTableFieldInfoMap(),"can not get TableFieldInfo by className:"+cascadeOption.getChildType().getName());
        for(String e:es){
                 String propertyName=e.split("=",2)[0];
                 TableInfo ssTableInfo = TableInfoHelper.getTableInfo(cascadeOption.getChildType());
                 if(ssTableInfo.getKeyProperty()!=null && ssTableInfo.getKeyProperty().equalsIgnoreCase(propertyName)){
                     findMap.put(ssTableInfo.getKeyProperty(),ometa.getValue(ssTableInfo.getKeyProperty()));//id 在ssTableInfo.getFieldList()获取不到主键信息
                 }else{
                     Objects.requireNonNull(cascadeOption.getTableFieldInfoMap().get(e.split("=",2)[0]),"can not get the propertyName:"+e.split("=",2)[0]+",className is "+cascadeOption.getChildType().getName());
                     String key=cascadeOption.getTableFieldInfoMap().get(e.split("=",2)[0]).getColumn();
                     Object value=ometa.getValue(e.split("=",2)[1]);
                     findMap.put(key,value);
                 }
        }
        return findMap;
    }

    public static void setJoinNull(Object obj,CascadeOption cascadeOption){
        String[] es=cascadeOption.getCascadeExpression().split(",");
        Map<String,Object> findMap=new HashMap<>();
        for(String e:es){
            String key=e.split("=",2)[0];
            ReflectUtils.setFieldValue(obj,key,null);
        }
    }

    public static void setJoin(Object mainObj,Object obj,CascadeOption cascadeOption){
        String[] es=cascadeOption.getCascadeExpression().split(",");
        for(String e:es){
            String keys[]=e.split("=",2);
            Object mainValue=ReflectUtils.getFieldValue(mainObj,keys[1]);
            ReflectUtils.setFieldValue(obj,keys[0],mainValue);
        }
    }

    public static Object setPrefix(final String prefix,final Object obj){
         if(Objects.isNull(prefix) || prefix.trim().length()==0){
               return obj;
         }else{
             return new HashMap<String,Object>(){
                 {
                     this.put(prefix,obj);
                 }
             };
         }
    }

}

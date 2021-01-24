package mybatis.plus.wrap;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.Getter;
import mybatis.plus.wrap.reflect.ReflectUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;

@Getter
public class Relationship {

    private List<Object> existsNewChildren=new ArrayList<>(); //需要新增的对象
    private List<Object> notExistsNewChildren=new ArrayList<>(); //需要更新的对象
    private List<Object> notExistsStorageChildren=new ArrayList<>(); //需要删除的对象

    private Object inputStorageObject;
    private Object inputNewObject;
    private CascadeOption cascadeOption;

    private Relationship(Object inputNewObject,Object inputStorageObject){
         this.inputNewObject=inputNewObject;
         this.inputStorageObject=inputStorageObject;
    }




    public  static Relationship  create(Object inputNewObject,Object inputStorageObject,CascadeOption cascadeOption){
        Relationship relationship=new Relationship(inputNewObject,inputStorageObject);
        relationship.findRelationshipMap.apply(cascadeOption);
        return relationship;
    }



    Function<CascadeOption,Boolean> findRelationshipMap=(cascadeOption)->{
        boolean a= Collection.class.isAssignableFrom(inputStorageObject.getClass());
        boolean b=Collection.class.isAssignableFrom(inputNewObject.getClass());

        Assert.isTrue(a==b,String.format("sourceObject is type:%s,findObject is type:",inputStorageObject.getClass().getName(),inputNewObject.getClass().getName()));
        final TableInfo propertyTableInfo = TableInfoHelper.getTableInfo(cascadeOption.getChildType());

        if(Collection.class.isAssignableFrom(inputStorageObject.getClass())){
            final Object storageObject=inputStorageObject==null?new ArrayList<>():inputStorageObject;
            final Object newObject=inputNewObject==null?new ArrayList<>():inputNewObject;
            if(cascadeOption.getComparator()==null){
                Assert.notNull(propertyTableInfo.getKeyProperty(),"className:"+cascadeOption.getChildType().getName()+",not found:keyProperty");
                Map<Object,Object> storageKeyMap=new HashMap<>();
                ((List)storageObject).forEach(s->{
                    Object key=ReflectUtils.getFieldValue(s,propertyTableInfo.getKeyProperty());
                    if(key!=null){
                        storageKeyMap.put(key,s);
                    }
                });

                Map<Object,Object> newKeyMap=new HashMap<>();
                ((List)newObject).forEach(s->{
                    Object key=ReflectUtils.getFieldValue(s,propertyTableInfo.getKeyProperty());
                    if(key!=null){
                        newKeyMap.put(key,s);
                    }else{
                        notExistsNewChildren.add(s);
                    }
                });

                newKeyMap.entrySet().forEach(s->{
                    if(!storageKeyMap.containsKey(s.getKey())){
                        existsNewChildren.add(s.getValue());
                    }else{
                        notExistsNewChildren.add(s.getValue());
                    }
                });
                storageKeyMap.entrySet().forEach(s->{
                    if(!newKeyMap.containsKey(s.getKey())){
                        notExistsStorageChildren.add(s.getValue());
                    }
                });

            }else{
                ((List)newObject).forEach(s->{
                    boolean check[]={false};
                    Object key[]={null};
                    ((List)storageObject).forEach(ss->{
                        if(cascadeOption.getComparator().apply(s,ss)){
                            check[0]=true;
                            key[0]=ReflectUtils.getFieldValue(ss,propertyTableInfo.getKeyProperty());
                            return;
                        }
                    });
                    if(check[0]){
                        ReflectUtils.setFieldValue(s,propertyTableInfo.getKeyProperty(),key[0]);
                        existsNewChildren.add(s);
                    }else{
                        notExistsNewChildren.add(s);
                    }
                });
                ((List)storageObject).forEach(s->{
                    boolean check[]={false};
                    ((List)newObject).forEach(ss->{
                        if(cascadeOption.getComparator().apply(s,ss)){
                            check[0]=true;
                            Object mainKeyValue=ReflectUtils.getFieldValue(s,propertyTableInfo.getKeyProperty());
                            ReflectUtils.setFieldValue(ss,propertyTableInfo.getKeyProperty(),mainKeyValue);
                            return;
                        }
                    });
                    if(!check[0]){
                        notExistsStorageChildren.add(s);
                    }
                });
            }
        }else{
            if(inputStorageObject==null && inputNewObject!=null){
                notExistsNewChildren.add(inputNewObject);
            }else if(inputNewObject==null && inputStorageObject!=null){
                notExistsStorageChildren.add(inputStorageObject);
            }else if(inputNewObject!=null && inputStorageObject!=null){
                if(cascadeOption.getComparator()==null) {
                    Object storageKey = ReflectUtils.getFieldValue(inputStorageObject, propertyTableInfo.getKeyProperty());
                    Object newKey = ReflectUtils.getFieldValue(inputNewObject, propertyTableInfo.getKeyProperty());
                    if (newKey == null && storageKey != null) { //用新对象更新老对象
                        notExistsNewChildren.add(inputNewObject);
                        notExistsStorageChildren.add(inputStorageObject);
                    }else if(newKey != null && storageKey==null){ //用新对象更新老对象
                        notExistsNewChildren.add(inputNewObject);
                        notExistsNewChildren.add(inputStorageObject);
                    }else if((newKey!=null && storageKey!=null) && storageKey.equals(newKey)){//用新对象更新老对象
                        existsNewChildren.add(inputNewObject);
                    }else if((newKey!=null && storageKey!=null) && !storageKey.equals(newKey)){
                        notExistsNewChildren.add(inputNewObject);//对象信息不一致。直接删除老对象，添加新对象
                        notExistsStorageChildren.add(inputStorageObject);
                    }
                }else{
                    if(cascadeOption.getComparator().apply(inputNewObject,inputStorageObject)){
                        notExistsStorageChildren.add(inputStorageObject);
                    }else{
                        notExistsNewChildren.add(inputNewObject);
                        notExistsStorageChildren.add(inputStorageObject);
                    }
                }
            }
        }
        return true;
    };
}

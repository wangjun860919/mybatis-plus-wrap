# mybatis-plus-cascade
基于 mybatis-plus 做了一级联保存的插件，里面里面包含了测试用例。插件还没有写完，希望同网友共同学习

要点：
1.插件所有的执行过程均给予SqlSessionTemplate，因此，插件是支持事务、线程安全的。这个可以验证
2.关于级联的定义全部在 CascadeOption这个类中.使用方式参考测试用例
3.ParamConvertMapBasePlugin 该插件没有具体的作用。只是用来监视参数的

CascadeOption
参数含义：
cascadeType 级联方式：DELETE_BREAK_JOIN 解除关联时删的子类
                     NOT_ACTION_BREAK_JOIN 解除关联时什么也不做
                     UPDETE_NULL_BREAK_JOIN 解除关系时将关联关系置空
                     CLEAR_EXISTS_BEFORE_SAVE 先清理子类再全部保存



cascadeExpression 关联关系的建立：表达式的值:child_property1=parent_property1,child_property2=parent_property2 可以写多个

comparator 比较器，用来比较准备保存children数据和数据库中的children数据是否为同一条数据。

propertyName: 属性名

代码量非常小、比较易懂

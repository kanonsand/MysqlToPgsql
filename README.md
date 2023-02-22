## 读取mysql的sql文件并导入

>目前只支持drop、create、insert语句

>自动转换int和text相关的字段类型，根据auto_increment修改为serial或者bigserial,

>charset相关内容自动去除

>不支持indexing,会自动从create语句删除

>jsqlparser对mysql的部分语法，比如using、lock、indexing支持不佳，改为druid的mysqlparser
> 进行解析。